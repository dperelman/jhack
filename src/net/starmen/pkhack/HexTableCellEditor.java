package net.starmen.pkhack;

/*
 * =====================================================================
 * HexTableCellEditor.java Created by Claude Duguay Copyright (c) 2001
 * =====================================================================
 */

import java.awt.Color;
import java.awt.Font;

import javax.swing.DefaultCellEditor;
import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

public class HexTableCellEditor extends DefaultCellEditor
{
    public HexTableCellEditor()
    {
        super(new JTextField());
        final JTextField field = (JTextField) getComponent();
        field.setBorder(null);
        field.setHorizontalAlignment(JTextField.CENTER);
        field.setForeground(Color.white);
        field.setBackground(Color.red);
        field.setFont(new Font("monospaced", Font.PLAIN, 12));

        field.setDocument(new PlainDocument()
        {
            public boolean isHexString(String s)
            {
                try
                {
                    Integer.parseInt(s, 16);
                    return true;
                }
                catch (NumberFormatException e)
                {
                    return false;
                }
            }
            public void insertString(int offs, String str, AttributeSet a)
                throws BadLocationException
            {
                {
                    if (field.getCaretPosition() == 2)
                        field.setCaretPosition(1);
                    
                    if (str == null || !isHexString(str))
                        return;
                    if (offs == 2)
                        offs = 0;
                    if (offs + str.length() > 2)
                        str = str.substring(0, 2 - offs);
                    if (offs < this.getLength())
                        this.remove(offs, str.length());
                    super.insertString(offs, str, a);
                }
            }
        });
    }
}