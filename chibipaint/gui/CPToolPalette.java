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

package chibipaint.gui;

import java.awt.*;
import java.awt.event.*;

import chibipaint.*;

public class CPToolPalette extends CPPalette implements ActionListener {

	public CPToolPalette(CPController controller) {
		super(controller);

		title = "Tools";
		setLayout(new FlowLayout());

		Image icons = controller.loadImage("icons.png");
		CPIconButton button;

		button = new CPIconButton(icons, 32, 32, 0, 1);
		add(button);

		button.addCPActionListener(controller);
		button.addCPActionListener(this);
		button.setCPActionCommand("CPRectSelection");
		if (controller.getCurMode() == CPController.M_RECT_SELECTION)
			button.setSelected (true);

		button = new CPIconButton(icons, 32, 32, 1, 1);
		add(button);

		button.addCPActionListener(controller);
		button.addCPActionListener(this);
		button.setCPActionCommand("CPMoveTool");
		if (controller.getCurMode() == CPController.M_MOVE_TOOL)
			button.setSelected (true);

		button = new CPIconButton(icons, 32, 32, 2, 1);
		add(button);

		button.addCPActionListener(controller);
		button.addCPActionListener(this);
		button.setCPActionCommand("CPFloodFill");
		if (controller.getCurMode() == CPController.M_FLOODFILL)
			button.setSelected (true);

		button = new CPIconButton(icons, 32, 32, 29, 1);
		add(button);

		button.addCPActionListener(controller);
		button.addCPActionListener(this);
		button.setCPActionCommand("CPRotateCanvas");
		button.setCPActionCommandDouble("CPResetCanvasRotation");
		if (controller.getCurMode() == CPController.M_ROTATE_CANVAS)
			button.setSelected (true);

		button = new CPIconButton(icons, 32, 32, 5, 1);
		add(button);

		button.addCPActionListener(controller);
		button.addCPActionListener(this);
		button.setCPActionCommand("CPPencil");
		if (   controller.getCurMode() == CPController.M_DRAW
				&& controller.getCurBrush() == CPController.T_PENCIL)
			button.setSelected (true);

		button = new CPIconButton(icons, 32, 32, 6, 1);
		add(button);

		button.addCPActionListener(controller);
		button.addCPActionListener(this);
		button.setCPActionCommand("CPPen");
		if (   controller.getCurMode() == CPController.M_DRAW
				&& controller.getCurBrush() == CPController.T_PEN)
			button.setSelected (true);

		button = new CPIconButton(icons, 32, 32, 7, 1);
		add(button);

		button.addCPActionListener(controller);
		button.addCPActionListener(this);
		button.setCPActionCommand("CPAirbrush");
		if (   controller.getCurMode() == CPController.M_DRAW
				&& controller.getCurBrush() == CPController.T_AIRBRUSH)
			button.setSelected (true);

		button = new CPIconButton(icons, 32, 32, 18, 1);
		add(button);

		button.addCPActionListener(controller);
		button.addCPActionListener(this);
		button.setCPActionCommand("CPWater");
		if (   controller.getCurMode() == CPController.M_DRAW
				&& controller.getCurBrush() == CPController.T_WATER)
			button.setSelected (true);

		button = new CPIconButton(icons, 32, 32, 8, 1);
		add(button);

		button.addCPActionListener(controller);
		button.addCPActionListener(this);
		button.setCPActionCommand("CPEraser");
		if (   controller.getCurMode() == CPController.M_DRAW
				&& controller.getCurBrush() == CPController.T_ERASER)
			button.setSelected (true);

		button = new CPIconButton(icons, 32, 32, 9, 1);
		add(button);

		button.addCPActionListener(controller);
		button.addCPActionListener(this);
		button.setCPActionCommand("CPSoftEraser");
		if (   controller.getCurMode() == CPController.M_DRAW
				&& controller.getCurBrush() == CPController.T_SOFTERASER)
			button.setSelected (true);

		button = new CPIconButton(icons, 32, 32, 24, 1);
		add(button);

		button.addCPActionListener(controller);
		button.addCPActionListener(this);
		button.setCPActionCommand("CPSmudge");
		if (   controller.getCurMode() == CPController.M_DRAW
				&& controller.getCurBrush() == CPController.T_SMUDGE)
			button.setSelected (true);

		button = new CPIconButton(icons, 32, 32, 28, 1);
		add(button);

		button.addCPActionListener(controller);
		button.addCPActionListener(this);
		button.setCPActionCommand("CPBlender");
		if (   controller.getCurMode() == CPController.M_DRAW
				&& controller.getCurBrush() == CPController.T_BLENDER)
			button.setSelected (true);

		button = new CPIconButton(icons, 32, 32, 16, 1);
		add(button);

		button.addCPActionListener(controller);
		button.addCPActionListener(this);
		button.setCPActionCommand("CPDodge");
		if (   controller.getCurMode() == CPController.M_DRAW
				&& controller.getCurBrush() == CPController.T_DODGE)
			button.setSelected (true);

		button = new CPIconButton(icons, 32, 32, 17, 1);
		add(button);

		button.addCPActionListener(controller);
		button.addCPActionListener(this);
		button.setCPActionCommand("CPBurn");
		if (   controller.getCurMode() == CPController.M_DRAW
				&& controller.getCurBrush() == CPController.T_BURN)
			button.setSelected (true);

		button = new CPIconButton(icons, 32, 32, 23, 1);
		add(button);

		button.addCPActionListener(controller);
		button.addCPActionListener(this);
		button.setCPActionCommand("CPBlur");
		if (   controller.getCurMode() == CPController.M_DRAW
				&& controller.getCurBrush() == CPController.T_BLUR)
			button.setSelected (true);

	}

	public void actionPerformed(ActionEvent e) {
		Component[] components = getComponents();
		for (Component c : components) {
			if (c != e.getSource()) {
				((CPIconButton) c).setSelected(false);
			}
		}

		((CPIconButton) e.getSource()).setSelected(true);
	}
}
