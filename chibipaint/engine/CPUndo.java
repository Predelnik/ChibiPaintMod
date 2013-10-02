package chibipaint.engine;

/**
* Created with IntelliJ IDEA.
* User: Sergey
* Date: 02.10.13
* Time: 22:39
* To change this template use File | Settings | File Templates.
*/
abstract public class CPUndo {

    abstract public void undo();

    abstract public void redo();

    @SuppressWarnings({ "unused", "static-method" })
    public boolean merge(CPUndo u)
    {
        return false;
    }

    @SuppressWarnings("static-method")
    public boolean noChange() {
        return false;
    }

    @SuppressWarnings({ "unused", "static-method" })
    public long getMemoryUsed(boolean undone, Object param)
    {
        return 0;
    }

}
