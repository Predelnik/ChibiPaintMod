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
import chibipaint.util.CPMath;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.util.ArrayList;

public class CPColorPalette extends CPPalette
{

enum paletteMode
{
  RECTANGLE,
  CIRCLE,
};

private final CPColor curColor = new CPColor ();
private final JToggleButton paletteModeButtons[] = new JToggleButton[paletteMode.values().length];
private final int iconSize = 16;
private final int buttonSize = 20;
private final CPCircleColorPaletteWidget circleColorPaletteWidget;
private final ArrayList<ArrayList<JComponent>> modeComponents = new ArrayList<ArrayList<JComponent>> ();
private final CPRectangleColorPaletteWidget rectangleColorPaletteWidget;

JToggleButton paletteModeButton (paletteMode mode)
{
  return paletteModeButtons[mode.ordinal ()];
}

ArrayList<JComponent> modeComponents (paletteMode mode)
{
  return modeComponents.get (mode.ordinal ());
}

void addComponent (JComponent component, paletteMode mode)
{
  add (component);
  modeComponents (mode).add (component);
}

void updateComponentsVisibility ()
{
  for (int i = 0; i < modeComponents.size (); i++)
    for (JComponent component : modeComponents.get (i))
      component.setVisible (false);

  for (int i = 0; i < modeComponents.size (); i++)
    if (paletteModeButtons[i].isSelected()) {
      for (JComponent component : modeComponents.get(i))
        component.setVisible(true);
    }
  if (getPaletteContainer () != null)
    getPaletteContainer ().pack ();
}

public CPColorPalette (CPCommonController controller)
{
  super (controller);

  // setSize(175, 185);

  title = "Color";
  // setBounds(getInnerDimensions());
  BufferedImage icons = controller.loadImage("paletteicons.png");
  JPanel modePanel = new JPanel ();
  add (modePanel);
  modePanel.setLayout (new BoxLayout(modePanel, BoxLayout.LINE_AXIS));
  for (int i = 0; i < paletteModeButtons.length; ++i)
    {
      paletteModeButtons[i] = new JToggleButton (new ImageIcon(icons.getSubimage(iconSize * i, 0, iconSize, iconSize)));
      paletteModeButtons[i].setPreferredSize(new Dimension(buttonSize, buttonSize));
      paletteModeButtons[i].setSize(new Dimension(buttonSize, buttonSize));
      modePanel.add(paletteModeButtons[i]);
      final int j = i;

      paletteModeButtons[i].addChangeListener (new ChangeListener()
      {
        @Override
        public void stateChanged(ChangeEvent e) {
          updateComponentsVisibility ();

        }
      });
      paletteModeButtons[i].setAlignmentX (Component.LEFT_ALIGNMENT);
      modeComponents.add (new ArrayList<JComponent>());
    }
  modePanel.add (Box.createHorizontalGlue ());

  paletteModeButton(paletteMode.RECTANGLE).setSelected (true);

  setLayout(new BoxLayout (this, BoxLayout.PAGE_AXIS));

  rectangleColorPaletteWidget = new CPRectangleColorPaletteWidget(controller);
  addComponent (rectangleColorPaletteWidget, paletteMode.RECTANGLE);
  controller.addColorChangeListener(rectangleColorPaletteWidget);

  circleColorPaletteWidget = new CPCircleColorPaletteWidget (controller);
  addComponent (circleColorPaletteWidget, paletteMode.CIRCLE);

  updateComponentsVisibility ();
}

public class CPColorSelect extends JComponent implements MouseListener, MouseMotionListener
{

  final int[] data;
  final int w;
  final int h;
  final Image img;
  final CPColor color;
  boolean needRefresh;

  public CPColorSelect ()
  {
    w = h = 128;
    setBackground (Color.black); // tmp to help see refresh problems
    setSize (new Dimension (w, h));

    data = new int[w * h];
    img = createImage (new MemoryImageSource (w, h, data, 0, w));
    color = new CPColor ();

    makeBitmap ();
    needRefresh = false;

    addMouseListener (this);
    addMouseMotionListener (this);
  }

  public void setHue (int hue)
  {
    if (color.getHue () != hue)
      {
        color.setHue (hue);
        needRefresh = true;
        repaint ();
        controller.setCurColor (color);
      }
  }

  public void setColor (CPColor newColor)
  {
    if (!color.isEqual (newColor))
      {
        color.copyFrom (newColor);
        needRefresh = true;
        repaint ();
        controller.setCurColor (color);
      }
  }

  void makeBitmap ()
  {
    CPColor col = (CPColor) color.clone ();
    for (int j = 0; j < h; j++)
      {
        col.setValue (255 - (j * 255) / h);
        for (int i = 0; i < w; i++)
          {
            col.setSaturation ((i * 255) / w);
            data[i + j * w] = 0xff000000 | col.rgb;
          }
      }
  }

