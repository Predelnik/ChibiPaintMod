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

import chibipaint.CPController;

import javax.swing.*;
import java.awt.*;

class CPMiscPalette extends CPPalette
{

private final CPIconButton zoomInButton;
private final CPIconButton zoomOutButton;
private final CPIconButton zoom100Button;
private final CPIconButton undoButton;
private final CPIconButton redoButton;
private CPIconButton sendButton = null;

public CPMiscPalette (CPController controller)
{
  super (controller);

  title = "Misc";
  setLayout (new FlowLayout ());

  Image icons = controller.loadImage ("icons.png");

  Component spacer;

  zoomInButton = new CPIconButton (icons, 32, 32, 13, 1);
  add (zoomInButton);
  zoomInButton.addCPActionListener (controller);
  zoomInButton.setCPActionCommand ("CPZoomIn");

  zoomOutButton = new CPIconButton (icons, 32, 32, 14, 1);
  add (zoomOutButton);
  zoomOutButton.addCPActionListener (controller);
  zoomOutButton.setCPActionCommand ("CPZoomOut");

  zoom100Button = new CPIconButton (icons, 32, 32, 15, 1);
  add (zoom100Button);
  zoom100Button.addCPActionListener (controller);
  zoom100Button.setCPActionCommand ("CPZoom100");

  spacer = new JPanel ();
  spacer.setSize (16, 32);
  add (spacer);

  undoButton = new CPIconButton (icons, 32, 32, 10, 1);
  add (undoButton);
  undoButton.addCPActionListener (controller);
  undoButton.setCPActionCommand ("CPUndo");

  redoButton = new CPIconButton (icons, 32, 32, 11, 1);
  add (redoButton);
  redoButton.addCPActionListener (controller);
  redoButton.setCPActionCommand ("CPRedo");

  if (controller.isRunningAsApplet ())
    {
      spacer = new JPanel ();
      spacer.setSize (16, 32);
      add (spacer);

      sendButton = new CPIconButton (icons, 32, 32, 12, 1);
      add (sendButton);
      sendButton.addCPActionListener (controller);
      sendButton.setCPActionCommand ("CPSend");
    }
}

void setEnabledForTransform (boolean enabled)
{
  undoButton.setEnabled (enabled);
  redoButton.setEnabled (enabled);
  if (sendButton != null)
    sendButton.setEnabled (enabled);
}

}
