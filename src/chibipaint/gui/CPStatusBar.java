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
import chibipaint.engine.CPArtwork;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;

public class CPStatusBar extends JPanel implements CPController.ICPViewListener, CPController.ICPEventListener
{

private final CPController controller;
private final JLabel memory;
private final JLabel viewport;

public CPStatusBar (CPController controller)
{
  super (new BorderLayout ());
  this.controller = controller;

  viewport = new JLabel ("");
  add (viewport, BorderLayout.LINE_START);

  memory = new JLabel ("");
  add (memory, BorderLayout.LINE_END);
  memory.addMouseListener (new MouseAdapter ()
  {

    @Override
    public void mouseClicked (MouseEvent e)
    {
      if (e.getClickCount () == 2)
        {
          Runtime r = Runtime.getRuntime ();
          r.gc ();
        }
    }
  });

  updateMemory ();
  controller.addViewListener (this);
  // controller.addCPEventListener(this);

  Timer timer = null;
  if (timer == null)
    {
      timer = new Timer (2000, new ActionListener ()
      {

        @Override
        public void actionPerformed (ActionEvent e)
        {
          updateMemory ();
        }
      });
      timer.setRepeats (true);
      timer.start ();
    }
}

@Override
public void viewChange (CPController.CPViewInfo viewInfo)
{
  DecimalFormat format = new DecimalFormat ("0.0%");
  viewport.setText (" Picture Size: " + viewInfo.width + " x " + viewInfo.height + ", Zoom: " + format.format (viewInfo.zoom));
}

void updateMemory ()
{
  DecimalFormat format = new DecimalFormat ("0.0");

  Runtime rt = java.lang.Runtime.getRuntime ();
  float maxMemory = rt.maxMemory () / (1024f * 1024f);
  float totalUsed = (rt.totalMemory () - rt.freeMemory ()) / (1024f * 1024f);
  float docMem = 0;
  float undoMem = 0;

  CPArtwork artwork = controller.getArtwork ();
  if (artwork != null)
    {
      docMem = artwork.getDocMemoryUsed () / (1024f * 1024f);
      undoMem = artwork.undoManager.getUndoMemoryUsed (artwork) / (1024f * 1024f);
    }

  if ((docMem + undoMem) / maxMemory > .5)
    {
      memory.setForeground (Color.RED);
    }
  else
    {
      memory.setForeground (Color.BLACK);
    }

  memory.setText ("Mem: " + format.format (totalUsed) + "/" + format.format (maxMemory) + ",D"
                          + format.format (docMem) + " U" + format.format (undoMem));
}

@Override
public void cpEvent ()
{
  updateMemory ();
}
}
