package net.starmen.pkhack;

/*
=====================================================================

  JHexEditTest.java
  
  Created by Claude Duguay
  Copyright (c) 2001
  
=====================================================================
*/

import java.io.*;
import java.awt.*;
import javax.swing.*;

public class JHexEditTest
{
  public static void main(String[] args)
    throws IOException
  {
    if (args.length != 1)
    {
      System.out.println("Usage: java JHexEditTest filename");
    }
    String filename = args[0];
    File file = new File(filename);
    if (!file.exists())
    {
      System.out.println("File '" + filename + "' not found.");
      System.exit(-1);
    }
  
    JFrame frame = new JFrame("JHexEdit Test");
    frame.getContentPane().setLayout(new GridLayout());
    frame.getContentPane().add(new JHexEdit(file));
    frame.pack();
    frame.setVisible(true);
  }
}

