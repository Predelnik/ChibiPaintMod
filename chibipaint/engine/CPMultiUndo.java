package chibipaint.engine;

/**
* Created with IntelliJ IDEA.
* User: Sergey
* Date: 02.10.13
* Time: 21:24
* To change this template use File | Settings | File Templates.
*/ // used to encapsulate multiple undo operation as one
class CPMultiUndo extends CPUndo {

    CPUndo[] undoes;

    public CPMultiUndo(CPUndo[] undoes) {
        this.undoes = undoes;
    }

    @Override
    public void undo() {
        for (int i = undoes.length - 1; i >= 0; i--) {
            undoes[i].undo();
        }
    }

    @Override
    public void redo() {
        for (int i = 0; i < undoes.length; i++) {
            undoes[i].redo();
        }
    }

    @Override
    public boolean merge(CPUndo u) {
        return false;
    }

    @Override
    public boolean noChange() {
        boolean noChange = true;
        for (int i = 0; i < undoes.length; i++) {
            noChange = noChange && undoes[i].noChange();
        }
        return noChange;
    }

    @Override
    public long getMemoryUsed(boolean undone, Object param) {
        long total = 0;
        for (CPUndo undo : undoes) {
            total += undo.getMemoryUsed(undone, param);
        }
        return total;
    }
}
