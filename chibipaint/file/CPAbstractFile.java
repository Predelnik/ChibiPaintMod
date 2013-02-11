// Little abstract class which should wrap other classes to work with files
// Though here's warning: now they will not be static!

package chibipaint.file;

import java.io.InputStream;
import java.io.OutputStream;

import javax.swing.filechooser.FileNameExtensionFilter;


import chibipaint.engine.CPArtwork;

public abstract class CPAbstractFile {
	public abstract boolean write(OutputStream os, CPArtwork a);
	public abstract CPArtwork read(InputStream is);
	public abstract String ext(); // Returns normal file extension
	public abstract boolean isNative (); // For now it's true only for chi file.

	// If any new extension supported please add it here
	public static String[] getSupportedExtensions ()
	{
		String[] sl = {"chi", "xcf", "png", "jpeg", "bmp", "gif"};
		return sl;
	}
	// Add here new extensions too, this function should also resolve stuff like jpeg/jpg
	// The most important thing to be able to get class instance for every extension listed above
	public static CPAbstractFile fromExtension (String ext)
	{
		String uExt = ext.toUpperCase();
		if (uExt.equals("CHI")) return new CPChibiFile ();
		else if (uExt.equals ("XCF")) return new CPXcfFile ();
		else if (uExt.equals ("PNG")) return new CPPngFile ();
		else if (uExt.equals ("JPEG") || uExt.equals ("JPG"))  return new CPJpegFile ();
		else if (uExt.equals ("BMP")) return new CPBmpFile ();
		else if (uExt.equals ("GIF")) return new CPGifFile ();
		else return null; // Bad Case
	}
	// String for further usage in file filter
	public abstract FileNameExtensionFilter fileFilter();
}
