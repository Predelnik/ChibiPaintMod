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

package chibipaint.gui;

import java.awt.event.*;
import java.util.*;
import java.util.prefs.Preferences;

import javax.swing.*;

import chibipaint.*;

public class CPPaletteManager implements ContainerListener {

	private final CPController controller;
	private JDesktopPane jdp;

	private Map<String, CPPalette> palettes = new HashMap<String, CPPalette>();
	private final List<CPPaletteFrame> paletteFrames = new Vector<CPPaletteFrame>();
	private final List<CPPaletteFrame> hiddenFrames = new Vector<CPPaletteFrame>();

	interface ICPPaletteContainer {

		public void addPalette(CPPalette palette);

		public void removePalette(CPPalette palette);

		public void showPalette(CPPalette palette);

		public List<CPPalette> getPalettesList();
	}

	class CPPaletteFrame extends JInternalFrame implements ICPPaletteContainer {

		private final List<CPPalette> list = new Vector<CPPalette>();

		public CPPaletteFrame(CPPalette palette) {
			super("", true, true, false, false); // resizable/closable frame
			putClientProperty("JInternalFrame.isPalette", Boolean.TRUE);
			addPalette(palette);
			setVisible(true);
		}

		@Override
		public void addPalette(CPPalette palette) {
			getContentPane().add(palette);
			setTitle(palette.title);
			palette.setContainer(this);

			list.add(palette);
		}

		@Override
		public void removePalette(CPPalette palette) {
		}

		@Override
		public void showPalette(CPPalette palette) {
		}

		@Override
		public List<CPPalette> getPalettesList() {
			return new Vector<CPPalette>(list);
		}
	}

	public CPPaletteManager(CPController controller, JDesktopPane desktop) {
		this.controller = controller;
		this.setJdp(desktop);

		desktop.addContainerListener(this);

		// Color Palette

		CPPalette palette = new CPColorPalette(controller);
		getPalettes().put("color", palette);

		CPPaletteFrame frame = new CPPaletteFrame(palette);
		paletteFrames.add(frame);

		frame.pack();
		frame.setSize(175, 175);
		frame.setLocation(0, 385);
		desktop.add(frame);

		// Brush Palette

		palette = new CPBrushPalette(controller);
		getPalettes().put("brush", palette);

		frame = new CPPaletteFrame(palette);
		paletteFrames.add(frame);

		frame.pack();
		frame.setLocation(638, 3);
		desktop.add(frame);

		// Layers Palette

		palette = new CPLayersPalette(controller);
		getPalettes().put("layers", palette);

		frame = new CPPaletteFrame(palette);
		paletteFrames.add(frame);

		frame.pack();
		frame.setSize(170, 300);
		frame.setLocation(629, 253);
		desktop.add(frame);

		// Stroke Palette

		palette = new CPStrokePalette(controller);
		getPalettes().put("stroke", palette);

		frame = new CPPaletteFrame(palette);
		paletteFrames.add(frame);

		frame.pack();
		frame.setLocation(110, 64);
		desktop.add(frame);

		// Tool Palette

		palette = new CPToolPalette(controller);
		getPalettes().put("tool", palette);

		frame = new CPPaletteFrame(palette);
		paletteFrames.add(frame);

		frame.pack();
		frame.setSize(90, 390);
		frame.setLocation(0, 0);
		desktop.add(frame);

		// Swatches Palette

		palette = new CPSwatchesPalette(controller);
		getPalettes().put("swatches", palette);

		frame = new CPPaletteFrame(palette);
		paletteFrames.add(frame);

		frame.pack();
		frame.setSize(111, 125);
		frame.setLocation(512, 5);
		desktop.add(frame);

		// Misc Palette

		palette = new CPMiscPalette(controller);
		getPalettes().put("misc", palette);

		frame = new CPPaletteFrame(palette);
		paletteFrames.add(frame);

		frame.pack();
		// frame.setSize(111, 125);
		frame.setLocation(110, 0);
		desktop.add(frame);

		// Misc Palette

		palette = new CPTexturePalette(controller);
		getPalettes().put("textures", palette);

		frame = new CPPaletteFrame(palette);
		paletteFrames.add(frame);

		frame.pack();
		frame.setSize(400, 220);
		frame.setLocation(190, 340);
		desktop.add(frame);
	}

