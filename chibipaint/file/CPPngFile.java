package chibipaint.file;

import javax.swing.filechooser.FileNameExtensionFilter;


public class CPPngFile extends CPStandardImageFile {
	@Override
	public FileNameExtensionFilter fileFilter ()
	{
		return new FileNameExtensionFilter ( "Portable Network Graphics Images (*.png)", "png");
	}

	@Override
	public String ext ()
	{
		return "PNG";
	}

	@Override
	public int imageTypeCorrection(int x) {
		return x;
	}
}