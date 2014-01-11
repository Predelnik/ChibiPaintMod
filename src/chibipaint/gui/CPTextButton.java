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

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;

class CPTextButton extends JButton
{
private String actionCommand;
private String actionCommandDoubleClick = null;
private final LinkedList<ActionListener> actionListeners = new LinkedList<ActionListener> ();

private boolean mouseOver = false;
private boolean mousePressed = false;
private boolean selected = false;
private final boolean onClickDown = false;

public CPTextButton ()
{
  super ();
  setAction (new textButtonAction ());
}

class textButtonAction extends AbstractAction
{
  public textButtonAction ()
  {
    super ();
  }

  public void actionPerformed (ActionEvent e)
  {
    callActionListeners ();
  }
}

public void setSelected (boolean s)
{
  if (selected != s)
    {
      selected = s;
      repaint ();
    }
}

void addCPActionListener (ActionListener l)
{
  actionListeners.addLast (l);
}

void callActionListeners ()
{
  for (Object l : actionListeners)
    {
      ActionEvent e = new ActionEvent (this, ActionEvent.ACTION_PERFORMED, actionCommand);
      ((ActionListener) l).actionPerformed (e);
    }
}
}

