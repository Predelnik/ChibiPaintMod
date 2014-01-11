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

import java.lang.reflect.Method;

public class CPTablet
{

private static CPTablet ref;

public static CPTablet getRef ()
{
  if (ref == null)
    {
      ref = new CPTablet ();
    }
  return ref;
}

private boolean tabletOK = false;
private Object tablet;
private Method mPoll;
private Method mGetPressure;
private Method mGetPressureExtent;

private int pressure = 0;
private int pressureExtent = 1;

private CPTablet ()
{
  try
    {
      Class<?> tabletClass = Class.forName ("cello.tablet.JTablet");
      tablet = tabletClass.newInstance ();

      mPoll = tabletClass.getMethod ("poll", (Class[]) null);
      mGetPressure = tabletClass.getMethod ("getPressure", (Class[]) null);
      mGetPressureExtent = tabletClass.getMethod ("getPressureExtent", (Class[]) null);
      // tablet_getAngle = jtablet.getMethod("getAngle",null);
      //
      // tablet_getOrientation = jtablet.getMethod("getOrientation",null);
      // tablet_getButtons = jtablet.getMethod("getButtons",null);

      tabletOK = true;
    }
  catch (Exception e)
    {
      System.out.print (e.toString ());
    }
}

private void getTabletInfo ()
{
  if (tabletOK)
    {
      try
        {
          if (((Boolean) mPoll.invoke (tablet, (Object[]) null)).booleanValue ())
            {
              pressure = (Integer) mGetPressure.invoke (tablet, (Object[]) null);
              pressureExtent = (Integer) mGetPressureExtent.invoke (tablet, (Object[]) null);
            }
        }
      catch (Exception e)
        {
          System.out.print (e.toString ());
        }
    }
}

public float getPressure ()
{
  getTabletInfo ();
  if (!tabletOK)
    {
      return 1.f;
    }
  else
    {
      return (float) pressure / pressureExtent;
    }
}

public void mouseDetect ()
{
  pressure = pressureExtent = 1;
  getTabletInfo ();
}
}
