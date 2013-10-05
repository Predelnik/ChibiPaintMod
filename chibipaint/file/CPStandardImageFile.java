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

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.MemoryImageSource;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import chibipaint.engine.CPArtwork;


// Basically - class to support images supported by imageIO interface
public abstract class CPStandardImageFile extends CPAbstractFile {
	@Override
	public abstract String ext ();
	public abstract int imageTypeCorrection (int x);
	@Override
	public boolean isNative () // Well this kind of files couldn't be native since they are not layered
	{
		return false;
	}
	@Override
	public boolean write(OutputStream os, CPArtwork a)
	{
		int [] data = a.getDisplayBM().getData();
		MemoryImageSource imgSource = new MemoryImageSource (a.width, a.height, data, 0, a.width);
		Image img = Toolkit.getDefaultToolkit().createImage (imgSource);
		int imageType = a.hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
		imageType = imageTypeCorrection (imageType);

		BufferedImage bI = new BufferedImage(a.width, a.height, imageType);
		Graphics2D g = bI.createGraphics();
		g.drawImage(img, 0, 0, null);
		try {
			ImageIO.write (bI, ext (), os);
		} catch (IOException e) {
			return false; // Well in that case we definitely can't save it
		}

		return true;
	}

	@Override
	public CPArtwork read(InputStream is)
	{
		// That's a lot of cuter way to do things + we can use it for all 4 supported image formats
		// The only bad thing is int[] - byte[] conversion
		BufferedImage bI;
		try {
			bI = ImageIO.read (is);
		} catch (IOException e) {
			return null;
		}
		CPArtwork a = new CPArtwork(bI.getWidth(), bI.getHeight ());
		BufferedImage bIConverted = new BufferedImage (a.width, a.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = bIConverted.createGraphics();
		g.drawImage(bI, 0, 0, null);
		a.getLayersVector().get(0).data = ((DataBufferInt)bIConverted.getData().getDataBuffer()).getData();
		return a;
	}
}
