package chibipaint.util;

import cello.jtablet.TabletDevice;
import cello.jtablet.TabletManager;
import cello.jtablet.event.TabletAdapter;
import cello.jtablet.event.TabletEvent;
import cello.jtablet.event.TabletListener;
import chibipaint.gui.CPCanvas;

public class CPTablet2 {
	public static void connectToCanvas (final CPCanvas c)
	{
		try
		{
			TabletListener eventHandler = new TabletAdapter() {
				public void cursorMoved(TabletEvent e) {
					c.setCursorX(e.getX ());
					c.setCursorY(e.getY ());
					TabletDevice device = e.getDevice();
					c.setLastPressure((device == null || device.getPressureSupport() != TabletDevice.Support.YES)  ? 1.0f : e.getPressure ());

					if (!c.isDontStealFocus()) {
						c.requestFocusInWindow();
					}

					c.getActiveMode().cursorMoveAction ();
				};

				public void cursorExited(TabletEvent e) {
				}

				public void cursorPressed(TabletEvent e) {
					c.setModifiers(e.getModifiersEx());
					c.setCursorX(e.getX ());
					c.setCursorY(e.getY ());
					c.setButton(e.getButton ());
					c.requestFocusInWindow();
					c.getActiveMode().cursorPressAction();
				}

				public void cursorDragged(TabletEvent e) {
					c.setCursorX(e.getX());
					c.setCursorY(e.getY());
					TabletDevice device = e.getDevice();
					c.setLastPressure((device == null || device.getPressureSupport() != TabletDevice.Support.YES)  ? 1.0f : e.getPressure ());


					c.getActiveMode().cursorDragAction ();
				}

				public void cursorReleased(TabletEvent e) {
					c.setButton(e.getButton ());
					c.getActiveMode().cursorReleaseAction ();
				}
			};
			TabletManager manager = TabletManager.getDefaultManager();
			manager.addTabletListener(c, eventHandler);
		}
		catch (NoClassDefFoundError e)
		{
		};
	}

}
