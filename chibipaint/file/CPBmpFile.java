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