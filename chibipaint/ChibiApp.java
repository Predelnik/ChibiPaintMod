/*
	ChibiPaint
    Copyright (c) 2006-2008 Marc Schefer

    This file is part of ChibiPaint.

    ChibiPaint is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    ChibiPaint is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with ChibiPaint. If not, see <http://www.gnu.org/licenses/>.

 */

package chibipaint;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.prefs.Preferences;

import javax.swing.*;

import chibipaint.engine.*;
import chibipaint.gui.*;

public class ChibiApp extends JFrame {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	CPControllerApplication controller;
	CPMainGUI mainGUI;

	public ChibiApp() {
		super("ChibiPaintMod");

		controller = new CPControllerApplication(this);

		controller.setCurrentFile (null);
		controller.setArtwork(new CPArtwork(600, 450));

		// FIXME: set a default tool so that we can start drawing
		controller.setTool(CPController.T_PEN);

		mainGUI = new CPMainGUI(controller);

		setContentPane(mainGUI.getGUI());
		setJMenuBar(mainGUI.getMenuBar());

		mainGUI.getPaletteManager ().loadPalettesSettings();

		final JFrame frame = this;

    	frame.addWindowListener(new WindowAdapter() {
    	    public void windowClosing(WindowEvent e) {
    			SaveWindowSettings (frame);
    			mainGUI.getPaletteManager ().savePalettesSettings();
    	     }
    	});
	}

	private static void LoadWindowSettings (JFrame frame) {

		Preferences userRoot = Preferences.userRoot();
	    Preferences preferences = userRoot.node( "chibipaintmod" );
		int s = preferences.getInt ("window_state", -1);
		int h = preferences.getInt("window_height" , -1);
		int w = preferences.getInt("window_width" , -1);
		if (s != -1)
			{
				frame.setExtendedState (s);
			}
		if (h != -1 && w != -1)
			frame.setSize (w, h);
		else
			frame.setSize(800, 600);

		frame.validate();
		frame.setVisible(true);
	}

	private static void SaveWindowSettings (JFrame frame) {

		Preferences userRoot = Preferences.userRoot();
	    Preferences preferences = userRoot.node( "chibipaintmod" );
		int s = frame.getExtendedState ();;
		preferences.putInt ("window_state", s);
		int h = frame.getHeight ();
		int w = frame.getWidth ();
		preferences.putInt ("window_height", h);
		preferences.putInt ("window_width", w);
	}

	private static void createChibiApp() {
		JFrame frame = new ChibiApp();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		LoadWindowSettings (frame);
	}

	public static void main(String[] args) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				createChibiApp();
			}
		});
	}

	public void windowClosing(WindowEvent e) {
		SaveWindowSettings (this);
		mainGUI.getPaletteManager ().savePalettesSettings();
	}

	public void recreateEverything(CPArtwork artwork, File file)
	{
		controller = new CPControllerApplication(this);

		controller.setArtwork(artwork);

		// FIXME: set a default tool so that we can start drawing
		controller.setTool(CPController.T_PEN);
		controller.setCurrentFile (file);
		mainGUI.getPaletteManager ().savePalettesSettings();

		mainGUI = new CPMainGUI(controller);

		setContentPane(mainGUI.getGUI());
		setJMenuBar(mainGUI.getMenuBar());

		mainGUI.getPaletteManager ().loadPalettesSettings();
	}
}
