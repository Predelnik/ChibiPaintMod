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

package chibipaint.util;

import cello.jtablet.TabletDevice;
import cello.jtablet.TabletManager;
import cello.jtablet.event.TabletAdapter;
import cello.jtablet.event.TabletEvent;
import cello.jtablet.event.TabletListener;
import chibipaint.gui.CPCanvas;

public class CPTablet2
{
public static void connectToCanvas (final CPCanvas c)
{
  try
    {
      TabletListener eventHandler = new TabletAdapter ()
      {
        @Override
        public void cursorMoved (TabletEvent e)
        {
          c.setCursorX (e.getX ());
          c.setCursorY (e.getY ());
          TabletDevice device = e.getDevice ();
          c.setLastPressure ((device == null || device.getPressureSupport () != TabletDevice.Support.YES) ? 1.0f : e.getPressure ());

          if (!c.isDontStealFocus ())
            {
              c.requestFocusInWindow ();
            }

          c.getActiveMode ().cursorMoveAction ();
        }

        @Override
        public void cursorExited (TabletEvent e)
        {
        }

        @Override
        public void cursorPressed (TabletEvent e)
        {
          if (!c.isCursorIn ())
            return;
          c.setModifiers (e.getModifiersEx ());
          c.setCursorX (e.getX ());
          c.setCursorY (e.getY ());
          c.setButton (e.getButton ());
          c.requestFocusInWindow ();
          c.getActiveMode ().cursorPressAction ();
        }

        @Override
        public void cursorDragged (TabletEvent e)
        {
          c.setCursorX (e.getX ());
          c.setCursorY (e.getY ());
          TabletDevice device = e.getDevice ();
          c.setLastPressure ((device == null || device.getPressureSupport () != TabletDevice.Support.YES) ? 1.0f : e.getPressure ());


          c.getActiveMode ().cursorDragAction ();
        }

        @Override
        public void cursorReleased (TabletEvent e)
        {
          c.setModifiers (e.getModifiersEx ());
          c.setButton (e.getButton ());
          c.getActiveMode ().cursorReleaseAction ();
        }
      };

      c.ShowLoadingTabletListenerMessage ();
      TabletManager manager = TabletManager.getDefaultManager ();
      manager.addTabletListener (c, eventHandler);
      c.HideLoadingTabletListenerMessage ();


    }
  catch (NoClassDefFoundError e)
    {
      e.printStackTrace ();
    }
}

}
