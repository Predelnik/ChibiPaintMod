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

import chibipaint.controller.CPCommandId;
import chibipaint.controller.CPCommonController;
import chibipaint.engine.CPBrushInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class CPToolPreferencesPalette extends CPPalette implements CPCommonController.ICPToolListener, ActionListener
{

private final CPAlphaSlider alphaSlider;
private final CPSizeSlider sizeSlider;

private final CPCheckBox alphaCB;
private final CPCheckBox sizeCB;
private final CPCheckBox scatteringCB;
private final CPSlider resatSlider;
private final CPSlider bleedSlider;
private final CPSlider spacingSlider;
private final CPSlider scatteringSlider;
private final CPSlider smoothingSlider;
private final CPBrushPreview brushPreview;
private final CPSlider instantFillOpacity;
private final CPCheckBox instantFillCB;

// For Floodfill
private final CPSlider colorDistanceSliderFloodFill;
private final CPSlider colorDistanceSliderMagicWand;

private final JComboBox<String> tipCombo;
private final String[] tipNames = {"Round Pixelated", "Round Hard Edge", "Round Soft", "Square Pixelated", "Square Hard Edge"};
private final JButton transformOkButton;
private final JButton transformCancelButton;
private final JButton transformFlipHButton;
private final JButton transformFlipVButton;
private final JButton transformRotate90CCWButton;
private final JButton transformRotate90CWButton;
private final JLabel transformLabel;

@SuppressWarnings ("serial")
public CPToolPreferencesPalette (CPCommonController ctrlr)
{
  super (ctrlr);

  setSize (160, 270);

  title = "ToolPreferences";


  setLayout (null);
  // setBounds(getInnerDimensions());

  // transform controls
  transformLabel = addLabel (10, 5, "Transform:");
  transformOkButton = addTextButton (10, 30, 60, 16, "Ok", CPCommandId.ApplyTransform);
  transformCancelButton = addTextButton (75, 30, 75, 16, "Cancel", CPCommandId.CancelTransform);

  transformFlipHButton = addTextButton (10, 100, 140, 16, "Flip Horizontally", CPCommandId.FlipHorizontally);
  transformFlipVButton = addTextButton (10, 125, 140, 16, "Flip Vertically", CPCommandId.FlipVertically);
  transformRotate90CCWButton = addTextButton (10, 150, 140, 16, "Rotate 90° CCW", CPCommandId.Rotate90CCW);
  transformRotate90CWButton = addTextButton (10, 175, 140, 16, "Rotate 90° CW", CPCommandId.Rotate90CW);

  // selection controls
  instantFillCB = new CPInstaFillCB ();

  instantFillCB.setLocation (2, 25);
  add (instantFillCB);

  instantFillOpacity = new CPSlider (100)
  {
    @Override
    public void onValueChange ()
    {
      controller.setSelectionFillAlpha (255 * value / 100);
      title = "Fill Opacity: " + value + "%";
    }
  };
  instantFillOpacity.setLocation (20, 25);
  add (instantFillOpacity);

  // fill controls
  colorDistanceSliderMagicWand = new CPColorDistanceSlider ()
  {
    @Override
    void updateValue ()
    {
      controller.setColorDistanceMagicWand (value);
    }
  };
  colorDistanceSliderMagicWand.setLocation (20, 25);
  colorDistanceSliderMagicWand.setSize (130, 16);
  add (colorDistanceSliderMagicWand);

  colorDistanceSliderFloodFill = new CPColorDistanceSlider ()
  {
    @Override
    void updateValue ()
    {
      controller.setColorDistanceFloodFill (value);
    }
  };
  colorDistanceSliderFloodFill.setLocation (20, 25);
  colorDistanceSliderFloodFill.setSize (130, 16);
  add (colorDistanceSliderFloodFill);

  // for brush controls:

  alphaSlider = new CPAlphaSlider ();
  alphaSlider.setLocation (20, 120);
  add (alphaSlider);

  brushPreview = new CPBrushPreview ();
  brushPreview.setLocation (5, 25);
  add (brushPreview);

  alphaCB = new CPAlphaCB ();
  alphaCB.setLocation (2, 120);
  add (alphaCB);

  sizeSlider = new CPSizeSlider ();
  sizeSlider.setLocation (20, 95);
  add (sizeSlider);

  sizeCB = new CPSizeCB ();
  sizeCB.setLocation (2, 95);
  add (sizeCB);

  tipCombo = new JComboBox<String> (tipNames);
  tipCombo.addActionListener (this);
  tipCombo.setLocation (5, 5);
  tipCombo.setSize (120, 16);
  add (tipCombo);

  resatSlider = new CPSlider (100)
  {

    @Override
    public void onValueChange ()
    {
      controller.getBrushInfo ().resat = value / 100f;
      controller.callToolListeners ();
      title = "Color: " + value + "%";
    }
  };
  resatSlider.setLocation (20, 145);
  add (resatSlider);

  bleedSlider = new CPSlider (100)
  {

    @Override
    public void onValueChange ()
    {
      controller.getBrushInfo ().bleed = value / 100f;
      controller.callToolListeners ();
      title = "Blend: " + value + "%";
    }
  };
  bleedSlider.setLocation (20, 170);
  add (bleedSlider);

  spacingSlider = new CPSlider (100)
  {

    @Override
    public void onValueChange ()
    {
      controller.getBrushInfo ().spacing = value / 100f;
      controller.callToolListeners ();
      title = "Spacing: " + value + "%";
    }
  };
  spacingSlider.setLocation (20, 195);
  add (spacingSlider);

  scatteringCB = new CPCheckBox ()
  {

    @Override
    public void onValueChange ()
    {
      controller.getBrushInfo ().pressureScattering = state;
      controller.callToolListeners ();
    }
  };
  scatteringCB.setLocation (2, 220);
  add (scatteringCB);

  scatteringSlider = new CPSlider (1000)
  {

    @Override
    public void onValueChange ()
    {
      controller.getBrushInfo ().scattering = value / 100f;
      controller.callToolListeners ();
      title = "Scattering: " + value + "%";
    }
  };
  scatteringSlider.setLocation (20, 220);
  add (scatteringSlider);

  smoothingSlider = new CPSlider (100)
  {

    @Override
    public void onValueChange ()
    {
      controller.getBrushInfo ().smoothing = value / 100f;
      controller.callToolListeners ();
      title = "Smoothing: " + value + "%";
    }
  };
  smoothingSlider.setLocation (20, 245);
  add (smoothingSlider);

  instantFillCB.setValue (ctrlr.getCurSelectionAction () == CPCommonController.selectionAction.FILL_AND_DESELECT);
  instantFillOpacity.setValue (ctrlr.getSelectionFillAlpha ());

  colorDistanceSliderMagicWand.setValue (ctrlr.getColorDistanceMagicWand ());
  colorDistanceSliderFloodFill.setValue (ctrlr.getColorDistanceFloodFill ());

  alphaSlider.setValue (ctrlr.getAlpha ());
  sizeSlider.setValue (ctrlr.getBrushSize ());
  sizeCB.setValue (ctrlr.getBrushInfo ().pressureSize);
  alphaCB.setValue (ctrlr.getBrushInfo ().pressureAlpha);
  tipCombo.setSelectedIndex (ctrlr.getBrushInfo ().type);

  resatSlider.setValue ((int) (ctrlr.getBrushInfo ().resat * 100));
  bleedSlider.setValue ((int) (ctrlr.getBrushInfo ().bleed * 100));
  spacingSlider.setValue ((int) (ctrlr.getBrushInfo ().spacing * 100));
  scatteringCB.setValue (ctrlr.getBrushInfo ().pressureScattering);
  scatteringSlider.setValue ((int) (ctrlr.getBrushInfo ().scattering * 100));
  smoothingSlider.setValue ((int) (ctrlr.getBrushInfo ().smoothing * 100));

  ctrlr.addToolListener (this);
  newTool (null); // Just to figure out visibility
}

@Override
public void newTool (CPBrushInfo toolInfo)
{
  JComponent[] brushControls = {alphaSlider, sizeSlider, alphaCB, sizeCB, scatteringCB, resatSlider,
          bleedSlider, spacingSlider, scatteringSlider, smoothingSlider, tipCombo, brushPreview};
  JComponent[] floodFillControls = {colorDistanceSliderFloodFill};
  JComponent[] magicWandControls = {colorDistanceSliderMagicWand};
  JComponent[] transformControls = {transformLabel, transformOkButton, transformCancelButton, transformFlipHButton, transformFlipVButton,
          transformRotate90CCWButton, transformRotate90CWButton};
  JComponent[] selectionControls = {instantFillCB, instantFillOpacity};
  JComponent[][] toolArrays = {brushControls, floodFillControls, transformControls, selectionControls, magicWandControls};

  for (JComponent[] toolArray : toolArrays)
    for (JComponent jc : toolArray)
      jc.setVisible (false);


  if (controller.getTransformIsOn ())
    {
      for (JComponent jc : transformControls)
        jc.setVisible (true);
    }
  else
    {
      switch (controller.getCurMode ())
        {
        case CPCommonController.M_DRAW:
          for (JComponent jc : brushControls)
            jc.setVisible (true);
          break;
        case CPCommonController.M_FLOODFILL:
          for (JComponent jc : floodFillControls)
            jc.setVisible (true);
          break;
        case CPCommonController.M_FREE_SELECTION:
          for (JComponent jc : selectionControls)
            jc.setVisible (true);
          break;
        case CPCommonController.M_MAGIC_WAND:
          for (JComponent jc : magicWandControls)
            jc.setVisible (true);
          break;
        }
    }

  if (toolInfo == null)
    return;

  if (controller.getColorDistanceMagicWand () != colorDistanceSliderMagicWand.value)
    colorDistanceSliderMagicWand.setValue (controller.getColorDistanceMagicWand ());

  if (controller.getColorDistanceFloodFill () != colorDistanceSliderFloodFill.value)
    colorDistanceSliderFloodFill.setValue (controller.getColorDistanceFloodFill ());

  if ((controller.getCurSelectionAction () == CPCommonController.selectionAction.FILL_AND_DESELECT) != instantFillCB.state)
    instantFillCB.setValue (controller.getCurSelectionAction () == CPCommonController.selectionAction.FILL_AND_DESELECT);

  if (controller.getSelectionFillAlpha () * 100 / 255 != instantFillOpacity.value)
    instantFillOpacity.setValue (controller.getSelectionFillAlpha () * 100 / 255);

  if (toolInfo.alpha != alphaSlider.value)
    {
      alphaSlider.setValue (toolInfo.alpha);
    }

  if (toolInfo.size != sizeSlider.value)
    {
      sizeSlider.setValue (toolInfo.size);
    }

  if (toolInfo.pressureSize != sizeCB.state)
    {
      sizeCB.setValue (toolInfo.pressureSize);
    }

  if (toolInfo.pressureAlpha != alphaCB.state)
    {
      alphaCB.setValue (toolInfo.pressureAlpha);
    }

  if (toolInfo.type != tipCombo.getSelectedIndex ())
    {
      tipCombo.setSelectedIndex (toolInfo.type);
    }

  if ((int) (toolInfo.resat * 100.f) != resatSlider.value)
    {
      resatSlider.setValue ((int) (toolInfo.resat * 100.f));
    }

  if ((int) (toolInfo.bleed * 100.f) != bleedSlider.value)
    {
      bleedSlider.setValue ((int) (toolInfo.bleed * 100.f));
    }

  if ((int) (toolInfo.spacing * 100.f) != spacingSlider.value)
    {
      spacingSlider.setValue ((int) (toolInfo.spacing * 100.f));
    }

  if (toolInfo.pressureScattering != scatteringCB.state)
    {
      scatteringCB.setValue (toolInfo.pressureScattering);
    }

  if ((int) (toolInfo.scattering * 100.f) != scatteringSlider.value)
    {
      scatteringSlider.setValue ((int) (toolInfo.scattering * 100.f));
    }

  if ((int) (toolInfo.smoothing * 100.f) != smoothingSlider.value)
    {
      smoothingSlider.setValue ((int) (toolInfo.smoothing * 100.f));
    }
}

@Override
public void actionPerformed (ActionEvent e)
{
  if (e.getSource () == tipCombo)
    {
      controller.getBrushInfo ().type = tipCombo.getSelectedIndex ();
    }
}

class CPBrushPreview extends JComponent implements MouseListener, MouseMotionListener, CPCommonController.ICPToolListener
{

  final int w;
  final int h;
  int size;

  public CPBrushPreview ()
  {
    w = h = 64;
    setBackground (Color.white);
    setSize (new Dimension (w, h));

    addMouseListener (this);
    addMouseMotionListener (this);
    controller.addToolListener (this);

    size = 16;
  }

  @Override
  public void paint (Graphics g)
  {
    g.drawOval (w / 2 - size / 2, h / 2 - size / 2, size, size);
  }

  public void mouseSelect (MouseEvent e)
  {
    int x = e.getX () - w / 2;
    int y = e.getY () - h / 2;

    int newSize = (int) Math.sqrt ((x * x + y * y)) * 2;
    size = Math.max (1, Math.min (200, newSize));

    repaint ();
    controller.setBrushSize (size);
  }

  @Override
  public void mouseEntered (MouseEvent e)
  {
    // To implement interface
  }

  @Override
  public void mouseExited (MouseEvent e)
  {
    // To implement interface
  }

  @Override
  public void mouseClicked (MouseEvent e)
  {
    // To implement interface
  }

  @Override
  public void mousePressed (MouseEvent e)
  {
    mouseSelect (e);
  }

  @Override
  public void mouseReleased (MouseEvent e)
  {
    // To implement interface
  }

  @Override
  public void mouseMoved (MouseEvent e)
  {
    // To implement interface
  }

  @Override
  public void mouseDragged (MouseEvent e)
  {
    mouseSelect (e);
  }

  @Override
  public Dimension getPreferredSize ()
  {
    return new Dimension (w, h);
  }

  @Override
  public void newTool (CPBrushInfo toolInfo)
  {
    if (toolInfo.size != size)
      {
        size = toolInfo.size;
        repaint ();
      }
  }
}

class CPAlphaSlider extends CPSlider
{

  public CPAlphaSlider ()
  {
    super (255);
    minValue = 1;
  }

  @Override
  public void onValueChange ()
  {
    controller.setAlpha (value);
    title = "Opacity: " + value;
  }
}

abstract class CPColorDistanceSlider extends CPSlider
{

  public CPColorDistanceSlider ()
  {
    super (255);
    minValue = 0;
  }

  abstract void updateValue ();

  @Override
  public void onValueChange ()
  {
    updateValue ();
    title = "Threshold: " + value;
  }
}

class CPSizeSlider extends CPSlider
{

  public CPSizeSlider ()
  {
    super (200);
    minValue = 1;
  }

  @Override
  public void onValueChange ()
  {
    controller.setBrushSize (value);
    title = "Brush Size: " + value;
  }
}

class CPCheckBox extends JComponent implements MouseListener
{

  boolean state = false;

  public CPCheckBox ()
  {
    setSize (16, 16);
    addMouseListener (this);
  }

  @Override
  public void paint (Graphics g)
  {
    Dimension d = getSize ();

    if (state)
      {
        g.fillOval (3, 3, d.width - 5, d.height - 5);
      }
    else
      {
        g.drawOval (3, 3, d.width - 6, d.height - 6);
      }
  }

  public void setValue (boolean b)
  {
    state = b;
    onValueChange ();
    repaint ();
  }

  public void onValueChange ()
  {
    // To implement interface
  }

  @Override
  public void mouseClicked (MouseEvent e)
  {
    // To implement interface
  }

  @Override
  public void mouseEntered (MouseEvent e)
  {
    // To implement interface
  }

  @Override
  public void mouseExited (MouseEvent e)
  {
    // To implement interface
  }

  @Override
  public void mousePressed (MouseEvent e)
  {
    setValue (!state);
  }

  @Override
  public void mouseReleased (MouseEvent e)
  {
    // To implement interface
  }
}

class CPAlphaCB extends CPCheckBox
{

  @Override
  public void onValueChange ()
  {
    controller.getBrushInfo ().pressureAlpha = state;
    controller.callToolListeners ();
  }
}

class CPInstaFillCB extends CPCheckBox
{

  @Override
  public void onValueChange ()
  {
    controller.setSelectionAction (state ? CPCommonController.selectionAction.FILL_AND_DESELECT : CPCommonController.selectionAction.SELECT);
  }
}

class CPSizeCB extends CPCheckBox
{

  @Override
  public void onValueChange ()
  {
    controller.getBrushInfo ().pressureSize = state;
    controller.callToolListeners ();
  }
}

}

// setLocation((int) (e.getX() + getLocation().getX()), (int) (e.getY() + getLocation().getY()));
