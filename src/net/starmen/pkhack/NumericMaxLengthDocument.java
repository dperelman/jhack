/*
 * Created on Aug 26, 2004
 */
package net.starmen.pkhack;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import javax.swing.text.AbstractDocument.Content;

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
    }

    public void insertString(int offs, String str, AttributeSet a)
        throws BadLocationException
    {
        super.insertString(offs, str.replaceAll("[^\\d]+", ""), a);
    }
}