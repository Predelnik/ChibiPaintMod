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

package chibipaint.util;

// left,mTop: upper left corner, mRight, mBottom: lower right corner,
// exclusive, right>left && bottom>top if not empty

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

public class CPRect implements Cloneable
{

public int left, top, right, bottom;

public CPRect (int left, int top, int right, int bottom)
{
  this.left = left;
  this.top = top;
  this.right = right;
  this.bottom = bottom;
}

public CPRect (int width, int height)
{
  this.left = 0;
  this.top = 0;
  this.right = width;
  this.bottom = height;
}

public CPRect ()
{
  makeEmpty ();
}

public CPRect (CPRect r)
{
  set (r);
}

public CPRect (Rectangle rectangle)
{
  this.left = (int) (Math.floor (rectangle.getX ()));
  this.top = (int) (Math.floor (rectangle.getY ()));
  this.right = (int) (Math.ceil (rectangle.getMaxX ()));
  this.bottom = (int) (Math.ceil (rectangle.getMaxY ()));
}

public void makeEmpty ()
{
  left = 0;
  top = 0;
  right = 0;
  bottom = 0;
}

public void union (CPRect rect)
{
  if (isEmpty ())
    {
      set (rect);
    }
  else
    {
      left = Math.min (left, rect.left);
      top = Math.min (top, rect.top);
      right = Math.max (right, rect.right);
      bottom = Math.max (bottom, rect.bottom);
    }
}

public void clip (CPRect rect)
{
  if (isEmpty ())
    {
      return;
    }

  if (rect.isEmpty ())
    {
      makeEmpty ();
    }
  else
    {
      left = Math.max (left, rect.left);
      top = Math.max (top, rect.top);
      right = Math.min (right, rect.right);
      bottom = Math.min (bottom, rect.bottom);
    }
}

public boolean isInside (CPRect rect)
{
  return left >= rect.left && top >= rect.top && right <= rect.right && bottom <= rect.bottom;
}

public boolean isInside (Point point)
{
  return left <= point.x && top <= point.y && right >= point.x && bottom >= point.y;
}

public boolean isInside (Point2D point)
{
  return left <= point.getX () && top <= point.getY () && right >= point.getX () && bottom >= point.getY ();
}

// First makes dstRect the same width and height (not modifying its top/left)
// Clips the dstRect with this rectangle and changes the srcRect so that
// it corresponds to the new clipped rectangle

public void clipSourceDest (CPRect srcRect, CPRect dstRect)
{
  dstRect.right = dstRect.left + srcRect.getWidth ();
  dstRect.bottom = dstRect.top + srcRect.getHeight ();

  if (isEmpty () || dstRect.left >= right || dstRect.top >= bottom || dstRect.right <= left
          || dstRect.bottom <= top)
    {
      srcRect.makeEmpty ();
      dstRect.makeEmpty ();
      return;
    }

  // bottom/right
  if (dstRect.right > right)
    {
      srcRect.right -= dstRect.right - right;
      dstRect.right = right;
    }

  if (dstRect.bottom > bottom)
    {
      srcRect.bottom -= dstRect.bottom - bottom;
      dstRect.bottom = bottom;
    }

  // top/left
  if (dstRect.left < left)
    {
      srcRect.left += left - dstRect.left;
      dstRect.left = left;
    }

  if (dstRect.top < top)
    {
      srcRect.top += top - dstRect.top;
      dstRect.top = top;
    }
}

public int getWidth ()
{
  return right - left;
}

public int getHeight ()
{
  return bottom - top;
}

public int getLeft ()
{
  return left;
}

public int getTop ()
{
  return top;
}

public int getRight ()
{
  return right;
}

public int getBottom ()
{
  return bottom;
}

public boolean isEmpty ()
{
  return right <= left || bottom <= top;
}

void set (CPRect r)
{
  left = r.left;
  top = r.top;
  right = r.right;
  bottom = r.bottom;
}

@Override
public Object clone ()
{
  try
    {
      return super.clone ();
    }
  catch (Exception ignored)
    {
      throw new Error ("Uh oh");
    }
}

public void translate (int x, int y)
{
  left += x;
  right += x;
  top += y;
  bottom += y;
}

public void moveTo (int x, int y)
{
  translate (x - left, y - top);
}

@Override
public boolean equals (Object o)
{
  if (o instanceof CPRect)
    {
      CPRect r = (CPRect) o;
      return left == r.left && right == r.right && top == r.top && bottom == r.bottom;
    }
  return false;
}


public Path2D toPath2D ()
{
  Path2D path = new Path2D.Float ();
  path.moveTo (left, top);
  path.lineTo (right, top);
  path.lineTo (right, bottom);
  path.lineTo (left, bottom);
  path.closePath ();
  return path;
}

public CPRect makeFuzzy ()
{
  CPRect rect = new CPRect ();
  rect.left = left - 1;
  rect.right = right + 1;
  rect.top = top - 1;
  rect.bottom = bottom + 1;
  return rect;
}
}
