/*
 * ChibiPaintMod
 *     Copyright (c) 2012-2013 Sergey Semushin
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

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;

public class CPTransformHandler
{

private boolean transformActive;

public CPTransformHandler (int width, int height)
{

}

public void clearTransforms ()
{
  activeAction.cancelTransform ();
}

public boolean isTransformActive ()
{
  return transformActive;
}

public void stopTransform ()
{
  transformActive = false;
}


private enum actionType
{
  NONE,
  MOVE,
  STRETCH_TO_SIDE,
  STRETCH_TO_ANGLE,
  ROTATE,
}


class CPTransformAction
{
  private final AffineTransform pendingTransform = new AffineTransform ();
  private final AffineTransform savedTransformScale = new AffineTransform ();
  private final AffineTransform savedTransformMovement = new AffineTransform ();

  actionType type;
  int sideIndex;
  int angleIndex;

  CPTransformAction ()
  {
    setToNothing ();
  }

  public void updateEditTransform (AffineTransform editTransform, Point2D pointOfOrigin, Point2D currentPoint)
  {
    Point2D currentPointTransformed = new Point2D.Float ();
    Point2D pointOfOriginTransformed = new Point2D.Float ();
    pendingTransform.setToIdentity ();
    try
      {
        AffineTransform inverseTransform = editTransform.createInverse ();
        inverseTransform.transform (currentPoint, currentPointTransformed);
        inverseTransform.transform (pointOfOrigin, pointOfOriginTransformed);
      }
    catch (NoninvertibleTransformException e)
      {
        e.printStackTrace ();  //To change body of catch statement use File | Settings | File Templates.
      }
    switch (type)
      {
      case NONE:
        break;
      case MOVE:
        pendingTransform.translate (currentPointTransformed.getX () - pointOfOriginTransformed.getX (),
                                    currentPointTransformed.getY () - pointOfOriginTransformed.getY ());
        break;
      case STRETCH_TO_SIDE:
        Line2D side = getSideByIndex (sideIndex);
        Line2D transformedSide = new Line2D.Float ();
        Point2D point1 = new Point2D.Float ();
        Point2D point2 = new Point2D.Float ();
        editTransform.transform (side.getP1 (), point1);
        editTransform.transform (side.getP2 (), point2);
        transformedSide.setLine (point1, point2);
        switch (sideIndex % 2)
          {
          case 0:
          {
            pendingTransform.setToIdentity ();
            double translateValue = side.getY1 ();
            double scaleCoefficient = (currentPointTransformed.getY () - side.getY1 ()) / (pointOfOriginTransformed.getY () - side.getY1 ());
            pendingTransform.translate (0, translateValue);
            pendingTransform.scale (1.0, scaleCoefficient);
            pendingTransform.translate (0, -translateValue);
            break;
          }
          case 1:
          {
            pendingTransform.setToIdentity ();
            double translateValue = side.getX1 ();
            double scaleCoefficient = (currentPointTransformed.getX () - side.getX1 ()) / (pointOfOriginTransformed.getX () - side.getX1 ());
            pendingTransform.translate (translateValue, 0);
            pendingTransform.scale (scaleCoefficient, 1.0);
            pendingTransform.translate (-translateValue, 0);
            break;
          }
          }
        break;
      case STRETCH_TO_ANGLE:
      {
        Point2D angle = getAngleByIndex (angleIndex);
        pendingTransform.setToIdentity ();
        double translateValueX = angle.getX (), translateValueY = angle.getY ();
        double scaleCoefficientX = (currentPointTransformed.getX () - angle.getX ()) / (pointOfOriginTransformed.getX () - angle.getX ());
        double scaleCoefficientY = (currentPointTransformed.getY () - angle.getY ()) / (pointOfOriginTransformed.getY () - angle.getY ());
        pendingTransform.translate (translateValueX, translateValueY);
        pendingTransform.scale (scaleCoefficientX, scaleCoefficientY);
        pendingTransform.translate (-translateValueX, -translateValueY);
        break;
      }
      case ROTATE:
      {
        Point2D center = getCenter ();
        Point2D transformedCenter = new Point2D.Float ();
        Point2D transformedByScaleOnlyCenter = new Point2D.Float ();
        savedTransformScale.transform (center, transformedByScaleOnlyCenter);
        editTransform.transform (center, transformedCenter);
        pendingTransform.setToIdentity ();

        double ax = (currentPoint.getX () - transformedCenter.getX ());
        double ay = (currentPoint.getY () - transformedCenter.getY ());
        double bx = (pointOfOrigin.getX () - transformedCenter.getX ());
        double by = (pointOfOrigin.getY () - transformedCenter.getY ());
        float angleValue = -(float) Math.atan2 (ax * by - ay * bx, ax * bx + ay * by);
        pendingTransform.rotate (angleValue, transformedByScaleOnlyCenter.getX (), transformedByScaleOnlyCenter.getY ());
        break;
      }
      }

    editTransform.setToIdentity ();
    editTransform.concatenate (savedTransformMovement);
    if (isMovement ())
      editTransform.concatenate (pendingTransform);
    editTransform.concatenate (savedTransformScale);
    if (isScale ())
      editTransform.concatenate (pendingTransform);
  }


  public void preserveTransform (AffineTransform editTransform)
  {
    if (isMovement ())
      savedTransformMovement.concatenate (pendingTransform);
    else if (isScale ())
      savedTransformScale.concatenate (pendingTransform);
    pendingTransform.setToIdentity ();
    editTransform.setToIdentity ();
    editTransform.concatenate (savedTransformMovement);
    editTransform.concatenate (savedTransformScale);
    setToNothing ();
  }

  public void cancelTransform ()
  {
    savedTransformScale.setToIdentity ();
    savedTransformMovement.setToIdentity ();
    pendingTransform.setToIdentity ();
  }

  void setToNothing ()
  {
    type = actionType.NONE;
  }

  void setToMove ()
  {
    type = actionType.MOVE;
  }

  void setToStretchToSide (int sideIndexArg)
  {
    type = actionType.STRETCH_TO_SIDE;
    sideIndex = sideIndexArg;
  }

  void setToStretchToAngle (int angleIndexArg)
  {
    type = actionType.STRETCH_TO_ANGLE;
    angleIndex = angleIndexArg;
  }

  void setToRotate ()
  {
    type = actionType.ROTATE;
  }

  public Cursor cursor ()
  {
    switch (type)
      {
      case NONE:
        return Cursor.getDefaultCursor ();
      case MOVE:
        return Cursor.getPredefinedCursor (Cursor.MOVE_CURSOR);
      case STRETCH_TO_SIDE:
        return getCursorByAngle (getAbsoluteAngleByLine (getFullyTransformedSideByIndex (sideIndex)));
      case STRETCH_TO_ANGLE:
        return getCursorByAngle ((float) (getAbsoluteAngleByLine (getFullyTransformedSideByIndex (angleIndex)) + Math.signum (finalTransform.getScaleX ()) * Math.signum (finalTransform.getScaleY ()) * Math.PI * 0.25))
                ;
      case ROTATE:
        return Cursor.getPredefinedCursor (Cursor.HAND_CURSOR); // For now (placeholder)
      }

    return Cursor.getDefaultCursor ();
  }

  private Cursor getCursorByAngle (float absoluteAngleByLine)
  {
    if (absoluteAngleByLine < 0.0)
      absoluteAngleByLine += 2 * Math.PI;
    if (absoluteAngleByLine > Math.PI)
      absoluteAngleByLine -= Math.PI;
    if (absoluteAngleByLine < Math.PI * 0.125)
      return Cursor.getPredefinedCursor (Cursor.S_RESIZE_CURSOR);
    else if (absoluteAngleByLine < Math.PI * 0.375)
      return Cursor.getPredefinedCursor (Cursor.SE_RESIZE_CURSOR);
    else if (absoluteAngleByLine < Math.PI * 0.625)
      return Cursor.getPredefinedCursor (Cursor.E_RESIZE_CURSOR);
    else if (absoluteAngleByLine < Math.PI * 0.875)
      return Cursor.getPredefinedCursor (Cursor.NE_RESIZE_CURSOR);
    else
      return Cursor.getPredefinedCursor (Cursor.N_RESIZE_CURSOR);
  }

  private float getAbsoluteAngleByLine (Line2D transformedSideByIndex)
  {
    double ax = 1.0;
    double ay = 0.0;
    double bx = transformedSideByIndex.getX2 () - transformedSideByIndex.getX1 ();
    double by = transformedSideByIndex.getY2 () - transformedSideByIndex.getY1 ();
    float angleValue = -(float) Math.atan2 (ax * by - ay * bx, ax * bx + ay * by);
    return angleValue;
  }

  public boolean isMovement ()
  {
    switch (type)
      {
      case ROTATE:
        return true;
      case MOVE:
      case STRETCH_TO_SIDE:
      case STRETCH_TO_ANGLE:
      case NONE:
        return false;
      }
    return false;
  }

  public boolean isScale ()
  {
    return (!isMovement () && type != actionType.NONE);
  }
}

CPColorBmp transformedPart;
CPRect transformingRect = new CPRect ();
CPLayer activeLayer = null;
CPSelection currentSelection = null;
CPTransformAction activeAction = new CPTransformAction ();
final Point2D pointOfOrigin = new Point2D.Float ();
final AffineTransform transform = new AffineTransform ();
final AffineTransform editTransform = new AffineTransform ();
private AffineTransform finalTransform; // Last used final transform ,no guarantee that it was last updated
int shiftX;
int shiftY;
Image transformedPartImage;
int artworkWidth, artworkHeight;
static float controlDistance = 10.0f;


// Initializes transformation mode
public void initialize (CPSelection currentSelectionArg, CPLayer activeLayerArg)
{
  currentSelection = currentSelectionArg;
  CPColorBmp tempBmp = new CPColorBmp (0, 0);
  tempBmp.copyDataFrom (activeLayerArg);
  artworkWidth = activeLayerArg.getWidth ();
  artworkHeight = activeLayerArg.getHeight ();
  tempBmp.cutBySelection (currentSelection);
  transformingRect = tempBmp.getBoundingBox ();
  transformedPart = new CPColorBmp (transformingRect.getWidth (), transformingRect.getHeight ());
  transformedPart.setFromBitmapRect (tempBmp, transformingRect);
  activeAction.setToNothing ();
  shiftX = transformingRect.getLeft ();
  shiftY = transformingRect.getTop ();
  editTransform.setToIdentity ();
  updateCurrentTransform ();
  activeLayerArg.removePartsCutBySelection (currentSelection);
  activeLayer = activeLayerArg; // TODO: Disable any actions with layer while transformation is in process.
  MemoryImageSource imgSource = new MemoryImageSource (transformedPart.getWidth (), transformedPart.getHeight (), transformedPart.getData (), 0, transformedPart.getWidth ());
  transformedPartImage = Toolkit.getDefaultToolkit ().createImage (imgSource);
  transformActive = true;
  currentSelection.makeEmpty ();
}

public void cursorPressed (Point2D p)
{
  // Point2D TransformedP = new Point2D.Float ();
  // transform.transform (p, TransformedP);
  pointOfOrigin.setLocation (p);
  getActionTypeByPosition (activeAction, p);
}

Line2D getSideByIndex (int index)
{
  switch (index % 4)
    {
    case 0:
      return new Line2D.Float (transformingRect.getLeft (), transformingRect.getTop (), transformingRect.getRight (), transformingRect.getTop ());
    case 1:
      return new Line2D.Float (transformingRect.getRight (), transformingRect.getTop (), transformingRect.getRight (), transformingRect.getBottom ());
    case 2:
      return new Line2D.Float (transformingRect.getRight (), transformingRect.getBottom (), transformingRect.getLeft (), transformingRect.getBottom ());
    case 3:
      return new Line2D.Float (transformingRect.getLeft (), transformingRect.getBottom (), transformingRect.getLeft (), transformingRect.getTop ());
    }
  return null;
}


Line2D getTransformedLine (Line2D line, AffineTransform transformArg)
{
  Line2D resultLine = new Line2D.Float ();
  Point2D point1 = new Point2D.Float ();
  Point2D point2 = new Point2D.Float ();
  transformArg.transform (line.getP1 (), point1);
  transformArg.transform (line.getP2 (), point2);
  resultLine.setLine (point1, point2);
  return resultLine;
}

Line2D getTransformedSideByIndex (int index, AffineTransform transformArg)
{
  Line2D line = getSideByIndex (index);
  return getTransformedLine (line, transformArg);
}

Line2D getCanvasTransformedSideByIndex (int index)
{
  return getTransformedSideByIndex (index, transform);
}

Line2D getFullyTransformedSideByIndex (int index)
{
  return getTransformedSideByIndex (index, finalTransform);
}

Line2D getFullyTransformedLine (Line2D line)
{
  return getTransformedLine (line, finalTransform);
}


public Point2D getCenter ()
{
  return new Point2D.Float ((transformingRect.getLeft () + transformingRect.getRight ()) * 0.5f,
                            (transformingRect.getTop () + transformingRect.getBottom ()) * 0.5f);
}

private Point2D getAngleByIndex (int index)
{
  switch (index % 4)
    {
    case 0:
      return new Point2D.Float (transformingRect.getLeft (), transformingRect.getTop ());
    case 1:
      return new Point2D.Float (transformingRect.getRight (), transformingRect.getTop ());
    case 2:
      return new Point2D.Float (transformingRect.getRight (), transformingRect.getBottom ());
    case 3:
      return new Point2D.Float (transformingRect.getLeft (), transformingRect.getBottom ());
    }
  return null;
}

public void getActionTypeByPosition (CPTransformAction action, Point2D p)
{
  Point2D transformedP = new Point2D.Float ();
  Point2D pointOfInterest = new Point2D.Float ();
  Line2D lineOfInterest = new Line2D.Float ();
  try
    {
      transform.createInverse ().transform (p, transformedP);
    }
  catch (NoninvertibleTransformException e)
    {
      e.printStackTrace ();  //To change body of catch statement use File | Settings | File Templates.
    }

  for (int i = 0; i < 4; i++)
    {
      transform.transform (getAngleByIndex (i), pointOfInterest);
      if (pointOfInterest.distance (p) < controlDistance)
        {
          action.setToStretchToAngle ((i + 2) % 4);
          return;
        }
    }

  for (int i = 0; i < 4; i++)
    {
      transform.transform (getAngleByIndex (i), pointOfInterest);
      if (pointOfInterest.distance (p) < 2 * controlDistance && !transformingRect.isInside (transformedP))
        {
          action.setToRotate ();
          return;
        }
    }

  for (int i = 0; i < 4; i++)
    {
      if (getCanvasTransformedSideByIndex (i).ptSegDist (p) < controlDistance)
        {
          action.setToStretchToSide ((i + 2) % 4);
          return;
        }
    }
  if (transformingRect.isInside (transformedP))
    {
      action.setToMove ();
      return;
    }

  return;
}

public void cursorReleased ()
{
  activeAction.preserveTransform (editTransform);
}

public void cursorDragged (Point2D p)
{
  activeAction.updateEditTransform (editTransform, pointOfOrigin, p);
  updateCurrentTransform ();
}

public void cursorMoved (Point2D p, CPCanvas canvas) // TODO: change it to some kind of cursor manager
{
  CPTransformAction tempAction = new CPTransformAction ();
  getActionTypeByPosition (tempAction, p);
  canvas.setCursor (tempAction.cursor ());
}

private void updateCurrentTransform ()
{
  transform.setToIdentity ();
  transform.concatenate (editTransform);
}

final int smallRectPixelSize = 6;

static Path2D.Float transformRectToPath (float left, float top, float right, float bottom, AffineTransform transformArg)
{
  float[] points = {left, top, right, top, right, bottom, left, bottom};
  float[] dstPoints = new float[points.length];
  transformArg.transform (points, 0, dstPoints, 0, points.length / 2);
  Path2D.Float path = new Path2D.Float ();
  path.moveTo (dstPoints[0], dstPoints[1]);
  path.lineTo (dstPoints[2], dstPoints[3]);
  path.lineTo (dstPoints[4], dstPoints[5]);
  path.lineTo (dstPoints[6], dstPoints[7]);
  path.closePath ();
  return path;
}

public void drawPreviewOn (CPLayer layer)
{
  drawItselfOnLayer (layer, RenderingHints.VALUE_INTERPOLATION_BILINEAR, false);
}

public Rectangle getRectNeededForUpdating ()
{
  // First we finding minimal rectangle we could put our transformed picture to.
  Path2D path = transformRectToPath (transformingRect.getLeft (), transformingRect.getTop (), transformingRect.getRight (), transformingRect.getBottom (), transform);
  Rectangle bounds = path.getBounds ();
  // Fuzzing them a little for better interpolation
  bounds.setRect (bounds.getX () - fuzzyValue, bounds.getY () - fuzzyValue, bounds.getWidth () + 2 * fuzzyValue, bounds.getHeight () + 2 * fuzzyValue);
  return bounds;
}

private void drawItselfOnLayer (CPLayer layer, Object interpolation, boolean updateSelection)
{
  Rectangle bounds = getRectNeededForUpdating ();
  BufferedImage bI = new BufferedImage ((int) bounds.getWidth (), (int) bounds.getHeight (), BufferedImage.TYPE_INT_ARGB);
  Graphics gBuffered = bI.createGraphics ();
  Graphics2D g = bI.createGraphics ();

  if (interpolation != null)
    {
      RenderingHints hints = g.getRenderingHints ();
      hints.put (RenderingHints.KEY_INTERPOLATION, interpolation);
      g.addRenderingHints (hints);
    }

  AffineTransform finalTransform = new AffineTransform ();
  finalTransform.translate (-bounds.getX (), -bounds.getY ());
  finalTransform.concatenate (transform);
  g.transform (finalTransform);
  // Image should transform to be put exactly into our prearranged BufferedImage
  // Uber cool render hints because we're rendering it only one time obviously
  g.drawImage (transformedPartImage, shiftX, shiftY, transformedPartImage.getWidth (null) + shiftX, transformedPartImage.getHeight (null) + shiftY, 0, 0, transformedPartImage.getWidth (null), transformedPartImage.getHeight (null), null);
  CPColorBmp transformedPartBmp = new CPColorBmp (bI);
  // Now all we need is to fusion this part with activeLayer, also change current selection
  if (updateSelection)
    currentSelection.make (transformedPartBmp, (int) (bounds.getX ()), (int) (bounds.getY ()));
  transformedPartBmp.drawItselfOnTarget (layer, (int) (bounds.getX ()), (int) (bounds.getY ()));
}

public void drawTransformHandles (Graphics2D g2d, AffineTransform canvasTransform)
{
  if (!isTransformActive ())
    return;
  finalTransform = new AffineTransform ();
  finalTransform.concatenate (canvasTransform);
  finalTransform.concatenate (transform);

  g2d.setClip (null);
  g2d.setXORMode (Color.white);
  Path2D path = transformRectToPath (transformingRect.getLeft (), transformingRect.getTop (), transformingRect.getRight (), transformingRect.getBottom (), finalTransform);
  g2d.draw (path);
  // Draw 8 handle rectangles
  for (int i = 0; i < 3; i++)
    for (int j = 0; j < 3; j++)
      {
        float xPosition = transformingRect.getLeft () + transformingRect.getWidth () * (i * 0.5f);
        float yPosition = transformingRect.getTop () + transformingRect.getHeight () * (j * 0.5f);
        Point2D transformedPoint = new Point2D.Float ();
        finalTransform.transform (new Point2D.Float (xPosition, yPosition), transformedPoint);
        if (i != 1 || j != 1)
          g2d.drawRect ((int) (transformedPoint.getX () - smallRectPixelSize * 0.5), (int) (transformedPoint.getY () - smallRectPixelSize * 0.5), smallRectPixelSize, smallRectPixelSize);
        else
          {
            g2d.drawLine ((int) (transformedPoint.getX () - smallRectPixelSize * 0.5), (int) transformedPoint.getY (), (int) (transformedPoint.getX () + smallRectPixelSize * 0.5), (int) (transformedPoint.getY ()));
            g2d.drawLine ((int) transformedPoint.getX (), (int) (transformedPoint.getY () - smallRectPixelSize * 0.5), (int) transformedPoint.getX (), (int) (transformedPoint.getY () + smallRectPixelSize * 0.5));
          }
      }
}

final float fuzzyValue = 5.0f;

public void finalizeTransform ()
{
  transformActive = false;
  drawItselfOnLayer (activeLayer, RenderingHints.VALUE_INTERPOLATION_BICUBIC, true);
}

}
