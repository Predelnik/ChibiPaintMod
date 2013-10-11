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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class CPToolPalette extends CPPalette implements ActionListener
{

private final Image icons;

private void addButton (int iconIndex, String action, int mode, String actionDouble, int brushType)
{
  CPIconButton button;
  int buttonSize = 32;
  button = new CPIconButton (icons, buttonSize, buttonSize, iconIndex, 1);
  add (button);

  button.addCPActionListener (controller);
  button.addCPActionListener (this);
  button.setCPActionCommand (action);
  if (actionDouble != null)
    button.setCPActionCommandDouble (actionDouble);

  if (mode != CPController.M_INVALID && controller.getCurMode () == mode &&
          (brushType == CPController.T_INVALID || controller.getCurBrush () == brushType))
    button.setSelected (true);
}

private void addButton (int iconIndex, String action, int mode, String actionDouble)
{
  addButton (iconIndex, action, mode, null, CPController.T_INVALID);
}

private void addButton (int iconIndex, String action, int mode)
{
  addButton (iconIndex, action, mode, null);
}

private void addButton (int iconIndex, String action)
{
  addButton (iconIndex, action, CPController.M_INVALID);
}

public CPToolPalette (CPController controller)
{
  super (controller);

  title = "Tools";
  setLayout (new FlowLayout ());

  icons = controller.loadImage ("icons.png");
  CPIconButton button;

  addButton (0, "CPRectSelection", CPController.M_RECT_SELECTION);
  addButton (30, "CPFreeSelection", CPController.M_FREE_SELECTION);
  addButton (31, "CPFreeTransform", CPController.M_FREE_TRANSFORM);
  addButton (2, "CPFloodFill", CPController.M_FLOODFILL);
  addButton (29, "CPRotateCanvas", CPController.M_ROTATE_CANVAS, "CPResetCanvasRotation");
  addButton (5, "CPPencil", CPController.M_DRAW, null, CPController.T_PENCIL);
  addButton (6, "CPPen", CPController.M_DRAW, null, CPController.T_PEN);
  addButton (7, "CPAirbrush", CPController.M_DRAW, null, CPController.T_AIRBRUSH);
  addButton (18, "CPWater", CPController.M_DRAW, null, CPController.T_WATER);
  addButton (8, "CPEraser", CPController.M_DRAW, null, CPController.T_ERASER);
  addButton (9, "CPSoftEraser", CPController.M_DRAW, null, CPController.T_SOFTERASER);
  addButton (24, "CPSmudge", CPController.M_DRAW, null, CPController.T_SMUDGE);
  addButton (28, "CPBlender", CPController.M_DRAW, null, CPController.T_BLENDER);
  addButton (16, "CPDodge", CPController.M_DRAW, null, CPController.T_DODGE);
  addButton (17, "CPBurn", CPController.M_DRAW, null, CPController.T_BURN);
  addButton (23, "CPBlur", CPController.M_DRAW, null, CPController.T_BLUR);
}

@Override
public void actionPerformed (ActionEvent e)
{
  Component[] components = getComponents ();
  for (Component c : components)
    {
      if (c != e.getSource ())
        {
          ((CPIconButton) c).setSelected (false);
        }
    }

  ((CPIconButton) e.getSource ()).setSelected (true);
}
}
