package chibipaint.file;

import java.awt.image.BufferedImage;

import javax.swing.filechooser.FileNameExtensionFilter;


public class CPGifFile extends CPStandardImageFile {

	@Override
	public FileNameExtensionFilter fileFilter ()
	{
		return new FileNameExtensionFilter ("Graphics Interchange Format Images (*.gif)", "gif");
	}

	@Override
	public String ext ()
	{
		return "GIF";
	}

	@Override
	public int imageTypeCorrection(int x) {
		return BufferedImage.TYPE_BYTE_INDEXED;
	}
}