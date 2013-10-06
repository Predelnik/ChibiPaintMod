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

package chibipaint.engine;

import chibipaint.util.CPRect;

import java.util.LinkedList;
import java.util.Vector;

public class CPUndoManager {

    private final LinkedList<CPUndo> undoList = new LinkedList<CPUndo>();

    private final LinkedList<CPUndo> redoList = new LinkedList<CPUndo>();

    public LinkedList<CPUndo> getUndoList() {
        return undoList;
    }

    public LinkedList<CPUndo> getRedoList() {
        return redoList;
    }

    static class CPUndoPaint extends CPUndo {

        final int layer;
        final CPRect rect;
        final int[] data;
        private final CPArtwork artwork;


        public CPUndoPaint(CPArtwork artwork) {
            this.artwork = artwork;
            layer = artwork.getActiveLayerNb();
            rect = new CPRect(artwork.undoArea);

            data = artwork.undoBuffer.copyRectXOR(artwork.curLayer, rect);
            artwork.undoArea.makeEmpty();
        }

        @Override
        public void undo() {
            artwork.getLayer(layer).setRectXOR(data, rect);
            artwork.invalidateFusion(rect);
        }

        @Override
        public void redo() {
            artwork.getLayer(layer).setRectXOR(data, rect);
            artwork.invalidateFusion(rect);
        }

        @Override
        public long getMemoryUsed(boolean undone, Object param) {
            return data.length * 4;
        }
    }

    static class CPUndoPaintAll extends CPUndo {

        final Vector<int[]> data;
        final CPRect rect;
        private final CPArtwork artwork;

        public CPUndoPaintAll(CPArtwork artwork) {
            this.artwork = artwork;
            data = new Vector<int[]>(artwork.getLayersVector().size());
            data.setSize(artwork.getLayersVector().size());
            rect = new CPRect(artwork.undoArea);
            for (int i = 0; i < artwork.getLayersVector().size(); i++)
                data.setElementAt(artwork.undoBufferAll.elementAt(i).copyRectXOR(artwork.getLayersVector().elementAt(i), rect), i);

            artwork.undoBufferAll = null; // Hope gc will be a good boy
        }

        @Override
        public void undo() {
            for (int i = 0; i < artwork.getLayersVector().size(); i++)
                artwork.getLayer(i).setRectXOR(data.elementAt(i), rect);

            artwork.invalidateFusion(rect);
        }

        @Override
        public void redo() {
            for (int i = 0; i < artwork.getLayersVector().size(); i++)
                artwork.getLayer(i).setRectXOR(data.elementAt(i), rect);

            artwork.invalidateFusion(rect);
        }

        @Override
        public long getMemoryUsed(boolean undone, Object param) {
            return (data.size() != 0) ? data.size() * data.elementAt(0).length * 4 : 0;
        }
    }

    static class CPUndoLayerVisible extends CPUndo {

        final int layer;
        final boolean oldVis;
        boolean newVis;
        private final CPArtwork artwork;

        public CPUndoLayerVisible(CPArtwork artwork, int layer, boolean oldVis, boolean newVis) {
            this.artwork = artwork;
            this.layer = layer;
            this.oldVis = oldVis;
            this.newVis = newVis;
        }

        @Override
        public void redo() {
            artwork.getLayer(layer).setVisible(newVis);
            artwork.invalidateFusion();
            artwork.callListenersLayerChange();
        }

        @Override
        public void undo() {
            artwork.getLayer(layer).setVisible(oldVis);
            artwork.invalidateFusion();
            artwork.callListenersLayerChange();
        }

        @Override
        public boolean merge(CPUndo u) {
            if (u instanceof CPUndoLayerVisible && layer == ((CPUndoLayerVisible) u).layer) {
                newVis = ((CPUndoLayerVisible) u).newVis;
                return true;
            }
            return false;
        }

        @Override
        public boolean noChange() {
            return oldVis == newVis;
        }
    }

    static class CPUndoMergeAllLayers extends CPUndo {

        final Vector<CPLayer> oldLayers;
        final int oldActiveLayer;
        private final CPArtwork artwork;

        @SuppressWarnings("unchecked")
        public CPUndoMergeAllLayers(CPArtwork artwork) {
            this.artwork = artwork;
            oldLayers = (Vector<CPLayer>) artwork.getLayersVector().clone();
            oldActiveLayer = artwork.getActiveLayerNb();
        }

        @Override
        @SuppressWarnings("unchecked")
        public void undo() {
            artwork.setLayers((Vector<CPLayer>) oldLayers.clone());
            artwork.setActiveLayer(oldActiveLayer);

            artwork.invalidateFusion();
            artwork.callListenersLayerChange();
        }

