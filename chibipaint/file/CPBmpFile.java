package chibipaint.file;

import java.awt.image.BufferedImage;

import javax.swing.filechooser.FileNameExtensionFilter;


public class CPBmpFile extends CPStandardImageFile {
	@Override
	public FileNameExtensionFilter fileFilter ()
	{
		return new FileNameExtensionFilter ("Bitmap Image Files (*.bmp)", "bmp");
	}

	@Override
	public String ext ()
	{
		return "BMP";
	}

	@Override
	public int imageTypeCorrection(int x) {
		return BufferedImage.TYPE_INT_RGB;
	}
}