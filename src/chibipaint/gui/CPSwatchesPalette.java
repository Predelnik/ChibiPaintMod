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

import chibipaint.controller.CPCommonController;
import chibipaint.util.CPColor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

class CPSwatchesPalette extends CPPalette
{

private final int[] initColors = {0xffffff, 0x000000, 0xff0000, 0x00ff00, 0x0000ff};

public CPSwatchesPalette (CPCommonController controller)
{
  super (controller);

  title = "Color Swatches";
  // innerContainer.setSize(innerContainer.getPreferredSize());
  setLayout (new GridLayout (5, 5));

  CPColorSwatch swatch;
  for (int i = 0; i < 25; i++)
    {
      swatch = new CPColorSwatch ();
      if (i < initColors.length)
        {
          swatch.setColor (initColors[i]);
        }
      add (swatch);
    }

  setSize (160, 180);
}

public class CPColorSwatch extends JComponent implements MouseListener
{

  CPColor color = null;

  public CPColorSwatch ()
  {
    addMouseListener (this);
  }

  @Override
  public void update (Graphics g)
  {
    paint (g);
  }

  @Override
  public void paint (Graphics g)
  {
    Dimension d = getSize ();
    if (color != null)
      {
        g.setColor (new Color (color.getRgb ()));
        g.fillRect (0, 0, d.width - 1, d.height - 1);
      }
    else
      {
        g.setColor (Color.lightGray);
        g.fillRect (0, 0, d.width - 1, d.height - 1);

        g.setColor (Color.black);
        g.drawLine (0, 0, d.width - 2, d.height - 2);
        g.drawLine (d.width - 2, 0, 0, d.height - 2);
      }
    g.setColor (Color.black);
    g.drawRect (0, 0, d.width - 2, d.height - 2);

  }

  @Override
  public void mousePressed (MouseEvent e)
  {
    if ((e.getModifiers () & InputEvent.BUTTON1_MASK) != 0 && color != null)
      {
        controller.setCurColor (color);
        repaint ();
      }
    if ((e.getModifiers () & InputEvent.BUTTON2_MASK) != 0 && color != null)
      {
        color = null;
        repaint ();
      }
    if ((e.getModifiers () & InputEvent.BUTTON3_MASK) != 0
            || ((e.getModifiers () & InputEvent.BUTTON1_MASK) != 0 && color == null))
      {
        color = (CPColor) controller.getCurColor ().clone ();
        repaint ();
      }
  }

  void setColor (int color)
  {
    this.color = new CPColor (color);
    repaint ();
  }

  @Override
  public void mouseEntered (MouseEvent e)
  {
  }

  @Override
  public void mouseExited (MouseEvent e)
  {
  }

  @Override
  public void mouseClicked (MouseEvent e)
  {
  }

  @Override
  public void mouseReleased (MouseEvent e)
  {
  }
}
}
