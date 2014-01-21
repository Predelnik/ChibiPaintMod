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

package chibipaint.controller;

public enum CPCommandId
{
  ZoomIn,
  ZoomOut,
  Zoom100,
  ZoomSpecific,
  Undo,
  Redo,
  ClearHistory,
  Pencil,
  Pen,
  Eraser,
  SoftEraser,
  AirBrush,
  Dodge,
  Burn,
  Water,
  Blur,
  Smudge,
  Blender,
  FloodFill,
  MagicWand,
  FreeSelection,
  FreeTransform,
  RectSelection,
  RotateCanvas,
  FreeHand,
  Line,
  Bezier,
  About,
  Test,
  LayerToggleAll,
  LayerDuplicate,
  LayerMergeDown,
  LayerMergeAll,
  Fill,
  Clear,
  SelectAll,
  InvertSelection,
  DeselectAll,
  MNoise,
  CNoise,
  FXBoxBlur,
  FXGaussianBlur,
  FXInvert,
  FXMakeGrayscaleByLuma,
  ApplyToAllLayers,
  ShowSelection,
  LinearInterpolation,
  ShowGrid,
  GridOptions,
  ResetCanvasRotation,
  PalColor,
  PalBrush,
  PalLayers,
  PalStroke,
  PalSwatches,
  PalTool,
  PalMisc,
  PalTextures,
  TogglePalettes,
  Copy,
  CopyMerged,
  Paste,
  Cut,
  ApplyTransform,
  CancelTransform,
  MoveTransform,
  FlipHorizontally,
  FlipVertically,
  Rotate90CCW,
  Rotate90CW,
  AddLayer,
  RemoveLayer,

// Don't forget to add ids to corresponding functions below

  // For App only:
  Export, // Additional in info is extension
  Import, // Additional in info is extension
  Exit,
  Save,
  New,
  OpenRecent, // Additional in info is number

  // For Applet only:
  Float,
  Send;

public boolean isForAppOnly ()
{
  switch (this)
    {
    case Export:
    case Import:
    case Exit:
    case OpenRecent:
    case Save:
    case New:
      return true;
    default:
      return false;
    }
}

public boolean isForAppletOnly ()
{
  switch (this)
    {
    case Float:
    case Send:
      return true;
    default:
      return false;
    }
}

public boolean isSpecificToType ()
{
  return (this.isForAppletOnly () || this.isForAppOnly ());
}
}
