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

import javax.swing.*;

import chibipaint.*;
import chibipaint.gui.CPPaletteManager.*;

class CPPalette extends JComponent {

	final CPController controller;
	private ICPPaletteContainer container;

	String title = "";

	CPPalette(CPController controller) {
		this.controller = controller;
	}

	public void setContainer(ICPPaletteContainer container) {
		this.container = container;
	}

	public ICPPaletteContainer getPaletteContainer() {
		return container;
	}
}
