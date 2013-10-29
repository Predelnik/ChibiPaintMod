/*
	ChibiPaintMod
   Copyright (c) 2012-2013 Sergey Semushin
   Copyright (c) 2006-2008 Marc Schefer

    This file is part of ChibiPaintMod (previously ChibiPaint).

    ChibiPaintMod is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    ChibiPaintMod is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with ChibiPaintMod. If not, see <http://www.gnu.org/licenses/>.

 */

package chibipaint.engine;

import chibipaint.util.CPRect;

public class CPBitmap
{
// The real width and height of the bitmap in memory
int width, height;

CPBitmap (int width, int height)
{
  this.width = width;
  this.height = height;
}

public int getWidth ()
{
  return width;
}

public int getHeight ()
{
  return height;
}

public CPRect getSize ()
{
  return new CPRect (width, height);
}

//
// Clipping related methods
//

boolean isInside (int x, int y)
{
  return x >= 0 && y >= 0 && x < width && y < height;
}
}
