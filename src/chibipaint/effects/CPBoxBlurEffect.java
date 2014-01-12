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

public class CPBoxBlurEffect extends CPBlurEffect
{
public CPBoxBlurEffect (int radius, int iterationsArg)
{
  super (radius, iterationsArg);
}

@Override
protected void blurLine (int[] src, int dst[], int radius, int startOffset, int endOffset)
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

@Override
protected void blurLine (int[] src, int dst[], int radius, int startOffset, int endOffset, float[] weights)
{
  float ta, s, tr, tg, tb;

  s = ta = tr = tg = tb = 0.0f;
  int pix;

  for (int i = startOffset; i < startOffset + radius && i <= endOffset; i++)
    {
      pix = src[i];
      ta += (pix >>> 24) * weights[i];
      tr += ((pix >>> 16) & 0xff) * weights[i];
      tg += ((pix >>> 8) & 0xff) * weights[i];
      tb += (pix & 0xff) * weights[i];
      s += weights[i];
    }
  for (int i = startOffset; i <= endOffset; i++)
    {
      if (i + radius <= endOffset)
        {
          pix = src[i + radius];
          ta += (pix >>> 24) * weights[i + radius];
          tr += ((pix >>> 16) & 0xff) * weights[i + radius];
          tg += ((pix >>> 8) & 0xff) * weights[i + radius];
          tb += (pix & 0xff) * weights[i + radius];
          s += weights[i + radius];
        }

      if (s > 1.e-3f)
        dst[i] = ((int) (ta / s) << 24) | ((int) (tr / s) << 16) | ((int) (tg / s) << 8) | (int) (tb / s);
      else
        dst[i] = 0;

      if (i - radius >= startOffset)
        {
          pix = src[i - radius];
          ta -= (pix >>> 24) * weights[i - radius];
          tr -= ((pix >>> 16) & 0xff) * weights[i - radius];
          tg -= ((pix >>> 8) & 0xff) * weights[i - radius];
          tb -= (pix & 0xff) * weights[i - radius];
          s -= weights[i - radius];
        }
    }
}
}
