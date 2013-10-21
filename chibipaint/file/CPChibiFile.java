/*
	ChibiPaintMod
   Copyright (c) 2012-2013 Sergey Semushin
   Copyright (c) 2006-2008 Marc Schefer

    This file is part of ChibiPaintMod (previously ChibiPaint).

    ChibiPaintMod is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    ChibiPaintMod is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with ChibiPaintMod. If not, see <http://www.gnu.org/licenses/>.

 */

package chibipaint.file;

import chibipaint.engine.CPArtwork;
import chibipaint.engine.CPLayer;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class CPChibiFile extends CPAbstractFile
{

private static final byte[] CHIB = {67, 72, 73, 66};
private static final byte[] IOEK = {73, 79, 69, 75};
private static final byte[] HEAD = {72, 69, 65, 68};
private static final byte[] LAYR = {76, 65, 89, 82};
private static final byte[] ZEND = {90, 69, 78, 68};

@Override
public boolean isNative ()
{
  return true; // One and only
}

@Override
public FileNameExtensionFilter fileFilter ()
{
  return new FileNameExtensionFilter ("ChibiPaintMod Files(*.chi)", "chi");
}

@Override
public boolean write (OutputStream os, CPArtwork a)
{
  try
    {
      writeMagic (os);
      os.flush ();

      Deflater def = new Deflater (7);
      DeflaterOutputStream dos = new DeflaterOutputStream (os, def);
      // OutputStream dos = os;

      writeHeader (dos, a);

      for (Object l : a.getLayersVector ())
        {
          writeLayer (dos, (CPLayer) l);
        }

      writeEnd (dos);

      dos.flush ();
      dos.close ();
      return true;
    }
  catch (IOException e)
    {
      return false;
    }
}

private static void writeInt (OutputStream os, int i) throws IOException
{
  byte[] temp = {(byte) (i >>> 24), (byte) ((i >>> 16) & 0xff), (byte) ((i >>> 8) & 0xff), (byte) (i & 0xff)};
  os.write (temp);
}

private static void writeIntArray (OutputStream os, int arr[]) throws IOException
{
  byte[] temp = new byte[arr.length * 4];
  int idx = 0;
  for (int i : arr)
    {
      temp[idx++] = (byte) (i >>> 24);
      temp[idx++] = (byte) ((i >>> 16) & 0xff);
      temp[idx++] = (byte) ((i >>> 8) & 0xff);
      temp[idx++] = (byte) (i & 0xff);
    }

  os.write (temp);
}

private static void writeMagic (OutputStream os) throws IOException
{
  os.write (CHIB);
  os.write (IOEK);
}

private static void writeEnd (OutputStream os) throws IOException
{
  os.write (ZEND);
  writeInt (os, 0);
}

private static void writeHeader (OutputStream os, CPArtwork a) throws IOException
{
  os.write (HEAD); // Chunk ID
  writeInt (os, 16); // ChunkSize

  writeInt (os, 0); // Current Version: Major: 0 Minor: 0
  writeInt (os, a.getWidth ());
  writeInt (os, a.getHeight ());
  writeInt (os, a.getLayersNb ());
}

private static void writeLayer (OutputStream os, CPLayer l) throws IOException
{
  byte[] title = l.getName ().getBytes ("UTF-8");

  os.write (LAYR); // Chunk ID
  writeInt (os, 20 + l.getData ().length * 4 + title.length); // ChunkSize

  writeInt (os, 20 + title.length); // Data offset from start of header
  writeInt (os, l.getBlendMode ()); // layer blend mode
  writeInt (os, l.getAlpha ()); // layer opacity
  writeInt (os, l.isVisible () ? 1 : 0); // layer visibility and future flags

  writeInt (os, title.length);
  os.write (title);

  writeIntArray (os, l.getData ());
}

@Override
public CPArtwork read (InputStream is)
{  try

    {
      if (!readMagic (is))
        {
          return null; // not a ChibiPaintMod file
        }

      InflaterInputStream iis = new InflaterInputStream (is);
      CPChibiChunk chunk = new CPChibiChunk (iis);
      if (!chunk.is (HEAD))
        {
          iis.close ();
          return null; // not a valid file
        }

      CPChibiHeader header = new CPChibiHeader (iis, chunk);
      if ((header.version >>> 16) > 0)
        {
          iis.close ();
          return null; // the file version is higher than what we can deal with, bail out
        }

      CPArtwork a = new CPArtwork (header.width, header.height);
      a.getLayersVector ().remove (0); // FIXME: it would be better not to have created it in the first place

      while (true)
        {
          chunk = new CPChibiChunk (iis);

          if (chunk.is (ZEND))
            {
              break;
            }
          else if (chunk.is (LAYR))
            {
              readLayer (iis, chunk, a);
            }
          else
            {
              realSkip (iis, chunk.chunkSize);
            }
        }

      a.setActiveLayer (0);
      iis.close ();
      return a;

    }
  catch (IOException e)
    {
      return null;
    }
  catch (Exception e)
    {
      return null;
    }
}

static private void readLayer (InputStream is, CPChibiChunk chunk, CPArtwork a) throws IOException
{
  CPLayer l = new CPLayer (a.getWidth (), a.getHeight ());

  int offset = readInt (is);
  l.setBlendMode (readInt (is)); // layer blend mode
  l.setAlpha (readInt (is));
  l.setVisible ((readInt (is) & 1) != 0);

  int titleLength = readInt (is);
  byte[] title = new byte[titleLength];
  realRead (is, title, titleLength);
  l.setName (new String (title, "UTF-8"));

  realSkip (is, offset - 20 - titleLength);
  readIntArray (is, l.getData (), l.getWidth () * l.getHeight ());

  a.getLayersVector ().add (l);

  realSkip (is, chunk.chunkSize - offset - l.getWidth () * l.getHeight () * 4);
}

static private void readIntArray (InputStream is, int[] intArray, int size) throws IOException
{
  byte[] buffer = new byte[size * 4];

  realRead (is, buffer, size * 4);

  int off = 0;
  for (int i = 0; i < size; i++)
    {
      intArray[i] = ((buffer[off++] & 0xff) << 24) | ((buffer[off++] & 0xff) << 16)
              | ((buffer[off++] & 0xff) << 8) | (buffer[off++] & 0xff);
    }
}

private static int readInt (InputStream is) throws IOException
{
  return is.read () << 24 | is.read () << 16 | is.read () << 8 | is.read ();
}

private static void realSkip (InputStream is, long bytesToSkip) throws IOException
{
  long skipped = 0, value;
  while (skipped < bytesToSkip)
    {
      value = is.read ();
      if (value < 0)
        {
          throw new RuntimeException ("EOF!");
        }

      skipped++;
      skipped += is.skip (bytesToSkip - skipped);
    }
}

private static void realRead (InputStream is, byte[] buffer, int bytesToRead) throws IOException
{
  int read = 0, value;
  while (read < bytesToRead)
    {
      value = is.read ();
      if (value < 0)
        {
          throw new RuntimeException ("EOF!");
        }

      buffer[read++] = (byte) value;
      read += is.read (buffer, read, bytesToRead - read);
    }
}

private static boolean readMagic (InputStream is) throws IOException
{
  byte[] buffer = new byte[4];

  realRead (is, buffer, 4);
  if (!Arrays.equals (buffer, CHIB))
    {
      return false;
    }

  realRead (is, buffer, 4);
  return Arrays.equals (buffer, IOEK);

}

static class CPChibiChunk
{

  final byte[] chunkType = new byte[4];
  int chunkSize;

  public CPChibiChunk (InputStream is) throws IOException
  {
    realRead (is, chunkType, 4);
    chunkSize = readInt (is);
  }

  boolean is (byte[] chunkTypeArg)
  {
    return Arrays.equals (this.chunkType, chunkTypeArg);
  }
}

static class CPChibiHeader
{

  int version, width, height, layersNb;

  public CPChibiHeader (InputStream is, CPChibiChunk chunk) throws IOException
  {
    version = readInt (is);
    width = readInt (is);
    height = readInt (is);
    layersNb = readInt (is);

    realSkip (is, chunk.chunkSize - 16);
  }
}

@Override
public String ext ()
{
  return "CHI";
}

}
