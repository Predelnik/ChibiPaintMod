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

import java.awt.image.BufferedImage;

public class CPCopyPasteImage extends CPColorBmp
{
private int posX;
private int posY;

public CPCopyPasteImage (int width, int height, int posXArg, int posYArg)
{
  super (width, height);
  posX = posXArg;
  posY = posYArg;
}

public CPCopyPasteImage (BufferedImage img)
{
  super (img);
  posX = 0;
  posY = 0;
}

public void paste (CPColorBmp target)
{
  drawItselfOnTarget (target, posX, posY);
}

public int getPosX ()
{
  return posX;
}

public int getPosY ()
{
  return posY;
}
}
