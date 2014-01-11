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
import chibipaint.controller.CPCommandSettings;
import chibipaint.controller.CPCommonController;
import chibipaint.controller.ICPController;
import chibipaint.engine.CPBrushInfo;

import java.awt.*;

public class CPStrokePalette extends CPPalette implements ICPController, CPCommonController.ICPToolListener
{

private final CPIconButton freeHandButton;
private final CPIconButton lineButton;
private final CPIconButton bezierButton;

public CPStrokePalette (CPCommonController controller)
{
  super (controller);

  title = "Stroke";
  setLayout (new FlowLayout ());

  Image icons = controller.loadImage ("icons.png");

  freeHandButton = new CPIconButton (icons, 32, 32, 19, 1);
  add (freeHandButton);

  freeHandButton.addController (controller);
  freeHandButton.addController (this);
  freeHandButton.setCPActionCommand (CPCommandId.FreeHand);
  freeHandButton.setSelected (true);

  lineButton = new CPIconButton (icons, 32, 32, 20, 1);
  add (lineButton);

  lineButton.addController (controller);
  lineButton.addController (this);
  lineButton.setCPActionCommand (CPCommandId.Line);

  bezierButton = new CPIconButton (icons, 32, 32, 22, 1);
  add (bezierButton);

  bezierButton.addController (controller);
  bezierButton.addController (this);
  bezierButton.setCPActionCommand (CPCommandId.Bezier);

  controller.addToolListener (this);
}

@Override
public void newTool (int tool, CPBrushInfo toolInfo)
{
  freeHandButton.setSelected (toolInfo.strokeMode == CPBrushInfo.SM_FREEHAND);
  lineButton.setSelected (toolInfo.strokeMode == CPBrushInfo.SM_LINE);
  bezierButton.setSelected (toolInfo.strokeMode == CPBrushInfo.SM_BEZIER);
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
