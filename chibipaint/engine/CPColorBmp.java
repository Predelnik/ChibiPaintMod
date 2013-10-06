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

package chibipaint.engine;

import java.util.*;

import chibipaint.util.*;

//
// A 32bpp bitmap class (ARGB format)
//

public class CPColorBmp extends CPBitmap {

	// The bitmap data
    private int[] data;

	//
	// Constructors
	//

	// Allocates a new bitmap
    CPColorBmp(int width, int height) {
		super(width, height);
		this.data = new int[width * height];
	}

	// Creates a CPBitmap object from existing bitmap data
	public CPColorBmp(int width, int height, int[] data) {
		super(width, height);
		this.data = data;
	}

	// Creates a CPBitmap by copying a part of another CPBitmap
	public CPColorBmp(CPColorBmp bmp, CPRect r) {
		super(r.getWidth(), r.getWidth());

		width = r.getWidth();
		height = r.getHeight();
		data = new int[width * height];

		setFromBitmapRect(bmp, r);
	}

	//
	// Pixel access with friendly clipping
	//

	public int getPixel(int xArg, int yArg) {
		int x = xArg, y = yArg;
		x = Math.max(0, Math.min(width - 1, x));
		y = Math.max(0, Math.min(height - 1, y));

		return getData()[x + y * width];
	}

	public void setPixel(int x, int y, int color) {
		if (x < 0 || y < 0 || x >= width || y >= height) {
			return;
		}

		getData()[x + y * width] = color;
	}

	//
	// Copy rectangular regions methods
	//

	// copies a a rect to an int array allocated by the method
	public int[] copyRectToIntArray(CPRect rect) {
		CPRect r = new CPRect(0, 0, width, height);
		r.clip(rect);

		int[] buffer = new int[r.getWidth() * r.getHeight()];
		int w = r.getWidth();
		int h = r.getHeight();
		for (int j = 0; j < h; j++) {
			System.arraycopy(getData(), (j + r.top) * width + r.left, buffer, j * w, w);
		}

		return buffer;
	}

	// copies a a rect to an int array
	public boolean copyRectToIntArray(CPRect rect, int[] buffer) {
		CPRect r = new CPRect(0, 0, width, height);
		r.clip(rect);

		if (buffer.length < (r.getWidth() * r.getHeight())) {
			return false;
		}
		int w = r.getWidth();
		int h = r.getHeight();
		for (int j = 0; j < h; j++) {
			System.arraycopy(getData(), (j + r.top) * width + r.left, buffer, j * w, w);
		}

		return true;
	}

	// sets the content of a rectangular region using data from an int array
	public void setRectFromIntArray(int[] buffer, CPRect rect) {
		CPRect r = new CPRect(0, 0, width, height);
		r.clip(rect);

		int w = r.getWidth();
		int h = r.getHeight();
		for (int j = 0; j < h; j++) {
			System.arraycopy(buffer, j * w, getData(), (j + r.top) * width + r.left, w);
		}
	}

	//
	// XOR Copy
	//

	public int[] copyRectXOR(CPColorBmp bmp, CPRect rect) {
		CPRect r = new CPRect(0, 0, width, height);
		r.clip(rect);

		int[] buffer = new int[r.getWidth() * r.getHeight()];
		int w = r.getWidth();
		int h = r.getHeight();
		for (int j = 0; j < h; j++) {
			for (int i = 0; i < w; i++) {
				buffer[i + j * w] = getData()[(j + r.top) * width + i + r.left] ^ bmp.getData()[(j + r.top) * width + i + r.left];
			}
		}

		return buffer;
	}

	public void setRectXOR(int[] buffer, CPRect rect) {
		CPRect r = new CPRect(0, 0, width, height);
		r.clip(rect);

		int w = r.getWidth();
		int h = r.getHeight();
		for (int j = 0; j < h; j++) {
			for (int i = 0; i < w; i++) {
				getData()[(j + r.top) * width + i + r.left] = getData()[(j + r.top) * width + i + r.left] ^ buffer[i + j * w];
			}
		}
	}

