/*
 * Created on Aug 26, 2004
 */
package net.starmen.pkhack;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;

/**
 * Limited length <code>Document</code> that can only contain numbers.
 * 
 * @author AnyoneEB
 * @see net.starmen.pkhack.MaxLengthDocument
 */
public class NumericMaxLengthDocument extends MaxLengthDocument
{
    public NumericMaxLengthDocument(int maxLength)
    {
        super(maxLength);
        try
        {
            insertString(0, "0", new SimpleAttributeSet());
        }
        catch (BadLocationException e)
        {}
    }

    public void insertString(int offs, String str, AttributeSet a)
        throws BadLocationException
    {
        while (this.getLength() > 0 && this.getText(0, 1).equals("0"))
        {
            super.remove(0, 1);
            if (offs > 0)
                offs--;
        }
        super.insertString(offs, str.replaceAll("[^\\d]+", ""), a);
        //remove leading zeros
        if (this.getLength() > 1 && this.getText(0, 1).equals("0"))
            remove(0, 1);
        else if (this.getLength() == 0)
            super.insertString(0, "0", new SimpleAttributeSet());
    }

    public void remove(int offs, int len) throws BadLocationException
    {
        super.remove(offs, len);
        if (this.getLength() == 0)
            super.insertString(0, "0", new SimpleAttributeSet());
        else if (this.getLength() > 1 && this.getText(0, 1).equals("0"))
            remove(0, 1);
    }
}