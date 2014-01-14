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

// utility class to help dealing with pixel algorithms (i.e. scanline)
public class CPPixelCoords implements Comparable<CPPixelCoords>
{
static public final int[][] Mv = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
static public final int[][] Corners = {{1, 1}, {0, 1}, {0, 0}, {1, 0}};
public int x;
public final int y;

public CPPixelCoords (int xArg, int yArg)
{
  x = xArg;
  y = yArg;
}

public CPPixelCoords (CPPixelCoords point)
{
  x = point.x;
  y = point.y;
}

CPPixelCoords left ()
{
  return new CPPixelCoords (x - 1, y);
}

public CPPixelCoords right ()
{
  return new CPPixelCoords (x + 1, y);
}

CPPixelCoords up ()
{
  return new CPPixelCoords (x, y - 1);
}

public CPPixelCoords down ()
{
  return new CPPixelCoords (x, y + 1);
}

public CPPixelCoords MoveByMv (int num)
{
  return new CPPixelCoords (x + Mv[num % 4][0], y + Mv[num % 4][1]);
}

public CPPixelCoords MoveToCorner (int num)
{
  return new CPPixelCoords (x + Corners[num % 4][0], y + Corners[num % 4][1]);
}

@Override
public int compareTo (CPPixelCoords o)
{
  if (x != o.x)
    return (x - o.x);
  else
    return (o.y - y);
}


}