	//
	// Copy another bitmap into this one using alpha blending
	//

	public void pasteAlphaRect(CPColorBmp bmp, CPRect srcRect, int x, int y) {
        CPRect srcRectCpy = null, dstRect = null;
        try {
            dstRect = new CPRect(x, y, 0, 0);
            srcRectCpy = (CPRect) srcRect.clone();
        } catch (Exception e) {
            return;
        }
        getSize().clipSourceDest(srcRectCpy, dstRect);

		int[] srcData = bmp.getData();
		for (int j = 0; j < dstRect.bottom - dstRect.top; j++) {
			int srcOffset = srcRectCpy.left + (srcRectCpy.top + j) * bmp.width;
			int dstOffset = dstRect.left + (dstRect.top + j) * width;
			for (int i = dstRect.left; i < dstRect.right; i++, srcOffset++, dstOffset++) {
				int color1 = srcData[srcOffset];
				int alpha1 = color1 >>> 24;

		if (alpha1 <= 0) {
			continue;
		}

		if (alpha1 == 255) {
			getData()[dstOffset] = color1;
			continue;
		}

		int color2 = getData()[dstOffset];
		int alpha2 = (color2 >>> 24);

		int newAlpha = alpha1 + alpha2 - alpha1 * alpha2 / 255;
		if (newAlpha > 0) {
			int realAlpha = alpha1 * 255 / newAlpha;
			int invAlpha = 255 - realAlpha;

			getData()[dstOffset] = newAlpha << 24
					| ((color1 >>> 16 & 0xff) + (((color2 >>> 16 & 0xff) * invAlpha - (color1 >>> 16 & 0xff)
							* invAlpha) / 255)) << 16
							| ((color1 >>> 8 & 0xff) + (((color2 >>> 8 & 0xff) * invAlpha - (color1 >>> 8 & 0xff)
									* invAlpha) / 255)) << 8
									| ((color1 & 0xff) + (((color2 & 0xff) * invAlpha - (color1 & 0xff) * invAlpha) / 255));
		}
			}
		}
	}

	// Sets the content of this CPBitmap using a rect from another bitmap
	// Assumes that the width and height of this bitmap and the rectangle are the same!!!
    void setFromBitmapRect(CPColorBmp bmp, CPRect r) {
		for (int i = 0; i < r.getHeight(); i++) {
			System.arraycopy(bmp.getData(), (i + r.top) * bmp.width + r.left, getData(), i * width, width);
		}
	}

	public void pasteBitmap(CPColorBmp bmp, int x, int y) {
		CPRect srcRect = bmp.getSize();
		CPRect dstRect = new CPRect(x, y, 0, 0);
		getSize().clipSourceDest(srcRect, dstRect);

		for (int i = 0; i < srcRect.getHeight(); i++) {
			System.arraycopy(bmp.getData(), (i + srcRect.top) * bmp.width + srcRect.left, getData(), (i + dstRect.top) * width
					+ dstRect.left, srcRect.getWidth());
		}
	}

	//
	// Copies the Alpha channel from another bitmap
	//

	public void copyAlphaFrom(CPColorBmp bmp, CPRect r) {
		r.clip(getSize());

		for (int j = r.top; j < r.bottom; j++) {
			for (int i = r.left; i < r.right; i++) {
				getData()[j * width + i] = (getData()[j * width + i] & 0xffffff) | (bmp.getData()[j * width + i] & 0xff000000);
			}
		}
	}

	public void copyDataFrom(CPColorBmp bmp) {
		if (bmp.width != width || bmp.height != height) {
			width = bmp.width;
			height = bmp.height;
			data = new int[width * height];
		}

		System.arraycopy(bmp.getData(), 0, getData(), 0, getData().length);
	}

    public int[] getData() {
        return data;
    }

    public void setData(int []dataArg) {
        data = dataArg;
    }

    //
	// Flood fill algorithm
	//

	static class CPFillLine {

		final int x1;
        final int x2;
        final int y;
        final int dy;

