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


// Little abstract class which should wrap other classes to work with files
// Though here's warning: now they will not be static!

package chibipaint.file;

import chibipaint.engine.CPArtwork;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class CPAbstractFile
{
public abstract boolean write (OutputStream os, CPArtwork a);

public abstract CPArtwork read (InputStream is);

public abstract String ext (); // Returns normal file extension

public abstract boolean isNative (); // For now it's true only for chi file.

// If any new extension supported please add it here
public static String[] getSupportedExtensions ()
{
  return new String[]{"chi", "xcf", "png", "jpeg", "bmp", "gif"};
}

// Add here new extensions too, this function should also resolve stuff like jpeg/jpg
// The most important thing to be able to get class instance for every extension listed above
public static CPAbstractFile fromExtension (String ext)
{
  String uExt = ext.toUpperCase ();
  if (uExt.equals ("CHI"))
    return new CPChibiFile ();
  else if (uExt.equals ("XCF"))
    return new CPXcfFile ();
  else if (uExt.equals ("PNG"))
    return new CPPngFile ();
  else if (uExt.equals ("JPEG") || uExt.equals ("JPG"))
    return new CPJpegFile ();
  else if (uExt.equals ("BMP"))
    return new CPBmpFile ();
  else if (uExt.equals ("GIF"))
    return new CPGifFile ();
  else
    return null; // Bad Case
}

// String for further usage in file filter
public abstract FileNameExtensionFilter fileFilter ();
}
