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

import chibipaint.util.CPIfaces;
import chibipaint.util.CPRect;
import chibipaint.util.CPTables;
import gnu.trove.stack.array.TIntArrayStack;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

//
// A 32bpp bitmap class (ARGB format)
//

public class CPColorBmp extends CPBitmap
{

// The bitmap data
private int[] data;

//
// Constructors
//

// Allocates a new bitmap
public CPColorBmp (int width, int height)
{
  super (width, height);
  this.data = new int[width * height];
}

// Creates a CPBitmap object from existing bitmap data
public CPColorBmp (int width, int height, int[] data)
{
  super (width, height);
  this.data = data;
}

// Creates a CPBitmap by copying a part of another CPBitmap
public CPColorBmp (CPColorBmp bmp, CPRect r)
{
  super (r.getWidth (), r.getHeight ());

  data = new int[width * height];

  setFromBitmapRect (bmp, r);
}

// To make process of building our custom types from internal easier
public CPColorBmp (BufferedImage image)
{
  super (image.getWidth (), image.getHeight ());

  data = ((DataBufferInt) image.getData ().getDataBuffer ()).getData (); // Magical trick
}

//
// Pixel access with friendly clipping
//

public int getPixel (int xArg, int yArg)
{
  int x = xArg, y = yArg;
  x = Math.max (0, Math.min (width - 1, x));
  y = Math.max (0, Math.min (height - 1, y));

  return getData ()[x + y * width];
}

public void setPixel (int x, int y, int color)
{
  if (x < 0 || y < 0 || x >= width || y >= height)
    {
      return;
    }

  getData ()[x + y * width] = color;
}

//
// Copy rectangular regions methods
//

// copies a a rect to an int array allocated by the method
public int[] copyRectToIntArray (CPRect rect)
{
  CPRect r = new CPRect (0, 0, width, height);
  r.clip (rect);

  int[] buffer = new int[r.getWidth () * r.getHeight ()];
  int w = r.getWidth ();
  int h = r.getHeight ();
  for (int j = 0; j < h; j++)
    {
      System.arraycopy (getData (), (j + r.top) * width + r.left, buffer, j * w, w);
    }

  return buffer;
}

// copies a a rect to an int array
public boolean copyRectToIntArray (CPRect rect, int[] buffer)
{
  CPRect r = new CPRect (0, 0, width, height);
  r.clip (rect);

  if (buffer.length < (r.getWidth () * r.getHeight ()))
    {
      return false;
    }
  int w = r.getWidth ();
  int h = r.getHeight ();
  for (int j = 0; j < h; j++)
    {
      System.arraycopy (getData (), (j + r.top) * width + r.left, buffer, j * w, w);
    }

  return true;
}

// sets the content of a rectangular region using data from an int array
public void setRectFromIntArray (int[] buffer, CPRect rect)
{
  CPRect r = new CPRect (0, 0, width, height);
  r.clip (rect);

  int w = r.getWidth ();
  int h = r.getHeight ();
  for (int j = 0; j < h; j++)
    {
      System.arraycopy (buffer, j * w, getData (), (j + r.top) * width + r.left, w);
    }
}

//
// XOR Copy
//

public int[] copyRectXOR (CPColorBmp bmp, CPRect rect)
{
  CPRect r = new CPRect (0, 0, width, height);
  r.clip (rect);

  int[] buffer = new int[r.getWidth () * r.getHeight ()];
  int w = r.getWidth ();
  int h = r.getHeight ();
  for (int j = 0; j < h; j++)
    {
      for (int i = 0; i < w; i++)
        {
          buffer[i + j * w] = getData ()[(j + r.top) * width + i + r.left] ^ bmp.getData ()[(j + r.top) * width + i + r.left];
        }
    }

  return buffer;
}


public void copyRectFrom (CPColorBmp bmp, CPRect rect)        // Copy rectangle of pixels exactly from the picture of the same size
{
  CPRect r = new CPRect (0, 0, width, height);
  r.clip (rect);

  int w = r.getWidth ();
  int h = r.getHeight ();
  for (int j = 0; j < h; j++)
    {
      int offset = (j + r.top) * width + r.left;
      for (int i = 0; i < w; i++, offset++)
        {
          getData ()[offset] = bmp.getData ()[offset];
        }
    }
}

public void setRectXOR (int[] buffer, CPRect rect)
{
  CPRect r = new CPRect (0, 0, width, height);
  r.clip (rect);

  int w = r.getWidth ();
  int h = r.getHeight ();
  for (int j = 0; j < h; j++)
    {
      for (int i = 0; i < w; i++)
        {
          getData ()[(j + r.top) * width + i + r.left] = getData ()[(j + r.top) * width + i + r.left] ^ buffer[i + j * w];
        }
    }
}

//
// Copy another bitmap into this one using alpha blending
//

public void pasteAlphaRect (CPColorBmp bmp, CPRect srcRect, int x, int y)
{
  CPRect srcRectCpy, dstRect;
  try
    {
      dstRect = new CPRect (x, y, 0, 0);
      srcRectCpy = (CPRect) srcRect.clone ();
    }
  catch (Exception e)
    {
      return;
    }
  getSize ().clipSourceDest (srcRectCpy, dstRect);

  int[] srcData = bmp.getData ();
  for (int j = 0; j < dstRect.bottom - dstRect.top; j++)
    {
      int srcOffset = srcRectCpy.left + (srcRectCpy.top + j) * bmp.width;
      int dstOffset = dstRect.left + (dstRect.top + j) * width;
      for (int i = dstRect.left; i < dstRect.right; i++, srcOffset++, dstOffset++)
        {
          int color1 = srcData[srcOffset];
          int alpha1 = color1 >>> 24;

          if (alpha1 <= 0)
            {
              continue;
            }

          if (alpha1 == 255)
            {
              getData ()[dstOffset] = color1;
              continue;
            }

          int color2 = getData ()[dstOffset];
          int alpha2 = (color2 >>> 24);

          int newAlpha = alpha1 + alpha2 - alpha1 * alpha2 / 255;
          if (newAlpha > 0)
            {
              int realAlpha = alpha1 * 255 / newAlpha;
              int invAlpha = 255 - realAlpha;

              getData ()[dstOffset] = newAlpha << 24
                      | ((color1 >>> 16 & 0xff) + (((color2 >>> 16 & 0xff) * invAlpha - (color1 >>> 16 & 0xff)
                      * invAlpha) / 255)) << 16
                      | ((color1 >>> 8 & 0xff) + (((color2 >>> 8 & 0xff) * invAlpha - (color1 >>> 8 & 0xff)
                      * invAlpha) / 255)) << 8
                      | ((color1 & 0xff) + (((color2 & 0xff) * invAlpha - (color1 & 0xff) * invAlpha) / 255));
            }
        }
    }
}

// Sets the content of this CPBitmap using a rect from another bitmap
// Assumes that the width and height of this bitmap and the rectangle are the same!!!
void setFromBitmapRect (CPColorBmp bmp, CPRect r)
{
  for (int i = 0; i < r.getHeight (); i++)
    {
      System.arraycopy (bmp.getData (), (i + r.top) * bmp.width + r.left, getData (), i * width, width);
    }
}

public void pasteBitmap (CPColorBmp bmp, int x, int y)
{
  CPRect srcRect = bmp.getSize ();
  CPRect dstRect = new CPRect (x, y, 0, 0);
  getSize ().clipSourceDest (srcRect, dstRect);

  for (int i = 0; i < srcRect.getHeight (); i++)
    {
      System.arraycopy (bmp.getData (), (i + srcRect.top) * bmp.width + srcRect.left, getData (), (i + dstRect.top) * width
              + dstRect.left, srcRect.getWidth ());
    }
}

//
// Copies the Alpha channel from another bitmap
//

public void copyAlphaFrom (CPColorBmp bmp, CPRect r)
{
  r.clip (getSize ());

  for (int j = r.top; j < r.bottom; j++)
    {
      for (int i = r.left; i < r.right; i++)
        {
          getData ()[j * width + i] = (getData ()[j * width + i] & 0xffffff) | (bmp.getData ()[j * width + i] & 0xff000000);
        }
    }
}

public void copyDataFrom (CPColorBmp bmp)
{
  if (bmp.width != width || bmp.height != height)
    {
      width = bmp.width;
      height = bmp.height;
      data = new int[width * height];
    }

  System.arraycopy (bmp.getData (), 0, getData (), 0, getData ().length);
}

public int[] getData ()
{
  return data;
}

public void setData (int[] dataArg)
{
  data = dataArg;
}

public void cutBySelection (CPSelection selection)
{
  for (int i = 0; i < height * width; i++)
    {
      int curAlpha = (data[i] & 0xFF000000) >>> 24;
      int selData = selection.data[i] & 0xFF;
      data[i] &= 0x00FFFFFF;
      data[i] |= (curAlpha > selData ? selData : curAlpha) << 24;
    }
}

public void removePartsCutBySelection (CPSelection selection)
{
  for (int i = 0; i < width * height; i++)
    {
      int curAlpha = (data[i] & 0xFF000000) >>> 24;
      int selData = selection.data[i] & 0xFF;
      data[i] &= 0x00FFFFFF;
      data[i] |= curAlpha - (curAlpha > selData ? selData : curAlpha) << 24;
    }
}

public void drawItselfOnTarget (CPColorBmp target, int shiftX, int shiftY)
{
  if (shiftY + target.height <= 0 || shiftX + target.width <= 0)
    return;
  int yBottom = (shiftY + height <= target.height ? height : target.height - shiftY);
  int yTop = shiftY >= 0 ? 0 : -shiftY;
  int xRight = (shiftX + width <= target.width ? width : target.width - shiftX);
  int xLeft = shiftX >= 0 ? 0 : -shiftX;
  for (int j = yTop; j < yBottom; j++)
    {
      int off = xLeft + j * width;
      int targetOff = xLeft + shiftX + (j + shiftY) * target.getWidth ();
      for (int i = xLeft; i < xRight; i++, off++, targetOff++)
        {
          int color1 = getData ()[off];
          int alpha1 = (color1 >> 24) & 0xFF;

          if (alpha1 == 0)
            continue;

          int color2 = target.getData ()[targetOff];
          int alpha2 = (color2 >> 24) & 0xFF;
          if (alpha1 == 255 || alpha2 == 0)
            {
              target.getData ()[targetOff] = color1;
              continue;
            }

          if (alpha2 == 255)
            {
              int invAlpha = 255 - alpha1;
              target.getData ()[targetOff] = 0xFF000000
                      | (CPTables.getRef ().divideBy255[((color1 >> 16 & 0xff) * alpha1 + (color2 >> 16 & 0xff)
                      * invAlpha)]) << 16
                      | (CPTables.getRef ().divideBy255[((color1 >> 8 & 0xff) * alpha1 + (color2 >> 8 & 0xff)
                      * invAlpha)]) << 8
                      | (CPTables.getRef ().divideBy255[(color1 & 0xff) * alpha1 + (color2 & 0xff)
                      * invAlpha]);
              continue;
            }

          int newAlpha = alpha1 + alpha2 - CPTables.getRef ().divideBy255[alpha1 * alpha2];
          int realAlpha = CPTables.getRef ().divide[alpha1 * 255 * 256 + newAlpha];
          int invAlpha = 255 - realAlpha;

          target.getData ()[targetOff] = newAlpha << 24
                  | (CPTables.getRef ().divideBy255[((color1 >> 16 & 0xff) * realAlpha + (color2 >> 16 & 0xff)
                  * invAlpha)]) << 16
                  | (CPTables.getRef ().divideBy255[((color1 >> 8 & 0xff) * realAlpha + (color2 >> 8 & 0xff)
                  * invAlpha)]) << 8
                  | (CPTables.getRef ().divideBy255[(color1 & 0xff) * realAlpha + (color2 & 0xff)
                  * invAlpha]);
        }
    }
}

public void copyDataFromSelectedPart (CPColorBmp bmp, CPSelection selection)
{
  CPRect rect = selection.getBoundingRect ();
  if (width != rect.getWidth () || height != rect.getHeight ())
    {
      width = rect.getWidth ();
      height = rect.getHeight ();
      data = new int[width * height];
    }

  for (int j = 0; j < height; j++)
    {
      int offSource = (rect.getTop () + j) * bmp.getWidth () + rect.getLeft ();
      int offDest = j * width;
      for (int i = 0; i < width; i++, offSource++, offDest++)
        {
          int selValue = selection.getData ()[offSource] & 0xFF;
          int alphaValue = bmp.getData ()[offSource] >>> 24;
          if (selValue < alphaValue)
            alphaValue = selValue;
          data[offDest] = (bmp.getData ()[offSource] & 0x00FFFFFF) | (alphaValue << 24);
        }
    }
}

abstract class bmpRectIterator
{
  bmpRectIterator (CPRect rect)
  {
    CPRect finalRect = new CPRect (width, height);
    finalRect.clip (rect);
    int off;
    for (int j = finalRect.top; j < finalRect.bottom; j++)
      {
        off = j * getWidth () + finalRect.left;
        for (int i = finalRect.left; i < finalRect.right; i++, off++)
          {
            modify (i, j, off);
          }
      }
  }

