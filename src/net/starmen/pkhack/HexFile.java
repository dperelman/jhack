package net.starmen.pkhack;

/*
=====================================================================

  HexFile.java
  
  Created by Claude Duguay
  Copyright (c) 2001
  
=====================================================================
*/

import java.io.*;

public class HexFile extends RandomAccessFile
  implements HexData
{
  public static final int ROW_SIZE = 16;
  
  protected int size;

  public HexFile(String filename)
    throws IOException
  {
    this(new File(filename));
  }
  
  public HexFile(File file)
    throws IOException
  {
    super(file, "rw");
    size = (int)length();
  }
  
  public int getRowCount()
  {
    int rows = size / ROW_SIZE;
    if (rows * ROW_SIZE < size) rows++;
    return rows;
  }
  
  public int getColumnCount()
  {
    return ROW_SIZE;
  }
  
  public int getLastRowSize()
  {
    int max = (getRowCount() - 1) * ROW_SIZE;
    if ((size - max) == 0) return ROW_SIZE;
    return size - max;
  }
  
  public byte getByte(int row, int col)
  {
    try
    {
      seek(row * ROW_SIZE + col);
      return (byte)read();
    }
    catch (IOException e)
    {
      e.printStackTrace();
      return 0;
    }
  }

  public void setByte(int row, int col, byte value)
  {
    try
    {
      seek(row * ROW_SIZE + col);
      write(value);
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }

  public byte[] getRow(int row)
  {
    int rowSize = ROW_SIZE;
    if (row == getRowCount() - 1)
      rowSize = getLastRowSize();
    byte[] data = new byte[rowSize];
    try
    {
      seek(row * ROW_SIZE);
      read(data);
      return data;
    }
    catch (IOException e)
    {
      e.printStackTrace();
      return data;
    }
  }
}

