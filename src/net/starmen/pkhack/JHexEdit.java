package net.starmen.pkhack;

/*
=====================================================================

  JHexEdit.java
  
  Created by Claude Duguay
  Copyright (c) 2001
  
=====================================================================
*/

import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class JHexEdit extends JPanel
{
    protected HexTable table;
  public JHexEdit(File file)
    throws IOException
  {
    this(new HexFile(file));
  }
  
  public JHexEdit(HexData data)
  {
    HexTableModel model = new HexTableModel(data);
    table = new HexTable(model);
    setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    setLayout(new GridLayout());
    add(new JScrollPane(table));
    setPreferredSize(new Dimension(
      table.getPreferredSize().width + 8, 400));
  }
}

