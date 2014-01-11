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

package chibipaint.file;

import chibipaint.engine.CPArtwork;
import chibipaint.engine.CPLayer;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public class CPXcfFile extends CPAbstractFile
{
private class ByteArrayOutputStreamWithSeek extends ByteArrayOutputStream
{
  public ByteArrayOutputStreamWithSeek ()
  {
    super ();
  }

  void seek (int pos)
  {
    if (pos > buf.length)
      {
        byte newbuf[] = new byte[Math.max (buf.length << 1, pos)];
        System.arraycopy (buf, 0, newbuf, 0, count);
        buf = newbuf;
      }
    count = pos; // Looking to source of ByteArrayOutputStream hints that in this case everything will work fine
    // I mean buffer just be rewritten in that position
  }
}

private static final byte[] gimp_xcf_ = {'g', 'i', 'm', 'p', ' ', 'x', 'c', 'f', ' '};
private static final byte[] v0 = {'f', 'i', 'l', 'e'};
private static final byte[] v1 = {'v', '0', '0', '1'};
private static final byte[] v2 = {'v', '0', '0', '2'};
private static final byte[] v3 = {'v', '0', '0', '3'};

// Consts for properties:
private static final int GIMP_CONST_END = 0;
private static final int GIMP_CONST_ACTIVE_LAYER = 2;
private static final int GIMP_CONST_OPACITY = 6;
private static final int GIMP_CONST_MODE = 7;
private static final int GIMP_CONST_VISIBLE = 8;
private static final int GIMP_CONST_OFFSETS = 15;

// GIMP Layer Modes:
private static final int GIMP_NORMAL = 0;
private static final int GIMP_MULTIPLY = 3;
private static final int GIMP_SCREEN = 4;
private static final int GIMP_OVERLAY = 5;
private static final int GIMP_ADD = 7;
private static final int GIMP_SUBTRACT = 8;
private static final int GIMP_DARKEN_ONLY = 9;
private static final int GIMP_LIGHTEN_ONLY = 10;
private static final int GIMP_DODGE = 16;
private static final int GIMP_BURN = 17;
private static final int GIMP_HARD_LIGHT = 18;
private static final int GIMP_SOFT_LIGHT = 19;

// GIMP Color Types:
private static final int COLOR_TYPE_RGB = 0;
private static final int COLOR_TYPE_RGBA = 1;
private static final int COLOR_TYPE_G = 2;
private static final int COLOR_TYPE_GA = 3;

private static final int BUFFER_SIZE = 4 * 1024 * 1024;

private static byte getByte (int x, int pos)
{
  return (byte) ((x >> pos) & 0xFF);
}

private static boolean isInBounds (int[] pos, int w, int h)
{
  return (pos[0] >= 0 && pos[0] < w && pos[1] >= 0 && pos[1] < h);
}

private static int minVersion (CPArtwork a)
{
  for (int i = 0; i < a.getLayersVector ().size (); i++)
    if (a.getLayersVector ().get (i).getBlendMode () == CPLayer.LM_SOFTLIGHT)
      return 2;
  return 0;
}

private static void movePos (int[] curPos, int[] start, int[] limit)
{
  curPos[0]++;
  if (curPos[0] >= limit[0])
    {
      curPos[0] = start[0];
      curPos[1]++;
    }
}

public static int position (InputStream is) throws IOException
{
  if (is instanceof FileInputStream)
    return (int) ((FileInputStream) is).getChannel ().position ();
  else
    throw new IOException ("Seeking isn't supported for this kind of Input Stream");
}

private static int position (OutputStream os) throws IOException
{
  if (os instanceof FileOutputStream)
    return (int) ((FileOutputStream) os).getChannel ().position ();
  else if (os instanceof ByteArrayOutputStream)
    return ((ByteArrayOutputStream) os).size ();
  else
    throw new IOException ("Seeking isn't supported for this kind of Ouput Stream");
}

static public float readFloat (InputStream is) throws IOException
{
  int data;
  data = readInt (is);
  return Float.intBitsToFloat (data);
}

private static int readInt (InputStream is) throws IOException
{
  return is.read () << 24 | is.read () << 16 | is.read () << 8 | is.read ();
}

// End of todo

private static boolean readLayer (
        InputStream is, int offset, CPLayer layer,
        int compression) throws IOException
{
  setPosition (is, offset); // Moving to the position of layer
  // data
  int layerWidth, layerHeight; // There's a problem if layer size
  // different from image
  int offsetX = 0, offsetY = 0;
  // Then we'll try to render what's visible
  int colorType;
  layerWidth = readInt (is);
  layerHeight = readInt (is);
  // Well for now we're not parsing indexed, others should be parsed
  // though
  colorType = readInt (is);
  if (colorType > 3)
    return false; // In that case layer will be empty, TODO:support
  // indexed layers

  layer.setName (readString (is));

  // Layer properties, big reading cycle actually
  int paramID;
  int payloadSize;
  outerCycle:
  while (true)
    {
      paramID = readInt (is);
      switch (paramID)
        {
        case GIMP_CONST_END:
          payloadSize = readInt (is); // should be zero
          break outerCycle;
        case GIMP_CONST_MODE:
        {
          payloadSize = readInt (is);
          // ignoring payload size, should be 4 though
          int layerMode = readInt (is);
          switch (layerMode)
            {
            case GIMP_NORMAL:
              layer.setBlendMode (CPLayer.LM_NORMAL);
              break;
            case GIMP_MULTIPLY:
              layer.setBlendMode (CPLayer.LM_MULTIPLY);
              break;
            case GIMP_OVERLAY:
              layer.setBlendMode (CPLayer.LM_OVERLAY);
              break;
            case GIMP_SCREEN:
              layer.setBlendMode (CPLayer.LM_SCREEN);
              break;
            case GIMP_ADD:
              layer.setBlendMode (CPLayer.LM_ADD);
              break;
            case GIMP_SUBTRACT:
              layer.setBlendMode (CPLayer.LM_SUBTRACT);
              break;
            case GIMP_DARKEN_ONLY:
              layer.setBlendMode (CPLayer.LM_DARKEN);
              break;
            case GIMP_LIGHTEN_ONLY:
              layer.setBlendMode (CPLayer.LM_LIGHTEN);
              break;
            case GIMP_BURN:
              layer.setBlendMode (CPLayer.LM_BURN);
              break;
            case GIMP_DODGE:
              layer.setBlendMode (CPLayer.LM_DODGE);
              break;
            case GIMP_HARD_LIGHT:
              layer.setBlendMode (CPLayer.LM_HARDLIGHT);
              break;
            case GIMP_SOFT_LIGHT:
              layer.setBlendMode (CPLayer.LM_SOFTLIGHT);
              break;
            default:
              layer.setBlendMode (CPLayer.LM_NORMAL);
            }
          break;
        }
        case GIMP_CONST_OFFSETS:
          payloadSize = readInt (is); // Should be 8
          offsetX = readInt (is);
          offsetY = readInt (is);
          break;
        case GIMP_CONST_OPACITY:
          payloadSize = readInt (is); // Should be 4
          layer.setAlpha (readInt (is) * 100 / 255);
          break;
        case GIMP_CONST_VISIBLE:
          payloadSize = readInt (is); // Should be 4
          layer.setVisible ((readInt (is) != 0));
          break;
        default:
        {
          payloadSize = readInt (is);
          // Then we're just skipping payload size of bytes
          for (int i = 0; i < payloadSize; i++)
            is.read ();
        }
        }
    }

  int pixelDataOffset = readInt (is);
  readInt (is); // Mask data, for now just skipping

  setPosition (is, pixelDataOffset);
  if (readInt (is) != layerWidth)
    return false;
  if (readInt (is) != layerHeight)
    return false;
  int bytesPerPixel = readInt (is); // Bytes per pixel
  // Just for the sake of it checking for consistency
  if (colorType == COLOR_TYPE_RGB && bytesPerPixel != 3)
    return false;
  if (colorType == COLOR_TYPE_RGBA && bytesPerPixel != 4)
    return false;
  if (colorType == COLOR_TYPE_G && bytesPerPixel != 1)
    return false;
  if (colorType == COLOR_TYPE_GA && bytesPerPixel != 2)
    return false;

  int levelOffset;
  levelOffset = readInt (is);
  while (true) // Skipping rest of the shit
    {
      int dummy = readInt (is);
      if (dummy == 0)
        break;
    }
  setPosition (is, levelOffset);
  // and once again useless height and width and so consistency check
  if (readInt (is) != layerWidth)
    return false;
  if (readInt (is) != layerHeight)
    return false;

  // Now there goes some pointers for tile data
  int wTiles = (int) (Math.ceil ((double) layerWidth / 64));
  int hTiles = (int) (Math.ceil ((double) layerHeight / 64));
  int numberOfTiles = wTiles * hTiles;
  int[] tilePointers = new int[numberOfTiles]; // Well there should be
  // fixed definite number
  // of tiles
  for (int i = 0; i < numberOfTiles; i++)
    {
      tilePointers[i] = readInt (is);
      if (tilePointers[i] == 0) // If there's some bullshit
        return false;
    }

  if (readInt (is) != 0) // Consistency check for terminating zero
    return false;
  // Then we're reading actual tiles
  for (int i = 0; i < hTiles; i++)
    // outer loop is vertical one
    for (int j = 0; j < wTiles; j++)
      {
        setPosition (is, tilePointers[i * wTiles + j]);
        if (compression == 1)
          // warning offset should be taken into account correctly and
          // pixels out of bound should be cutSelected
          try
            {
              readTileRLE (is, layer.getData (), j, i, layerWidth,
                           layerHeight, layer.getWidth (), layer.getHeight (), offsetX, offsetY, colorType, colorType == COLOR_TYPE_RGB || colorType == COLOR_TYPE_G); // reading
              // actual
              // tile
              // Info
            }
          catch (Exception e)
            {
              return false;
            }
        else
          {
            // For now disabled TODO:make ability to read uncompressed
            // xcf
          }
      }
  return true;
}

private static boolean readMagic (InputStream is) throws IOException
{
  byte[] buf = new byte[gimp_xcf_.length];
  is.read (buf, 0, buf.length);
  return Arrays.equals (buf, gimp_xcf_);
}

// We pay any attention only to compression that's why returning it.
private static int readProperties (InputStream is) throws IOException
{
  int paramID;
  int compressionType = 1; // RLE default anyway
  int payloadSize;
  while (true)
    {
      paramID = readInt (is);
      switch (paramID)
        {
        case 0:
          payloadSize = readInt (is); // should be 0
          return compressionType;
        case 17:
        {
          payloadSize = readInt (is);
          // ignoring payload size, should be 1 though
          compressionType = is.read ();
          break;
        }
        default:
        {
          payloadSize = readInt (is);
          // Then we're just skipping payload size of bytes
          for (int i = 0; i < payloadSize; i++)
            is.read ();
        }
        }
    }
}

private static String readString (InputStream is) throws IOException
{
  int length = readInt (is); // size of bytes + 1 for terminating byte
  byte[] data = new byte[length];
  is.read (data, 0, length);
  return new String (data, "UTF-8");
}

private static void readTileRLE (
        InputStream is, int[] data, int x,
        int y, int w, int h, int dstW, int dstH, int ox, int oy, int colorType, boolean fillAlpha)
        throws IOException
{
  int sizeX = (x + 1) * 64 <= w ? 64 : w - x * 64;
  int sizeY = (y + 1) * 64 <= h ? 64 : h - y * 64;
  int byteNum = 4;
  switch (colorType)
    {
    case COLOR_TYPE_RGB:
      byteNum = 3;
      break;
    case COLOR_TYPE_RGBA:
      byteNum = 4;
      break;
    case COLOR_TYPE_GA:
      byteNum = 2;
      break;
    case COLOR_TYPE_G:
      byteNum = 1;
      break;
    }
  int[] bytePos = new int[byteNum];
  // -1 means all 3 rgb positions (0, 1, 2)
  // Here are bytes in int are measured in offsets from zero ie 3210,
  // somewhere upper it was vice versa
  switch (colorType)
    {
    case COLOR_TYPE_RGB:
      bytePos[0] = 16;
      bytePos[1] = 8;
      bytePos[2] = 0;
      break;
    case COLOR_TYPE_RGBA:
      bytePos[0] = 16;
      bytePos[1] = 8;
      bytePos[2] = 0;
      bytePos[3] = 24;
      break;
    case COLOR_TYPE_GA:
      bytePos[0] = -1;
      bytePos[1] = 24;
      break;
    case COLOR_TYPE_G:
      bytePos[0] = -1;
      break;
    }
  int[] startPos = {ox + x * 64, oy + y * 64};
  if (startPos[0] >= dstW || startPos[1] >= dstH)
    return; // Optimizing: if tile is entirely out of picture, do nothing
  int[] limit = {startPos[0] + sizeX, startPos[1] + sizeY};
  if (limit[0] < 0 || limit[1] < 0)
    return;  // Optimizing: if tile is entirely out of picture, do nothing
  int[] curPos = new int[2];
  int longRunNumber = 0;
  int leadingByte = 0;
  int fillingByte = 0;
  int curByte = 0;

  if (fillAlpha)
    {
      for (int i = startPos[0]; i < limit[0]; i++)
        for (int j = startPos[1]; j < limit[1]; j++)
          {
            curPos[0] = i;
            curPos[1] = j;
            if (isInBounds (curPos, dstW, dstH))
              {
                data[j * dstW + i] = transformImageInt (
                        data[j * dstW + i], 0xFF,
                        24);
              }
          }
    }

  for (int i = 0; i < byteNum; i++)
    {
      curPos = startPos.clone ();
      while (true)
        {
          leadingByte = is.read ();
          if (leadingByte <= 126)
            {
              // Repeating next byte (leadingByte + 1) times
              fillingByte = is.read ();
              for (int j = 0; j < leadingByte + 1; j++)
                {
                  if (isInBounds (curPos, dstW, dstH))
                    {
                      data[curPos[1] * dstW + curPos[0]] = transformImageInt (
                              data[curPos[1] * dstW + curPos[0]], fillingByte,
                              bytePos[i]);
                    }
                  movePos (curPos, startPos, limit);
                }
            }
          else if (leadingByte >= 129)
            {
              // Reading (256 - leadingByte) different bytes
              for (int j = 0; j < 256 - leadingByte; j++)
                {
                  curByte = is.read ();
                  if (isInBounds (curPos, dstW, dstH))
                    {
                      data[curPos[1] * dstW + curPos[0]] = transformImageInt (
                              data[curPos[1] * dstW + curPos[0]], curByte,
                              bytePos[i]);
                    }
                  movePos (curPos, startPos, limit);
                }
            }
          else
            {
              // Reading long run number
              longRunNumber = (is.read () << 8) | is.read ();
              if (leadingByte == 127)
                fillingByte = is.read ();
              // Either long different or same bytes sequence
              for (int j = 0; j < longRunNumber; j++)
                {
                  if (leadingByte == 128)
                    curByte = is.read ();
                  if (isInBounds (curPos, dstW, dstH))
                    {
                      data[curPos[1] * dstW + curPos[0]] = transformImageInt (
                              data[curPos[1] * dstW + curPos[0]],
                              (leadingByte == 127) ? fillingByte : curByte,
                              bytePos[i]);
                    }
                  movePos (curPos, startPos, limit);
                }
            }
          if (curPos[1] >= limit[1])
            break;
        }
    }
}

private static boolean readVersion (InputStream is) throws IOException
{
  byte[] b = new byte[4];
  is.read (b, 0, b.length);

  if (!Arrays.equals (b, v0) && !Arrays.equals (b, v1)
          && !Arrays.equals (b, v2) && !Arrays.equals (b, v3))
    return false; // If something wrong is written we are still quit,
  // though
  // version doesn't mean anything in our case
  is.read (); // Eat 0 terminating byte
  return true;
}

private static void setPosition (InputStream is, int position) throws IOException
{
  if (is instanceof FileInputStream)
    ((FileInputStream) is).getChannel ().position (position);
  if (is instanceof ByteArrayInputStream)
    {
      is.reset ();
      is.skip (position);
    }
  else
    throw new IOException ("Seeking isn't supported for this kind of Input Stream");
}

private static void setPosition (OutputStream os, int position) throws IOException
{
  if (os instanceof FileOutputStream)
    ((FileOutputStream) os).getChannel ().position (position);
  else if (os instanceof ByteArrayOutputStreamWithSeek)
    {
      ((ByteArrayOutputStreamWithSeek) os).seek (position);
    }
  else
    throw new IOException ("Seeking isn't supported for this kind of Ouput Stream");
}

private static int transformImageInt (int data_arg, int byte_arg, int pos)
{
  int data = data_arg;
  if (pos != -1)
    {
      data = data & (~(0xFF << pos)); // generating something like
      // 0x0..0FF0..0
      data = data | (byte_arg << pos);
    }
  else
    {
      data &= 0xFF000000;
      data = data | byte_arg | (byte_arg << 8) | (byte_arg << 16);
    }
  return data;
}

private static void writeFloat (OutputStream os, float f)
        throws IOException
{
  writeInt (os, Float.floatToRawIntBits (f));
}

// TODO:These functions should be outside of these classes:
private static void writeInt (OutputStream os, int i) throws IOException
{
  byte[] temp = {(byte) (i >>> 24), (byte) ((i >>> 16) & 0xff),
          (byte) ((i >>> 8) & 0xff), (byte) (i & 0xff)};
  os.write (temp);
}

private static void writeLayer (
        OutputStream os, CPLayer layer,
        boolean isActive) throws IOException
{
  writeInt (os, layer.getWidth ()); // layer width // Well in our case they
  // are the as picture all the time
  writeInt (os, layer.getHeight ()); // layer height
  writeInt (os, 1); // layer type, 1 - means 24 bit color with alpha
  writeString (os, layer.getName ()); // layer name
  // Layer properties

  // Is Layer Active
  if (isActive) // TODO:implement this stuff in chi also (very convinient)
    {
      writeInt (os, GIMP_CONST_ACTIVE_LAYER);
      writeInt (os, 0); // empty payload
    }

  // Layer Mode
  writeInt (os, GIMP_CONST_MODE); // PROP_MODE
  writeInt (os, 4); // payload size, 1 int
  switch (layer.getBlendMode ())
    {
    case CPLayer.LM_NORMAL:
      writeInt (os, GIMP_NORMAL);
      break;

    case CPLayer.LM_MULTIPLY:
      writeInt (os, GIMP_MULTIPLY);
      break;

    case CPLayer.LM_ADD:
      writeInt (os, GIMP_ADD);
      break;

    case CPLayer.LM_SCREEN:
      writeInt (os, GIMP_SCREEN);
      break;

    case CPLayer.LM_LIGHTEN:
      writeInt (os, GIMP_LIGHTEN_ONLY);
      break;

    case CPLayer.LM_DARKEN:
      writeInt (os, GIMP_DARKEN_ONLY);
      break;

    case CPLayer.LM_SUBTRACT:
      writeInt (os, GIMP_SUBTRACT);
      break;

    case CPLayer.LM_DODGE:
      writeInt (os, GIMP_DODGE);
      break;

    case CPLayer.LM_BURN:
      writeInt (os, GIMP_BURN);
      break;

    case CPLayer.LM_OVERLAY:
      writeInt (os, GIMP_OVERLAY);
      break;

    case CPLayer.LM_HARDLIGHT:
      writeInt (os, GIMP_HARD_LIGHT);
      break;

    case CPLayer.LM_SOFTLIGHT:
      writeInt (os, GIMP_SOFT_LIGHT);
      break;
    // TODO: Three below looks like missing in gimp, maybe I should check
    // formulas to investigate it
    case CPLayer.LM_VIVIDLIGHT:
      writeInt (os, GIMP_HARD_LIGHT);
      break;

    case CPLayer.LM_LINEARLIGHT:
      writeInt (os, GIMP_HARD_LIGHT);
      break;

    case CPLayer.LM_PINLIGHT:
      writeInt (os, GIMP_HARD_LIGHT);
      break;
    }

  // Offsets in our case it's dummy 0s

  writeInt (os, GIMP_CONST_OFFSETS);
  writeInt (os, 8); // Two ints
  writeInt (os, 0);
  writeInt (os, 0);

  // Layer opacity:

  writeInt (os, GIMP_CONST_OPACITY);
  writeInt (os, 4); // one Int
  writeInt (os, layer.getAlpha () * 255 / 100);

  // Layer visibility:

  writeInt (os, GIMP_CONST_VISIBLE);
  writeInt (os, 4); // one Int
  writeInt (os, layer.isVisible () ? 1 : 0);

  writeInt (os, GIMP_CONST_END); // PROP_END
  writeInt (os, 0); // empty payload

  // Pixel Data
  writeInt (os, position (os) + 8); // After this and
  // 0 for mask
  // goes
  // hierarchy
  // structure

  // Mask (in our case 0)
  writeInt (os, 0);

  // Hierarchy Structure
  writeInt (os, layer.getWidth ()); // Once again width
  writeInt (os, layer.getHeight ()); // Once again height
  writeInt (os, 4); // Bytes per pixel
  // Now we need to calculate how many level structures will be there

  int curPos = position (os);
  writeInt (os, curPos + 4 + 4); // The pointer to structure beyond this
  // pointer and terminating zero
  writeInt (os, 0); // Terminating zero for levels

  // Actual level
  writeInt (os, layer.getWidth ());
  writeInt (os, layer.getHeight ());
  // Now there goes some pointers for tile data
  int wTiles = (int) (Math.ceil ((double) layer.getWidth () / 64));
  int hTiles = (int) (Math.ceil ((double) layer.getHeight () / 64));
  int numberOfTiles = wTiles * hTiles;
  int[] tilePointers = new int[numberOfTiles];
  int pointerPos = position (os); // Remembering
  // position to fill
  // it up later
  for (int i = 0; i < numberOfTiles; i++)
    writeInt (os, 0); // Just reserving places for future writing of
  // actual pointer

  writeInt (os, 0); // Terminating zero;
  // Then we're starting to write actual tiles
  for (int i = 0; i < hTiles; i++)
    // outer loop is vertical one
    for (int j = 0; j < wTiles; j++)
      {
        tilePointers[i * wTiles + j] = position (os);
        writeTileRLE (os, layer.getData (), j, i, layer.getWidth (),
                      layer.getHeight ()); // Writing actual tile Info
      }

  int actualPos = position (os);

  setPosition (os, pointerPos);
  for (int i = 0; i < numberOfTiles; i++)
    writeInt (os, tilePointers[i]);

  setPosition (os, actualPos);
}

private static void writeMagic (OutputStream os) throws IOException
{
  os.write (gimp_xcf_);
}

private static void writeProperties (OutputStream os) throws IOException
{
  // Compression, using default one - RLE
  writeInt (os, 17); // PROP_COMPRESSION
  writeInt (os, 1); // payload size
  os.write (1); // Means RLE

  writeInt (os, 19); // PROP_RESOLUTION
  writeInt (os, 8); // 2 Floats payload
  writeFloat (os, 72.0f);
  writeFloat (os, 72.0f);

  writeInt (os, 0); // PROP_END
  writeInt (os, 0); // empty payload
}

private static void writeString (OutputStream os, String s)
        throws IOException
{
  byte[] s_data = s.getBytes ("UTF-8"); // converting to utf-8
  writeInt (os, s_data.length + 1); // size of bytes + 1 for terminating
  // byte
  os.write (s_data);
  os.write (0); // null-terminating byte
}

private static void writeTileRLE (
        OutputStream os, int[] data, int x,
        int y, int w, int h) throws IOException
{
  int sizeX = (x + 1) * 64 <= w ? 64 : w - x * 64;
  int sizeY = (y + 1) * 64 <= h ? 64 : h - y * 64;
  int m = sizeX * sizeY;
  byte[] arr = new byte[m]; // uncompressed info
  int[] offsets = {16, 8, 0, 24}; // To write in RGBA instead of ARGB
  for (int p = 0; p < 4; p++)
    {
      int t = 0;
      for (int i = 0; i < sizeY; i++)
        for (int j = 0; j < sizeX; j++)
          {
            arr[t] = getByte (data[(y * 64 + i) * w + (x * 64 + j)],
                              offsets[p]);
            t++;
          }
      int curPos = 0;

      while (curPos < m)
        {
          // scanning for identical bytes
          if (curPos + 1 < m && arr[curPos] == arr[curPos + 1])
            {
              int pos = curPos + 2;
              while (pos < m && arr[pos] == arr[curPos])
                pos++;
              pos--; // getting back where we were still identical / in
              // array boundaries
              // writing (pos - curPos + 1) identical bytes, 2 cases
              if (pos - curPos <= 126) // short run of identical bytes
                {
                  os.write (pos - curPos); // -1, cause value will be
                  // repeated n+1 times
                  // actually
                  os.write (arr[curPos]);
                }
              else // long run of identical bytes
                {
                  os.write (127);
                  int count = pos - curPos + 1;
                  os.write (getByte (count, 8));
                  os.write (getByte (count, 0));
                  os.write (arr[curPos]);
                }
              curPos = pos + 1;
            }
          if (curPos >= m)
            break;
          // scanning for different bytes
          if (curPos + 1 >= m || arr[curPos] != arr[curPos + 1])
            {
              int pos = 0;
              if (curPos + 1 >= m)
                pos = m - 1;
              else
                {
                  pos = curPos + 1;
                  while (pos + 1 < m && arr[pos] != arr[pos + 1])
                    pos++;
                  if (pos != m - 1)
                    pos--;
                  // getting back where we were still different
                }

              // writing (pos - curPos + 1) different bytes, 2 cases
              if (pos - curPos + 1 <= 127) // short run of different bytes
                {
                  os.write (256 - (pos - curPos + 1));
                  for (int i = curPos; i <= pos; i++)
                    os.write (arr[i]);
                }
              else // long run of identical bytes
                {
                  os.write (128);
                  int count = pos - curPos + 1;
                  os.write (getByte (count, 8));
                  os.write (getByte (count, 0));
                  for (int i = curPos; i <= pos; i++)
                    os.write (arr[i]);
                }
              curPos = pos + 1;
            }
        }
    }
}

private static void writeVersion (OutputStream os, int minVersion)
        throws IOException
{
  switch (minVersion)
    {
    case 0:
      os.write (v0);
      break;
    case 2:
      os.write (v2);
      break;
    }
  os.write (0); // zero-terminator byte for version
}

@Override
public String ext ()
{
  return "XCF";
}

@Override
public FileNameExtensionFilter fileFilter ()
{
  return new FileNameExtensionFilter ("GIMP XCF Images (.xcf)", "xcf");
}

@Override
public boolean isNative ()
{
  return false;
}

@Override
public CPArtwork read (InputStream isArg)
{
  try
    {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream ();
      int nRead;
      byte[] data = new byte[BUFFER_SIZE];
      while ((nRead = isArg.read (data, 0, data.length)) != -1)
        {
          buffer.write (data, 0, nRead);
        }

      buffer.flush ();
      ByteArrayInputStream is = new ByteArrayInputStream (buffer.toByteArray ());


      if (!readMagic (is)) // Checking on initial magic
        return null;
      if (!readVersion (is))
        {
          // return null (For now ignore version)
        }
      int width, height;
      width = readInt (is);
      height = readInt (is);
      int colorScheme;
      colorScheme = readInt (is);
      if (colorScheme > 1) // RGB or grayscale
        return null;
      int compression = readProperties (is);
      CPArtwork a = new CPArtwork (width, height);
      a.getLayersVector ().remove (0); // TODO: pass argument to constructor
      // to not create layers

      // Now we're reading layer links, array of unknown size
      ArrayList<Integer> layerLinks = new ArrayList<Integer> ();
      while (true)
        {
          int l = readInt (is);
          if (l != 0)
            layerLinks.add (l);
          else
            break;
        }
      while (true) // Reading masks (for now just skipping...)
        {
          int l = readInt (is);
          if (l == 0)
            break;
        }
      int layersCount = layerLinks.size ();
      for (Integer layerLink : layerLinks)
        {
          CPLayer layer = new CPLayer (width, height);
          if (!readLayer (is, layerLink, layer,
                          compression))
            {
              is.close ();
              return null;
            }
          a.getLayersVector ().insertElementAt (layer, 0);
        }

      return a;
    }
  catch (IOException e)
    {
      // Well since this is internal function anyway
      return null;
    }
}

@Override
public boolean write (OutputStream osArg, CPArtwork a)
{
  try
    {
      ByteArrayOutputStreamWithSeek os = new ByteArrayOutputStreamWithSeek ();
      writeMagic (os);
      writeVersion (os, minVersion (a));
      writeInt (os, a.getWidth ());  // image width
      writeInt (os, a.getHeight ()); // image height
      writeInt (os, 0); // RGB Color - mode, other modes don't concern us
      // Image properties
      writeProperties (os);
      // LayerLinks
      int[] layerLinks = new int[a.getLayersVector ().size ()];
      int layerLinksPoisiton = position (os);
      // dummy links to fill later
      for (int i = 0; i < a.getLayersVector ().size (); i++)
        writeInt (os, 0);
      writeInt (os, 0); // Terminator int for layers
      // Channels (?)
      writeInt (os, 0); // Terminator int for channels
      // Layers
      for (int i = 0; i < a.getLayersVector ().size (); i++)
        {
          layerLinks[i] = position (os);
          int usedLayer = a.getLayersVector ().size () - 1 - i; // Since layers are reversed in xcf
          writeLayer (os, a.getLayersVector ().get (usedLayer), a.getActiveLayerNum () == usedLayer);
        }

      int actualPosition = position (os);
      setPosition (os, layerLinksPoisiton);
      for (int i = 0; i < a.getLayersVector ().size (); i++)
        writeInt (os, layerLinks[i]);

      setPosition (os, actualPosition);

      os.writeTo (osArg);

      os.close ();
      return true;
    }
  catch (IOException e)
    {
      return false;
    }
}
}
