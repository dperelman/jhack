package net.starmen.pkhack;

/*
=====================================================================

  HexTableCellRenderer.java
  
  Created by Claude Duguay
  Copyright (c) 2001
  
=====================================================================
*/

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;

public class HexTableCellRenderer
  extends DefaultTableCellRenderer
{
  protected static final Font FONT =
    new Font("monospaced", Font.PLAIN, 12);
  
  public HexTableCellRenderer() {}

  public Component getTableCellRendererComponent(
    JTable table, Object value,
    boolean isSelected, boolean hasFocus,
    int row, int col)
  {
    super.getTableCellRendererComponent(
      table, value, isSelected, hasFocus, row, col);
    int max = table.getModel().getColumnCount() - 1;
    if (col == 0 || col == max)
    {
      setBorder(null);
      setBackground(Color.blue);
      setForeground(Color.white);
      if (col == 0)
      {
        setHorizontalAlignment(RIGHT);
      }
      else
      {
        setHorizontalAlignment(LEFT);
      }
    }
    else
    {
      if (isSelected || hasFocus)
      {
        setBackground(Color.blue);
        setForeground(Color.white);
      }
      else
      {
        setForeground(Color.black);
        setBackground(Color.white);
      }
      setHorizontalAlignment(CENTER);
    }
    setFont(FONT);
    return this;
  }
}