  abstract void modify (int i, int j, int off);
}

public void drawRectangle (CPRect rect, final int color, final boolean xor)
{
  new bmpRectIterator (rect)
  {
    @Override
    void modify (int i, int j, int off)
    {
      if (xor)
        getData ()[off] = getData ()[off] ^ color;
      else
        getData ()[off] = color;
    }
  };
}


//
// Flood fill algorithm
//

private static boolean areColorsNear (int color_1, int color_2, int distance)
{
  int[] dist = new int[4];
  for (int i = 0; i < 4; i++)
    {
      dist[i] = (Math.abs ((color_1 & 0xFF) - (color_2 & 0xFF)));
      color_1 = color_1 >> 8;
      color_2 = color_2 >> 8;
    }
  return Math.max (dist[3] * 3, (dist[0] + dist[1] + dist[2])) <= distance * 3;
}

private static boolean areColorsNearAlpha (int color_1, int color_2, int distance)
{
  return Math.abs (((color_1 >> 24) & 0xFF) - ((color_2 >> 24) & 0xFF)) <= distance;

}

static public void floodFill (int xArg, int yArg, final CPLayer useDataFrom, final int colorDistance, final CPLayer destination, final int destinationColor, final CPSelection selection)
{
  if (!useDataFrom.isInside (xArg, yArg))
    {
      return;
    }

  final int oldColor;
  int width = useDataFrom.getWidth ();
  int height = useDataFrom.getHeight ();
  int offset;
  boolean checkOnlyAlpha;
  int destinationColorWithoutAlpha = destinationColor & 0xFFFFFF;

  if ((useDataFrom.getPixel (xArg, yArg) & 0xff000000) == 0)
    {
      oldColor = 0;
      checkOnlyAlpha = true;
    }
  else
    {
      oldColor = useDataFrom.getPixel (xArg, yArg);
      checkOnlyAlpha = false;
    }

  CPIfaces.IntChecker shouldWeFill;
  if (selection == null)
    {
      if (checkOnlyAlpha)
        {
          shouldWeFill = new CPIfaces.IntChecker ()
          {
            int counter = 0;

            public boolean check (int arg)
            {
              counter++;
              return areColorsNearAlpha (useDataFrom.getData ()[arg], oldColor, colorDistance) && (destination.getData ()[arg] == 0);
            }
          };
        }
      else
        {
          shouldWeFill = new CPIfaces.IntChecker ()
          {
            public boolean check (int arg)
            {
              return areColorsNear (useDataFrom.getData ()[arg], oldColor, colorDistance) && (destination.getData ()[arg] == 0);
            }
          };
        }
    }
  else
    {
      if (checkOnlyAlpha)
        {
          shouldWeFill = new CPIfaces.IntChecker ()
          {
            public boolean check (int arg)
            {
              return areColorsNearAlpha (useDataFrom.getData ()[arg], oldColor, colorDistance) && (destination.getData ()[arg] == 0) && (selection.getData ()[arg] != 0);
            }
          };
        }
      else
        {
          shouldWeFill = new CPIfaces.IntChecker ()
          {
            public boolean check (int arg)
            {
              return areColorsNear (useDataFrom.getData ()[arg], oldColor, colorDistance) && (destination.getData ()[arg] == 0) && (selection.getData ()[arg] != 0);
            }
          };
        }
    }

  if (!shouldWeFill.check (yArg * width + xArg))
    return;

  TIntArrayStack S = new TIntArrayStack ();
  // to go Bottom
  if (yArg < height - 1 && shouldWeFill.check ((yArg + 1) * width + xArg))
    {
      S.push (xArg);
      S.push (xArg);
      S.push (yArg + 1);
      S.push (1);
    }
  // to go top
  S.push (xArg);
  S.push (xArg);
  S.push (yArg);
  S.push (-1);

  // TODO: Reduce code duplication
  while (S.size () != 0)
    {
      int dir = S.pop ();
      int y = S.pop ();
      int xr = S.pop ();
      int xl = S.pop ();
      int xlOld = xl;
      int xrOld = xr;
      offset = y * width + xl;
      int newX = -1;
      int offsetLower = offset - dir * width;
      xl--;
      offset--;
      offsetLower--;
      boolean yExpansion = (y - dir >= 0 && y - dir < height);
      while (xl >= 0 && shouldWeFill.check (offset))
        {
          if (selection == null)
            destination.getData ()[offset] = destinationColor;
          else
            destination.getData ()[offset] = destinationColorWithoutAlpha | selection.getData ()[offset] << 24;
          if (yExpansion && shouldWeFill.check (offsetLower))
            {
              if (newX == -1)
                newX = xl;
            }
          else
            {
              if (newX != -1)
                {
                  S.push (xl + 1);
                  S.push (newX);
                  S.push (y - dir);
                  S.push (-dir);
                }
            }
          xl--;
          offset--;
          offsetLower--;
        }

      if (newX != -1)
        {
          S.push (xl + 1);
          S.push (newX);
          S.push (y - dir);
          S.push (-dir);
          newX = -1;
        }
      xl++;

      offset = y * width + xr;
      offsetLower = offset - dir * width;
      newX = -1;
      xr++;
      offset++;
      offsetLower++;
      while (xr < width && shouldWeFill.check (offset))
        {
          if (selection == null)
            destination.getData ()[offset] = destinationColor;
          else
            destination.getData ()[offset] = destinationColorWithoutAlpha | selection.getData ()[offset] << 24;
          if (yExpansion && shouldWeFill.check (offsetLower))
            {
              if (newX == -1)
                newX = xr;
            }
          else
            {
              if (newX != -1)
                {
                  S.push (newX);
                  S.push (xr - 1);
                  S.push (y - dir);
                  S.push (-dir);
                }
            }
          xr++;
          offset++;
          offsetLower++;
        }

      if (newX != -1)
        {
          S.push (newX);
          S.push (xr - 1);
          S.push (y - dir);
          S.push (-dir);
          newX = -1;
        }
      xr--;

      yExpansion = (y + dir >= 0 && y + dir < height);
      offset = y * width + xl;
      int offsetHigher = offset + dir * width;
      newX = -1;
      for (int i = xl; i <= xr; i++, offset++, offsetHigher++)
        {
          // all of our interval should be filled, we know it
          if (i >= xlOld && i <= xrOld)
            {
              if (selection == null)
                destination.getData ()[offset] = destinationColor;
              else
                destination.getData ()[offset] = destinationColorWithoutAlpha | selection.getData ()[offset] << 24;
            }
          if (yExpansion && shouldWeFill.check (offsetHigher))
            {
              if (newX == -1)
                newX = i;
            }
          else
            {
              if (newX != -1)
                {
                  S.push (newX);
                  S.push (i - 1);
                  S.push (y + dir);
                  S.push (dir);
                  newX = -1;
                }
            }
        }
      if (newX != -1)
        {
          S.push (newX);
          S.push (xr);
          S.push (y + dir);
          S.push (dir);
        }

    }

}

//
// Box Blur algorithm
//

public void boxBlur (CPRect r, int radiusX, int radiusY)
{
  CPRect rect = new CPRect (0, 0, width, height);
  rect.clip (r);

  int w = rect.getWidth ();
  int h = rect.getHeight ();
  int l = Math.max (w, h);

  int[] src = new int[l];
  int[] dst = new int[l];

  for (int j = rect.top; j < rect.bottom; j++)
    {
      System.arraycopy (getData (), rect.left + j * width, src, 0, w);
      multiplyAlpha (src, w);
      boxBlurLine (src, dst, w, radiusX);
      System.arraycopy (dst, 0, getData (), rect.left + j * width, w);
    }

  for (int i = rect.left; i < rect.right; i++)
    {
      copyColumnToArray (i, rect.top, h, src);
      boxBlurLine (src, dst, h, radiusY);
      separateAlpha (dst, h);
      copyArrayToColumn (i, rect.top, h, dst);
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

private static void separateAlpha (int[] buffer, int len)
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

private static void boxBlurLine (int[] src, int dst[], int len, int radius)
{
  int s, ta, tr, tg, tb;
  s = ta = tr = tg = tb = 0;
  int pix;

  for (int i = 0; i < radius && i <= len; i++)
    {
      pix = src[i];
      ta += pix >>> 24;
      tr += (pix >>> 16) & 0xff;
      tg += (pix >>> 8) & 0xff;
      tb += pix & 0xff;
      s++;
    }
  for (int i = 0; i < len; i++)
    {
      if (i + radius < len)
        {
          pix = src[i + radius];
          ta += pix >>> 24;
          tr += (pix >>> 16) & 0xff;
          tg += (pix >>> 8) & 0xff;
          tb += pix & 0xff;
          s++;
        }

      dst[i] = (ta / s << 24) | (tr / s << 16) | (tg / s << 8) | tb / s;

      if (i - radius >= 0)
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

void copyColumnToArray (int x, int y, int len, int[] buffer)
{
  for (int i = 0; i < len; i++)
    {
      buffer[i] = getData ()[x + (i + y) * width];
    }
}

void copyArrayToColumn (int x, int y, int len, int[] buffer)
{
  for (int i = 0; i < len; i++)
    {
      getData ()[x + (i + y) * width] = buffer[i];
    }
}

public CPRect getBoundingBox ()
{
  CPRect rect;
  int minX = width, maxX = 0, minY = height, maxY = 0;
  for (int i = 0; i < width; i++)
    for (int j = 0; j < height; j++)
      {
        if ((data[j * width + i] & 0xFF000000) != 0)
          {
            minX = Math.min (minX, i);
            maxX = Math.max (maxX, i);
            minY = Math.min (minY, j);
            maxY = Math.max (maxY, j);
          }
      }
  return new CPRect (minX, minY, maxX, maxY);
}

}
