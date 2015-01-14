package chibipaint.util;

import javax.swing.*;
import javax.swing.text.DefaultFormatter;

public class CPSwingUtils
{
public static void allowOnlyCorrectValues (JSpinner spinner)
{
  JComponent comp = spinner.getEditor ();
  JFormattedTextField field = (JFormattedTextField) comp.getComponent (0);
  DefaultFormatter formatter = (DefaultFormatter) field.getFormatter ();
  formatter.setAllowsInvalid (false);
}

  public static class NoneSelectedButtonGroup extends ButtonGroup {

    @Override
    public void setSelected(ButtonModel model, boolean selected) {

      if (selected) {

        super.setSelected(model, selected);

      } else {

        clearSelection();
      }
    }
  }
}
