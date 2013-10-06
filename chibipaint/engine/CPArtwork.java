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

import java.awt.event.InputEvent;
import java.util.*;

import chibipaint.*;
import chibipaint.engine.CPBrushManager.*;
import chibipaint.util.*;

//FIXME: BROKEN: use setForegroundColor and setBrush, controller's layerChanged replaced by the ICPArtworkListener mechanism

public class CPArtwork {

	private final int width;
    private final int height;

	private Vector<CPLayer> layers;
	private CPLayer curLayer;
	private int activeLayer;

	private final CPLayer fusion;
    private final CPLayer opacityBuffer;
	Vector<CPLayer> undoBufferAll;
	private final CPRect fusionArea;
    CPRect undoArea;
    private final CPRect opacityArea;

    CPSelection curSelection;

    private final Random rnd = new Random();

    public CPUndoManager getUndoManager() {
        return undoManager;
    }

    private final CPUndoManager undoManager = new CPUndoManager(this);

    public void DoSelection(int modifiers, CPSelection selection)
    {
        boolean ShiftPressed = (modifiers & InputEvent.SHIFT_DOWN_MASK) != 0;
        boolean ControlPressed = (modifiers & InputEvent.CTRL_DOWN_MASK) != 0;
        CPSelection pastSelection = curSelection.copy();
        if (ShiftPressed || ControlPressed)
        {
            if (ShiftPressed)
            {
                if (!ControlPressed)
                    curSelection.AddToSelection (selection);
                else
                    curSelection.IntersectWithSelection(selection);
            }
            else
                curSelection.SubtractFromSelection (selection);
        }
        else
            curSelection = selection;
        CPRect rect = pastSelection.getBoundingRect ();
        rect.union (curSelection.getBoundingRect ());
        addUndo (new CPUndoManager.CPUndoSelection(this, pastSelection, rect));
    }

    public CPSelection getCurSelection() {
        return curSelection;
    }

