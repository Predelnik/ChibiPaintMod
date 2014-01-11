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
import chibipaint.util.CPRect;

public abstract class CPEffect
{
public abstract void doEffectOn (CPLayer layer, CPSelection selection);

int modify (int[] data, byte[] selData, int offset)
{
  return 0;
  // Do nothing
}

int modify (int[] data, int offset)
{
  return 0;
  // Do nothing
}

int modify (int[] data, byte[] selData, int i, int j, int offset)
{
  return 0;
  // Do nothing
}

int modify (int[] data, int i, int j, int offset)
{
  return 0;
  // Do nothing
}


void modifyByOffset (CPLayer layer, CPSelection selection)
{
  if (selection.isEmpty ())
    {
      for (int off = 0; off < layer.getWidth () * layer.getHeight (); off++)
        layer.getData ()[off] = modify (layer.getData (), off);
    }
  else
    {
      CPRect rect = selection.getBoundingRect ();
      int off;
      for (int j = 0; j < rect.getHeight (); j++)
        {
          off = (j + rect.getTop ()) * layer.getWidth () + rect.getLeft ();
          for (int i = 0; i < rect.getWidth (); i++, off++)
            {
              layer.getData ()[off] = modify (layer.getData (), selection.getData (), off);
            }
        }
    }
}

public void modifyByIndices (CPLayer layer, CPSelection selection)
{
  if (selection.isEmpty ())
    {
      int off;
      for (int j = 0; j < layer.getHeight (); j++)
        {
          off = j * layer.getWidth ();
          for (int i = 0; i < layer.getWidth (); i++, off++)
            layer.getData ()[off] = modify (layer.getData (), i, j, off);
        }
    }
  else
    {
      CPRect rect = selection.getBoundingRect ();
      int off;
      for (int j = 0; j < rect.getHeight (); j++)
        {
          off = (j + rect.getTop ()) * layer.getWidth () + rect.getLeft ();
          for (int i = 0; i < rect.getWidth (); i++, off++)
            {
              layer.getData ()[off] = modify (layer.getData (), selection.getData (), i, j, off);
            }
        }
    }
}

// Colors the pixel with current color according to layer's transparency and selection. (color's own transparency being disregarded)
int colorInOpaque (int destColor, int selValue, int newColor)
{
  int destAlpha = destColor >>> 24;
  int srcAlpha = selValue & 0xFF;
  int newLayerAlpha = srcAlpha + destAlpha * (255 - srcAlpha) / 255;
  if (newLayerAlpha > 0)
    {
      int realAlpha = 255 * srcAlpha / newLayerAlpha;
      int invAlpha = 255 - realAlpha;

      if (srcAlpha > 0)
        {
          int finalColor = (((newColor >>> 16 & 0xff) * realAlpha + (destColor >>> 16 & 0xff) * invAlpha) / 255) << 16
                  & 0xff0000
                  | (((newColor >>> 8 & 0xff) * realAlpha + (destColor >>> 8 & 0xff) * invAlpha) / 255) << 8
                  & 0xff00 | (((newColor & 0xff) * realAlpha + (destColor & 0xff) * invAlpha) / 255) & 0xff;
          finalColor |= newLayerAlpha << 24 & 0xff000000;
          return finalColor;
        }
      else
        return destColor;
    }
  else
    return 0x00FFFFFF;
}
}

