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