		CPFillLine(int x1, int x2, int y, int dy) {
			this.x1 = x1;
			this.x2 = x2;
			this.y = y;
			this.dy = dy;
		}
	}

	// we building metric in which our new color will always be counted as not near (so it passed in farAwayColor parameter)
	private static boolean is_colors_near(int color_1Arg, int color_2Arg, int maskArg, int distance, int farAwayColor)
	{
		int color_1 = color_1Arg, color_2 = color_2Arg, mask = maskArg;
		int[] dist = new int[4];
		if (color_1 == farAwayColor && color_2 == farAwayColor)
			return true;
		if (color_1 == farAwayColor)
			return false;
		if (color_2 == farAwayColor)
			return false;

		for (int i = 0; i < 4; i++) {
			dist[i] = (Math.abs((color_1 & 0xFF) - (color_2 & 0xFF))) & (mask & 0xFF);
			color_1 = color_1 >> 8;
		color_2 = color_2 >> 8;
		mask = mask >> 8;
		}

		return Math.max(dist[3], (dist[0] + dist[1] + dist[2]) * (1.0 / 3.0)) <= distance;
	}

	public void floodFill(int x, int y, int color, CPLayer useDataFrom, int distance) {
		if (!isInside(x, y)) {
			return;
		}

		int oldColor, colorMask, dataOldColor, dataColorMask;
		oldColor = getPixel(x, y);

		// If we are filling 100% transparent areas
		// then we need to ignore the residual color information
		// (it would also be possible to clear it when erasing, but then
		// the performance impact would be on the eraser rather than
		// on this low importance flood fill)

		if ((oldColor & 0xff000000) == 0) {
			colorMask = 0xff000000;
			oldColor = 0;
		} else {
			colorMask = 0xffffffff;
		}

		if (color == oldColor) {
			return;
		}

		dataOldColor = useDataFrom.getPixel(x, y);


		if ((dataOldColor & 0xff000000) == 0) {
			dataColorMask = 0xff000000;
			dataOldColor = 0;
		} else {
			dataColorMask = 0xffffffff;
		}

		LinkedList<CPFillLine> stack = new LinkedList<CPFillLine>();
		stack.addLast(new CPFillLine(x, x, y, -1));
		stack.addLast(new CPFillLine(x, x, y + 1, 1));

		CPRect clip = new CPRect(width, height);
		while (!stack.isEmpty()) {
			CPFillLine line = stack.removeFirst();

			if (line.y < clip.top || line.y >= clip.bottom) {
				continue;
			}

			int lineOffset = line.y * width;

			int left = line.x1, next;
			while (left >= clip.left && is_colors_near (getData()[left + lineOffset], oldColor, colorMask, distance, color)
					&& is_colors_near (useDataFrom.getData()[left + lineOffset], dataOldColor, dataColorMask, distance, color))
			{
				getData()[left + lineOffset] = color;
				left--;
			}
			if (left >= line.x1) {
				while (    left <= line.x2 && (!is_colors_near (getData()[left + lineOffset], oldColor, colorMask, distance, color)
						|| !is_colors_near (useDataFrom.getData()[left + lineOffset], dataOldColor, dataColorMask, distance, color))
						) {
					left++;
				}
				next = left + 1;
				if (left > line.x2) {
					continue;
				}
			} else {
				left++;
				if (left < line.x1) {
					stack.addLast(new CPFillLine(left, line.x1 - 1, line.y - line.dy, -line.dy));
				}
				next = line.x1 + 1;
			}

			do {
				getData()[left + lineOffset] = color;
				while (next < clip.right && is_colors_near (getData()[next + lineOffset], oldColor, colorMask, distance, color)
						&& is_colors_near (useDataFrom.getData()[next + lineOffset], dataOldColor, dataColorMask, distance, color)) {
					getData()[next + lineOffset] = color;
					next++;
				}
				stack.addLast(new CPFillLine(left, next - 1, line.y + line.dy, line.dy));

				if (next - 1 > line.x2) {
					stack.addLast(new CPFillLine(line.x2 + 1, next - 1, line.y - line.dy, -line.dy));
				}

				left = next + 1;
				while (left <= line.x2 && (!is_colors_near (getData()[left + lineOffset], oldColor, colorMask, distance, color)
						|| !is_colors_near (useDataFrom.getData()[left + lineOffset], dataOldColor, dataColorMask, distance, color))) {
					left++;
				}

				next = left + 1;
			} while (left <= line.x2);
		}
	}