    public void setCurSelection(CPSelection cpSelection) {
        curSelection = cpSelection;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public CPLayer getCurLayer() {
        return curLayer;
    }

    public interface ICPArtworkListener {

		void updateRegion(CPArtwork artwork, CPRect region);

		void layerChange(CPArtwork artwork);
	}

	private final LinkedList<ICPArtworkListener> artworkListeners = new LinkedList<ICPArtworkListener>();

	// Clipboard

	public static class CPClip {

		final CPColorBmp bmp;
		final int x;
        final int y;

		CPClip(CPColorBmp bmp, int x, int y) {
			this.bmp = bmp;
			this.x = x;
			this.y = y;
		}
	}

    private final CPClip clipboard = null;

	private CPBrushInfo curBrush;

	// FIXME: shouldn't be public
	public final CPBrushManager brushManager = new CPBrushManager();

	private float lastX;
    private float lastY;
    private float lastPressure;
	private int[] brushBuffer = null;

	public int maxUndo = 30;

	//
	// Current Engine Parameters
	//

	private boolean sampleAllLayers = false;
	private boolean lockAlpha = false;

	private int curColor;

	private final CPBrushTool[] paintingModes = { new CPBrushToolSimpleBrush(), new CPBrushToolEraser(), new CPBrushToolDodge(),
			new CPBrushToolBurn(), new CPBrushToolWatercolor(), new CPBrushToolBlur(), new CPBrushToolSmudge(),
			new CPBrushToolOil(), };

	private static final int BURN_CONSTANT = 260;
	private static final int BLUR_MIN = 64;
	private static final int BLUR_MAX = 1;

	public CPArtwork(int width, int height) {
		this.width = width;
		this.height = height;

		setLayers(new Vector<CPLayer>());

		CPLayer defaultLayer = new CPLayer(width, height);
		defaultLayer.setName(getDefaultLayerName());
		defaultLayer.clear(0xffffffff);
		getLayersVector().add(defaultLayer);

		curLayer = getLayersVector().get(0);
		fusionArea = new CPRect(0, 0, width, height);
		undoArea = new CPRect();
		opacityArea = new CPRect();
		setActiveLayerNum(0);

		// we reserve a double sized buffer to be used as a 16bits per channel buffer
		opacityBuffer = new CPLayer(width, height);

		fusion = new CPLayer(width, height);
	}

	public long getDocMemoryUsed() {
		return (long) getWidth() * getHeight() * 4 * (3 + getLayersVector().size())
				+ (clipboard != null ? clipboard.bmp.getWidth() * clipboard.bmp.getHeight() * 4 : 0);
	}

	public long getUndoMemoryUsed() {
		long total = 0;

		CPColorBmp lastBitmap = clipboard != null ? clipboard.bmp : null;

		for (int i = undoManager.getRedoList().size() - 1; i >= 0; i--) {
			CPUndo undo = undoManager.getRedoList().get(i);

			total += undo.getMemoryUsed(true, lastBitmap);
		}

		for (CPUndo undo : undoManager.getUndoList()) {
			total += undo.getMemoryUsed(false, lastBitmap);
		}

		return total;
	}

	public CPLayer getDisplayBM() {
		fusionLayers();
		return fusion;

		// for(int i=0; i<opacityBuffer.data.length; i++)
		// opacityBuffer.data[i] |= 0xff000000;
		// return opacityBuffer;
	}

	public void fusionLayers() {
		if (fusionArea.isEmpty()) {
			return;
		}

		mergeOpacityBuffer(curColor, false);

		fusion.clear(fusionArea, 0x00ffffff);
		boolean fullAlpha = true, first = true;
		for (CPLayer l : getLayersVector()) {
			if (!first) {
				fullAlpha = fullAlpha && fusion.hasAlpha(fusionArea);
			}

			if (l.isVisible()) {
				first = false;
				if (fullAlpha) {
					l.fusionWithFullAlpha(fusion, fusionArea);
				} else {
					l.fusionWith(fusion, fusionArea);
				}
			}
		}

		fusionArea.makeEmpty();
	}

	// ///////////////////////////////////////////////////////////////////////////////////
	// Listeners
	// ///////////////////////////////////////////////////////////////////////////////////

	public void addListener(ICPArtworkListener listener) {
		artworkListeners.addLast(listener);
	}

	public void removeListener(ICPArtworkListener listener) {
		artworkListeners.remove(listener);
	}

	void callListenersUpdateRegion(CPRect region) {
		for (ICPArtworkListener l : artworkListeners) {
			l.updateRegion(this, region);
		}
	}

	public void callListenersLayerChange() {
		for (ICPArtworkListener l : artworkListeners) {
			l.layerChange(this);
		}
	}

	// ///////////////////////////////////////////////////////////////////////////////////
	// Global Parameters
	// ///////////////////////////////////////////////////////////////////////////////////

	public void setSampleAllLayers(boolean b) {
		sampleAllLayers = b;
	}

	public void setLockAlpha(boolean b) {
		lockAlpha = b;
	}

	public void setForegroundColor(int color) {
		curColor = color;
	}

	public void setBrush(CPBrushInfo brush) {
		curBrush = brush;
	}

	// ///////////////////////////////////////////////////////////////////////////////////
	// Paint engine
	// ///////////////////////////////////////////////////////////////////////////////////

	public void beginStroke(float x, float y, float pressure) {
		if (curBrush == null) {
			return;
		}

		paintingModes[curBrush.paintMode].beginStroke(x, y, pressure);
	}

	public void continueStroke(float x, float y, float pressure) {
		if (curBrush == null) {
			return;
		}

		paintingModes[curBrush.paintMode].continueStroke(x, y, pressure);
	}

	public void endStroke() {
		if (curBrush == null) {
			return;
		}

		paintingModes[curBrush.paintMode].endStroke();
	}

	void mergeOpacityBuffer(int color, boolean clear) {
		if (!opacityArea.isEmpty()) {

            for (int j = opacityArea.top; j < opacityArea.bottom; j++) {
                int dstOffset = opacityArea.left + j * getWidth();
                for (int i = opacityArea.left; i < opacityArea.right; i++, dstOffset++) {
                    opacityBuffer.getData()[dstOffset] = (int) (opacityBuffer.getData()[dstOffset] * curSelection.getData (i, j));
                }
            }
			paintingModes[curBrush.paintMode].mergeOpacityBuf(opacityArea, color);

			// Allow to eraser lower alpha with 'lock alpha' because it's all more logical and comfortable (look at gimp and other stuff)
			if (isLockAlpha() && curBrush.paintMode != CPBrushInfo.M_ERASE) {
				undoManager.restoreCurLayerAlpha(opacityArea);
			}

			if (clear) {
				opacityBuffer.clear(opacityArea, 0);
			}

			opacityArea.makeEmpty();
		}
	}

    // Extend this class to create new tools and brush types
	abstract class CPBrushTool {

		abstract public void beginStroke(float x, float y, float pressure);

		abstract public void continueStroke(float x, float y, float pressure);

		abstract public void endStroke();

		abstract public void mergeOpacityBuf(CPRect dstRect, int color);
	}

	abstract class CPBrushToolBase extends CPBrushTool {

		@Override
		public void beginStroke(float x, float y, float pressure) {
            undoManager.preserveCurLayerState ();
			undoArea.makeEmpty();

			opacityBuffer.clear();
			opacityArea.makeEmpty();

			lastX = x;
			lastY = y;
			lastPressure = pressure;
			paintDab(x, y, pressure);
		}

		@Override
		public void continueStroke(float x, float y, float pressure) {
			float dist = (float) Math.sqrt(((lastX - x) * (lastX - x) + (lastY - y) * (lastY - y)));
			float spacing = Math.max(curBrush.minSpacing, curBrush.curSize * curBrush.spacing);

			if (dist > spacing) {
				float nx = lastX, ny = lastY, np = lastPressure;

				float df = (spacing - 0.001f) / dist;
				for (float f = df; f <= 1.f; f += df) {
					nx = f * x + (1.f - f) * lastX;
					ny = f * y + (1.f - f) * lastY;
					np = f * pressure + (1.f - f) * lastPressure;
					paintDab(nx, ny, np);
				}
				lastX = nx;
				lastY = ny;
				lastPressure = np;
			}
		}

		@Override
		public void endStroke() {
			undoArea.clip(getSize());
			if (!undoArea.isEmpty()) {
				mergeOpacityBuffer(curColor, false);
				addUndo(new CPUndoManager.CPUndoPaint(CPArtwork.this));
			}
			brushBuffer = null;
		}

		void paintDab(float xArg, float yArg, float pressure) {
			float x = xArg, y = yArg;
			curBrush.applyPressure(pressure);
			if (curBrush.scattering > 0f) {
				x += rnd.nextGaussian() * curBrush.curScattering / 4f;
				y += rnd.nextGaussian() * curBrush.curScattering / 4f;
				// x += (rnd.nextFloat() - .5f) * tool.scattering;
				// y += (rnd.nextFloat() - .5f) * tool.scattering;
			}
			CPBrushDab dab = brushManager.getDab(x, y, curBrush);
			paintDab(dab);
		}

		void paintDab(CPBrushDab dab) {
			CPRect srcRect = new CPRect(dab.width, dab.height);
			CPRect dstRect = new CPRect(dab.width, dab.height);
			dstRect.translate(dab.x, dab.y);

			clipSourceDest(srcRect, dstRect);

			// drawing entirely outside the canvas
			if (dstRect.isEmpty()) {
				return;
			}

			undoArea.union(dstRect);
			opacityArea.union(dstRect);
			invalidateFusion(dstRect);

			paintDabImplementation(srcRect, dstRect, dab);
		}

		abstract void paintDabImplementation(CPRect srcRect, CPRect dstRect, CPBrushDab dab);
	}

	class CPBrushToolSimpleBrush extends CPBrushToolBase {

		@Override
		void paintDabImplementation(CPRect srcRect, CPRect dstRect, CPBrushDab dab) {
			// FIXME: there should be no reference to a specific tool here
			// create a new brush parameter instead
			if (curBrush.isAirbrush) {
				paintFlow(srcRect, dstRect, dab.brush, dab.width, Math.max(1, dab.alpha / 8));
			} else if (curBrush.toolNb == CPController.T_PEN) {
				paintFlow(srcRect, dstRect, dab.brush, dab.width, Math.max(1, dab.alpha / 2));
			} else {
				// paintOpacityFlow(srcRect, dstRect, brush, dab.stride, alpha, 255);
				// paintOpacityFlow(srcRect, dstRect, brush, dab.stride, 128, alpha);
				paintOpacity(srcRect, dstRect, dab.brush, dab.width, dab.alpha);
			}
		}

		@Override
		public void mergeOpacityBuf(CPRect dstRect, int color) {
			int[] opacityData = opacityBuffer.getData();
			int[] undoData = undoManager.getCurLayerPreservedData ();

			for (int j = dstRect.top; j < dstRect.bottom; j++) {
				int dstOffset = dstRect.left + j * getWidth();
				for (int i = dstRect.left; i < dstRect.right; i++, dstOffset++) {
					int opacityAlpha = opacityData[dstOffset] / 255;
					if (opacityAlpha > 0) {
						int destColor = undoData[dstOffset];

						int destAlpha = destColor >>> 24;
						int newLayerAlpha = opacityAlpha + destAlpha * (255 - opacityAlpha) / 255;
						int realAlpha = 255 * opacityAlpha / newLayerAlpha;
						int invAlpha = 255 - realAlpha;

						int newColor = (((color >>> 16 & 0xff) * realAlpha + (destColor >>> 16 & 0xff) * invAlpha) / 255) << 16
								& 0xff0000
								| (((color >>> 8 & 0xff) * realAlpha + (destColor >>> 8 & 0xff) * invAlpha) / 255) << 8
								& 0xff00 | (((color & 0xff) * realAlpha + (destColor & 0xff) * invAlpha) / 255) & 0xff;

						newColor |= newLayerAlpha << 24 & 0xff000000;
						getCurLayer().getData()[dstOffset] = newColor;
					}
				}
			}
		}

		void paintOpacity(CPRect srcRect, CPRect dstRect, byte[] brush, int w, int alpha) {
			int[] opacityData = opacityBuffer.getData();

			int by = srcRect.top;
			for (int j = dstRect.top; j < dstRect.bottom; j++, by++) {
				int srcOffset = srcRect.left + by * w;
				int dstOffset = dstRect.left + j * getWidth();
				for (int i = dstRect.left; i < dstRect.right; i++, srcOffset++, dstOffset++) {
					int brushAlpha = (brush[srcOffset] & 0xff) * alpha;
					if (brushAlpha != 0) {
						int opacityAlpha = opacityData[dstOffset];
						if (brushAlpha > opacityAlpha) {
							opacityData[dstOffset] = brushAlpha;
						}
					}

				}
			}
		}

		void paintFlow(CPRect srcRect, CPRect dstRect, byte[] brush, int w, int alpha) {
			int[] opacityData = opacityBuffer.getData();

			int by = srcRect.top;
			for (int j = dstRect.top; j < dstRect.bottom; j++, by++) {
				int srcOffset = srcRect.left + by * w;
				int dstOffset = dstRect.left + j * getWidth();
				for (int i = dstRect.left; i < dstRect.right; i++, srcOffset++, dstOffset++) {
					int brushAlpha = (brush[srcOffset] & 0xff) * alpha;
					if (brushAlpha != 0) {
						int opacityAlpha = Math.min(255 * 255, opacityData[dstOffset]
								+ (255 - opacityData[dstOffset] / 255) * brushAlpha / 255);
						opacityData[dstOffset] = opacityAlpha;
					}

				}
			}
		}

    }

	class CPBrushToolEraser extends CPBrushToolSimpleBrush {

		@Override
		public void mergeOpacityBuf(CPRect dstRect, int color) {
			int[] opacityData = opacityBuffer.getData();
			int[] undoData = undoManager.getCurLayerPreservedData();

			for (int j = dstRect.top; j < dstRect.bottom; j++) {
				int dstOffset = dstRect.left + j * getWidth();
				for (int i = dstRect.left; i < dstRect.right; i++, dstOffset++) {
					int opacityAlpha = opacityData[dstOffset] / 255;
					if (opacityAlpha > 0) {
						int destColor = undoData[dstOffset];
						int destAlpha = destColor >>> 24;

			int realAlpha = destAlpha * (255 - opacityAlpha) / 255;
			getCurLayer().getData()[dstOffset] = destColor & 0xffffff | realAlpha << 24;
					}
				}
			}
		}
	}

	class CPBrushToolDodge extends CPBrushToolSimpleBrush {

		@Override
		public void mergeOpacityBuf(CPRect dstRect, int color) {
			int[] opacityData = opacityBuffer.getData();
			int[] undoData = undoManager.getCurLayerPreservedData();

			for (int j = dstRect.top; j < dstRect.bottom; j++) {
				int dstOffset = dstRect.left + j * getWidth();
				for (int i = dstRect.left; i < dstRect.right; i++, dstOffset++) {
					int opacityAlpha = opacityData[dstOffset] / 255;
					if (opacityAlpha > 0) {
						int destColor = undoData[dstOffset];
						if ((destColor & 0xff000000) != 0) {
							opacityAlpha += 255;
							int r = (destColor >>> 16 & 0xff) * opacityAlpha / 255;
							int g = (destColor >>> 8 & 0xff) * opacityAlpha / 255;
							int b = (destColor & 0xff) * opacityAlpha / 255;

							if (r > 255) {
								r = 255;
							}
							if (g > 255) {
								g = 255;
							}
							if (b > 255) {
								b = 255;
							}

							int newColor = destColor & 0xff000000 | r << 16 | g << 8 | b;
							getCurLayer().getData()[dstOffset] = newColor;
						}
					}
				}
			}
		}
	}

	class CPBrushToolBurn extends CPBrushToolSimpleBrush {

		@Override
		public void mergeOpacityBuf(CPRect dstRect, int color) {
			int[] opacityData = opacityBuffer.getData();
			int[] undoData = undoManager.getCurLayerPreservedData();

			for (int j = dstRect.top; j < dstRect.bottom; j++) {
				int dstOffset = dstRect.left + j * getWidth();
				for (int i = dstRect.left; i < dstRect.right; i++, dstOffset++) {
					int opacityAlpha = opacityData[dstOffset] / 255;
					if (opacityAlpha > 0) {
						int destColor = undoData[dstOffset];
						if ((destColor & 0xff000000) != 0) {
							// opacityAlpha = 255 - opacityAlpha;

							int r = destColor >>> 16 & 0xff;
							int g = destColor >>> 8 & 0xff;
							int b = destColor & 0xff;

							r = r - (BURN_CONSTANT - r) * opacityAlpha / 255;
							g = g - (BURN_CONSTANT - g) * opacityAlpha / 255;
							b = b - (BURN_CONSTANT - b) * opacityAlpha / 255;

							if (r < 0) {
								r = 0;
							}
							if (g < 0) {
								g = 0;
							}
							if (b < 0) {
								b = 0;
							}

							int newColor = destColor & 0xff000000 | r << 16 | g << 8 | b;
							getCurLayer().getData()[dstOffset] = newColor;
						}
					}
				}
			}
		}
	}

	class CPBrushToolBlur extends CPBrushToolSimpleBrush {

		@Override
		public void mergeOpacityBuf(CPRect dstRect, int color) {
			int[] opacityData = opacityBuffer.getData();
			int[] undoData = undoManager.getCurLayerPreservedData();

			for (int j = dstRect.top; j < dstRect.bottom; j++) {
				int dstOffset = dstRect.left + j * getWidth();
				for (int i = dstRect.left; i < dstRect.right; i++, dstOffset++) {
					int opacityAlpha = opacityData[dstOffset] / 255;
					if (opacityAlpha > 0) {
						int blur = BLUR_MIN + (BLUR_MAX - BLUR_MIN) * opacityAlpha / 255;

						int destColor = undoData[dstOffset];
						int a = blur * (destColor >>> 24 & 0xff);
						int r = blur * (destColor >>> 16 & 0xff);
						int g = blur * (destColor >>> 8 & 0xff);
						int b = blur * (destColor & 0xff);
						int sum = blur + 4;

						destColor = undoData[j > 0 ? dstOffset - getWidth() : dstOffset];
						a += destColor >>> 24 & 0xff;
						r += destColor >>> 16 & 0xff;
						g += destColor >>> 8 & 0xff;
						b += destColor & 0xff;

						destColor = undoData[j < getHeight() - 1 ? dstOffset + getWidth() : dstOffset];
						a += destColor >>> 24 & 0xff;
						r += destColor >>> 16 & 0xff;
						g += destColor >>> 8 & 0xff;
						b += destColor & 0xff;

						destColor = undoData[i > 0 ? dstOffset - 1 : dstOffset];
						a += destColor >>> 24 & 0xff;
						r += destColor >>> 16 & 0xff;
						g += destColor >>> 8 & 0xff;
						b += destColor & 0xff;

						destColor = undoData[i < getWidth() - 1 ? dstOffset + 1 : dstOffset];
						a += destColor >>> 24 & 0xff;
						r += destColor >>> 16 & 0xff;
						g += destColor >>> 8 & 0xff;
						b += destColor & 0xff;

						a /= sum;
						r /= sum;
						g /= sum;
						b /= sum;
						getCurLayer().getData()[dstOffset] = a << 24 | r << 16 | g << 8 | b;
					}
				}
			}
		}
	}

	// Brushes derived from this class use the opacity buffer
	// as a simple alpha layer
	class CPBrushToolDirectBrush extends CPBrushToolSimpleBrush {

		@Override
		public void mergeOpacityBuf(CPRect dstRect, int color) {
			int[] opacityData = opacityBuffer.getData();
			int[] undoData = undoManager.getCurLayerPreservedData();

			for (int j = dstRect.top; j < dstRect.bottom; j++) {
				int dstOffset = dstRect.left + j * getWidth();
				for (int i = dstRect.left; i < dstRect.right; i++, dstOffset++) {
					int color1 = opacityData[dstOffset];
					int alpha1 = (color1 >>> 24);
					if (alpha1 <= 0) {
						continue;
					}
					int color2 = undoData[dstOffset];
					int alpha2 = (color2 >>> 24) * fusion.alpha / 100;

					int newAlpha = alpha1 + alpha2 - alpha1 * alpha2 / 255;
					if (newAlpha > 0) {
						int realAlpha = alpha1 * 255 / newAlpha;
						int invAlpha = 255 - realAlpha;

						getCurLayer().getData()[dstOffset] = newAlpha << 24
								| (((color1 >>> 16 & 0xff) * realAlpha + (color2 >>> 16 & 0xff) * invAlpha) / 255) << 16
								| (((color1 >>> 8 & 0xff) * realAlpha + (color2 >>> 8 & 0xff) * invAlpha) / 255) << 8
								| (((color1 & 0xff) * realAlpha + (color2 & 0xff) * invAlpha) / 255);
					}
				}
			}
		}
	}

	class CPBrushToolWatercolor extends CPBrushToolDirectBrush {

		static final int wcMemory = 50;
		static final int wxMaxSampleRadius = 64;

		LinkedList<CPColorFloat> previousSamples;

		@Override
		public void beginStroke(float x, float y, float pressure) {
			previousSamples = null;

			super.beginStroke(x, y, pressure);
		}

		@Override
		void paintDabImplementation(CPRect srcRect, CPRect dstRect, CPBrushDab dab) {
			if (previousSamples == null) {
				CPColorFloat startColor = sampleColor((dstRect.left + dstRect.right) / 2,
						(dstRect.top + dstRect.bottom) / 2, Math.max(1, Math.min(wxMaxSampleRadius,
								dstRect.getWidth() * 2 / 6)), Math.max(1, Math.min(wxMaxSampleRadius, dstRect
										.getHeight() * 2 / 6)));

				previousSamples = new LinkedList<CPColorFloat>();
				for (int i = 0; i < wcMemory; i++) {
					previousSamples.addLast(startColor);
				}
			}
			CPColorFloat wcColor = new CPColorFloat(0, 0, 0);
			for (CPColorFloat sample : previousSamples) {
				wcColor.r += sample.r;
				wcColor.g += sample.g;
				wcColor.b += sample.b;
			}
			wcColor.r /= previousSamples.size();
			wcColor.g /= previousSamples.size();
			wcColor.b /= previousSamples.size();

			// resaturation
			int color = curColor & 0xffffff;
			wcColor.mixWith(new CPColorFloat(color), curBrush.resat * curBrush.resat);

			int newColor = wcColor.toInt();

			// bleed
			wcColor.mixWith(sampleColor((dstRect.left + dstRect.right) / 2, (dstRect.top + dstRect.bottom) / 2, Math
					.max(1, Math.min(wxMaxSampleRadius, dstRect.getWidth() * 2 / 6)), Math.max(1, Math.min(
							wxMaxSampleRadius, dstRect.getHeight() * 2 / 6))), curBrush.bleed);

			previousSamples.addLast(wcColor);
			previousSamples.removeFirst();

			paintDirect(srcRect, dstRect, dab.brush, dab.width, Math.max(1, dab.alpha / 4), newColor);
			mergeOpacityBuffer(0, false);
			if (isSampleAllLayers()) {
				fusionLayers();
			}
		}

		void paintDirect(CPRect srcRect, CPRect dstRect, byte[] brush, int w, int alpha, int color1) {
			int[] opacityData = opacityBuffer.getData();

			int by = srcRect.top;
			for (int j = dstRect.top; j < dstRect.bottom; j++, by++) {
				int srcOffset = srcRect.left + by * w;
				int dstOffset = dstRect.left + j * getWidth();
				for (int i = dstRect.left; i < dstRect.right; i++, srcOffset++, dstOffset++) {
					int alpha1 = (brush[srcOffset] & 0xff) * alpha / 255;
					if (alpha1 <= 0) {
						continue;
					}

					int color2 = opacityData[dstOffset];
					int alpha2 = (color2 >>> 24);

					int newAlpha = alpha1 + alpha2 - alpha1 * alpha2 / 255;
					if (newAlpha > 0) {
						int realAlpha = alpha1 * 255 / newAlpha;
						int invAlpha = 255 - realAlpha;

						// The usual alpha blending formula C = A * alpha + B * (1 - alpha)
						// has to rewritten in the form C = A + (1 - alpha) * B - (1 - alpha) *A
						// that way the rounding up errors won't cause problems

						int newColor = newAlpha << 24
								| ((color1 >>> 16 & 0xff) + (((color2 >>> 16 & 0xff) * invAlpha - (color1 >>> 16 & 0xff)
										* invAlpha) / 255)) << 16
										| ((color1 >>> 8 & 0xff) + (((color2 >>> 8 & 0xff) * invAlpha - (color1 >>> 8 & 0xff)
												* invAlpha) / 255)) << 8
												| ((color1 & 0xff) + (((color2 & 0xff) * invAlpha - (color1 & 0xff) * invAlpha) / 255));

						opacityData[dstOffset] = newColor;
					}
				}
			}
		}

		CPColorFloat sampleColor(int x, int y, int dx, int dy) {
			LinkedList<CPColorFloat> samples = new LinkedList<CPColorFloat>();

			CPLayer layerToSample = isSampleAllLayers() ? fusion : getActiveLayer();

			samples.addLast(new CPColorFloat(layerToSample.getPixel(x, y) & 0xffffff));

			for (float r = 0.25f; r < 1.001f; r += .25f) {
				samples.addLast(new CPColorFloat(layerToSample.getPixel((int) (x + r * dx), y) & 0xffffff));
				samples.addLast(new CPColorFloat(layerToSample.getPixel((int) (x - r * dx), y) & 0xffffff));
				samples.addLast(new CPColorFloat(layerToSample.getPixel(x, (int) (y + r * dy)) & 0xffffff));
				samples.addLast(new CPColorFloat(layerToSample.getPixel(x, (int) (y - r * dy)) & 0xffffff));

				samples.addLast(new CPColorFloat(layerToSample.getPixel((int) (x + r * .7f * dx), (int) (y + r * .7f
						* dy)) & 0xffffff));
				samples.addLast(new CPColorFloat(layerToSample.getPixel((int) (x + r * .7f * dx), (int) (y - r * .7f
						* dy)) & 0xffffff));
				samples.addLast(new CPColorFloat(layerToSample.getPixel((int) (x - r * .7f * dx), (int) (y + r * .7f
						* dy)) & 0xffffff));
				samples.addLast(new CPColorFloat(layerToSample.getPixel((int) (x - r * .7f * dx), (int) (y - r * .7f
						* dy)) & 0xffffff));
			}

			CPColorFloat average = new CPColorFloat(0, 0, 0);
			for (CPColorFloat sample : samples) {
				average.r += sample.r;
				average.g += sample.g;
				average.b += sample.b;
			}
			average.r /= samples.size();
			average.g /= samples.size();
			average.b /= samples.size();

			return average;
		}
	}

	class CPBrushToolOil extends CPBrushToolDirectBrush {

		@Override
		void paintDabImplementation(CPRect srcRect, CPRect dstRect, CPBrushDab dab) {
			if (brushBuffer == null) {
				brushBuffer = new int[dab.width * dab.height];
				for (int i = brushBuffer.length - 1; --i >= 0;) {
					brushBuffer[i] = 0;
				}
				// curLayer.copyRect(dstRect, brushBuffer);
				oilAccumBuffer(srcRect, dstRect, brushBuffer, dab.width, 255);
			} else {
				oilResatBuffer(srcRect, dstRect, brushBuffer, dab.width, (int) ((curBrush.resat <= 0f) ? 0 : Math.max(
						1, (curBrush.resat * curBrush.resat) * 255)), curColor & 0xffffff);
				oilPasteBuffer(srcRect, dstRect, brushBuffer, dab.brush, dab.width, dab.alpha);
				oilAccumBuffer(srcRect, dstRect, brushBuffer, dab.width, (int) ((curBrush.bleed) * 255));
			}
			mergeOpacityBuffer(0, false);
			if (isSampleAllLayers()) {
				fusionLayers();
			}
		}

		private void oilAccumBuffer(CPRect srcRect, CPRect dstRect, int[] buffer, int w, int alpha) {
			CPLayer layerToSample = isSampleAllLayers() ? fusion : getActiveLayer();

			int by = srcRect.top;
			for (int j = dstRect.top; j < dstRect.bottom; j++, by++) {
				int srcOffset = srcRect.left + by * w;
				int dstOffset = dstRect.left + j * getWidth();
				for (int i = dstRect.left; i < dstRect.right; i++, srcOffset++, dstOffset++) {
					int color1 = layerToSample.getData()[dstOffset];
					int alpha1 = (color1 >>> 24) * alpha / 255;
					if (alpha1 <= 0) {
						continue;
					}

					int color2 = buffer[srcOffset];
					int alpha2 = (color2 >>> 24);

					int newAlpha = alpha1 + alpha2 - alpha1 * alpha2 / 255;
					if (newAlpha > 0) {
						int realAlpha = alpha1 * 255 / newAlpha;
						int invAlpha = 255 - realAlpha;

						int newColor = newAlpha << 24
								| ((color1 >>> 16 & 0xff) + (((color2 >>> 16 & 0xff) * invAlpha - (color1 >>> 16 & 0xff)
										* invAlpha) / 255)) << 16
										| ((color1 >>> 8 & 0xff) + (((color2 >>> 8 & 0xff) * invAlpha - (color1 >>> 8 & 0xff)
												* invAlpha) / 255)) << 8
												| ((color1 & 0xff) + (((color2 & 0xff) * invAlpha - (color1 & 0xff) * invAlpha) / 255));

						buffer[srcOffset] = newColor;
					}
				}
			}
		}

		private void oilResatBuffer(CPRect srcRect, CPRect dstRect, int[] buffer, int w, int alpha1, int color1) {
			if (alpha1 <= 0) {
				return;
			}

			int by = srcRect.top;
			for (int j = dstRect.top; j < dstRect.bottom; j++, by++) {
				int srcOffset = srcRect.left + by * w;
				for (int i = dstRect.left; i < dstRect.right; i++, srcOffset++) {
					int color2 = buffer[srcOffset];
					int alpha2 = (color2 >>> 24);

					int newAlpha = alpha1 + alpha2 - alpha1 * alpha2 / 255;
					if (newAlpha > 0) {
						int realAlpha = alpha1 * 255 / newAlpha;
						int invAlpha = 255 - realAlpha;

						int newColor = newAlpha << 24
								| ((color1 >>> 16 & 0xff) + (((color2 >>> 16 & 0xff) * invAlpha - (color1 >>> 16 & 0xff)
										* invAlpha) / 255)) << 16
										| ((color1 >>> 8 & 0xff) + (((color2 >>> 8 & 0xff) * invAlpha - (color1 >>> 8 & 0xff)
												* invAlpha) / 255)) << 8
												| ((color1 & 0xff) + (((color2 & 0xff) * invAlpha - (color1 & 0xff) * invAlpha) / 255));

						buffer[srcOffset] = newColor;
					}
				}
			}
		}

		private void oilPasteBuffer(CPRect srcRect, CPRect dstRect, int[] buffer, byte[] brush, int w, int alpha) {
			int[] opacityData = opacityBuffer.getData();

			int by = srcRect.top;
			for (int j = dstRect.top; j < dstRect.bottom; j++, by++) {
				int srcOffset = srcRect.left + by * w;
				int dstOffset = dstRect.left + j * getWidth();
				for (int i = dstRect.left; i < dstRect.right; i++, srcOffset++, dstOffset++) {
					int color1 = buffer[srcOffset];
					int alpha1 = (color1 >>> 24) * (brush[srcOffset] & 0xff) * alpha / (255 * 255);
					if (alpha1 <= 0) {
						continue;
					}

					int color2 = getCurLayer().getData()[dstOffset];
					int alpha2 = (color2 >>> 24);

					int newAlpha = alpha1 + alpha2 - alpha1 * alpha2 / 255;
					if (newAlpha > 0) {
						int realAlpha = alpha1 * 255 / newAlpha;
						int invAlpha = 255 - realAlpha;

						int newColor = newAlpha << 24
								| ((color1 >>> 16 & 0xff) + (((color2 >>> 16 & 0xff) * invAlpha - (color1 >>> 16 & 0xff)
										* invAlpha) / 255)) << 16
										| ((color1 >>> 8 & 0xff) + (((color2 >>> 8 & 0xff) * invAlpha - (color1 >>> 8 & 0xff)
												* invAlpha) / 255)) << 8
												| ((color1 & 0xff) + (((color2 & 0xff) * invAlpha - (color1 & 0xff) * invAlpha) / 255));

						opacityData[dstOffset] = newColor;

					}
				}
			}
		}
	}

	class CPBrushToolSmudge extends CPBrushToolDirectBrush {

		@Override
		void paintDabImplementation(CPRect srcRect, CPRect dstRect, CPBrushDab dab) {
			if (brushBuffer == null) {
				brushBuffer = new int[dab.width * dab.height];
				smudgeAccumBuffer(srcRect, dstRect, brushBuffer, dab.width, 0);
			} else {
				smudgeAccumBuffer(srcRect, dstRect, brushBuffer, dab.width, dab.alpha);
				smudgePasteBuffer(srcRect, dstRect, brushBuffer, dab.brush, dab.width);

				if (isLockAlpha()) {
					undoManager.restoreCurLayerAlpha(dstRect);
				}
			}
			opacityArea.makeEmpty();
			if (isSampleAllLayers()) {
				fusionLayers();
			}
		}

		@Override
		public void mergeOpacityBuf(CPRect dstRect, int color) {
			// Don't know what should be there
		}

		private void smudgeAccumBuffer(CPRect srcRect, CPRect dstRect, int[] buffer, int w, int alpha) {

			CPLayer layerToSample = isSampleAllLayers() ? fusion : getActiveLayer();

			int by = srcRect.top;
			for (int j = dstRect.top; j < dstRect.bottom; j++, by++) {
				int srcOffset = srcRect.left + by * w;
				int dstOffset = dstRect.left + j * getWidth();
				for (int i = dstRect.left; i < dstRect.right; i++, srcOffset++, dstOffset++) {
					int layerColor = layerToSample.getData()[dstOffset];
					int opacityAlpha = 255 - alpha;
					if (opacityAlpha > 0) {
						int destColor = buffer[srcOffset];

						int destAlpha = 255;
						int newLayerAlpha = opacityAlpha + destAlpha * (255 - opacityAlpha) / 255;
						int realAlpha = 255 * opacityAlpha / newLayerAlpha;
						int invAlpha = 255 - realAlpha;

						int newColor = (((layerColor >>> 24 & 0xff) * realAlpha + (destColor >>> 24 & 0xff) * invAlpha) / 255) << 24
								& 0xff000000
								| (((layerColor >>> 16 & 0xff) * realAlpha + (destColor >>> 16 & 0xff) * invAlpha) / 255) << 16
								& 0xff0000
								| (((layerColor >>> 8 & 0xff) * realAlpha + (destColor >>> 8 & 0xff) * invAlpha) / 255) << 8
								& 0xff00
								| (((layerColor & 0xff) * realAlpha + (destColor & 0xff) * invAlpha) / 255)
								& 0xff;

						if (newColor == destColor) {
							if ((layerColor & 0xff0000) > (destColor & 0xff0000)) {
								newColor += 1 << 16;
							} else if ((layerColor & 0xff0000) < (destColor & 0xff0000)) {
								newColor -= 1 << 16;
							}

							if ((layerColor & 0xff00) > (destColor & 0xff00)) {
								newColor += 1 << 8;
							} else if ((layerColor & 0xff00) < (destColor & 0xff00)) {
								newColor -= 1 << 8;
							}

							if ((layerColor & 0xff) > (destColor & 0xff)) {
								newColor += 1;
							} else if ((layerColor & 0xff) < (destColor & 0xff)) {
								newColor -= 1;
							}
						}

						buffer[srcOffset] = newColor;
					}
				}
			}

			if (srcRect.left > 0) {
				int fill = srcRect.left;
				for (int j = srcRect.top; j < srcRect.bottom; j++) {
					int offset = j * w;
					int fillColor = buffer[offset + srcRect.left];
					for (int i = 0; i < fill; i++) {
						buffer[offset++] = fillColor;
					}
				}
			}

			if (srcRect.right < w) {
				int fill = w - srcRect.right;
				for (int j = srcRect.top; j < srcRect.bottom; j++) {
					int offset = j * w + srcRect.right;
					int fillColor = buffer[offset - 1];
					for (int i = 0; i < fill; i++) {
						buffer[offset++] = fillColor;
					}
				}
			}

			for (int j = 0; j < srcRect.top; j++) {
				System.arraycopy(buffer, srcRect.top * w, buffer, j * w, w);
			}

			for (int j = srcRect.bottom; j < w; j++) {
				System.arraycopy(buffer, (srcRect.bottom - 1) * w, buffer, j * w, w);
			}
		}

		private void smudgePasteBuffer(CPRect srcRect, CPRect dstRect, int[] buffer, byte[] brush, int w) {
			int[] undoData = undoManager.getCurLayerPreservedData();

			int by = srcRect.top;
			for (int j = dstRect.top; j < dstRect.bottom; j++, by++) {
				int srcOffset = srcRect.left + by * w;
				int dstOffset = dstRect.left + j * getWidth();
				for (int i = dstRect.left; i < dstRect.right; i++, srcOffset++, dstOffset++) {
					int bufferColor = buffer[srcOffset];
					int opacityAlpha = (bufferColor >>> 24) * (brush[srcOffset] & 0xff) / 255;
					if (opacityAlpha > 0) {
						int destColor = undoData[dstOffset];

						int realAlpha = 255;
						int invAlpha = 255 - realAlpha;

						int newColor = (((bufferColor >>> 24 & 0xff) * realAlpha + (destColor >>> 24 & 0xff) * invAlpha) / 255) << 24
								& 0xff000000
								| (((bufferColor >>> 16 & 0xff) * realAlpha + (destColor >>> 16 & 0xff) * invAlpha) / 255) << 16
								& 0xff0000
								| (((bufferColor >>> 8 & 0xff) * realAlpha + (destColor >>> 8 & 0xff) * invAlpha) / 255) << 8
								& 0xff00
								| (((bufferColor & 0xff) * realAlpha + (destColor & 0xff) * invAlpha) / 255)
								& 0xff;

						getCurLayer().getData()[dstOffset] = newColor;
					}
				}
			}
		}
	}

	// ///////////////////////////////////////////////////////////////////////////////////
	// Layer methods
	// ///////////////////////////////////////////////////////////////////////////////////

	public void setActiveLayer(int i) {
		if (i < 0 || i >= getLayersVector().size()) {
			return;
		}

		setActiveLayerNum(i);
		curLayer = getLayersVector().get(i);
		callListenersLayerChange();
	}

	public int getActiveLayerNb() {
		return getActiveLayerNum();
	}

	public CPLayer getActiveLayer() {
		return getCurLayer();
	}

	public CPLayer getLayer(int i) {
		if (i < 0 || i >= getLayersVector().size()) {
			return null;
		}

		return getLayersVector().get(i);
	}

	//
	// Undo / Redo
	//

	public void undo() {
		if (!canUndo()) {
			return;
		}

		CPUndo undo = undoManager.getUndoList().removeFirst();
		undo.undo();
        undoManager.getRedoList().addFirst(undo);
	}

	public void redo() {
		if (!canRedo()) {
			return;
		}

		CPUndo redo = undoManager.getRedoList().removeFirst();
		redo.redo();
        undoManager.getUndoList().addFirst(redo);
	}

	boolean canUndo() {
		return !undoManager.getUndoList().isEmpty();
	}

	boolean canRedo() {
		return !undoManager.getRedoList().isEmpty();
	}

    // TODO: Move to undo manager
	void addUndo(CPUndo undo) {
		if (undoManager.getUndoList().isEmpty() || !(undoManager.getUndoList().getFirst()).merge(undo)) {
			if (undoManager.getUndoList().size() >= maxUndo) {
                undoManager.getUndoList().removeLast();
			}
            undoManager.getUndoList().addFirst(undo);
		} else {
			// Two merged changes can mean no change at all
			// don't leave a useless undo in the list
			if ((undoManager.getUndoList().getFirst()).noChange()) {
                undoManager.getUndoList().removeFirst();
			}
		}
        undoManager.getRedoList().clear();
	}

	public void clearHistory() {
		undoManager.getUndoList().clear();
        undoManager.getRedoList().clear();

		Runtime r = Runtime.getRuntime();
		r.gc();
	}

	//
	//
	//

	public int colorPicker(float x, float y) {
		// not really necessary and could potentially the repaint
		// of the canvas to miss that area
		// fusionLayers();
		if (isSampleAllLayers())
			return fusion.getPixel((int) x, (int) y) & 0xffffff;
		else
			return getCurLayer().getPixel((int) x, (int) y) & 0xffffff;
	}

	public boolean isPointWithin(float x, float y) {
		return x >= 0 && y >= 0 && (int) x < getWidth() && (int) y < getHeight();
	}

	// FIXME: 2007-01-13 I'm moving this to the CPRect class
	// find where this version is used and change the
	// code to use the CPRect version

	void clipSourceDest(CPRect srcRect, CPRect dstRect) {
		// FIXME:
		// /!\ dstRect bottom and right are ignored and instead we clip
		// against the width, height of the layer. :/
		//

		// this version would be enough in most cases (when we don't need
		// srcRect bottom and right to be clipped)
		// it's left here in case it's needed to make a faster version
		// of this function
		// dstRect.right = Math.min(width, dstRect.left + srcRect.getWidth());
		// dstRect.bottom = Math.min(height, dstRect.top + srcRect.getHeight());

		// new dest bottom/right
		dstRect.right = dstRect.left + srcRect.getWidth();
		if (dstRect.right > getWidth()) {
			srcRect.right -= dstRect.right - getWidth();
			dstRect.right = getWidth();
		}

		dstRect.bottom = dstRect.top + srcRect.getHeight();
		if (dstRect.bottom > getHeight()) {
			srcRect.bottom -= dstRect.bottom - getHeight();
			dstRect.bottom = getHeight();
		}

		// new src top/left
		if (dstRect.left < 0) {
			srcRect.left -= dstRect.left;
			dstRect.left = 0;
		}

		if (dstRect.top < 0) {
			srcRect.top -= dstRect.top;
			dstRect.top = 0;
		}
	}

	public Object[] getLayers() {
		return getLayersVector().toArray();
	}

	public int getLayersNb() {
		return getLayersVector().size();
	}

	public CPRect getSize() {
		return new CPRect(getWidth(), getHeight());
	}

	//
	// Selection methods
	//


	//
	//
	//

	public void invalidateFusion(CPRect r) {
		fusionArea.union(r);
		callListenersUpdateRegion(r);
	}

	public void invalidateFusion() {
		invalidateFusion(new CPRect(0, 0, getWidth(), getHeight()));
	}

	public void setLayerVisibility(int layer, boolean visible) {
		addUndo(new CPUndoManager.CPUndoLayerVisible(this, layer, getLayer(layer).isVisible(), visible));
		getLayer(layer).setVisible(visible);
		invalidateFusion();
		callListenersLayerChange();
	}

	public void addLayer() {
		addUndo(new CPUndoManager.CPUndoAddLayer(this, getActiveLayerNum()));

		CPLayer newLayer = new CPLayer(getWidth(), getHeight());
		newLayer.setName(getDefaultLayerName());
		getLayersVector().add(getActiveLayerNum() + 1, newLayer);
		setActiveLayer(getActiveLayerNum() + 1);

		invalidateFusion();
		callListenersLayerChange();
	}

	public void removeLayer() {
		if (getLayersVector().size() > 1) {
			addUndo(new CPUndoManager.CPUndoRemoveLayer(this, getActiveLayerNum(), getCurLayer()));
			getLayersVector().remove(getActiveLayerNum());
			setActiveLayer(getActiveLayerNum() < getLayersVector().size() ? getActiveLayerNum() : getActiveLayerNum() - 1);
			invalidateFusion();
			callListenersLayerChange();
		}
	}

	public void toggleLayers ()
	{
		int i, first_unchecked_pos = 0;
		addUndo (new CPUndoToggleLayers ());
		for (i = 0; i < getLayersVector().size (); i++)
			if (!getLayersVector().elementAt (i).isVisible())
				break;
		first_unchecked_pos = i;

		for (i = 0; i < getLayersVector().size (); i++)
			getLayersVector().elementAt (i).setVisible((first_unchecked_pos < getLayersVector().size ()));

		invalidateFusion();
		callListenersLayerChange();
	}

	public void duplicateLayer() {
		String copySuffix = " Copy";

		addUndo(new CPUndoManager.CPUndoDuplicateLayer(this, getActiveLayerNum()));
		CPLayer newLayer = new CPLayer(getWidth(), getHeight());
		newLayer.copyFrom(getLayersVector().elementAt(getActiveLayerNum()));
		if (!newLayer.getName().endsWith(copySuffix)) {
			newLayer.setName(newLayer.getName() + copySuffix);
		}
		getLayersVector().add(getActiveLayerNum() + 1, newLayer);

		setActiveLayer(getActiveLayerNum() + 1);
		invalidateFusion();
		callListenersLayerChange();
	}

	public void mergeDown(boolean createUndo) {
		if (getLayersVector().size() > 0 && getActiveLayerNum() > 0) {
			if (createUndo) {
				addUndo(new CPUndoManager.CPUndoMergeDownLayer(this, getActiveLayerNum()));
			}

			getLayersVector().elementAt(getActiveLayerNum()).fusionWithFullAlpha(getLayersVector().elementAt(getActiveLayerNum() - 1),
					new CPRect(getWidth(), getHeight()));
			getLayersVector().remove(getActiveLayerNum());
			setActiveLayer(getActiveLayerNum() - 1);

			invalidateFusion();
			callListenersLayerChange();
		}
	}

	public void mergeAllLayers(boolean createUndo) {
		if (getLayersVector().size() > 1) {
			if (createUndo) {
				addUndo(new CPUndoManager.CPUndoMergeAllLayers(this));
			}

			fusionLayers();
			getLayersVector().clear();

			CPLayer layer = new CPLayer(getWidth(), getHeight());
			layer.setName(getDefaultLayerName());
			layer.copyDataFrom(fusion);
			getLayersVector().add(layer);
			setActiveLayer(0);

			invalidateFusion();
			callListenersLayerChange();
		}
	}

	public void moveLayer(int from, int to) {
		if (from < 0 || from >= getLayersNb() || to < 0 || to > getLayersNb() || from == to) {
			return;
		}
		addUndo(new CPUndoManager.CPUndoMoveLayer(this, from, to));
		moveLayerReal(from, to);
	}

	void moveLayerReal(int from, int to) {
		CPLayer layer = getLayersVector().remove(from);
		if (to <= from) {
			getLayersVector().add(to, layer);
			setActiveLayer(to);
		} else {
			getLayersVector().add(to - 1, layer);
			setActiveLayer(to - 1);
		}

		invalidateFusion();
		callListenersLayerChange();
	}

	public void setLayerAlpha(int layer, int alpha) {
		if (getLayer(layer).getAlpha() != alpha) {
			addUndo(new CPUndoManager.CPUndoLayerAlpha(this, layer, alpha));
			getLayer(layer).setAlpha(alpha);
			invalidateFusion();
			callListenersLayerChange();
		}
	}

	public void setBlendMode(int layer, int blendMode) {
		if (getLayer(layer).getBlendMode() != blendMode) {
			addUndo(new CPUndoManager.CPUndoLayerMode(this, layer, blendMode));
			getLayer(layer).setBlendMode(blendMode);
			invalidateFusion();
			callListenersLayerChange();
		}
	}

	public void setLayerName(int layer, String name) {
		if (!getLayer(layer).getName().equals(name)) {
			addUndo(new CPUndoManager.CPUndoLayerRename(this, layer, name));
			getLayer(layer).setName(name);
			callListenersLayerChange();
		}
	}

	public void floodFill(float x, float y, int colorDistance) {
        undoManager.preserveCurLayerState();
		undoArea = new CPRect(getWidth(), getHeight());

		getCurLayer().floodFill((int) x, (int) y, curColor | 0xff000000, isSampleAllLayers() ? fusion : getCurLayer(), colorDistance);

		addUndo(new CPUndoManager.CPUndoPaint(this));
		invalidateFusion();
	}

	public void fill(int color, boolean applyToAllLayers) {
        // TODO: Limit to selection
        CPRect r = getSize();
		undoArea = r;

		if (!applyToAllLayers)
		{
			undoManager.preserveCurLayerState();

			getCurLayer().clear(r, color);
			addUndo(new CPUndoManager.CPUndoPaint(this));
		}
		else
		{
			undoBufferAll = new Vector <CPLayer> (getLayersVector().size ());
			undoBufferAll.setSize (getLayersVector().size ());
			for (int i = 0; i < getLayersVector().size (); i++)
			{
				undoBufferAll.setElementAt(new CPLayer (getWidth(), getHeight()), i);
				undoBufferAll.elementAt(i).copyFrom (getLayer (i));
				getLayersVector().elementAt(i).clear (r, color);

			}
			addUndo(new CPUndoManager.CPUndoPaintAll(this));
		}

		invalidateFusion();
	}

	public void clear(boolean applyToAllLayers) {
		fill(0xffffff, applyToAllLayers);
	}

	public void hFlip(boolean applyToAllLayers) {
		undoArea = getSize();

		if (!applyToAllLayers)
		{
            undoManager.preserveCurLayerState();

			getCurLayer().copyRegionHFlip(getSize(), undoManager.getCurLayerPreservedData());
			addUndo(new CPUndoManager.CPUndoPaint(this));
		}
		else
		{
			undoBufferAll = new Vector <CPLayer> (getLayersVector().size ());
			undoBufferAll.setSize (getLayersVector().size ());
			for (int i = 0; i < getLayersVector().size (); i++)
			{
				undoBufferAll.setElementAt(new CPLayer (getWidth(), getHeight()), i);
				undoBufferAll.elementAt(i).copyFrom (getLayer (i));
				getLayersVector().elementAt(i).copyRegionHFlip(getSize(), undoBufferAll.elementAt(i).getData());

			}
			addUndo(new CPUndoManager.CPUndoPaintAll(this));
		}

		invalidateFusion();
	}

	public void vFlip(boolean applyToAllLayers) {
		undoArea = getSize ();

		if (!applyToAllLayers)
		{
            undoManager.preserveCurLayerState();

			getCurLayer().copyRegionVFlip(getSize(), undoManager.getCurLayerPreservedData());
			addUndo(new CPUndoManager.CPUndoPaint(this));
		}
		else
		{
			undoBufferAll = new Vector <CPLayer> (getLayersVector().size ());
			undoBufferAll.setSize (getLayersVector().size ());
			for (int i = 0; i < getLayersVector().size (); i++)
			{
				undoBufferAll.setElementAt(new CPLayer (getWidth(), getHeight()), i);
				undoBufferAll.elementAt(i).copyFrom (getLayer (i));
				getLayersVector().elementAt(i).copyRegionVFlip(getSize(), undoBufferAll.elementAt(i).getData());

			}
			addUndo(new CPUndoManager.CPUndoPaintAll(this));
		}
		invalidateFusion();
	}

	public void monochromaticNoise(boolean applyToAllLayers) {
		undoArea = getSize();

		if (!applyToAllLayers)
		{
            undoManager.preserveCurLayerState();

			getCurLayer().fillWithNoise(getSize());
			addUndo(new CPUndoManager.CPUndoPaint(this));
		}
		else
		{
			undoBufferAll = new Vector <CPLayer> (getLayersVector().size ());
			undoBufferAll.setSize (getLayersVector().size ());
			for (int i = 0; i < getLayersVector().size (); i++)
			{
				undoBufferAll.setElementAt(new CPLayer (getWidth(), getHeight()), i);
				undoBufferAll.elementAt(i).copyFrom (getLayer (i));
				getLayersVector().elementAt(i).fillWithNoise(getSize());

			}
			addUndo(new CPUndoManager.CPUndoPaintAll(this));
		}

		invalidateFusion();
	}

	public void colorNoise(boolean applyToAllLayers) {
		undoArea = getSize();

		if (!applyToAllLayers)
		{
            undoManager.preserveCurLayerState();

			getCurLayer().fillWithColorNoise(getSize());
			addUndo(new CPUndoManager.CPUndoPaint(this));
		}
		else
		{
			undoBufferAll = new Vector <CPLayer> (getLayersVector().size ());
			undoBufferAll.setSize (getLayersVector().size ());
			for (int i = 0; i < getLayersVector().size (); i++)
			{
				undoBufferAll.setElementAt(new CPLayer (getWidth(), getHeight()), i);
				undoBufferAll.elementAt(i).copyFrom (getLayer (i));
				getLayersVector().elementAt(i).fillWithColorNoise(getSize());

			}
			addUndo(new CPUndoManager.CPUndoPaintAll(this));
		}

		invalidateFusion();
	}

	public void boxBlur(int radiusX, int radiusY, int iterations, boolean applyToAllLayers) {
		undoArea = getSize();

		if (!applyToAllLayers)
		{
            undoManager.preserveCurLayerState();

			for (int c = 0; c < iterations; c++) {
				getCurLayer().boxBlur(getSize(), radiusX, radiusY);
			}
			addUndo(new CPUndoManager.CPUndoPaint(this));
		}
		else
		{
			undoBufferAll = new Vector <CPLayer> (getLayersVector().size ());
			undoBufferAll.setSize (getLayersVector().size ());
			for (int i = 0; i < getLayersVector().size (); i++)
			{
				undoBufferAll.setElementAt(new CPLayer (getWidth(), getHeight()), i);
				undoBufferAll.elementAt(i).copyFrom (getLayer (i));
				for (int c = 0; c < iterations; c++) {
					getLayersVector().elementAt(i).boxBlur(getSize(), radiusX, radiusY);
				}
			}
			addUndo(new CPUndoManager.CPUndoPaintAll(this));
		}

		invalidateFusion();
	}

	public void invert(boolean applyToAllLayers) {
		undoArea = getSize();

		if (!applyToAllLayers)
		{
            undoManager.preserveCurLayerState();

			getCurLayer().invert(getSize());

			addUndo(new CPUndoManager.CPUndoPaint(this));
		}
		else
		{
			undoBufferAll = new Vector <CPLayer> (getLayersVector().size ());
			undoBufferAll.setSize (getLayersVector().size ());
			for (int i = 0; i < getLayersVector().size (); i++)
			{
				undoBufferAll.setElementAt(new CPLayer (getWidth(), getHeight()), i);
				undoBufferAll.elementAt(i).copyFrom (getLayer (i));
				getLayersVector().elementAt(i).invert(getSize());
			}
			addUndo(new CPUndoManager.CPUndoPaintAll(this));
		}

		invalidateFusion();
	}

	public void makeMonochrome(boolean applyToAllLayers, int type) {
		undoArea = getSize();

		if (!applyToAllLayers)
		{
            undoManager.preserveCurLayerState();

			getCurLayer().makeMonochrome(getSize(), type, curColor);

			addUndo(new CPUndoManager.CPUndoPaint(this));
		}
		else
		{
			undoBufferAll = new Vector <CPLayer> (getLayersVector().size ());
			undoBufferAll.setSize (getLayersVector().size ());
			for (int i = 0; i < getLayersVector().size (); i++)
			{
				undoBufferAll.setElementAt(new CPLayer (getWidth(), getHeight()), i);
				undoBufferAll.elementAt(i).copyFrom (getLayer (i));
				getLayersVector().elementAt(i).makeMonochrome(getSize(), type, curColor);
			}
			addUndo(new CPUndoManager.CPUndoPaintAll(this));
		}

		invalidateFusion();
	}

    // ////
	// Copy/Paste

    // ////////////////////////////////////////////////////
	// Miscellaneous functions

	public String getDefaultLayerName() {
		String prefix = "Layer ";
		int highestLayerNb = 0;
		for (CPLayer l : getLayersVector()) {
			if (l.getName().matches("^" + prefix + "[0-9]+$")) {
				highestLayerNb = Math.max(highestLayerNb, Integer.parseInt(l.getName().substring(prefix.length())));
			}
		}
		return prefix + (highestLayerNb + 1);
	}

	public boolean hasAlpha() {
		return fusion.hasAlpha();
	}

	// ////////////////////////////////////////////////////
	// Undo classes
	public Vector<CPLayer> getLayersVector() {
		return layers;
	}

	public void setLayers(Vector<CPLayer> layers) {
		this.layers = layers;
	}

	public int getActiveLayerNum() {
		return activeLayer;
	}

	void setActiveLayerNum(int activeLayer) {
		this.activeLayer = activeLayer;
	}

	public boolean isLockAlpha() {
		return lockAlpha;
	}

	public boolean isSampleAllLayers() {
		return sampleAllLayers;
	}

    class CPUndoToggleLayers extends CPUndo
	{
		final Vector<Boolean> mask;
		boolean         toggleType; // true - we checking everything, false - unchecking
		public CPUndoToggleLayers() {
			mask = new Vector<Boolean> ();
			mask.setSize (getLayersVector().size ());
			boolean first = false;
			toggleType = false;
			for (int i = 0; i < getLayersVector().size (); i++)
			{
				if (!first && !getLayersVector().elementAt(i).isVisible())
				{
					toggleType = true;
					first = true;
				}
				mask.setElementAt(getLayersVector().elementAt (i).isVisible(), i);
			}
		}

		@Override
		public void undo() {
			for (int i = 0; i < getLayersVector().size (); i++)
				getLayersVector().elementAt (i).setVisible(mask.elementAt(i));
			invalidateFusion();
			callListenersLayerChange();
		}

		@Override
		public void redo() {
			for (int i = 0; i < getLayersVector().size (); i++)
				getLayersVector().elementAt (i).setVisible(toggleType);
			invalidateFusion();
			callListenersLayerChange();
		}
	}

}
