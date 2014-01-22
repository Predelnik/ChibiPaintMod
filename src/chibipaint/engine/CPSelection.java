/*
 * ChibiPaintMod
 *     Copyright (c) 2012-2014 Sergey Semushin
 *     Copyright (c) 2006-2008 Marc Schefer
 *
 *     This file is part of ChibiPaintMod (previously ChibiPaint).
 *
 *     ChibiPaintMod is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     ChibiPaintMod is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with ChibiPaintMod. If not, see <http://www.gnu.org/licenses/>.
 */

package chibipaint.engine;

import chibipaint.gui.CPCanvas;
import chibipaint.util.CPRect;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.stack.array.TIntArrayStack;
import sun.awt.SunHints;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.Arrays;

public class CPSelection extends CPGreyBmp
{
private final int PIXEL_IN_SELECTION_THRESHOLD = 32;     // Pixels with value > this treated as once that belong to selection visually
final float DASH_ZOOM_INDEPENDENT_LENGTH = 8.2f; // Dash final length, independent from current zoom
private byte[] markerArray;
private float initialDashPiece = 0.0f;
private boolean initialDashDrawn = false;
private ArrayList<TIntArrayList> CurPixelLines;
private int minX = 1;
private int minY = 1;
private int maxX = 0;
private int maxY = 0;
private boolean neededForDrawing; // Turn need for drawing off before CPU consuming operation and after it turn it on if result of this operation will never be used for drawing (huge optimization)

public CPSelection (int width, int height)
{
  super (width, height);
  markerArray = new byte[(width + 1) * (height + 1)];
  Arrays.fill (data, (byte) 0);
  neededForDrawing = false;
}

public void RaiseInitialDash ()
{
  initialDashPiece += 0.1;
  if (initialDashPiece >= 1.0)
    {
      initialDashDrawn = !initialDashDrawn;
      initialDashPiece -= 1.0;
    }
}

public CPSelection (CPSelection original)
{
  super (original);
  System.arraycopy (original.markerArray, 0, markerArray, 0, width * height);
  neededForDrawing = true;
}

public void AddToSelection (CPSelection otherSelection)
{
  CPRect rect = getBoundingRect ();
  rect.union (otherSelection.getBoundingRect ());
  for (int i = 0; i < height * width; i++)
    {
      if ((data[i] & 0xff) < (otherSelection.data[i] & 0xff))
        data[i] = otherSelection.data[i];
    }
  precalculateSelection (rect);
}

public void copyFromSelection (CPSelection otherSelection)
{
  CPRect rect = getBoundingRect ();
  rect.union (otherSelection.getBoundingRect ());
  System.arraycopy (otherSelection.data, 0, data, 0, width * height);
  precalculateSelection (rect);
}


public void SubtractFromSelection (CPSelection otherSelection)
{
  CPRect rect = getBoundingRect ();
  for (int i = 0; i < height * width; i++)
    {
      int difference = data[i] & 0xff - otherSelection.data[i] & 0xff;
      data[i] = difference < 0 ? 0 : (byte) difference;
    }
  precalculateSelection (rect);
}


public void IntersectWithSelection (CPSelection otherSelection)
{
  CPRect rect = getBoundingRect ();
  rect.union (otherSelection.getBoundingRect ());
  rect.clip (otherSelection.getBoundingRect ());
  for (int i = 0; i < height * width; i++)
    {
      data[i] = (byte) Math.min (data[i] & 0xff, otherSelection.data[i] & 0xff);
    }
  precalculateSelection (rect);
}

public void makeRectangularSelection (CPRect rect)
{
  rect.clip (getSize ());
  for (int j = 0; j < height; j++)
    {
      int off = j * width;
      if (j < rect.top || j >= rect.bottom)
        Arrays.fill (data, off, off + width - 1, (byte) 0);
      else
        {
          if (rect.left > 0)
            Arrays.fill (data, off, off + rect.left, (byte) 0);
          Arrays.fill (data, off + rect.left, off + rect.right, (byte) 0xFF);
          if (rect.right < width - 1)
            Arrays.fill (data, off + rect.right, off + width, (byte) 0);
        }
    }
  precalculateSelection (rect);
}

public void make (CPColorBmp src, int offsetX, int offsetY)
{
  makeEmpty ();
  for (int j = 0; j < src.getHeight (); j++)
    {
      if (j + offsetY >= height || j + offsetY < 0)
        continue;
      int offset = (j + offsetY) * width + offsetX;
      int srcOffset = j * src.getWidth ();
      for (int i = 0; i < src.getWidth (); i++, offset++, srcOffset++)
        {
          if (i + offsetX >= width || i + offsetX < 0)
            continue;
          data[offset] = (byte) ((src.getData ()[srcOffset] >> 24) & 0xFF);
        }
    }
  precalculateSelection ();
}

public void makeSelectionFromPolygon (Path2D polygon, AffineTransform canvasTransform)
{
  BufferedImage bImage = new BufferedImage (width, height, BufferedImage.TYPE_BYTE_GRAY);
  Path2D transformedPolygon = (Path2D) polygon.clone ();
  try
    {
      transformedPolygon.transform (canvasTransform.createInverse ());
    }
  catch (NoninvertibleTransformException e)
    {
      e.printStackTrace ();
    }
  Graphics2D g = bImage.createGraphics ();
  g.setColor (Color.WHITE);
  RenderingHints hints = g.getRenderingHints ();
  hints.put (SunHints.KEY_ANTIALIASING, SunHints.VALUE_ANTIALIAS_ON);
  g.addRenderingHints (hints);
  transformedPolygon.setWindingRule (Path2D.WIND_EVEN_ODD);
  g.fill (transformedPolygon);
  data = ((DataBufferByte) bImage.getData ().getDataBuffer ()).getData ();

  precalculateSelection (new CPRect (transformedPolygon.getBounds ()));
}

private boolean getIsActive (int i, int j)
{
  return isInside (i, j) && getIsActiveInBounds (i, j);
}

private boolean getIsNonActive (int i, int j)
{
  return ((data[j * width + i] & 0xff) <= PIXEL_IN_SELECTION_THRESHOLD);
}

private boolean getIsNonActive (int off)
{
  return ((data[off] & 0xff) <= PIXEL_IN_SELECTION_THRESHOLD);
}


private boolean getIsActiveInBounds (int i, int j)
{
  return ((data[j * width + i] & 0xff) > PIXEL_IN_SELECTION_THRESHOLD);
}

private boolean getIsActiveInBounds (int off)
{
  return ((data[off] & 0xff) > PIXEL_IN_SELECTION_THRESHOLD);
}

private boolean isActive (int x, int y)
{
  return isInside (x, y) && getIsActiveInBounds (x, y);
}

private static void drawLine (Graphics2D g2d, CPCanvas canvas, float x1, float y1, float x2, float y2)
{
  Point2D.Float p1 = canvas.coordToDisplay (new Point2D.Float (x1, y1));
  Point2D.Float p2 = canvas.coordToDisplay (new Point2D.Float (x2, y2));
  g2d.draw (new Line2D.Float (p1, p2));
}

public void makeEmpty ()
{
  Arrays.fill (data, (byte) 0);
  minX = 1;
  maxX = 0;
  minY = 1;
  maxY = 0;
}

public void selectAll ()
{
  Arrays.fill (data, (byte) -1);
  precalculateSelection ();
}

public CPSelection copy ()
{
  CPSelection copySelection = new CPSelection (width, height);
  copySelection.data = data.clone ();
  copySelection.precalculateSelection ();
  return copySelection;
}

public void copyFrom (CPSelection selection)
{
  if (selection.width != width || selection.height != height)
    {
      width = selection.width;
      height = selection.height;
      data = new byte[width * height];
      markerArray = new byte[(width + 1) * (height + 1)];
    }
  System.arraycopy (selection.data, 0, data, 0, data.length);

  precalculateSelection (selection.getBoundingRect ());
}

public void applySelectionToData (int dataArg[])
{
  for (int off = 0; off < width * height; off++)
    {
      int selectionValue = getData (off);
      int sourceAlpha = ((dataArg[off] & (0xff000000)) >>> 24);
      dataArg[off] = (dataArg[off] & (0x00ffffff)) | ((sourceAlpha >= (selectionValue & 0xFF) ? selectionValue & 0xFF : sourceAlpha) << 24);
    }
}

public void makeSelectionFromAlpha (int[] dataArg, CPRect rect)
{
  for (int off = 0; off < width * height; off++)
    {
      data[off] = (byte) ((dataArg[off] & (0xff000000)) >>> 24);
    }
  precalculateSelection (rect);
}

public int getData (int i, int j)
{
  return data[j * width + i];
}

public byte getData (int offset)
{
  return data[offset];
}

public int cutOpacity (int value, int i, int j)
{
  if (isEmpty ())
    return value;
  int newValue = value;
  int selection = (data[j * width + i] & 0xFF) * 255;
  if (selection < newValue)
    newValue = selection;
  return newValue;
}

public byte[] getData ()
{
  return data;
}

public void cutByData (CPLayer activeLayerArg)
{
  for (int off = 0; off < width * height; off++)
    {
      byte alpha = (byte) (activeLayerArg.getData ()[off] >> 24);
      if ((data[off] & 0xFF) > (alpha & 0xFF))
        data[off] = alpha;
    }
  precalculateSelection ();
}

public boolean isNeededForDrawing ()
{
  return neededForDrawing;
}

public void setNeededForDrawing (boolean neededForDrawing)
{
  this.neededForDrawing = neededForDrawing;
}

public void invert ()
{
  for (int off = 0; off < width * height; off++)
    getData ()[off] = (byte) (getData ()[off] ^ 0xFF);
}

private static final int directionsNum = 4;

enum Directions
{
  Right,
  Down,
  Left,
  Top,
}


int GetNextDirectionNum (int cX, int cY, int pX, int pY)
{
  int result = -1;
  for (int i = 0; i < directionsNum; i++)
    if (cX + Corners[i * 2] == pX &&
            cY + Corners[i * 2 + 1] == pY)
      {
        result = i;
        break;
      }
  return result;
}

public Rectangle getBoundingBox (CPCanvas canvas)
{
  Point2D.Float[] points = {canvas.coordToDisplay (new Point2D.Float (minX, minY)), canvas.coordToDisplay (new Point2D.Float (minX, maxY + 2.f)),
          canvas.coordToDisplay (new Point2D.Float (maxX + 2.f, maxY + 2.f)), canvas.coordToDisplay (new Point2D.Float (maxX + 2.f, minY))};
  Path2D.Float path = new Path2D.Float ();
  path.moveTo (points[0].x, points[0].y);
  path.lineTo (points[1].x, points[1].y);
  path.lineTo (points[2].x, points[2].y);
  path.lineTo (points[3].x, points[3].y);
  path.closePath ();
  Rectangle2D rect = path.getBounds2D ();
  return new Rectangle ((int) rect.getX (), (int) rect.getY (), (int) Math.ceil (rect.getMaxX ()) - (int) rect.getX (),
                        (int) Math.ceil (rect.getMaxY ()) - (int) rect.getY ());
}

public CPRect getBoundingRect ()
{
  return new CPRect (minX, minY, maxX + 1, maxY + 1);
}

static public final int[] Mv = {1, 0, 0, 1, -1, 0, 0, -1};
static public final int[] Corners = {1, 1, 0, 1, 0, 0, 1, 0};

void createSingleLinesFromPoint (ArrayList<TIntArrayList> pixelLinesTarget, int x, int y)
{
  int sX = x;
  int sY = y;
  boolean fillMode = isActive (sX, sY);
  int cX = x;
  int cY = y;
  TIntArrayList sL = new TIntArrayList ();
  sL.add (sX);
  sL.add (sY + 1);
  int lX = sX;
  int lY = sY + 1;
  boolean Finished = false;
  do
    {
      int scanningDirection = GetNextDirectionNum (cX, cY, lX, lY);
      if (scanningDirection == -1)
        break;
      for (int i = 1; i < directionsNum; i++)
        {
          scanningDirection++;
          if (scanningDirection == 4)
            scanningDirection = 0;

          if (isActive (cX + Mv[scanningDirection * 2], cY + Mv[scanningDirection * 2 + 1]) != fillMode)
            {
              int nX = cX + Corners[scanningDirection * 2];
              int nY = cY + Corners[scanningDirection * 2 + 1];
              if (nX != x || nY != y + 1)
                {
                  sL.add (nX);
                  sL.add (nY);
                  lX = nX;
                  lY = nY;
                }
              else
                {
                  Finished = true;
                  break;
                }
            }
          else
            {
              int nX = cX + Mv[scanningDirection * 2];
              int nY = cY + Mv[scanningDirection * 2 + 1];
              int l = GetNextDirectionNum (nX, nY, lX, lY) + 1;
              if (l == 4)
                l = 0;
              int panX = nX + Mv[l * 2];
              int panY = nY + Mv[l * 2 + 1];
              if (isActive (panX, panY) != fillMode)
                {
                  cX = nX;
                  cY = nY;
                }
              else
                {
                  cX = panX;
                  cY = panY;
                }
              break;
            }
        }
      if (Finished)
        break;
    }
  while (true);

  if (!fillMode)
    {
      for (int i = 0; i < sL.size () / 4; i++)
        {
          for (int j = 0; j < 2; j++)
            {
              int temp = sL.get (2 * i + j);
              sL.set (2 * i + j, sL.get (sL.size () - 2 - 2 * i + j));
              sL.set (sL.size () - 2 * i - 2 + j, temp);
            }
        }
    }
  pixelLinesTarget.add (sL);
}

// set 1 in markerArray array for all the connected pixels with similar activity as target
void markOutWithSimilarActivity (int xArg, int yArg)
{
  TIntArrayStack S = new TIntArrayStack ();
  S.push (xArg);
  S.push (yArg);
  boolean activity = getIsActiveInBounds (xArg, yArg);
  while (S.size () != 0)
    {
      int y = S.pop ();
      int x = S.pop ();
      int offset = y * width;
      // Skipping all the stuff we should add into our lump from the left
      while (x >= 0 && getIsActiveInBounds (offset + x) == activity)
        x--;
      x++; // Now we find the left side of this part
      // careful: px.x is used as an iterator and px.y is constant
      boolean spanTop = false;
      boolean spanBottom = false;

      offset += x;
      int offsetMinus1 = offset - width;
      int offsetPlus1 = offset + width;
      while (x < width && getIsActiveInBounds (offset) == activity && markerArray[offset] == 0)
        {
          markerArray[offset] = 1;
          if (!spanTop && y > 0 && getIsActiveInBounds (offsetMinus1) == activity && markerArray[offsetMinus1] == 0)
            {
              S.push (x);
              S.push (y - 1);
              spanTop = true;
            }
          else if (spanTop && y > 0 && (!(getIsActiveInBounds (offsetMinus1) == activity) || markerArray[offsetMinus1] == 1))
            {
              spanTop = false;
            }
          if (!spanBottom && y < height - 1 && getIsActiveInBounds (offsetPlus1) == activity && markerArray[offsetPlus1] == 0)
            {
              S.push (x);
              S.push (y + 1);
              spanBottom = true;
            }
          else if (spanBottom && y < height - 1 && (!(getIsActiveInBounds (offsetPlus1) == activity) || markerArray[offsetPlus1] == 1))
            {
              spanBottom = false;
            }
          x++;
          offset++;
          offsetMinus1++;
          offsetPlus1++;
        }
    }
}

public boolean isEmpty ()
{
  return (minX >= maxX || minY >= maxY);
}

public void drawItself (Graphics2D g2dArg, CPCanvas canvas)
{
  if (minX > maxX || minY > maxY || !neededForDrawing)
    return;

  Graphics2D g2d = (Graphics2D) g2dArg.create ();
  float dashLength = DASH_ZOOM_INDEPENDENT_LENGTH / canvas.getZoom ();

  for (int i = 0; i < CurPixelLines.size (); i++)
    {

      boolean dashDrawn = initialDashDrawn;
      float dashCurLength = initialDashPiece * dashLength;
      for (int j = 0; j < CurPixelLines.get (i).size () / 2; j++)
        {
          TIntArrayList CurPSL = CurPixelLines.get (i);
          int fpX = CurPSL.get (j * 2);
          int fpY = CurPSL.get (j * 2 + 1);
          int nextIndex = (j + 1);
          if (nextIndex == CurPSL.size () / 2)
            nextIndex = 0;
          int spX = CurPSL.get (nextIndex * 2);
          int spY = CurPSL.get (nextIndex * 2 + 1);
          if (dashLength - dashCurLength > 1.0f)
            {
              if (dashDrawn)
                g2d.setColor (Color.black);
              else
                g2d.setColor (Color.white);

              drawLine (g2d, canvas, fpX, fpY, spX, spY);
              dashCurLength += 1.0f;
            }
          else
            {
              float segmentStart = 0.0f;
              boolean leaveCycle = false;
              while (true)
                {
                  float segmentEnd = segmentStart + (dashLength - dashCurLength);
                  if (segmentEnd > 1.0f)
                    {
                      dashCurLength = 1.0f - segmentStart;
                      segmentEnd = 1.0f;
                      leaveCycle = true;
                    }
                  if (dashDrawn)
                    g2d.setColor (Color.black);
                  else
                    g2d.setColor (Color.white);

                  drawLine (g2d, canvas, fpX + (spX - fpX) * (segmentStart), fpY + (spY - fpY) * (segmentStart),
                            fpX + (spX - fpX) * (segmentEnd),
                            fpY + (spY - fpY) * (segmentEnd));


                  if (leaveCycle)
                    break;
                  segmentStart = segmentEnd;
                  dashDrawn = !dashDrawn;
                  dashCurLength = 0.0f;
                }
            }
        }
    }
}

private void CalculateBoundingBox (CPRect rect)
{
  for (int i = rect.top; i < rect.bottom; i++)
    {
      int off = i * width + rect.left;
      for (int j = rect.left; j < rect.right; j++, off++)
        {
          if (data[off] != 0)
            {
              minX = Math.min (minX, j);
              maxX = Math.max (maxX, j);
              minY = Math.min (minY, i);
              maxY = Math.max (maxY, i);
            }
        }
    }
}

public void precalculateSelection ()
{
  precalculateSelection (getSize ());
}

private void precalculateForDrawing (CPRect rect)
{
  // cutting out inactive lumps connected with border - these are one we do not care about.
  Arrays.fill (markerArray, (byte) 0);
  for (int i = 0; i < width; i++)
    {
      if (markerArray[i] == 0 && getIsNonActive (i))
        {
          markOutWithSimilarActivity (i, 0);
        }
      if (markerArray[(height - 1) * width + i] == 0 && getIsNonActive ((height - 1) * width + i))
        {
          markOutWithSimilarActivity (i, height - 1);
        }
    }

  int off = width;
  for (int i = 1; i < height - 1; i++, off += width)
    {
      if (markerArray[off] == 0 && getIsNonActive (off))
        {
          markOutWithSimilarActivity (0, i);
        }
      if (markerArray[off + width - 1] == 0 && getIsNonActive (off + width - 1))
        {
          markOutWithSimilarActivity (width - 1, i);
        }
    }
  // Now the actual part - we're running through all of the pixels, we're interested only in unmarked
  off = 0;
  CurPixelLines = new ArrayList<TIntArrayList> ();
  for (int j = 0; j < height; j++)
    {
      for (int i = 0; i < width; i++, off++)
        {
          if (markerArray[off] == 0) // we're found interesting pixels.
            {
              createSingleLinesFromPoint (CurPixelLines, i, j); // create single lines from this point
              markOutWithSimilarActivity (i, j);
            }
        }
    }
  // That's all folks!
}


public void precalculateSelection (CPRect rect)
{
  minX = width;
  maxX = 0;
  minY = height;
  maxY = 0;
  rect.clip (getSize ());
  CalculateBoundingBox (rect); // Warning: We count everything non-zero into bounding box, so the function is separate.
  if (neededForDrawing)
    {
      precalculateForDrawing (rect);
    }
}

}
