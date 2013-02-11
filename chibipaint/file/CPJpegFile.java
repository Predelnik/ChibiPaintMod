package chibipaint.file;

import java.awt.image.BufferedImage;

import javax.swing.filechooser.FileNameExtensionFilter;


public class CPJpegFile extends CPStandardImageFile {

	@Override
	public FileNameExtensionFilter fileFilter ()
	{
		return new FileNameExtensionFilter ( "jPEG Images (*.jpeg, *.jpg)", "jpg", "jpeg");
	}

	@Override
	public String ext ()
	{
		return "JPEG";
	}

	@Override
	public int imageTypeCorrection(int x) {
		return BufferedImage.TYPE_INT_RGB;
	}
}