        @Override
        public void redo() {
            artwork.mergeAllLayers(false);
        }

        @Override
        public long getMemoryUsed(boolean undone, Object param) {
            return undone ? 0 : oldLayers.size() * artwork.width * artwork.height * 4;
        }
    }

    static class CPUndoMergeDownLayer extends CPUndo {

        final int layer;
        CPLayer layerBottom, layerTop;
        private final CPArtwork artwork;

        public CPUndoMergeDownLayer(CPArtwork artwork, int layer) {
            this.artwork = artwork;
            this.layer = layer;
            layerBottom = new CPLayer(artwork.width, artwork.height);
            layerBottom.copyFrom(artwork.getLayersVector().elementAt(layer - 1));
            layerTop = artwork.getLayersVector().elementAt(layer);
        }

        @Override
        public void undo() {
            artwork.getLayersVector().elementAt(layer - 1).copyFrom(layerBottom);
            artwork.getLayersVector().add(layer, layerTop);
            artwork.setActiveLayer(layer);

            layerBottom = layerTop = null;

            artwork.invalidateFusion();
            artwork.callListenersLayerChange();
        }

        @Override
        public void redo() {
            layerBottom = new CPLayer(artwork.width, artwork.height);
            layerBottom.copyFrom(artwork.getLayersVector().elementAt(layer - 1));
            layerTop = artwork.getLayersVector().elementAt(layer);

            artwork.setActiveLayer(layer);
            artwork.mergeDown(false);
        }

        @Override
        public long getMemoryUsed(boolean undone, Object param) {
            return undone ? 0 : artwork.width * artwork.height * 4 * 2;
        }
    }

    static class CPUndoMoveLayer extends CPUndo {

        final int from;
        final int to;
        private final CPArtwork artwork;

        public CPUndoMoveLayer(CPArtwork artwork, int from, int to) {
            this.artwork = artwork;
            this.from = from;
            this.to = to;
        }

        @Override
        public void undo() {
            if (to <= from) {
                artwork.moveLayerReal(to, from + 1);
            } else {
                artwork.moveLayerReal(to - 1, from);
            }
        }

        @Override
        public void redo() {
            artwork.moveLayerReal(from, to);
        }
    }

    static class CPUndoRemoveLayer extends CPUndo {

        final int layer;
        final CPLayer layerObj;
        private final CPArtwork artwork;

        public CPUndoRemoveLayer(CPArtwork artwork, int layer, CPLayer layerObj) {
            this.artwork = artwork;
            this.layer = layer;
            this.layerObj = layerObj;
        }

        @Override
        public void undo() {
            artwork.getLayersVector().add(layer, layerObj);
            artwork.setActiveLayer(layer);
            artwork.invalidateFusion();
            artwork.callListenersLayerChange();
        }

        @Override
        public void redo() {
            artwork.getLayersVector().remove(layer);
            artwork.setActiveLayer(layer < artwork.getLayersVector().size() ? layer : layer - 1);
            artwork.invalidateFusion();
            artwork.callListenersLayerChange();
        }

        @Override
        public long getMemoryUsed(boolean undone, Object param) {
            return undone ? 0 : artwork.width * artwork.height * 4;
        }

    }

    static class CPUndoSelection extends CPUndo {

        final CPRect rect;
        final byte[] data;
        private final CPArtwork artwork;

        public CPUndoSelection(CPArtwork artwork, CPSelection pastSelection, CPRect changedRect) {
            this.artwork = artwork;
            rect = (CPRect) changedRect.clone();
            data = pastSelection.copyRectXOR(artwork.curSelection, changedRect);
        }

        @Override
        public void undo() {
            artwork.curSelection.setRectXOR(data, rect);
            artwork.curSelection.precalculateSelection ();
            artwork.invalidateFusion(rect);
        }

        @Override
        public void redo() {
            artwork.curSelection.setRectXOR(data, rect);
            artwork.curSelection.precalculateSelection ();
            artwork.invalidateFusion(rect);
        }

        @Override
        public boolean merge(CPUndo u) {
            return false;
        }

        @Override
        public boolean noChange() {
            for (byte aData : data) {
                if (aData > 0) {
                    return false;
                }
            }

            return true;
        }
    }

    static class CPUndoAddLayer extends CPUndo {

        final int layer;
        private final CPArtwork artwork;

        public CPUndoAddLayer(CPArtwork artwork, int layer) {
            this.artwork = artwork;
            this.layer = layer;
        }

