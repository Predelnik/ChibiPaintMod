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

import chibipaint.engine.CPLayer;
import chibipaint.engine.CPSelection;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.MemoryImageSource;

public class CPImageUtils
{
public static void PasteImageToOrigin (CPLayer layer, Image img)
{
  int[] data = layer.getData ();
  MemoryImageSource imgSource = new MemoryImageSource (layer.getWidth (), layer.getHeight (), data, 0, layer.getWidth ());
  Image layerImage = Toolkit.getDefaultToolkit ().createImage (imgSource);
  int imageType = BufferedImage.TYPE_INT_ARGB;
  BufferedImage bI = new BufferedImage (layer.getWidth (), layer.getHeight (), BufferedImage.TYPE_INT_ARGB);
  Graphics2D g = bI.createGraphics ();
  g.drawImage (layerImage, 0, 0, null);
  g.drawImage (img, 0, 0, null);
  layer.setData (((DataBufferInt) bI.getData ().getDataBuffer ()).getData ());
}

public static Image RenderLayerSelectionToImage (CPLayer layer, CPSelection curSelection)
{
  CPRect rect = curSelection.getBoundingRect ();
  int[] dataClone = layer.getData ().clone ();
  curSelection.applySelectionToData (dataClone);
  MemoryImageSource imgSource = new MemoryImageSource (layer.getWidth (), layer.getHeight (), dataClone, 0, layer.getWidth ());
  Image layerImage = Toolkit.getDefaultToolkit ().createImage (imgSource);
  BufferedImage bI = new BufferedImage (layer.getWidth (), layer.getHeight (), BufferedImage.TYPE_INT_ARGB);
  Graphics2D g = bI.createGraphics ();
  g.drawImage (layerImage, 0, 0, null);
  return bI.getSubimage (rect.left, rect.top, rect.getWidth (), rect.getHeight ());
}
}
