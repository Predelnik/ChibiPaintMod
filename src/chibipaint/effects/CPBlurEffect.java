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

import chibipaint.engine.CPColorBmp;
import chibipaint.engine.CPLayer;
import chibipaint.engine.CPSelection;
import chibipaint.util.CPRect;

import java.util.Arrays;

abstract public class CPBlurEffect extends CPEffect
{
private final int radius;
private final int iterations;

CPBlurEffect (int radiusArg, int iterationsArg)
{
  radius = radiusArg;
  iterations = iterationsArg;
}

@Override
public void doEffectOn (CPLayer layer, CPSelection selection)
{
  if (selection.isEmpty ())
    boxBlur (layer, iterations);
  else
    boxBlurWithWeights (layer, selection, iterations);
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
          blurLine (src, dst, radius, layerWidth - 1);
          copyArrayToRow (layer, j, layerWidth, dst);
        }

      for (int i = 0; i < layerWidth; i++)
        {
          copyColumnToArray (layer, i, layerHeight, src);
          Arrays.fill (dst, 0);
          blurLine (src, dst, radius, layerHeight - 1);
          restoreAlpha (dst, layerHeight);
          copyArrayToColumn (layer, i, layerHeight, dst);
        }
    }
}

private void boxBlurWithWeights (CPLayer layer, CPSelection selection, int iterations)
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
  float[] weights = new float[maxPossibleLength];

  for (int k = 0; k < iterations; k++)
    {
      for (int j = 0; j < rectHeight; j++)
        {
          makeWeightsFromRow (selection, j, rect.getLeft (), rect.getTop (), rectWidth, weights);
          System.arraycopy (tempBmp.getData (), j * rectWidth, src, 0, rectWidth);
          multiplyAlpha (src, rectWidth);
          blurLine (src, dst, radius, rectWidth - 1, weights);
          copyArrayToRow (tempBmp, j, rectWidth, dst);
        }

      for (int i = 0; i < rectWidth; i++)
        {
          makeWeightsFromColumn (selection, i, rect.getLeft (), rect.getTop (), rectHeight, weights);
          copyColumnToArray (tempBmp, i, rectHeight, src);
          blurLine (src, dst, radius, rectHeight - 1, weights);
          restoreAlpha (dst, rectHeight);
          copyArrayToColumn (tempBmp, i, rectHeight, dst);
        }
    }

  // Post processing
  for (int j = 0; j < rectHeight; j++)
    {
      int off = j * rectWidth;
      int selOff = (j + rect.getTop ()) * selection.getWidth () + rect.getLeft ();
      for (int i = 0; i < rectWidth; i++, off++, selOff++)
        {
          int selValue = (int) selection.getData ()[selOff] & 0xFF;
          if (selValue == 0)
            tempBmp.getData ()[off] = (tempBmp.getData ()[off] & 0xFFFFFF) | (selValue << 24);
        }
    }

  tempBmp.drawItselfOnTarget (layer, rect.getLeft (), rect.getTop ());
}

private void makeWeightsFromRow (CPSelection sel, int j, int initialXOffset, int initialYOffset, int len, float[] weights)
{
  int offset = (j + initialYOffset) * sel.getWidth () + initialXOffset;
  for (int i = 0; i < len; i++, offset++)
    {
      weights[i] = ((sel.getData ()[offset] & 0xFF) / 255.0f);
    }
}

void makeWeightsFromColumn (CPSelection sel, int x, int initialXOffset, int initialYOffset, int len, float[] weights)
{
  for (int i = 0; i < len; i++)
    {
      int offset = initialXOffset + x + (i + initialYOffset) * sel.getWidth ();
      weights[i] = ((sel.getData ()[offset] & 0xFF) / 255.0f);
    }
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

abstract protected void blurLine (int[] src, int dst[], int radius, int length);

abstract protected void blurLine (int[] src, int dst[], int radius, int length, float[] weights);

}