	public void showPalette(String paletteName, boolean show) {
		CPPalette palette = getPalettes().get(paletteName);
		if (palette == null) {
			return;
		}

		showPalette(palette, show);
	}

	void showPalette(CPPalette palette, boolean show) {
		// FIXME: this will need to be replaced by something more generic
		CPPaletteFrame frame = (CPPaletteFrame) palette.getPaletteContainer();
		if (frame == null) {
			return;
		}

		if (show) {
			getJdp().add(frame, 0);
			frame.setVisible(true);
		} else {
			frame.setVisible(false);
			getJdp().remove(frame);
		}
		controller.getMainGUI().setPaletteMenuItem(palette.title, show);

		// FIXME: focus hack
		controller.canvas.grabFocus();
	}

	@Override
	public void componentAdded(ContainerEvent e) {
	}

	@Override
	public void componentRemoved(ContainerEvent e) {
		if (e.getChild() instanceof CPPaletteFrame) {
			CPPaletteFrame frame = (CPPaletteFrame) e.getChild();
			for (CPPalette palette : frame.getPalettesList()) {
				controller.getMainGUI().setPaletteMenuItem(palette.title, false);
			}
		}
	}

	public void togglePalettes() {
		if (hiddenFrames.isEmpty()) {
			for (CPPaletteFrame frame : paletteFrames) {
				if (frame.isVisible()) {
					for (CPPalette pal : frame.getPalettesList()) {
						showPalette(pal, false);
					}
					hiddenFrames.add(frame);
				}
			}
		} else {
			for (CPPaletteFrame frame : hiddenFrames) {
				if (!frame.isVisible()) {
					for (CPPalette pal : frame.getPalettesList()) {
						showPalette(pal, true);
					}
				}
			}
			hiddenFrames.clear();
		}
	}

	public void loadPalettesSettings ()
	{
		Preferences userRoot = Preferences.userRoot();
		Preferences preferences = userRoot.node( "chibipaintmod" );
		for (CPPaletteFrame frame : paletteFrames)
		{
			for (CPPalette pal : frame.getPalettesList())
			{
				int x_value = preferences.getInt(pal.title + "(x pos)", -1);
				int y_value = preferences.getInt(pal.title + "(y pos)", -1);
				boolean visibility =  preferences.getBoolean(pal.title + "(visibility)", true);

				showPalette (pal, visibility);

				if (x_value != -1 && y_value != -1)
					frame.setLocation (x_value, y_value);
			}
		}

		if (preferences.getBoolean ("Palettes Hidden", false))
			togglePalettes ();
	}

	public void savePalettesSettings ()
	{
		Preferences userRoot = Preferences.userRoot();
		Preferences preferences = userRoot.node( "chibipaintmod" );
		for (CPPaletteFrame frame : paletteFrames)
		{
			for (CPPalette pal : frame.getPalettesList())
			{
				preferences.putInt(pal.title + "(x pos)", frame.getX());
				preferences.putInt(pal.title + "(y pos)", frame.getY());
				preferences.putBoolean(pal.title + "(visibility)", frame.isVisible () || hiddenFrames.contains(frame));
			}
		}

		preferences.putBoolean ("Palettes Hidden", !hiddenFrames.isEmpty());
	}

	JDesktopPane getJdp() {
		return jdp;
	}

	void setJdp(JDesktopPane jdp) {
		this.jdp = jdp;
	}

	public Map<String, CPPalette> getPalettes() {
		return palettes;
	}

	public void setPalettes(Map<String, CPPalette> palettes) {
		this.palettes = palettes;
	}

}