  @Override
  public void update (Graphics g)
  {
    paint (g);
  }

  @Override
  public void paint (Graphics g)
  {
    if (needRefresh)
      {
        makeBitmap ();
        needRefresh = false;
      }
    img.flush ();
    g.drawImage (img, 0, 0, Color.red, null);

    int x = color.getSaturation () * w / 255;
    int y = (255 - color.getValue ()) * h / 255;
    g.setColor (Color.white);
    g.setXORMode (Color.black);
    g.drawOval (x - 5, y - 5, 10, 10);
  }

  public void mouseSelect (MouseEvent e)
  {
    int sat = e.getX () * 255 / w;
    int value = 255 - e.getY () * 255 / h;

    color.setSaturation (Math.max (0, Math.min (255, sat)));
    color.setValue (Math.max (0, Math.min (255, value)));

    repaint ();
    controller.setCurColor (color);
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
  public void mousePressed (MouseEvent e)
  {
    mouseSelect (e);
  }

  @Override
  public void mouseReleased (MouseEvent e)
  {
  }

  @Override
  public void mouseMoved (MouseEvent e)
  {
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
}

  public class CPRectangleColorPaletteWidget extends JComponent implements CPCommonController.ICPColorChangeListener {
    CPColorSelect colorSelect;
    CPColorSlider colorSlider;
    CPColorShow colorShow;
    final int horMargin = 5, verMargin = 5;

    CPRectangleColorPaletteWidget(CPCommonController controller)
    {
      setLayout (null);
      colorSelect = new CPColorSelect ();
      add(colorSelect);
      Dimension size = colorSelect.getPreferredSize();
      colorSelect.setBounds (0, 0, size.width, size.height);
      colorSlider = new CPColorSlider (colorSelect);
      size = colorSlider.getPreferredSize();
      colorSlider.setBounds (colorSelect.getPreferredSize().width + horMargin, 0, size.width, size.height);
      add(colorSlider);
      colorShow = new CPColorShow ();
      colorShow.setPreferredSize (new Dimension (colorSelect.getPreferredSize().width + horMargin + colorSlider.getPreferredSize().width, 20));
      size = colorShow.getPreferredSize();
      colorShow.setBounds (0, colorSelect.getPreferredSize().height + verMargin, size.width, size.height);
      add(colorShow);
      controller.addColorChangeListener (colorShow);
    }

    @Override
    public Dimension getPreferredSize ()
    {
      return new Dimension (colorSelect.getPreferredSize().width + colorSlider.getPreferredSize().width + horMargin,
                            colorSelect.getPreferredSize().height + colorShow.getPreferredSize().height + verMargin
              );
    }

    @Override
    public void colorChanged(CPColor color) {
        if (!curColor.isEqual (color))
        {
          curColor.copyFrom (color);
          colorSelect.setColor (color);
          colorSlider.setHue (curColor.getHue ());
        }
        colorShow.repaint ();
    }
  }

  public class CPCircleColorPaletteWidget extends JComponent
  {
    CPCircleColorPalette circleColorPalette;
    CPColorShow colorShow;
    final int verMargin = 5;

    public CPCircleColorPaletteWidget(CPCommonController controller)
    {
      setLayout (null);
      circleColorPalette = new CPCircleColorPalette (controller);
      add(circleColorPalette);
      Dimension size = circleColorPalette.getPreferredSize();
      colorShow = new CPColorShow ();
      colorShow.setPreferredSize (new Dimension (circleColorPalette.getPreferredSize().width, 20));
      size = colorShow.getPreferredSize();
      colorShow.setBounds (0, circleColorPalette.getPreferredSize().height + verMargin, size.width, size.height);
      add(colorShow);
      controller.addColorChangeListener (circleColorPalette);
      controller.addColorChangeListener (colorShow);
    }

    @Override
    public Dimension getPreferredSize ()
    {
      return new Dimension (circleColorPalette.getPreferredSize().width,
              circleColorPalette.getPreferredSize().height + colorShow.getPreferredSize().height + verMargin
      );
    }
  }

  static public class CPCircleColorPalette extends JComponent implements MouseListener, MouseMotionListener, CPCommonController.ICPColorChangeListener
  {
    final int[] data;
    final Image img;
    int cachedHue;
    final int side = 150;
    final int circleThickness = 16;
    final double bigRadius = side * 0.5;
    final double smallRadius = side * 0.5 - circleThickness;
    final int squareL = (int)(smallRadius / (Math.sqrt (2.0) * 0.5));
    final double radius = (bigRadius + smallRadius) / 2;
    final Point center = new Point (side / 2, side / 2);
    CPCommonController controller;

    enum selectionMode
    {
      NONE,

      HUE,
      SAT_VAL
    };

    selectionMode curSelectionMode = selectionMode.NONE;

    public CPCircleColorPalette (CPCommonController controllerArg)
    {
      controller = controllerArg;
      setBackground (Color.black); // tmp to help see refresh problems
      setSize(new Dimension(side, side));

      data = new int[side * side];
      img = createImage (new MemoryImageSource (side, side, data, 0, side));
      cachedHue = -1;

      makeBitmap (controller.getCurColor ());

      addMouseListener (this);
      addMouseMotionListener (this);
    }

    Integer getHueFromPoint (int i, int j)
    {
      double dist = center.distance (i, j);
      if (dist > smallRadius && dist <= bigRadius)
        return getHueFromPointUnrestricted (i, j);
      else
       return null;
    }

    Integer getHueFromPointUnrestricted (int i, int j)
    {
      return (((int) Math.toDegrees(Math.atan2(j - center.y, i - center.x)) + 360) % 360);
    }

    Integer getSaturationFromPointUnrestricted (int i, int j)
    {
      return CPMath.bound(255 * (i - (center.x - squareL / 2)) / squareL, 0, 255);
    }

    Integer getSaturationFromPoint (int i, int j)
    {
      if (Math.abs (i - center.x) > squareL / 2)
        return null;

      return getSaturationFromPointUnrestricted (i, j);
    }

    Integer getValueFromPointUnrestricted (int i, int j)
    {
      return CPMath.bound (255 * (center.y + squareL / 2 - j) / squareL, 0, 255);
    }

    Integer getValueFromPoint (int i, int j)
    {
      if (Math.abs (j - center.y) > squareL / 2)
        return null;

      return getValueFromPointUnrestricted (i, j);
    }

    Integer get (int i, int j)
    {
      if (Math.abs (i - center.x) > squareL / 2)
        return null;

      return (255 * (i - (center.x - squareL / 2)) / squareL);
    }

    void makeBitmap (CPColor color) {
      cachedHue = color.getHue();
      CPColor colorLocal = new CPColor(0, 0xFF, 0xFF);

      for (int i = 0; i < side; i++)
        for (int j = 0; j < side; j++) {
          {
            int alpha = 0;
            Integer hue = null;
            Integer hueTmp = getHueFromPoint(i, j);
            if (hueTmp != null) {
              alpha += 255 / 2;
              if (hue == null)
                hue =  hueTmp;
            }
            for (int k = 0; k <= 1; k++)
              {
                hueTmp = getHueFromPoint (i + (k * 2 - 1), j);
                if (hueTmp != null) {
                  alpha += 255 / 16;
                  if (hue == null)
                    hue =  hueTmp;
                }
                hueTmp = getHueFromPoint (i, j + (k * 2 - 1));
                if (hueTmp != null) {
                  alpha += 255 / 16;
                  if (hue == null)
                    hue = hueTmp;
                }

                  hueTmp = getHueFromPoint (i + (k * 2 - 1), j + (k * 2 - 1));
                  if (hueTmp != null) {
                    alpha += 255 / 16;
                    if (hue == null)
                      hue =  hueTmp;
                  }
                  hueTmp = getHueFromPoint (i + (k * 2 - 1), j - (k * 2 - 1));
                  if (hueTmp != null) {
                    alpha += 255 / 16;
                    if (hue == null)
                      hue =  hueTmp;
                }
              }

            if (alpha != 0) {
              colorLocal.setHue(hue);
              data[i + j * side] = alpha << 24 | (colorLocal.rgb & 0x00FFFFFF);
            }
          }

          Integer sat = getSaturationFromPoint(i, j);
          Integer val = getValueFromPoint(i, j);
          if (sat != null && val != null) {
            CPColor tempColor = (CPColor) color.clone();
            tempColor.setSaturation(sat);
            tempColor.setValue(val);
            data[i + j * side] = tempColor.rgb;
          }
        }
    }

    @Override
    public void update (Graphics g)
    {
      paint (g);
    }

    @Override
    public void paint (Graphics g)
    {
      img.flush();
      g.drawImage (img, 0, 0, null);
      CPColor color = controller.getCurColor ();
      double hueAngle = Math.toRadians(color.getHue());
      Point huePoint = new Point (center.x + (int) (Math.cos(hueAngle) * radius), center.y + (int) (Math.sin (hueAngle) * radius));
      g.setXORMode (Color.white);
      int smallCircleRad = 5;
      g.drawOval (huePoint.x - smallCircleRad, huePoint.y - smallCircleRad, smallCircleRad * 2, smallCircleRad * 2);

      Point hvPoint = new Point (center.x - squareL / 2, center.y - squareL / 2);
      hvPoint.translate(color.getSaturation() * squareL / 255, (255 - color.getValue()) * squareL / 255);
      g.drawOval (hvPoint.x - smallCircleRad, hvPoint.y - smallCircleRad, smallCircleRad * 2, smallCircleRad * 2);
    }

    public void mouseSelect (MouseEvent e)
    {
      Integer hue = null;
      hue = curSelectionMode == selectionMode.HUE ? getHueFromPointUnrestricted(e.getX (), e.getY ()) : getHueFromPoint(e.getX (), e.getY ());
      CPColor color = controller.getCurColor ();

      if (hue != null && (curSelectionMode == selectionMode.NONE || curSelectionMode == selectionMode.HUE)) {
        color.setHue (hue);
        controller.setCurColor (color);
        curSelectionMode = selectionMode.HUE;
        return;
      }

      Integer sat = curSelectionMode == selectionMode.SAT_VAL ? getSaturationFromPointUnrestricted (e.getX (), e.getY ()) : getSaturationFromPoint(e.getX (), e.getY ());
      Integer val = curSelectionMode == selectionMode.SAT_VAL ? getValueFromPointUnrestricted(e.getX (), e.getY ()) : getValueFromPoint(e.getX (), e.getY ());
      if (sat != null && val != null && (curSelectionMode == selectionMode.NONE || curSelectionMode == selectionMode.SAT_VAL)) {
        color.setSaturation(sat);
        color.setValue(val);
        curSelectionMode = selectionMode.SAT_VAL;
        controller.setCurColor(color);
        return;
      }

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
    public void mousePressed (MouseEvent e)
    {
      mouseSelect (e);
    }

    @Override
    public void mouseReleased (MouseEvent e)
    {
      curSelectionMode = selectionMode.NONE;
    }

    @Override
    public void mouseMoved (MouseEvent e)
    {
    }

    @Override
    public void mouseDragged (MouseEvent e)
    {
      mouseSelect (e);
    }
    @Override
    public Dimension getPreferredSize ()
    {
      return new Dimension (side, side);
    }

    @Override
    public void colorChanged(CPColor color) {
      if (cachedHue != color.getHue ())
        makeBitmap (color);
      repaint ();
    }
  }

public class CPColorSlider extends JComponent implements MouseListener, MouseMotionListener
{
  final int[] data;
  final int w;
  final int h;
  final Image img;
  int hue;

  final CPColorSelect selecter;

  public CPColorSlider (CPColorSelect selecter)
  {
    w = 24;
    h = 128;

    this.selecter = selecter;

    setBackground (Color.black); // tmp to help see refresh problems
    setSize (new Dimension (w, h));

    data = new int[w * h];
    img = createImage (new MemoryImageSource (w, h, data, 0, w));
    hue = 0;

    makeBitmap ();

    addMouseListener (this);
    addMouseMotionListener (this);
  }

  void makeBitmap ()
  {
    CPColor color = new CPColor (0, 255, 255);
    for (int j = 0; j < h; j++)
      {
        color.setHue ((j * 359) / h);
        for (int i = 0; i < w; i++)
          {
            data[i + j * w] = 0xff000000 | color.rgb;
          }
      }
  }

  @Override
  public void update (Graphics g)
  {
    paint (g);
  }

  @Override
  public void paint (Graphics g)
  {
    img.flush ();
    g.drawImage (img, 0, 0, Color.red, null);

    int y = (hue * h) / 360;
    g.setColor (Color.white);
    g.setXORMode (Color.black);
    g.drawLine (0, y, w, y);
  }

  public void mouseSelect (MouseEvent e)
  {
    int _hue = e.getY () * 360 / h;
    hue = Math.max (0, Math.min (359, _hue));
    repaint ();

    if (selecter != null)
      {
        selecter.setHue (hue);
        // controller.setCurColor(color.GetRgb());
      }
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
  public void mousePressed (MouseEvent e)
  {
    mouseSelect (e);
  }

  @Override
  public void mouseReleased (MouseEvent e)
  {
  }

  @Override
  public void mouseMoved (MouseEvent e)
  {
  }

  @Override
  public void mouseDragged (MouseEvent e)
  {
    mouseSelect (e);
  }

  void setHue (int h)
  {
    hue = h;
    repaint ();
  }

  @Override
  public Dimension getPreferredSize ()
  {
    return new Dimension (w, h);
  }
}

public class CPColorShow extends JComponent implements CPCommonController.ICPColorChangeListener
{

  int curColor;

  @Override
  public void update (Graphics g)
  {
    paint (g);
  }

  @Override
  public void paint (Graphics g)
  {
    Dimension d = getSize ();
    g.setColor (new Color (curColor));
    g.fillRect (0, 0, d.width, d.height);
  }

  @Override
  public void colorChanged(CPColor color) {
    curColor = color.getRgb ();
    repaint ();
  }
}
}
