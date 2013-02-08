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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.prefs.Preferences;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import chibipaint.engine.CPArtwork;
import chibipaint.engine.CPChibiFile;
import chibipaint.engine.CPUndo;
import chibipaint.engine.CPXcfFile;
import chibipaint.gui.CPMainGUI;

public class CPControllerApplication extends CPController {

	JFrame mainFrame;
	File currentFile;
	CPUndo latestRedoAction = null;
	CPUndo latestUndoAction = null;
	boolean redoActionMayChange = false;
	boolean changed;

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("CPSavePng")) {
			savePng();
		}

		if (e.getActionCommand().equals("CPSaveChi")) {
			saveChi();
		}

		if (e.getActionCommand().equals("CPExportXcf")) {
			saveXcf();
		}

		if (e.getActionCommand().equals("CPSave")) {
			save();
		}

		if (e.getActionCommand().equals("CPLoadChi")) {
			loadChi();
		}

		if (e.getActionCommand().equals("CPNew")) {
			newDialog();
		}

		if (e.getActionCommand().startsWith("CPOpenRecent")) {
			String command = e.getActionCommand();
			int num = command.charAt(command.length() - 1) - '0';
			openRecent(num);
		}
		super.actionPerformed(e);
	}

	public CPControllerApplication(JFrame mainFrame) {
		this.mainFrame = mainFrame;
	}

	public Component getDialogParent() {
		return mainFrame;
	}

	public void resetEverything(CPArtwork newArtwork, File file) {
		((ChibiApp) mainFrame).recreateEverything(newArtwork, file);
	}

	public void updateTitle() {
		String titleString;
		titleString = (changed ? "*" : "");
		if (currentFile != null)
			titleString += currentFile.getName() + " - ChibiPaintMod";
		else
			titleString += "Untitled - ChibiPaintMod";

		// TODO: probably add some good visible progress bar
		if (((ChibiApp) mainFrame).getAppIsBusy ())
			titleString += " (Saving...)";
		mainFrame.setTitle(titleString);
	}

	public void setCurrentFile(File file) {
		if (file != null)
			currentFile = new File(file.getAbsolutePath());
		else
			currentFile = null;
		updateTitle();
	}

	public File getCurrentFile() {
		return currentFile;
	}

	public void updateChanges(CPUndo undoAction, CPUndo redoAction) {
		changed = (latestUndoAction != undoAction || latestRedoAction != redoAction); // Trivial
		// logic
		// -
		// our
		// position
		// changed
		// so
		// file
		// is
		// changed
		if (latestUndoAction == undoAction && redoAction == null) // We have
			// returned
			// to our
			// saved
			// state
			// (which
			// was
			// without
			// any redo
			// action
			// initially)
			changed = false; // So file isn't changed (This logic is needed
		// cause redo action may have changed in the
		// future)
		updateTitle();

		if ((latestUndoAction == null && latestRedoAction == null) // If
				// latestRedoaction
				// wasn't
				// set at
				// all
				|| (redoActionMayChange // Or latestRedoAction may change
						// according to the later logic
						&& (changed) // But change of position has happened
						&& (redoAction != latestUndoAction || latestUndoAction == null))) // And
			// it's
			// not
			// undo
			// change
		{
			latestRedoAction = undoAction; // Then really changing
			// latestRedoaction
			redoActionMayChange = false; // And disabling flag
		}

		if (!changed) // lastestRedoAction may change when we returned to
			// initial (saved) position
			redoActionMayChange = true; // Cause changes may be rewritten
		// Some bugs mat still be present, need further testing
	}

	public void setLatestAction(CPUndo undoAction, CPUndo redoAction) {
		latestRedoAction = redoAction;
		latestUndoAction = undoAction;
		updateChanges(undoAction, redoAction);
	}

	void openRecent(int index) {
		Preferences userRoot = Preferences.userRoot();
		Preferences preferences = userRoot.node("chibipaintmod");
		String recentFileName = preferences.get(recent_file_string(index), "");
		saveLoadImageFile(save_file_type.CHI_FILE,
				action_save_load.ACTION_LOAD, recentFileName);
	}

	void newDialog() {
		JPanel panel1 = new JPanel();

		panel1.add(new JLabel("Width:"));
		JTextField widthNum = new JTextField(
				String.valueOf(this.artwork.width), 10);
		panel1.add(widthNum);

		JPanel panel2 = new JPanel();

		panel2.add(new JLabel("Height:"));
		JTextField heightNum = new JTextField(
				String.valueOf(this.artwork.height), 10);
		panel2.add(heightNum);

		Object[] array = { "Select Width and Height:\n\n", panel1, panel2 };
		int choice = JOptionPane.showConfirmDialog(getDialogParent(), array,
				"Create New Image", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);

		if (choice == JOptionPane.OK_OPTION) {
			try {
				if (!((ChibiApp) mainFrame).confirmDialog())
					return;
				CPArtwork new_artwork = new CPArtwork(Integer.valueOf(widthNum
						.getText()), Integer.valueOf(heightNum.getText()));
				setCurrentFile(null);
				setLatestAction (null, null);
				resetEverything(new_artwork, null);
			} catch (OutOfMemoryError E) {
				JOptionPane
				.showMessageDialog(
						mainFrame,
						"Sorry, not Enough Memory. Please restart the application or try to use lesser image size.");
			}
		}
	}

	public enum save_file_type {
		PNG_FILE, CHI_FILE, XCF_FILE
	};

	public enum action_save_load {
		ACTION_SAVE, ACTION_LOAD
	}

	public boolean savePng() {
		return saveLoadImageFile(save_file_type.PNG_FILE,
				action_save_load.ACTION_SAVE, "");
	}

	public boolean saveXcf() {
		return saveLoadImageFile(save_file_type.XCF_FILE,
				action_save_load.ACTION_SAVE, "");
	}

	public boolean save() {
		if (getCurrentFile() != null)
		{
			if (!changed) // Nothing to do in that case
				return true;

			return saveLoadImageFile(save_file_type.CHI_FILE,
					action_save_load.ACTION_SAVE, getCurrentFile()
					.getAbsolutePath());
		}
		else
			return saveLoadImageFile(save_file_type.CHI_FILE,
					action_save_load.ACTION_SAVE, "");
	}

	public boolean saveChi() {
		return saveLoadImageFile(save_file_type.CHI_FILE,
				action_save_load.ACTION_SAVE, "");
	}

	public boolean loadChi() {
		return saveLoadImageFile(save_file_type.CHI_FILE,
				action_save_load.ACTION_LOAD, "");
	}

	static String recent_file_string(int i) {
		return "Recent File[" + i + "]";
	}

	static String ext = "";

	private boolean saveLoadImageFile(save_file_type type,
			action_save_load action, String file_name) {

		int returnVal = JFileChooser.CANCEL_OPTION;
		Preferences userRoot = Preferences.userRoot();
		Preferences preferences = userRoot.node("chibipaintmod");
		String directoryName = preferences.get("lastDirectory", "");
		File dir = new File(directoryName);

		switch (type) {
		case CHI_FILE:
			ext = ".chi";
			break;
		case PNG_FILE:
			ext = ".png";
			break;
		case XCF_FILE:
			ext =".xcf";
			break;
		}

		final JFileChooser fc = new JFileChooser(dir) {
			/**
			 *
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void approveSelection() {
				File f = getSelectedFile();
				if (getDialogType() == SAVE_DIALOG) {
					String filePath = f.getPath();

					if (!filePath.toLowerCase().endsWith(ext)) {
						f = new File(filePath + ext);
					}
					if (f.exists())
					{
						int result = JOptionPane.showConfirmDialog(this,
								"The file exists, overwrite?", "Existing file",
								JOptionPane.YES_NO_OPTION);
						switch (result) {
						case JOptionPane.YES_OPTION:
							super.approveSelection();
							return;
						case JOptionPane.NO_OPTION:
							return;
						case JOptionPane.CLOSED_OPTION:
							return;
						}
					}
				}
				super.approveSelection();
			}
		};

		if (file_name == "") {

			FileNameExtensionFilter filter = null;
			switch (type) {
			case CHI_FILE:
				filter = new FileNameExtensionFilter("ChibiPaint Files(*.chi)",
						"chi");
				break;
			case PNG_FILE:
				filter = new FileNameExtensionFilter("PNG Files(*.png)", "png");
				if (currentFile != null)
					fc.setSelectedFile(new File(currentFile.getName()
							.substring(0,
									currentFile.getName().lastIndexOf('.'))));

				break;
			case XCF_FILE:
				filter = new FileNameExtensionFilter("XCF Files(*.xcf)", "xcf");
				if (currentFile != null)
					fc.setSelectedFile(new File(currentFile.getName()
							.substring(0,
									currentFile.getName().lastIndexOf('.'))));
				break;
			}
			fc.setAcceptAllFileFilterUsed(false);
			fc.addChoosableFileFilter(filter);

			returnVal = 0;

			switch (action) {
			case ACTION_LOAD:
				returnVal = fc.showOpenDialog(canvas);
				break;
			case ACTION_SAVE:
				returnVal = fc.showSaveDialog(canvas);
				break;
			}
		}

		if (returnVal == JFileChooser.APPROVE_OPTION || file_name != "") {
			File selectedFile;
			if (file_name != "")
				selectedFile = new File(file_name);
			else
				selectedFile = fc.getSelectedFile();

			if (action == action_save_load.ACTION_SAVE) {
				String filePath = selectedFile.getPath();

				if (!filePath.toLowerCase().endsWith(ext)) {
					selectedFile = new File(filePath + ext);
				}
			}

			preferences.put("lastDirectory", selectedFile.getParent());
			byte[] data = null;

			// Writing file to recent

			if (action == action_save_load.ACTION_LOAD
					&& !((ChibiApp) mainFrame).confirmDialog())
				return false;

			if (!selectedFile.exists()
					&& action == action_save_load.ACTION_LOAD) {
				// TODO:
				// Here should be code about asking for removal from recent file
				// list
				return false;
			}

			if (type == save_file_type.CHI_FILE) {
				Boolean found = false;
				for (int i = 0; i < 10; i++) {
					String file_name_from_list = preferences.get("Recent File["
							+ i + "]", "");
					if (file_name_from_list.length() != 0
							&& file_name_from_list.equals(selectedFile
									.getAbsolutePath())) {
						for (int j = i - 1; j >= 0; j--)
							preferences.put(recent_file_string(j + 1),
									preferences.get(recent_file_string(j), ""));

						found = true;
						break;
					}
				}
				if (!found) {
					for (int j = 8; j >= 0; j--)
						preferences.put(recent_file_string(j + 1),
								preferences.get(recent_file_string(j), ""));
				}

				preferences.put(recent_file_string(0),
						selectedFile.getAbsolutePath());
			}

			if (action == action_save_load.ACTION_SAVE) // settting that app is busy so program won't exit until saving is done
			{
				((ChibiApp) mainFrame).setAppIsBusy (true);
			}

			switch (action) {
			case ACTION_LOAD:
				switch (type) {
				case CHI_FILE:
					FileInputStream fos = null;
					try {
						fos = new FileInputStream(selectedFile);
					} catch (FileNotFoundException e1) {
						return false;
					}
					try {
						CPArtwork artwork = CPChibiFile.read(fos);
						setLatestAction (null, null);
						resetEverything(artwork, selectedFile);
					} catch (OutOfMemoryError E) {
						JOptionPane
						.showMessageDialog(
								mainFrame,
								"Sorry, not Enough Memory. Please restart the application or try to use lesser image size.");
					}

					try {
						fos.close();
					} catch (IOException e2) {
						return false;
					}
					break;
				case PNG_FILE:
					// Do nothing for now
					// TODO: add png reading (import)
					break;
				case XCF_FILE:
					// Do nothing for now
					// TODO: add xcf reading (import)
					break;
				}
				break;
			case ACTION_SAVE:
				switch (type) {
				case CHI_FILE:
					ByteArrayOutputStream chibiFileStream = new ByteArrayOutputStream(
							1024);
					CPChibiFile.write(chibiFileStream, artwork);
					data = chibiFileStream.toByteArray();
					break;
				case XCF_FILE:
					// For now Xcf works only with FileOutputStream
					// TODO: unify this stuff
					FileOutputStream fos;
					try {
						fos = new FileOutputStream(selectedFile);
					} catch (FileNotFoundException e2) {
						return false;
					}
					try {
						CPXcfFile.write(fos, artwork);
						fos.close();
						((ChibiApp) mainFrame).setAppIsBusy (false);
					} catch (IOException e) {
						((ChibiApp) mainFrame).setAppIsBusy (false);
						return false;
					}
					return true;
				case PNG_FILE:
					data = getPngData(canvas.img);
					break;
				}
				FileOutputStream fos = null;
				try {
					fos = new FileOutputStream(selectedFile);
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					((ChibiApp) mainFrame).setAppIsBusy (false);
					return false;
				}
				try {
					fos.write(data);
					fos.close();
				} catch (IOException e) {
					((ChibiApp) mainFrame).setAppIsBusy (false);
					return false;
				}
				((ChibiApp) mainFrame).setAppIsBusy (false);
				break;
			}

			// Adding name to frame title
			setCurrentFile(selectedFile);

			if (action == action_save_load.ACTION_SAVE) {
				((ChibiApp) mainFrame).resetMainMenu();
				setLatestAction(artwork.getUndoList().size() > 0 ? artwork
						.getUndoList().getFirst() : null, artwork
						.getRedoList().size() > 0 ? artwork.getRedoList()
								.getFirst() : null);
				((ChibiApp) mainFrame).setAppIsBusy (false);
			}

			return true;
		}
		return false;
	}

	void saveControllerSettings() {
		Preferences userRoot = Preferences.userRoot();
		Preferences preferences = userRoot.node("chibipaintmod");
		preferences.putInt ("Mode", getCurMode());
		preferences.putInt ("Brush", getCurBrush());
		for (int i = 0; i < T_MAX; i++) {
			preferences.putInt("Tool " + String.valueOf(i) + " - Type",
					tools[i].type);
			preferences.putInt("Tool " + String.valueOf(i) + " - Size",
					tools[i].size);
			preferences.putInt("Tool " + String.valueOf(i) + " - Alpha",
					tools[i].alpha);
			preferences.putFloat("Tool " + String.valueOf(i) + " - Color",
					tools[i].resat);
			preferences.putFloat("Tool " + String.valueOf(i) + " - Blend",
					tools[i].bleed);
			preferences.putFloat("Tool " + String.valueOf(i) + " - Spacing",
					tools[i].spacing);
			preferences.putFloat("Tool " + String.valueOf(i) + " - Scattering",
					tools[i].scattering);
			preferences.putFloat("Tool " + String.valueOf(i) + " - Smoothing",
					tools[i].smoothing);
			preferences.putBoolean("Tool " + String.valueOf(i)
					+ " - Alpha from Pressure", tools[i].pressureAlpha);
			preferences.putBoolean("Tool " + String.valueOf(i)
					+ " - Size from Pressure", tools[i].pressureSize);
			preferences.putBoolean("Tool " + String.valueOf(i)
					+ " - Scattering from Pressure",
					tools[i].pressureScattering);
		}
	}

	void loadUrgentSettings() {
		// Settings that should be loaded before gui to avoid rewriting a lots of code
		Preferences userRoot = Preferences.userRoot();
		Preferences preferences = userRoot.node("chibipaintmod");
		setMode (preferences.getInt ("Mode", getCurMode()));
		if (getCurMode() == M_DRAW)
			setTool (preferences.getInt ("Brush", getCurBrush()));
	}

	void loadControllerSettings() {
		Preferences userRoot = Preferences.userRoot();
		Preferences preferences = userRoot.node("chibipaintmod");

		setMode (preferences.getInt ("Mode", getCurMode())); // Note these settings reading two times
		if (getCurMode() == M_DRAW)					         // Because we should set them one more time after enabling
			setTool (preferences.getInt ("Brush", getCurBrush())); // of some canvas listeners

		for (int i = 0; i < T_MAX; i++) {
			tools[i].type = preferences.getInt("Tool " + String.valueOf(i)
					+ " - Type", tools[i].type);
			tools[i].size = preferences.getInt("Tool " + String.valueOf(i)
					+ " - Size", tools[i].size);
			tools[i].alpha = preferences.getInt("Tool " + String.valueOf(i)
					+ " - Alpha", tools[i].alpha);
			tools[i].resat = preferences.getFloat("Tool " + String.valueOf(i)
					+ " - Color", tools[i].resat);
			tools[i].bleed = preferences.getFloat("Tool " + String.valueOf(i)
					+ " - Blend", tools[i].bleed);
			tools[i].spacing = preferences.getFloat("Tool " + String.valueOf(i)
					+ " - Spacing", tools[i].spacing);
			tools[i].scattering = preferences.getFloat(
					"Tool " + String.valueOf(i) + " - Scattering",
					tools[i].scattering);
			tools[i].smoothing = preferences.getFloat(
					"Tool " + String.valueOf(i) + " - Smoothing",
					tools[i].smoothing);
			tools[i].pressureAlpha = preferences.getBoolean(
					"Tool " + String.valueOf(i) + " - Alpha from Pressure",
					tools[i].pressureAlpha);
			tools[i].pressureSize = preferences.getBoolean(
					"Tool " + String.valueOf(i) + " - Size from Pressure",
					tools[i].pressureSize);
			tools[i].pressureScattering = preferences.getBoolean("Tool "
					+ String.valueOf(i) + " - Scattering from Pressure",
					tools[i].pressureScattering);
		}
	}
}