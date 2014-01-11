/*
 * ChibiPaintMod
 *     Copyright (c) 2012-2013 Sergey Semushin
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

package chibipaint.controller;

import chibipaint.gui.CPIconButton;

public interface CPCommandSettings
{
public class fileExtension implements CPCommandSettings
{
  public String extension;

  public fileExtension (String value)
  {
    extension = value;
  }
}

public class recentFileNumber implements CPCommandSettings
{
  public int number;

  public recentFileNumber (int numberArg)
  {
    number = numberArg;
  }
}

public class checkBoxState implements CPCommandSettings
{
  public boolean checked;

  public checkBoxState (boolean checkedArg)
  {
    checked = checkedArg;
  }
}

// This is because icon button rely heavily on this
// TODO: remove this logic
public class sourceIconButton implements CPCommandSettings
{
  public CPIconButton button;

  public sourceIconButton (CPIconButton buttonArg)
  {
    button = buttonArg;
  }
}


}