package net.starmen.pkhack;

/*
 * =====================================================================
 * HexTable.java Created by Claude Duguay Copyright (c) 2001
 * =====================================================================
 */

import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JTable;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

public class HexTable extends JTable
{
    public HexTable(HexTableModel model)
    {
        super(model);
        setDefaultRenderer(String.class, new HexTableCellRenderer());
        setDefaultEditor(String.class, new HexTableCellEditor());

        int count = model.getColumnCount();
        for (int i = 0; i < count; i++)
        {
            int w = 22;
            if (i == 0)
                w = 55;
            if (i == (count - 1))
                w = 140;
            String name = getColumnName(i);
            TableColumn column = getColumn(name);
            column.setWidth(w);
            column.setMinWidth(w);
            column.setPreferredWidth(w);
        }

        setShowGrid(false);
        setRowSelectionAllowed(false);
        setCellSelectionEnabled(true);
        setFont(new Font("monospaced", Font.PLAIN, 12));
        setIntercellSpacing(new Dimension(4, 0));
    }

    public void setModel(TableModel model)
    {
        if (model instanceof HexTableModel)
        {
            super.setModel(model);
        }
        else
        {
            throw new IllegalArgumentException(
                "HexTable expects to use a HexTableModel");
        }
    }

    public void gotoOff(int offset)
    {
        int r = offset / 16, c = (offset % 16) + 1;
        try
        {
            setRowSelectionInterval(r, r);
            setColumnSelectionInterval(c, c);
            scrollRectToVisible(getCellRect(r, c, true));
        }
        catch (IllegalArgumentException e)
        {
            e.printStackTrace();
        }
    }
}