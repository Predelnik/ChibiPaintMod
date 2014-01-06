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

import chibipaint.controller.CPController;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class CPToolsPalette extends CPPalette implements ActionListener
{

public CPToolsPalette (CPController controller)
{
  super (controller);

  title = "Tools";
  setLayout (new FlowLayout ());

  CPIconButton button;
  icons = controller.loadImage ("icons.png");
  addIconButton (0, "CPRectSelection", CPController.M_RECT_SELECTION);
  addIconButton (30, "CPFreeSelection", CPController.M_FREE_SELECTION);
  addIconButton (2, "CPFloodFill", CPController.M_FLOODFILL);
  addIconButton (29, "CPRotateCanvas", CPController.M_ROTATE_CANVAS, "CPResetCanvasRotation");
  addIconButton (5, "CPPencil", CPController.M_DRAW, null, CPController.T_PENCIL);
  addIconButton (6, "CPPen", CPController.M_DRAW, null, CPController.T_PEN);
  addIconButton (7, "CPAirbrush", CPController.M_DRAW, null, CPController.T_AIRBRUSH);
  addIconButton (18, "CPWater", CPController.M_DRAW, null, CPController.T_WATER);
  addIconButton (8, "CPEraser", CPController.M_DRAW, null, CPController.T_ERASER);
  addIconButton (9, "CPSoftEraser", CPController.M_DRAW, null, CPController.T_SOFTERASER);
  addIconButton (24, "CPSmudge", CPController.M_DRAW, null, CPController.T_SMUDGE);
  addIconButton (28, "CPBlender", CPController.M_DRAW, null, CPController.T_BLENDER);
  addIconButton (16, "CPDodge", CPController.M_DRAW, null, CPController.T_DODGE);
  addIconButton (17, "CPBurn", CPController.M_DRAW, null, CPController.T_BURN);
  addIconButton (23, "CPBlur", CPController.M_DRAW, null, CPController.T_BLUR);
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
