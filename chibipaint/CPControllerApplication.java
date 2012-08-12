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
import java.io.File;

import javax.swing.*;

import chibipaint.engine.CPArtwork;
import chibipaint.engine.CPUndo;

public class CPControllerApplication extends CPController {

	JFrame mainFrame;
	File currentFile;
	CPUndo latestAction = null;
	boolean changed;

	public CPControllerApplication(JFrame mainFrame) {
		this.mainFrame = mainFrame;
	}

	public Component getDialogParent() {
		return mainFrame;
	}

	public void resetEverything(CPArtwork newArtwork, File file)
	{
		((ChibiApp) mainFrame).recreateEverything (newArtwork, file);
	}

	private void updateTitle ()
	{
		String titleString;
		titleString = (changed ? "*" : "");
		if (currentFile != null)
		  titleString += currentFile.getName () + " - ChibiPaintMod";
		else
		  titleString += "Untitled - ChibiPaintMod";
		mainFrame.setTitle (titleString);
	}

	public void setCurrentFile (File file)
	{
		if (file != null)
			currentFile = new File (file.getAbsolutePath ());
		else
			currentFile = null;
		updateTitle ();
	}

	public File getCurrentFile ()
	{
		return currentFile;
	}

	public void updateChanges (CPUndo action)
	{
		changed = (latestAction != action);
		updateTitle ();
	}

	public void setLatestAction (CPUndo action)
	{
		changed = false;
		latestAction = action;
		updateTitle ();
	}
}
