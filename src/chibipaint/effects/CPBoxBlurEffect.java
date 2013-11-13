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

package chibipaint.effects;

import chibipaint.engine.CPColorBmp;
import chibipaint.engine.CPLayer;
import chibipaint.engine.CPSelection;
import chibipaint.util.CPRect;

import java.util.Arrays;

public class CPBoxBlurEffect extends CPEffect
{
private final int radiusX;
private final int radiusY;
private final int iterations;

public CPBoxBlurEffect (int radiusXArg, int radiusYArg, int iterationsArg)
{
  radiusX = radiusXArg;
  radiusY = radiusYArg;
  iterations = iterationsArg;
}

@Override
public void doEffectOn (CPLayer layer, CPSelection selection)
{
  if (selection.isEmpty ())
    boxBlur (layer, iterations);
  else
    boxBlur (layer, selection, iterations);
}

private void boxBlur (CPLayer layer, int iterations)
{
  int layerWidth = layer.getWidth ();
  int layerHeight = layer.getHeight ();

  // At first we need to copy modifiable part, sadly it's inevitable.
  // selection.applySelectionToData ();
  int maxPossibleLength = Math.max (layerWidth, layerHeight);

  int[] src = new int[maxPossibleLength];
  int[] dst = new int[maxPossibleLength];

  for (int k = 0; k < iterations; k++)
    {
      for (int j = 0; j < layerHeight; j++)
        {
          System.arraycopy (layer.getData (), j * layerWidth, src, 0, layerWidth);
          multiplyAlpha (src, layerWidth);
          Arrays.fill (dst, 0);
          boxBlurLine (src, dst, radiusX, 0, layerWidth - 1);
          copyArrayToRow (layer, j, layerWidth, dst);
        }

      for (int i = 0; i < layerWidth; i++)
        {
          copyColumnToArray (layer, i, layerHeight, src);
          Arrays.fill (dst, 0);
          boxBlurLine (src, dst, radiusY, 0, layerHeight - 1);
          restoreAlpha (dst, layerHeight);
          copyArrayToColumn (layer, i, layerHeight, dst);
        }
    }
}

private void boxBlur (CPLayer layer, CPSelection selection, int iterations)
{
  int layerWidth = layer.getWidth ();
  int layerHeight = layer.getHeight ();
  CPRect rect = new CPRect (0, 0, layerWidth, layerHeight);
  rect.clip (selection.getBoundingRect ());

  // At first we need to copy modifiable part, sadly it's inevitable.
  // selection.applySelectionToData ();
  CPColorBmp tempBmp = new CPColorBmp (0, 0);
  tempBmp.copyDataFromSelectedPart (layer, selection);
  layer.removePartsCutBySelection (selection);
  int rectWidth = rect.getWidth ();
  int rectHeight = rect.getHeight ();
  int maxPossibleLength = Math.max (rectWidth, rectHeight);

  int[] src = new int[maxPossibleLength];
  int[] dst = new int[maxPossibleLength];

  for (int k = 0; k < iterations; k++)
    {
      for (int j = 0; j < rectHeight; j++)
        {
          System.arraycopy (tempBmp.getData (), j * rectWidth, src, 0, rectWidth);
          multiplyAlpha (src, rectWidth);
          int leftLimit = 0, rightLimit = rectWidth;
          int off = (j + rect.getTop ()) * layerWidth + rect.getLeft ();
          while (leftLimit < rectWidth && ((selection.getData ()[off] & 0xFF) < 32))
            {
              leftLimit++;
              off++;
            }

          off = (j + rect.getTop ()) * layerWidth + rect.getRight ();
          while (rightLimit > 0 && ((selection.getData ()[off] & 0xFF) < 32))
            {
              rightLimit--;
              off--;
            }

          Arrays.fill (dst, 0);
          if (leftLimit < rightLimit)
            boxBlurLine (src, dst, radiusX, leftLimit, rightLimit);
          copyArrayToRow (tempBmp, j, rectWidth, dst);
        }

      for (int i = 0; i < rectWidth; i++)
        {
          copyColumnToArray (tempBmp, i, rectHeight, src);
          int bottomLimit = 0, topLimit = rectHeight;
          int off = (rect.getTop ()) * layerWidth + i + rect.getLeft ();
          while (bottomLimit < rectWidth && (selection.getData ()[off] & 0xFF) < 32)
            {
              bottomLimit++;
              off += layerWidth;
            }

          off = rect.getBottom () * layerWidth + i + rect.getLeft ();
          while (topLimit > 0 && (selection.getData ()[off] & 0xFF) < 32)
            {
              topLimit--;
              off -= layerWidth;
            }

          Arrays.fill (dst, 0);
          if (bottomLimit < topLimit)
            boxBlurLine (src, dst, radiusY, bottomLimit, topLimit);
          restoreAlpha (dst, rectHeight);
          copyArrayToColumn (tempBmp, i, rectHeight, dst);
        }
    }

  tempBmp.drawItselfOnTarget (layer, rect.getLeft (), rect.getTop ());
}

void copyColumnToArray (CPColorBmp layer, int x, int len, int[] buffer)
{
  for (int i = 0; i < len; i++)
    {
      int offset = x + i * layer.getWidth ();
      buffer[i] = layer.getData ()[offset];
    }
}

void copyArrayToColumn (CPColorBmp layer, int x, int len, int[] buffer)
{
  for (int i = 0; i < len; i++)
    {
      int offset = x + i * layer.getWidth ();
      layer.getData ()[offset] = buffer[i];
    }
}


void copyArrayToRow (CPColorBmp layer, int y, int len, int[] buffer)
{
  int offset = y * layer.getWidth ();
  for (int i = 0; i < len; i++, offset++)
    {
      layer.getData ()[offset] = buffer[i];
    }
}

private static void multiplyAlpha (int[] buffer, int len)
{
  for (int i = 0; i < len; i++)
    {
      buffer[i] = buffer[i] & 0xff000000 | ((buffer[i] >>> 24) * (buffer[i] >>> 16 & 0xff) / 255) << 16
              | ((buffer[i] >>> 24) * (buffer[i] >>> 8 & 0xff) / 255) << 8 | (buffer[i] >>> 24)
              * (buffer[i] & 0xff) / 255;
    }
}

private static void restoreAlpha (int[] buffer, int len)
{
  for (int i = 0; i < len; i++)
    {
      if ((buffer[i] & 0xff000000) != 0)
        {
          buffer[i] = buffer[i] & 0xff000000
                  | Math.min ((buffer[i] >>> 16 & 0xff) * 255 / (buffer[i] >>> 24), 255) << 16
                  | Math.min ((buffer[i] >>> 8 & 0xff) * 255 / (buffer[i] >>> 24), 255) << 8
                  | Math.min ((buffer[i] & 0xff) * 255 / (buffer[i] >>> 24), 255);
        }
    }
}

private static void boxBlurLine (int[] src, int dst[], int radius, int startOffset, int endOffset)
{
  int s, ta, tr, tg, tb;
  s = ta = tr = tg = tb = 0;
  int pix;

  for (int i = startOffset; i < startOffset + radius && i <= endOffset; i++)
    {
      pix = src[i];
      ta += pix >>> 24;
      tr += (pix >>> 16) & 0xff;
      tg += (pix >>> 8) & 0xff;
      tb += pix & 0xff;
      s++;
    }
  for (int i = startOffset; i <= endOffset; i++)
    {
      if (i + radius <= endOffset)
        {
          pix = src[i + radius];
          ta += pix >>> 24;
          tr += (pix >>> 16) & 0xff;
          tg += (pix >>> 8) & 0xff;
          tb += pix & 0xff;
          s++;
        }

      dst[i] = (ta / s << 24) | (tr / s << 16) | (tg / s << 8) | tb / s;

      if (i - radius >= startOffset)
        {
          pix = src[i - radius];
          ta -= pix >>> 24;
          tr -= (pix >>> 16) & 0xff;
          tg -= (pix >>> 8) & 0xff;
          tb -= pix & 0xff;
          s--;
        }
    }
}


}