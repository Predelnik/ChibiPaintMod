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
import chibipaint.file.CPAbstractFile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.prefs.Preferences;

public class CPMainGUI
{

private final CPController controller;
private final HashMap<String, JMenuItem> menuItems = new HashMap<String, JMenuItem> ();
private CPPaletteManager paletteManager;

private JMenuBar menuBar;
private JMenuBar menuBarTemporary; // Tempoarary menu bar for safe replacement with actual one
private JMenu lastMenu, lastSubMenu;
private JMenuItem lastItem;
private JPanel mainPanel;
private JDesktopPane jdp;
private JPanel bg;

// FIXME: replace this hack by something better
private final Map<String, JCheckBoxMenuItem> paletteItems = new HashMap<String, JCheckBoxMenuItem> ();

public CPMainGUI (CPController controller)
{
  this.controller = controller;
  controller.setMainGUI (this);

  // try {
  // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
  // } catch (Exception ex2) {} */

  createMainMenu ();
  menuBar = menuBarTemporary;
  menuBarTemporary = null;
  createGUI ();
}

public JComponent getGUI ()
{
  return mainPanel;
}

public JMenuBar getMenuBar ()
{
  return menuBar;
}

public void recreateMenuBar ()
{
  createMainMenu ();
  menuBar = menuBarTemporary;
  menuBarTemporary = null;
}

private void createGUI ()
{
  mainPanel = new JPanel (new BorderLayout ());

  jdp = new CPDesktop ();
  setPaletteManager (new CPPaletteManager (controller, jdp));

  createCanvasGUI (jdp);
  mainPanel.add (jdp, BorderLayout.CENTER);

  JPanel statusBar = new CPStatusBar (controller);
  mainPanel.add (statusBar, BorderLayout.PAGE_END);

  // jdp.addContainerListener(this);
}

void createCanvasGUI (JComponent c)
{
  CPCanvas canvas = new CPCanvas (controller);
  setBg (canvas.getCanvasContainer ());

  c.add (getBg ());
  canvas.grabFocus ();
}

// it's internal so boolean argument is ok
private void addMenuItemInternal (String title, int mnemonic, String command, String description, boolean isCheckable, boolean checked)
{
  JMenuItem menuItem = null;
  if (isCheckable)
    {
      menuItem = new JCheckBoxMenuItem (title, checked);
      menuItem.setMnemonic (mnemonic);
    }
  else
    {
      menuItem = new JMenuItem (title, mnemonic);
    }
  menuItem.getAccessibleContext ().setAccessibleDescription (description);
  menuItem.setActionCommand (command);
  menuItem.addActionListener (controller);
  menuItems.put (command, menuItem);
  if (lastSubMenu != null)
    lastSubMenu.add (menuItem);
  else
    lastMenu.add (menuItem);
  lastItem = menuItem;
}

JMenuItem getMenuItemByCmd (String cmd)
{
  return menuItems.get (cmd);
}

void setEnabledForTransform (boolean enabled)
{
  Iterator it = menuItems.entrySet ().iterator ();
  while (it.hasNext ())
    {
      Map.Entry pairs = (Map.Entry) it.next ();
      JMenuItem item = (JMenuItem) pairs.getValue ();
      item.setEnabled (enabled);
    }
  ((CPLayersPalette) getPaletteManager ().getPalettes ().get ("layers")).setEnabledForTransform (enabled);
  ((CPMiscPalette) getPaletteManager ().getPalettes ().get ("misc")).setEnabledForTransform (enabled);
}

void addMenuItem (String title, int mnemonic, String command)
{
  addMenuItemInternal (title, mnemonic, command, "", false, false);
}

void addMenuItem (String title, int mnemonic, String command, String description)
{
  addMenuItemInternal (title, mnemonic, command, description, false, false);
}

void addMenuItem (String title, int mnemonic, String command, String description, KeyStroke accelerator)
{
  addMenuItem (title, mnemonic, command, description);
  lastItem.setAccelerator (accelerator);
}

void addCheckBoxMenuItem (String title, int mnemonic, String command, String description, boolean checked)
{
  addMenuItemInternal (title, mnemonic, command, description, true, checked);
}

void addCheckBoxMenuItem (String title, int mnemonic, String command, boolean checked)
{
  addMenuItemInternal (title, mnemonic, command, "", true, checked);
}

void addCheckBoxMenuItem (String title, int mnemonic, String command, String description, KeyStroke accelerator, boolean checked)
{
  addMenuItemInternal (title, mnemonic, command, description, true, checked);
  lastItem.setAccelerator (accelerator);
}

void addMenu (String Title, int mnemonic)
{
  JMenu menu = new JMenu (Title);
  menu.setMnemonic (mnemonic);
  menuBarTemporary.add (menu);
  lastSubMenu = null;
  lastMenu = menu;
}

void addSubMenu (String Title, int mnemonic)
{
  JMenu menu = new JMenu (Title);
  menu.setMnemonic (KeyEvent.VK_F);
  lastMenu.add (menu);
  lastSubMenu = menu;
}

void endSubMenu ()
{
  lastSubMenu = null;
}

void addSeparator ()
{
  if (lastSubMenu != null)
    lastSubMenu.add (new JSeparator ());
  else
    lastMenu.add (new JSeparator ());
}

void createMainMenu ()
{
  menuBarTemporary = new JMenuBar ();
  lastMenu = null;
  lastSubMenu = null;
  menuItems.clear ();

  // File
  addMenu ("File", KeyEvent.VK_F);

  if (controller.isRunningAsApplet ())
    addMenuItem ("Send Oekaki", KeyEvent.VK_S, "CPSend", "Sends the oekaki to the server and exits ChibiPaintMod");

  if (controller.isRunningAsApplication ())
    {
      addMenuItem ("New File", KeyEvent.VK_N, "CPNew", "Create new file");

      addSeparator ();

      addMenuItem ("Save", KeyEvent.VK_S, "CPSave", "Save existing file", KeyStroke.getKeyStroke (KeyEvent.VK_S, InputEvent.CTRL_MASK));
      addMenuItem ("Save as...", KeyEvent.VK_A, "CPSaveCHI", "Save .chi File", KeyStroke.getKeyStroke (KeyEvent.VK_S, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
      addMenuItem ("Open...", KeyEvent.VK_O, "CPLoadCHI", "Open .chi File", KeyStroke.getKeyStroke (KeyEvent.VK_O, InputEvent.CTRL_MASK));

      boolean subMenuCreated = false;
      Preferences userRoot = Preferences.userRoot ();
      Preferences preferences = userRoot.node ("chibipaintmod");
      int recent_file_num = 0;
      for (int i = 0; i < 10; i++)
        {
          String recentFileName = preferences.get ("Recent File[" + i + "]", "");
          if (recentFileName.length () != 0)
            {
              if (!subMenuCreated)
                {
                  addSubMenu ("Open Recent", KeyEvent.VK_R);
                  subMenuCreated = true;
                }
              File recentFile = new File (recentFileName);
              addMenuItem (recentFile.getName (), 0, "CPOpenRecent " + i, "Open Recent File " + i, KeyStroke.getKeyStroke (KeyEvent.VK_0 + (i + 1) % 10, InputEvent.CTRL_MASK));
              recent_file_num++;
            }
        }

      if (subMenuCreated)
        endSubMenu ();
      addSeparator ();

      String[] importExport = {"Import", "Export"};
      String[] loadSave = {"CPLoad", "CPSave"};
      int[] mnemonics = {KeyEvent.VK_I, KeyEvent.VK_X};
      String[] supportedExtensions = CPAbstractFile.getSupportedExtensions ();

      for (int i = 0; i < 2; i++)
        {
          addSubMenu (importExport[i], mnemonics[i]);
          CPAbstractFile file;
          for (String supportedExt : supportedExtensions)
            {
              file = CPAbstractFile.fromExtension (supportedExt);
              if (file.isNative ())
                continue;

              addMenuItem (supportedExt.toUpperCase () + " File...", 0, loadSave[i] + supportedExt.toUpperCase (), importExport[i] + " " + supportedExt.toUpperCase () + "Files");
            }
          endSubMenu ();
        }

      addSeparator ();

      addMenuItem ("Exit", KeyEvent.VK_E, "CPExit", "Exit the application", KeyStroke.getKeyStroke (KeyEvent.VK_Q, InputEvent.CTRL_MASK));
    }

  // Edit
  addMenu ("Edit", KeyEvent.VK_E);
  addMenuItem ("Undo", KeyEvent.VK_U, "CPUndo", "Undo the most recent action", KeyStroke.getKeyStroke (KeyEvent.VK_Z, InputEvent.CTRL_MASK));
  addMenuItem ("Redo", KeyEvent.VK_U, "CPRedo", "Redo a previously undone action", KeyStroke.getKeyStroke (KeyEvent.VK_Z, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
  addMenuItem ("Clear History", KeyEvent.VK_H, "CPClearHistory", "Remove all undo/redo information to regain memory");
  addSeparator ();

  addMenuItem ("Cut", KeyEvent.VK_T, "CPCut", "Cuts the selected part of the layer", KeyStroke.getKeyStroke (KeyEvent.VK_X, InputEvent.CTRL_MASK));
  addMenuItem ("Copy", KeyEvent.VK_C, "CPCopy", "Copy the selected part of the layer", KeyStroke.getKeyStroke (KeyEvent.VK_C, InputEvent.CTRL_MASK));
  addMenuItem ("Copy Merged", KeyEvent.VK_Y, "CPCopyMerged", "Copy the selected part of all the layers merged", KeyStroke.getKeyStroke (KeyEvent.VK_C, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
  addMenuItem ("Paste", KeyEvent.VK_P, "CPPaste", "Paste the stuff that have been copied", KeyStroke.getKeyStroke (KeyEvent.VK_V, InputEvent.CTRL_MASK));
  addSeparator ();

  addMenuItem ("Select All", KeyEvent.VK_A, "CPSelectAll", "Selects the whole canvas", KeyStroke.getKeyStroke (KeyEvent.VK_A, InputEvent.CTRL_MASK));
  addMenuItem ("Deselect", KeyEvent.VK_D, "CPDeselectAll", "Deselects the whole canvas", KeyStroke.getKeyStroke (KeyEvent.VK_D, InputEvent.CTRL_MASK));

  // Layers
  addMenu ("Layers", KeyEvent.VK_L);
  addMenuItem ("Show / Hide All Layers", KeyEvent.VK_A, "CPLayerToggleAll", "Toggle All Layers Visibility", KeyStroke.getKeyStroke (KeyEvent.VK_A, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
  addSeparator ();
  addMenuItem ("Duplicate", KeyEvent.VK_D, "CPLayerDuplicate", "Creates a copySelected of the currently selected layer", KeyStroke.getKeyStroke (KeyEvent.VK_D, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
  addSeparator ();
  addMenuItem ("Merge Down", KeyEvent.VK_E, "CPLayerMergeDown", "Merges the currently selected layer with the one directly below it", KeyStroke.getKeyStroke (KeyEvent.VK_E, InputEvent.CTRL_MASK));
  addMenuItem ("Merge All Layers", KeyEvent.VK_A, "CPLayerMergeAll", "Merges all the layers");

  // Effects
  addMenu ("Effects", KeyEvent.VK_E);
  addMenuItem ("Free Transform Selected", KeyEvent.VK_T, "CPFreeTransform", "Transform selected part of the current layer", KeyStroke.getKeyStroke (KeyEvent.VK_T, InputEvent.CTRL_MASK));
  addMenuItem ("Flip Horizontal", KeyEvent.VK_H, "CPHFlip", "Flips the current selected area horizontally");
  addMenuItem ("Flip Vertical", KeyEvent.VK_V, "CPVFlip", "Flips the current selected area vertically");
  addSeparator ();
  addMenuItem ("Clear", KeyEvent.VK_C, "CPClear", "Clears the selected area", KeyStroke.getKeyStroke (KeyEvent.VK_DELETE, 0));
  addMenuItem ("Fill", KeyEvent.VK_F, "CPFill", "Fills the selected area with the current color", KeyStroke.getKeyStroke (KeyEvent.VK_F, InputEvent.CTRL_MASK));
  addMenuItem ("Invert", KeyEvent.VK_F, "CPFXInvert", "Invert the image colors");

  addMenuItem ("Make Grayscale", KeyEvent.VK_M, "CPFXMakeMonochromeByLuma", "Make image grayscale by Luma Formula");

  addSubMenu ("Blur", KeyEvent.VK_B);
  addMenuItem ("Box Blur...", KeyEvent.VK_B, "CPFXBoxBlur", "Blur Effect");
  endSubMenu ();

  addSubMenu ("Noise", KeyEvent.VK_N);
  addMenuItem ("Render Monochromatic", KeyEvent.VK_M, "CPMNoise", "Fills the selection with noise");
  addMenuItem ("Render Color", KeyEvent.VK_C, "CPCNoise", "Fills the selection with colored noise");
  endSubMenu ();
  addSeparator ();
  addCheckBoxMenuItem ("Apply to All Layers", KeyEvent.VK_T, "CPApplyToAllLayers", "Apply all listed above effects to all layers instead of just current", false);
  paletteItems.put ("Apply to All Layers", (JCheckBoxMenuItem) lastItem);

  // View

  addMenu ("View", KeyEvent.VK_V);

  if (controller.isRunningAsApplet ())
    {
      addMenuItem ("Floating mode", KeyEvent.VK_F, "CPFloat", "Opens ChibiPaintMod in an independent window");
      addSeparator ();
    }

  addMenuItem ("Zoom In", KeyEvent.VK_I, "CPZoomIn", "Zooms In", KeyStroke.getKeyStroke (KeyEvent.VK_ADD, InputEvent.CTRL_MASK));
  addMenuItem ("Zoom Out", KeyEvent.VK_O, "CPZoomOut", "Zooms Out", KeyStroke.getKeyStroke (KeyEvent.VK_SUBTRACT, InputEvent.CTRL_MASK));
  addMenuItem ("Zoom 100%", KeyEvent.VK_O, "CPZoom100", "Resets the zoom factor to 100%", KeyStroke.getKeyStroke (KeyEvent.VK_NUMPAD0, InputEvent.CTRL_MASK));
  addSeparator ();
  addCheckBoxMenuItem ("Use Linear Interpolation", KeyEvent.VK_L, "CPLinearInterpolation", "Linear interpolation is used to give a smoothed looked to the picture when zoomed in", false);
  paletteItems.put ("Use Linear Interpolation", (JCheckBoxMenuItem) lastItem);
  addSeparator ();
  addCheckBoxMenuItem ("Show Grid", KeyEvent.VK_G, "CPToggleGrid", "Displays a grid over the image", KeyStroke.getKeyStroke (KeyEvent.VK_G, InputEvent.CTRL_MASK), false);
  paletteItems.put ("Show Grid", (JCheckBoxMenuItem) lastItem);
  addMenuItem ("Grid options...", KeyEvent.VK_D, "CPGridOptions", "Shows the grid options dialog box");
  addSeparator ();

  addSubMenu ("Palettes", KeyEvent.VK_P);
  addMenuItem ("Toggle Palettes", KeyEvent.VK_P, "CPTogglePalettes", "Hides or shows all palettes", KeyStroke.getKeyStroke (KeyEvent.VK_TAB, 0));
  addSeparator ();
  addCheckBoxMenuItem ("Show Brush", KeyEvent.VK_B, "CPPalBrush", true);
  paletteItems.put ("Brush", (JCheckBoxMenuItem) lastItem);
  addCheckBoxMenuItem ("Show Color", KeyEvent.VK_C, "CPPalColor", true);
  paletteItems.put ("Color", (JCheckBoxMenuItem) lastItem);
  addCheckBoxMenuItem ("Show Layers", KeyEvent.VK_Y, "CPPalLayers", true);
  paletteItems.put ("Layers", (JCheckBoxMenuItem) lastItem);
  addCheckBoxMenuItem ("Show Misc", KeyEvent.VK_M, "CPPalMisc", true);
  paletteItems.put ("Misc", (JCheckBoxMenuItem) lastItem);
  addCheckBoxMenuItem ("Show Stroke", KeyEvent.VK_S, "CPPalStroke", true);
  paletteItems.put ("Stroke", (JCheckBoxMenuItem) lastItem);
  addCheckBoxMenuItem ("Show Swatches", KeyEvent.VK_S, "CPPalSwatches", true);
  paletteItems.put ("Color Swatches", (JCheckBoxMenuItem) lastItem);
  addCheckBoxMenuItem ("Show Textures", KeyEvent.VK_S, "CPPalTextures", true);
  paletteItems.put ("Textures", (JCheckBoxMenuItem) lastItem);
  addCheckBoxMenuItem ("Show Tools", KeyEvent.VK_S, "CPPalTool", true);
  paletteItems.put ("Tools", (JCheckBoxMenuItem) lastItem);
  endSubMenu ();

  // Help
  addMenu ("Help", KeyEvent.VK_H);
  addMenuItem ("About...", KeyEvent.VK_A, "CPAbout", "Displays some information about ChibiPaintMod");
}

public void showPalette (String palette, boolean show)
{
  getPaletteManager ().showPalette (palette, show);
}

public void setPaletteMenuItem (String title, boolean selected)
{
  JCheckBoxMenuItem item = paletteItems.get (title);
  if (item != null)
    {
      item.setSelected (selected);
    }
}

public void togglePalettes ()
{
  getPaletteManager ().togglePalettes ();
}

public CPPaletteManager getPaletteManager ()
{
  return paletteManager;
}

void setPaletteManager (CPPaletteManager paletteManager)
{
  this.paletteManager = paletteManager;
}

JPanel getBg ()
{
  return bg;
}

void setBg (JPanel bg)
{
  this.bg = bg;
}

class CPDesktop extends JDesktopPane
{

  public CPDesktop ()
  {
    addComponentListener (new ComponentAdapter ()
    {

      @Override
      public void componentResized (ComponentEvent e)
      {
        getBg ().setSize (getSize ());
        getBg ().validate ();
      }
    });
  }
}
}

