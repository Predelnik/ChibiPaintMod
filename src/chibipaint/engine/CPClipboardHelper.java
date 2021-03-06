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


package chibipaint.engine;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.io.IOException;

class CPClipboardHelper
{
private static class TransferableImage implements Transferable
{

  private final boolean limited;
  private final CPCopyPasteImage image;

  public TransferableImage (CPCopyPasteImage imageArg, boolean limitedArg)
  {
    image = imageArg;
    limited = limitedArg;
  }

  public Object getTransferData (DataFlavor flavor)
          throws UnsupportedFlavorException, IOException
  {
    if (flavor.equals (getCpmImageFlavor ()) && image != null)
      {
        return image;
      }
    else if (!limited && flavor.equals (DataFlavor.imageFlavor) && image != null)
      {
        MemoryImageSource imgSource = new MemoryImageSource (image.getWidth (), image.getHeight (), image.getData (), 0, image.getWidth ());
        return Toolkit.getDefaultToolkit ().createImage (imgSource);
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

static DataFlavor getCpmImageFlavor ()
{
  if (cpmImageFlavor == null)
    cpmImageFlavor = new DataFlavor ("image/cpm-image; class=" + chibipaint.engine.CPCopyPasteImage.class.getName (), "ChibiPaint Image");

  return cpmImageFlavor;
}

private static DataFlavor cpmImageFlavor = null;

static public CPCopyPasteImage GetClipboardImage (boolean limited)
{
  if (limited)
    return staticImage;

  Clipboard clipboard = Toolkit.getDefaultToolkit ().getSystemClipboard ();
  try
    {
      return (CPCopyPasteImage) clipboard.getData (getCpmImageFlavor ());
    }
  catch (UnsupportedFlavorException e)
    {
      if (limited)
        return null;
    }
  catch (IOException e)
    {
      return null;
    }

  try
    {
      Image img = (Image) clipboard.getData (DataFlavor.imageFlavor);
      BufferedImage bI = new BufferedImage (img.getWidth (null), img.getHeight (null), BufferedImage.TYPE_INT_ARGB);
      Graphics g = bI.createGraphics ();
      g.drawImage (img, 0, 0, null);
      g.dispose ();
      return new CPCopyPasteImage (bI);
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

static private CPCopyPasteImage staticImage = null;

static public void SetClipboardImage (CPCopyPasteImage image, boolean limited)
{
  if (!limited)
    {
      TransferableImage transferable = new TransferableImage (image, limited);
      Clipboard clipboard = Toolkit.getDefaultToolkit ().getSystemClipboard ();
      clipboard.setContents (transferable, null);
    }
  else
    {
      staticImage = image;
    }
}
}

