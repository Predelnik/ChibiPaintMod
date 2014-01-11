/*
 * ChibiPaintMod
 *     Copyright (c) 2012-2014 Sergey Semushin
 *     Copyright (c) 2006-2008 Marc Schefer
 *
 *     This file is part of ChibiPaintMod (previously ChibiPaint).
 *
 *     ChibiPaintMod is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     ChibiPaintMod is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with ChibiPaintMod. If not, see <http://www.gnu.org/licenses/>.
 */

package chibipaint.gui;

import chibipaint.controller.CPCommandId;
import chibipaint.controller.CPCommonController;
import chibipaint.controller.ICPController;
import chibipaint.gui.CPPaletteManager.ICPPaletteContainer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class CPPalette extends JComponent
{

protected Image icons = null;
final CPCommonController controller;
private ICPPaletteContainer container;

String title = "";

CPPalette (CPCommonController controller)
{
  this.controller = controller;
}

public void setContainer (ICPPaletteContainer container)
{
  this.container = container;
}

public ICPPaletteContainer getPaletteContainer ()
{
  return container;
}

protected JLabel addLabel (int x, int y, String text)
{
  // transform controls
  JLabel label = new JLabel ();
  label.setText (text);
  label.setLocation (x, y);
  label.setSize (label.getPreferredSize ());
  add (label);
  return label;
}

protected JButton addTextButton (int x, int y, int width, int height, String text, final CPCommandId commandId)
{
  // transform controls
  CPTextButton button = new CPTextButton ();
  button.setLocation (x, y);
  button.setSize (width, height);
  button.setText (text);
  button.addCPActionListener (new ActionListener ()
  {
    @Override
    public void actionPerformed (ActionEvent e)
    {
      controller.performCommand (commandId, null);
    }
  });
  add (button);
  return button;
}

protected CPIconButton addIconButton (int iconIndex, final CPCommandId commandId, int mode, CPCommandId commandDoubleId, int brushType)
{
  CPIconButton button;
  int buttonSize = 32;
  button = new CPIconButton (icons, buttonSize, buttonSize, iconIndex, 1);
  add (button);

  button.addController (controller);
  // If inherited class isn't listener itself than we're supposing that controller is actual listener
  if (ICPController.class.isAssignableFrom (getClass ()))
    button.addController ((ICPController) this);

  button.setCPActionCommand (commandId);
  button.setCPActionCommandDouble (commandDoubleId);

  if (mode != CPCommonController.M_INVALID && controller.getCurMode () == mode &&
          (brushType == CPCommonController.T_INVALID || controller.getCurBrush () == brushType))
    button.setSelected (true);
  return button;
}

protected CPIconButton addIconButton (int iconIndex, CPCommandId id, int mode, String actionDouble)
{
  return addIconButton (iconIndex, id, mode, null, CPCommonController.T_INVALID);
}

protected CPIconButton addIconButton (int iconIndex, CPCommandId id, int mode)
{
  return addIconButton (iconIndex, id, mode, null);
}

protected CPIconButton addIconButton (int iconIndex, CPCommandId id)
{
  return addIconButton (iconIndex, id, CPCommonController.M_INVALID);
}

protected void addSpacer ()
{
  JPanel spacer = new JPanel ();
  spacer.setSize (16, 32);
  add (spacer);
}
}
