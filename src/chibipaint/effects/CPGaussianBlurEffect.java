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

import java.awt.image.Kernel;

public class CPGaussianBlurEffect extends CPBlurEffect
{
private final Kernel kernel;

public CPGaussianBlurEffect (int radiusArg, int iterationsArg)
{
  super (radiusArg, iterationsArg);
  kernel = makeKernel (radiusArg);
}

@Override
protected void blurLine (int[] src, int dst[], int radius, int length)
{
  float s, ta, tr, tg, tb;
  int pix;
  int m;

  float[] matrix = kernel.getKernelData (null);

  for (int i = 0; i <= length; i++)
    {
      int start = i - radius;
      int end = i + radius;
      ta = tr = tg = tb = 0;
      m = 0;

      if (start < 0)
        {
          pix = src[0];
          for (int j = start; j < 0; j++, m++)
            {
              ta += ((pix >>> 24) & 0xff) * matrix[m];
              tr += ((pix >>> 16) & 0xff) * matrix[m];
              tg += ((pix >>> 8) & 0xff) * matrix[m];
              tb += (pix & 0xff) * matrix[m];
            }
          start = 0;
        }

      if (end > length + 1)
        {
          pix = src[length];
          for (int j = length + 1, m1 = length + 1 - start; j < end; j++, m1++)
            {
              ta += ((pix >>> 24) & 0xff) * matrix[m1];
              tr += ((pix >>> 16) & 0xff) * matrix[m1];
              tg += ((pix >>> 8) & 0xff) * matrix[m1];
              tb += (pix & 0xff) * matrix[m1];
            }
          end = length;
        }

      for (int j = start; j < end; j++, m++)
        {
          pix = src[j];
          ta += ((pix >>> 24) & 0xff) * matrix[m];
          tr += ((pix >>> 16) & 0xff) * matrix[m];
          tg += ((pix >>> 8) & 0xff) * matrix[m];
          tb += (pix & 0xff) * matrix[m];
        }
      dst[i] = ((int) (ta) << 24) | ((int) (tr) << 16) | ((int) (tg) << 8) | (int) (tb);
    }
}

@Override
protected void blurLine (int[] src, int dst[], int radius, int length, float[] weights)
{
  float s, ta, tr, tg, tb;
  int pix;
  int actualIndex = 0;

  float[] matrix = kernel.getKernelData (null);

  for (int i = 0; i <= length; i++)
    {
      int start = i - radius;
      int end = i + radius;
      s = ta = tr = tg = tb = 0.0f;
      for (int j = start, m = 0; j <= end; j++, m++)
        {
          if (j >= 0 && j <= length)
            actualIndex = j;
          else if (j < 0)
            actualIndex = 0;
          else if (j > length)
            actualIndex = length;

          pix = src[actualIndex];
          ta += ((pix >>> 24) & 0xff) * matrix[m] * weights[actualIndex];
          tr += ((pix >>> 16) & 0xff) * matrix[m] * weights[actualIndex];
          tg += ((pix >>> 8) & 0xff) * matrix[m] * weights[actualIndex];
          tb += (pix & 0xff) * matrix[m] * weights[actualIndex];
          s += weights[actualIndex] * matrix[m];
        }

      if ((((src[i] >> 24) & 0xFF) - ta / s) > 100.0)
        {
          s = s + 1.0f;
          s = s - 1.0f;
        }

      if (s > 1.e-3f)
        dst[i] = ((int) (ta / s) << 24) | ((int) (tr / s) << 16) | ((int) (tg / s) << 8) | (int) (tb / s);
      else
        dst[i] = 0;
    }
}

private static Kernel makeKernel (float radius)
{
  int r = (int) Math.ceil (radius);
  int rows = r * 2 + 1;
  float[] matrix = new float[rows];
  float sigma = radius / 3;
  float sigma22 = 2 * sigma * sigma;
  float sigmaPi2 = 2 * (float) Math.PI * sigma;
  float sqrtSigmaPi2 = (float) Math.sqrt (sigmaPi2);
  float radius2 = radius * radius;
  float total = 0;
  int index = 0;
  for (int row = -r; row <= r; row++)
    {
      float distance = row * row;
      if (distance > radius2)
        matrix[index] = 0;
      else
        matrix[index] = (float) Math.exp (-(distance) / sigma22) / sqrtSigmaPi2;
      total += matrix[index];
      index++;
    }
  for (int i = 0; i < rows; i++)
    matrix[i] /= total;

  return new Kernel (rows, 1, matrix);
}
}
