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

public class CPTables
{
private static CPTables ref;
public final int[] divide;
public final int[] divideBy255;

public static CPTables getRef ()
{
  return ref;
}

public static void init ()
{
  ref = new CPTables ();
}


private CPTables ()
{
  divide = new int[65026 * 256];
  for (int i = 0; i < 65026; i++)
    for (int j = 1; j < 256; j++)
      divide[i * 256 + j] = i / j;

  divideBy255 = new int[65026 * 2];
  for (int i = 0; i < 65026 * 2; i++)
    divideBy255[i] = i / 255;
}
}
