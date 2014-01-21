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
import chibipaint.util.CPPixelCoords;
import chibipaint.util.CPRect;
import gnu.trove.stack.array.TIntArrayStack;
import sun.awt.SunHints;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class CPSelection extends CPGreyBmp
{
private final int PIXEL_IN_SELECTION_THRESHOLD = 32;     // Pixels with value > this treated as once that belong to selection visually
final float DASH_ZOOM_INDEPENDENT_LENGTH = 8.2f; // Dash final length, independent from current zoom
private byte[] markerArray;
private float initialDashPiece = 0.0f;
private boolean initialDashDrawn = false;
private ArrayList<PixelSingleLine> CurPixelLines;
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
  neededForDrawing = true;
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

private boolean isActive (CPPixelCoords px)
{
  return isInside (px.x, px.y) && getIsActiveInBounds (px.x, px.y);
}

private int isMarked (CPPixelCoords px)
{
  if (isInside (px.x, px.y))
    return (markerArray[px.y * width + px.x]);
  else
    return 15;
}

private static void drawLine (Graphics2D g2d, CPCanvas canvas, float x1, float y1, float x2, float y2)
{
  Point2D.Float p1 = canvas.coordToDisplay (new Point2D.Float (x1, y1));
  Point2D.Float p2 = canvas.coordToDisplay (new Point2D.Float (x2, y2));
  g2d.draw (new Line2D.Float (p1, p2));
}

private final int[] OppositeDirection = {2, 3, 0, 1};
private final int[] PowersOf2 = {1, 2, 4, 8};

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


private class Lump extends ArrayList<CPPixelCoords>
{
}

private class PixelSingleLine extends ArrayList<CPPixelCoords>
{
  boolean backwards = false;
}

private static final int directionsNum = 4;

enum Directions
{
  Right,
  Down,
  Left,
  Top,
}


int GetNextDirectionNum (CPPixelCoords currentPoint, CPPixelCoords prevPoint)
{
  int result = -1;
  for (int i = 0; i < directionsNum; i++)
    if (currentPoint.x + CPPixelCoords.Corners[i * 2] == prevPoint.x &&
            currentPoint.y + CPPixelCoords.Corners[i * 2 + 1] == prevPoint.y)
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

void createSingleLinesFromPoint (ArrayList<PixelSingleLine> pixelLinesTarget, int x, int y)
{
  CPPixelCoords startingPoint = new CPPixelCoords (x, y);
  int fillMode = isActive (startingPoint) ? 1 : 0;
  CPPixelCoords currentPoint = new CPPixelCoords (startingPoint);
  PixelSingleLine sL = new PixelSingleLine ();
  sL.add (startingPoint.right ().down ());
  boolean Finished = false;
  do
    {
      int scanningDirection = GetNextDirectionNum (currentPoint, sL.get (sL.size () - 1));
      if (scanningDirection == -1)
        break;
      for (int i = 1; i < directionsNum; i++)
        {
          scanningDirection++;
          if (scanningDirection == 4)
            scanningDirection = 0;

          if (isActive (currentPoint.MoveByMv (scanningDirection)) == (fillMode == 0))
            {
              if (currentPoint.MoveToCorner (scanningDirection).compareTo (sL.get (0)) != 0)
                sL.add (currentPoint.MoveToCorner (scanningDirection));
              else
                {
                  Finished = true;
                  break;
                }
            }
          else
            {
              CPPixelCoords nextPoint = currentPoint.MoveByMv (scanningDirection);
              int l = GetNextDirectionNum (nextPoint, sL.get (sL.size () - 1)) + 1;
              if (l == 4)
                l = 0;
              CPPixelCoords pointAfterNext = nextPoint.MoveByMv (l);
              if (isActive (pointAfterNext) == (fillMode == 0))
                currentPoint = nextPoint;
              else
                currentPoint = pointAfterNext;
              break;
            }
        }
      if (Finished)
        break;
    }
  while (true);

  if (fillMode == 0)
    Collections.reverse (sL);
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

void convertLumpToPixelSingleLines (ArrayList<PixelSingleLine> pixelLinesTarget, Lump x)
{
  // TODO: Rewrite using scanline technique
  int fillMode = 1;
  CPPixelCoords startingPoint = Collections.min (x);
  PixelSingleLine sL = new PixelSingleLine ();
  sL.add (startingPoint.right ().down ());
  do
    {
      CPPixelCoords currentPoint = new CPPixelCoords (startingPoint);
      boolean Finished = false;
      do
        {
          int scanningDirection = GetNextDirectionNum (currentPoint, sL.get (sL.size () - 1));
          if (scanningDirection == -1)
            break;
          for (int i = 1; i < directionsNum; i++)
            {
              scanningDirection++;
              if (scanningDirection == 4)
                scanningDirection = 0;

              if (isActive (currentPoint.MoveByMv (scanningDirection)) == (fillMode == 0))
                {
                  if (fillMode == 1)
                    markerArray[currentPoint.y * width + currentPoint.x] |= PowersOf2[scanningDirection];
                  else
                    markerArray[(currentPoint.y + CPPixelCoords.Mv[scanningDirection * 2 + 1]) * width + currentPoint.x + CPPixelCoords.Mv[scanningDirection * 2]] |= (PowersOf2[OppositeDirection[scanningDirection]]);
                  if (currentPoint.MoveToCorner (scanningDirection).compareTo (sL.get (0)) != 0)
                    sL.add (currentPoint.MoveToCorner (scanningDirection));
                  else
                    {
                      Finished = true;
                      break;
                    }
                }
              else
                {
                  CPPixelCoords nextPoint = currentPoint.MoveByMv (scanningDirection);
                  int l = GetNextDirectionNum (nextPoint, sL.get (sL.size () - 1)) + 1;
                  if (l == 4)
                    l = 0;
                  CPPixelCoords pointAfterNext = nextPoint.MoveByMv (l);
                  if (isActive (pointAfterNext) == (fillMode == 0))
                    currentPoint = nextPoint;
                  else
                    currentPoint = pointAfterNext;
                  break;
                }
            }
          if (Finished)
            break;

        }
      while (true);
      if (fillMode == 0)
        Collections.reverse (sL);
      pixelLinesTarget.add (sL);
      fillMode = 0;
      boolean holeFound = false;
      // Checking for inner holes
      for (int i = 0; i < x.size (); i++)
        {
          for (int j = 0; j < directionsNum; j++)
            {
              CPPixelCoords px = x.get (i).MoveByMv (j);
              if (!isActive (px) && ((isMarked (x.get (i)) & PowersOf2[j]) == 0))
                {
                  startingPoint = px;
                  sL = new PixelSingleLine ();
                  for (int k = 0; k < directionsNum; k++)
                    {
                      if (isActive (startingPoint.MoveByMv (k)))
                        {
                          int l = k + 3;
                          if (l >= 4)
                            l -= 4;
                          sL.add (startingPoint.MoveToCorner (l));
                          break;
                        }
                    }

                  holeFound = true;
                  break;
                }
            }
          if (holeFound)
            break;
        }
      if (!holeFound)
        break;
    }
  while (true);
}

public boolean isEmpty ()
{
  return (minX >= maxX || minY >= maxY);
}

public void drawItself (Graphics2D g2d, CPCanvas canvas)
{
  if (minX > maxX || minY > maxY || !neededForDrawing)
    return;

  Color prevColor = g2d.getColor ();
  float dashLength = DASH_ZOOM_INDEPENDENT_LENGTH / canvas.getZoom ();

  for (int i = 0; i < CurPixelLines.size (); i++)
    {

      boolean dashDrawn = initialDashDrawn;
      float dashCurLength = initialDashPiece * dashLength;
      for (int j = 0; j < CurPixelLines.get (i).size (); j++)
        {
          PixelSingleLine CurPSL = CurPixelLines.get (i);
          CPPixelCoords FirstPoint = CurPSL.get (j);
          CPPixelCoords SecondPoint = CurPSL.get ((j + 1) % CurPixelLines.get (i).size ());
          if (dashLength - dashCurLength > 1.0f)
            {
              if (dashDrawn)
                g2d.setColor (Color.black);
              else
                g2d.setColor (Color.white);

              drawLine (g2d, canvas, FirstPoint.x, FirstPoint.y, SecondPoint.x, SecondPoint.y);
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

                  drawLine (g2d, canvas, FirstPoint.x + (SecondPoint.x - FirstPoint.x) * (segmentStart), FirstPoint.y + (SecondPoint.y - FirstPoint.y) * (segmentStart),
                            FirstPoint.x + (SecondPoint.x - FirstPoint.x) * (segmentEnd),
                            FirstPoint.y + (SecondPoint.y - FirstPoint.y) * (segmentEnd));


                  if (leaveCycle)
                    break;
                  segmentStart = segmentEnd;
                  dashDrawn = !dashDrawn;
                  dashCurLength = 0.0f;
                }
            }
        }
    }

  g2d.setColor (prevColor);
  // Preparing
  // And then the MAGICS!
  // g2d.setStroke(stroke);
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
  /*
  // First step: we're dividing everything on separate 4-connected regions
  ArrayList<Lump> lumps = new ArrayList<Lump> ();
  Arrays.fill (markerArray, (byte) 0);
  for (int j = rect.top; j < rect.bottom; j++)
    {
      int off = j * width + rect.left;
      for (int i = rect.left; i < rect.right; i++, off++)
        {
          if (isInside (i, j) && getIsActiveInBounds (off) && markerArray[off] == 0) // If something active and not marked found then we're making Depth-first search
            {
              lumps.add (MakeLumpByScanLines (i, j));
            }
        }
    }

  Arrays.fill (markerArray, (byte) 0);
  // Now we've got our pixels lumped according to 4-connections, now let's build their boundary
  // They will be converted to pixel single lines class which is basically - lines to be drawn.
  CurPixelLines = new ArrayList<PixelSingleLine> ();
  for (int i = 0; i < lumps.size (); i++)
    {
      convertLumpToPixelSingleLines (CurPixelLines, lumps.get (i));
    }
    */
  // Trying completely new algorithm:
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
  CurPixelLines = new ArrayList<PixelSingleLine> ();
  for (int j = height - 1; j >= 0; j--)
    {
      off = j * width;
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
    precalculateForDrawing (rect);
}

private Lump MakeLumpByScanLines (int xArg, int yArg)
{
  TIntArrayStack S = new TIntArrayStack ();
  S.push (xArg);
  S.push (yArg);
  Lump ResultingLump = new Lump ();
  while (S.size () != 0)
    {
      int y = S.pop ();
      int x = S.pop ();
      int offset = y * width;
      // Skipping all the stuff we should add into our lump from the left
      while (x >= 0 && getIsActiveInBounds (offset + x))
        x--;
      x++; // Now we find the left side of this part
      // careful: px.x is used as an iterator and px.y is constant
      boolean spanTop = false;
      boolean spanBottom = false;

      offset += x;
      int offsetMinus1 = offset - width;
      int offsetPlus1 = offset + width;
      while (x < width && getIsActiveInBounds (offset) && markerArray[offset] == 0)
        {
          ResultingLump.add (new CPPixelCoords (x, y));
          markerArray[offset] = 1;
          if (!spanTop && y > 0 && getIsActiveInBounds (offsetMinus1) && markerArray[offsetMinus1] == 0)
            {
              S.push (x);
              S.push (y - 1);
              spanTop = true;
            }
          else if (spanTop && y > 0 && (!getIsActiveInBounds (offsetMinus1) || markerArray[offsetMinus1] == 1))
            {
              spanTop = false;
            }
          if (!spanBottom && y < height - 1 && getIsActiveInBounds (offsetPlus1) && markerArray[offsetPlus1] == 0)
            {
              S.push (x);
              S.push (y + 1);
              spanBottom = true;
            }
          else if (spanBottom && y < height - 1 && (!getIsActiveInBounds (offsetPlus1) || markerArray[offsetPlus1] == 1))
            {
              spanBottom = false;
            }
          x++;
          offset++;
          offsetMinus1++;
          offsetPlus1++;
        }
    }
  return ResultingLump;
}
}
