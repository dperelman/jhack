package net.starmen.pkhack;

/*
=====================================================================

  HexTableModel.java
  
  Created by Claude Duguay
  Copyright (c) 2001
  
=====================================================================
*/

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

public class HexTableModel
  implements TableModel
{
  protected static final String HEX = "0123456789ABCDEF";
  
  protected ArrayList listeners = new ArrayList();
  
  protected String[] names =
  {
    "Address",
    "0", "1", "2", "3", "4", "5", "6", "7",
    "8", "9", "A", "B", "C", "D", "E", "F",
    "Text"
  };
  
  protected HexData data;
  
  public HexTableModel(HexData data)
  {
    this.data = data;
  }
  
  public Class getColumnClass(int index)
  {
    return String.class;
  }
 
  public String getColumnName(int col) 
  {
    return names[col];
  }
  
  public int getColumnCount()
  {
    return names.length;
  }
  
  public int getRowCount()
  {
    return data.getRowCount();
  }
  
  public boolean isCellEditable(int row, int col)
  {
      return true;
//    if (row == (getRowCount() - 1) &&
//      col >= data.getLastRowSize())
//    {
//      return false;
//    }
//    int max = getColumnCount() - 1;
//    return col != 0 && col != max;
  }
 
  public Object getValueAt(int row, int col)
  {
    int max = getColumnCount() - 1;
    if (col == 0)
    {
      return HackModule.addZeros(Integer.toHexString(row * 16), 6) + ":";
    }
    if (col == max)
    {
      return new String(data.getRow(row));
    }
//    if (row == (data.getRowCount() - 1) &&
//      col >= data.getLastRowSize())
//    {
//      return "";
//    }
    return HackModule.addZeros(Integer.toHexString(0xff & data.getByte(row, col - 1)), 2);
  }
  
  public void setValueAt(Object value, int row, int col)
  {
    String hex = HackModule.addZeros(value.toString(), 2);
    data.setByte(row, col - 1,
      (byte)Integer.parseInt(hex, 16));
    int max = getColumnCount() - 1;
    fireModelChangeEvent(row, col - 1);
    fireModelChangeEvent(row, max);
  }

  protected void fireModelChangeEvent(int row, int col)
  {
    List list = (ArrayList)listeners.clone();
    TableModelEvent event = new TableModelEvent(
      this, row, row, col);
    for (int i = 0; i < list.size(); i++)
    {
      TableModelListener listener =
        (TableModelListener)list.get(i);
      listener.tableChanged(event);
    }
  }
  
  public void addTableModelListener(
    TableModelListener listener)
  {
    listeners.add(listener);
  }
 
  public void removeTableModelListener(
    TableModelListener listener)
  {
    listeners.remove(listener);
  }
}

