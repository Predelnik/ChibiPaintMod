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


package chibipaint.engine;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class CPClipboardHelper
{
private static class TransferableImage implements Transferable
{

  private final Image image;

  public TransferableImage (Image imageArg)
  {
    image = imageArg;
  }

  public Object getTransferData (DataFlavor flavor)
          throws UnsupportedFlavorException, IOException
  {
    if (flavor.equals (DataFlavor.imageFlavor) && image != null)
      {
        return image;
      }
    else
      {
        throw new UnsupportedFlavorException (flavor);
      }
  }

  public DataFlavor[] getTransferDataFlavors ()
  {
    DataFlavor[] flavors = new DataFlavor[1];
    flavors[0] = DataFlavor.imageFlavor;
    return flavors;
  }

  public boolean isDataFlavorSupported (DataFlavor flavor)
  {
    DataFlavor[] flavors = getTransferDataFlavors ();
    for (DataFlavor flavorIt : flavors)
      {
        if (flavor.equals (flavorIt))
          {
            return true;
          }
      }
    return false;
  }
}

static public Image GetClipboardImage ()
{
  Clipboard clipboard = Toolkit.getDefaultToolkit ().getSystemClipboard ();
  try
    {
      return (Image) clipboard.getData (DataFlavor.imageFlavor);
    }
  catch (UnsupportedFlavorException e)
    {
      return null;
    }
  catch (IOException e)
    {
      return null;
    }
}

static public void SetClipboardImage (Image image)
{
  TransferableImage transferable = new TransferableImage (image);
  Clipboard clipboard = Toolkit.getDefaultToolkit ().getSystemClipboard ();
  clipboard.setContents (transferable, null);
}
}

