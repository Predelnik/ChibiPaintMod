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

import chibipaint.controller.CPCommandId;
import chibipaint.controller.CPCommandSettings;
import chibipaint.controller.CPCommonController;
import chibipaint.controller.ICPController;

import java.awt.*;

class CPToolsPalette extends CPPalette implements ICPController
{

public CPToolsPalette (CPCommonController controller)
{
  super (controller);

  title = "Tools";
  setLayout (new FlowLayout ());

  CPIconButton button;
  icons = controller.loadImage ("icons.png");
  addIconButton (0, CPCommandId.RectSelection, CPCommonController.M_RECT_SELECTION);
  addIconButton (30, CPCommandId.FreeSelection, CPCommonController.M_FREE_SELECTION);
  addIconButton (2, CPCommandId.FloodFill, CPCommonController.M_FLOODFILL);
  addIconButton (29, CPCommandId.RotateCanvas, CPCommonController.M_ROTATE_CANVAS, "CPResetCanvasRotation");
  addIconButton (5, CPCommandId.Pencil, CPCommonController.M_DRAW, null, CPCommonController.T_PENCIL);
  addIconButton (6, CPCommandId.Pen, CPCommonController.M_DRAW, null, CPCommonController.T_PEN);
  addIconButton (7, CPCommandId.AirBrush, CPCommonController.M_DRAW, null, CPCommonController.T_AIRBRUSH);
  addIconButton (18, CPCommandId.Water, CPCommonController.M_DRAW, null, CPCommonController.T_WATER);
  addIconButton (8, CPCommandId.Eraser, CPCommonController.M_DRAW, null, CPCommonController.T_ERASER);
  addIconButton (9, CPCommandId.SoftEraser, CPCommonController.M_DRAW, null, CPCommonController.T_SOFTERASER);
  addIconButton (24, CPCommandId.Smudge, CPCommonController.M_DRAW, null, CPCommonController.T_SMUDGE);
  addIconButton (28, CPCommandId.Blender, CPCommonController.M_DRAW, null, CPCommonController.T_BLENDER);
  addIconButton (16, CPCommandId.Dodge, CPCommonController.M_DRAW, null, CPCommonController.T_DODGE);
  addIconButton (17, CPCommandId.Burn, CPCommonController.M_DRAW, null, CPCommonController.T_BURN);
  addIconButton (23, CPCommandId.Blur, CPCommonController.M_DRAW, null, CPCommonController.T_BLUR);
}

@Override
public void performCommand (CPCommandId commandId, CPCommandSettings commandSettings)
{
  Component[] components = getComponents ();
  CPIconButton sourceButton = ((CPCommandSettings.sourceIconButton) commandSettings).button;
  for (Component c : components)
    {
      if (c != sourceButton)
        {
          ((CPIconButton) c).setSelected (false);
        }
    }
  sourceButton.setSelected (true);
}
}