        @Override
        public void undo() {
            artwork.getLayersVector().remove(layer + 1);
            artwork.setActiveLayer(layer);
            artwork.invalidateFusion();
            artwork.callListenersLayerChange();
        }

        @Override
        public void redo() {
            CPLayer newLayer = new CPLayer(artwork.width, artwork.height);
            newLayer.setName(artwork.getDefaultLayerName());
            artwork.getLayersVector().add(layer + 1, newLayer);

            artwork.setActiveLayer(layer + 1);
            artwork.invalidateFusion();
            artwork.callListenersLayerChange();
        }
    }

    static class CPUndoDuplicateLayer extends CPUndo {

        final int layer;
        private final CPArtwork artwork;

        public CPUndoDuplicateLayer(CPArtwork artwork, int layer) {
            this.artwork = artwork;
            this.layer = layer;
        }

        @Override
        public void undo() {
            artwork.getLayersVector().remove(layer + 1);
            artwork.setActiveLayer(layer);
            artwork.invalidateFusion();
            artwork.callListenersLayerChange();
        }

        @Override
        public void redo() {
            String copySuffix = " Copy";

            CPLayer newLayer = new CPLayer(artwork.width, artwork.height);
            newLayer.copyFrom(artwork.getLayersVector().elementAt(layer));
            if (!newLayer.getName().endsWith(copySuffix)) {
                newLayer.setName(newLayer.getName() + copySuffix);
            }
            artwork.getLayersVector().add(layer + 1, newLayer);

            artwork.setActiveLayer(layer + 1);
            artwork.invalidateFusion();
            artwork.callListenersLayerChange();
        }
    }

    static class CPUndoLayerAlpha extends CPUndo {

        final int layer;
        final int from;
        int to;
        private final CPArtwork artwork;

        public CPUndoLayerAlpha(CPArtwork artwork, int layer, int alpha) {
            this.artwork = artwork;
            this.from = artwork.getLayer(layer).getAlpha();
            this.to = alpha;
            this.layer = layer;
        }

        @Override
        public void undo() {
            artwork.getLayer(layer).setAlpha(from);
            artwork.invalidateFusion();
            artwork.callListenersLayerChange();
        }

        @Override
        public void redo() {
            artwork.getLayer(layer).setAlpha(to);
            artwork.invalidateFusion();
            artwork.callListenersLayerChange();
        }

        @Override
        public boolean merge(CPUndo u) {
            if (u instanceof CPUndoLayerAlpha && layer == ((CPUndoLayerAlpha) u).layer) {
                to = ((CPUndoLayerAlpha) u).to;
                return true;
            }
            return false;
        }

        @Override
        public boolean noChange() {
            return from == to;
        }
    }

    static class CPUndoLayerMode extends CPUndo {

        final int layer;
        final int from;
        int to;
        private final CPArtwork artwork;

        public CPUndoLayerMode(CPArtwork artwork, int layer, int mode) {
            this.artwork = artwork;
            this.from = artwork.getLayer(layer).getBlendMode();
            this.to = mode;
            this.layer = layer;
        }

        @Override
        public void undo() {
            artwork.getLayer(layer).setBlendMode(from);
            artwork.invalidateFusion();
            artwork.callListenersLayerChange();
        }

        @Override
        public void redo() {
            artwork.getLayer(layer).setBlendMode(to);
            artwork.invalidateFusion();
            artwork.callListenersLayerChange();
        }

        @Override
        public boolean merge(CPUndo u) {
            if (u instanceof CPUndoLayerMode && layer == ((CPUndoLayerMode) u).layer) {
                to = ((CPUndoLayerMode) u).to;
                return true;
            }
            return false;
        }

        @Override
        public boolean noChange() {
            return from == to;
        }
    }

    static class CPUndoLayerRename extends CPUndo {

        final int layer;
        final String from;
        String to;
        private final CPArtwork artwork;

        public CPUndoLayerRename(CPArtwork artwork, int layer, String name) {
            this.artwork = artwork;
            this.from = artwork.getLayer(layer).getName();
            this.to = name;
            this.layer = layer;
        }

        @Override
        public void undo() {
            artwork.getLayer(layer).setName(from);
            artwork.callListenersLayerChange();
        }

        @Override
        public void redo() {
            artwork.getLayer(layer).setName(to);
            artwork.callListenersLayerChange();
        }

        @Override
        public boolean merge(CPUndo u) {
            if (u instanceof CPUndoLayerRename && layer == ((CPUndoLayerRename) u).layer) {
                to = ((CPUndoLayerRename) u).to;
                return true;
            }
            return false;
        }

        @Override
        public boolean noChange() {
            return from.equals(to);
        }
    }
}
