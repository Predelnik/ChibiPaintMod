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

public class CPTransparentFillEffect extends CPEffect
{
private final int color;

public CPTransparentFillEffect (int colorArg)
{
  color = colorArg;
}

@Override
public void doEffectOn (CPLayer layer, CPSelection selection)
{
  modifyByOffset (layer, selection);
}

public int modify (int[] data, byte[] selData, int offset)
{
  return colorIn (data[offset], selData[offset] & 0xFF, color);
}

public int modify (int[] data, int offset)
{
  return color;
}

}


