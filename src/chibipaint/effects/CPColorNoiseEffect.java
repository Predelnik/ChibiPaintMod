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

package chibipaint.effects;

import chibipaint.engine.CPLayer;
import chibipaint.engine.CPSelection;

import java.util.Random;

public class CPColorNoiseEffect extends CPEffect
{
private final Random rand;

public CPColorNoiseEffect ()
{
  rand = new Random ();
}

@Override
public void doEffectOn (CPLayer layer, CPSelection selection)
{
  modifyByOffset (layer, selection);
}

public int modify (int[] data, byte[] selData, int offset)
{
  return colorInOpaque (data[offset], selData[offset], rand.nextInt () | 0xff000000);
}

public int modify (int[] data, int offset)
{
  return rand.nextInt () | 0xff000000;
}

}