	//
	// Box Blur algorithm
	//

	public void boxBlur(CPRect r, int radiusX, int radiusY) {
		CPRect rect = new CPRect(0, 0, width, height);
		rect.clip(r);

		int w = rect.getWidth();
		int h = rect.getHeight();
		int l = Math.max(w, h);

		int[] src = new int[l];
		int[] dst = new int[l];

		for (int j = rect.top; j < rect.bottom; j++) {
			System.arraycopy(getData(), rect.left + j * width, src, 0, w);
			multiplyAlpha(src, w);
			boxBlurLine(src, dst, w, radiusX);
			System.arraycopy(dst, 0, getData(), rect.left + j * width, w);
		}

		for (int i = rect.left; i < rect.right; i++) {
			copyColumnToArray(i, rect.top, h, src);
			boxBlurLine(src, dst, h, radiusY);
			separateAlpha(dst, h);
			copyArrayToColumn(i, rect.top, h, dst);
		}
	}

	private static void multiplyAlpha(int[] buffer, int len) {
		for (int i = 0; i < len; i++) {
			buffer[i] = buffer[i] & 0xff000000 | ((buffer[i] >>> 24) * (buffer[i] >>> 16 & 0xff) / 255) << 16
					| ((buffer[i] >>> 24) * (buffer[i] >>> 8 & 0xff) / 255) << 8 | (buffer[i] >>> 24)
					* (buffer[i] & 0xff) / 255;
		}
	}

	private static void separateAlpha(int[] buffer, int len) {
		for (int i = 0; i < len; i++) {
			if ((buffer[i] & 0xff000000) != 0) {
				buffer[i] = buffer[i] & 0xff000000
						| Math.min((buffer[i] >>> 16 & 0xff) * 255 / (buffer[i] >>> 24), 255) << 16
						| Math.min((buffer[i] >>> 8 & 0xff) * 255 / (buffer[i] >>> 24), 255) << 8
						| Math.min((buffer[i] & 0xff) * 255 / (buffer[i] >>> 24), 255);
			}
		}
	}

	private static void boxBlurLine(int[] src, int dst[], int len, int radius) {
		int s, ta, tr, tg, tb;
		s = ta = tr = tg = tb = 0;
		int pix;

		for (int i = 0; i < radius && i <= len; i++) {
			pix = src[i];
			ta += pix >>> 24;
		tr += (pix >>> 16) & 0xff;
		tg += (pix >>> 8) & 0xff;
		tb += pix & 0xff;
		s++;
		}
		for (int i = 0; i < len; i++) {
			if (i + radius < len) {
				pix = src[i + radius];
				ta += pix >>> 24;
		tr += (pix >>> 16) & 0xff;
		tg += (pix >>> 8) & 0xff;
		tb += pix & 0xff;
		s++;
			}

			dst[i] = (ta / s << 24) | (tr / s << 16) | (tg / s << 8) | tb / s;

			if (i - radius >= 0) {
				pix = src[i - radius];
				ta -= pix >>> 24;
			tr -= (pix >>> 16) & 0xff;
			tg -= (pix >>> 8) & 0xff;
			tb -= pix & 0xff;
			s--;
			}
		}
	}

	void copyColumnToArray(int x, int y, int len, int[] buffer) {
		for (int i = 0; i < len; i++) {
			buffer[i] = getData()[x + (i + y) * width];
		}
	}

	void copyArrayToColumn(int x, int y, int len, int[] buffer) {
		for (int i = 0; i < len; i++) {
			getData()[x + (i + y) * width] = buffer[i];
		}
	}
}
