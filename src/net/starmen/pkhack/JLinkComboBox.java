/*
 * Created on Aug 26, 2004
 */
package net.starmen.pkhack;

import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComboBox;
import javax.swing.UIManager;

/**
 * Creates a searchable <code>JComboBox</code> with a link to a
 * <code>HackModule</code>.
 * <p>
 * The <code>JLabel</code> labeling the link is drawn with in blue with an
 * underline like the default style for a hyperlink in HTML. The
 * <code>HackModule</code>'s{@link HackModule#show(Object)}method is called
 * through <code>MainGUI.showModule()</code>. The <code>Object</code> used
 * is an <code>Integer</code> created using
 * <code>HackModule.getNumberOfString()</code> on the item selected in the
 * combo box. (If the user is typing in a combo box with the type set to
 * {@link net.starmen.pkhack.JSearchableComboBox#SEARCH_EDIT}, then the click
 * on the link is ignored.)
 * </p>
 * <p>
 * The combo box can either be given to one of the constructors or created by
 * giving a <code>String[]</code> and having
 * <code>HackModule.createComboBox()</code> create it. Either way, the combo
 * box must be labeled by or in the same way as
 * <code>HackModule.getNumberedString()</code>.
 * </p>
 * 
 * @author AnyoneEB
 * @see net.starmen.pkhack.JSearchableComboBox
 * @see net.starmen.pkhack.MainGUI#showModule(Class, Object)
 * @see HackModule#getNumberedString(String, int)
 * @see HackModule#getNumberOfString(String)
 * @see HackModule#createComboBox(Object[])
 */
public class JLinkComboBox extends JSearchableComboBox
{
    /** String provided by the caller as a label. */
    protected final String text;

    /**
     * Constructor for <code>JLinkComboBox</code>. The search mode is set to
     * the defaule value of <code>SEARCH_LEFT</code>. This means that a text
     * field is shown to the left of the combo box with a search button. The
     * size of the text field is set to the default value of 10.
     * 
     * @param c Class to link to. This must represent a class extending
     *            <code>HackModule</code>. The class must also appear in the
     *            file net/starmen/pkhack/modulelist.txt, so that
     *            <code>MainGUI</code> will have loaded it.
     * @param items A <code>String[]</code> to call
     *            {@link HackModule#createComboBox(Object[])}on in order to get
     *            the <code>JComboBox</code> for this to display.
     * @param text Text to label the JComboBox with. This will be shown in blue
     *            with an underline and after it will be the string ": ".
     * @see JSearchableComboBox#SEARCH_LEFT
     * @see HackModule
     * @see MainGUI
     */
    public JLinkComboBox(final Class c, String[] items, String text)
    {
        this(c, items, text, SEARCH_LEFT);
    }

    /**
     * Constructor for <code>JLinkComboBox</code>. The search mode is set to
     * the defaule value of <code>SEARCH_LEFT</code>. This means that a text
     * field is shown to the left of the combo box with a search button. The
     * size of the text field is set to the default value of 10.
     * 
     * @param c Class to link to. This must represent a class extending
     *            <code>HackModule</code>. The class must also appear in the
     *            file net/starmen/pkhack/modulelist.txt, so that
     *            <code>MainGUI</code> will have loaded it.
     * @param jcb <code>JComboBox</code> to display and get number from for
     *            link
     * @param text Text to label the JComboBox with. This will be shown in blue
     *            with an underline and after it will be the string ": ".
     * @see JSearchableComboBox#SEARCH_LEFT
     * @see HackModule
     * @see MainGUI
     */
    public JLinkComboBox(final Class c, JComboBox jcb, String text)
    {
        this(c, jcb, text, SEARCH_LEFT);
    }

    /**
     * Constructor for <code>JLinkComboBox</code>. The size of the text field
     * if <code>smode</code> is <code>SEARCH_LEFT</code> or
     * <code>SEARCH_RIGHT</code> is set to the default value of 10.
     * 
     * @param c Class to link to. This must represent a class extending
     *            <code>HackModule</code>. The class must also appear in the
     *            file net/starmen/pkhack/modulelist.txt, so that
     *            <code>MainGUI</code> will have loaded it.
     * @param items A <code>String[]</code> to call
     *            {@link HackModule#createComboBox(Object[])}on in order to get
     *            the <code>JComboBox</code> for this to display.
     * @param text Text to label the JComboBox with. This will be shown in blue
     *            with an underline and after it will be the string ": ".
     * @param smode Search mode, one of {@link #SEARCH_NONE},
     *            {@link #SEARCH_LEFT},{@link #SEARCH_RIGHT},
     *            {@link #SEARCH_EDIT}.
     * @see HackModule
     * @see MainGUI
     */
    public JLinkComboBox(final Class c, String[] items, String text, int smode)
    {
        this(c, items, text, 10, smode);
    }

    /**
     * Constructor for <code>JLinkComboBox</code>. The size of the text field
     * if <code>smode</code> is <code>SEARCH_LEFT</code> or
     * <code>SEARCH_RIGHT</code> is set to the default value of 10.
     * 
     * @param c Class to link to. This must represent a class extending
     *            <code>HackModule</code>. The class must also appear in the
     *            file net/starmen/pkhack/modulelist.txt, so that
     *            <code>MainGUI</code> will have loaded it.
     * @param jcb <code>JComboBox</code> to display and get number from for
     *            link
     * @param text Text to label the JComboBox with. This will be shown in blue
     *            with an underline and after it will be the string ": ".
     * @param smode Search mode, one of {@link #SEARCH_NONE},
     *            {@link #SEARCH_LEFT},{@link #SEARCH_RIGHT},
     *            {@link #SEARCH_EDIT}.
     * @see HackModule
     * @see MainGUI
     */
    public JLinkComboBox(final Class c, JComboBox jcb, String text, int smode)
    {
        this(c, jcb, text, 10, smode);
    }

    /**
     * Constructor for <code>JLinkComboBox</code>.
     * 
     * @param c Class to link to. This must represent a class extending
     *            <code>HackModule</code>. The class must also appear in the
     *            file net/starmen/pkhack/modulelist.txt, so that
     *            <code>MainGUI</code> will have loaded it.
     * @param items A <code>String[]</code> to call
     *            {@link HackModule#createComboBox(Object[])}on in order to get
     *            the <code>JComboBox</code> for this to display.
     * @param text Text to label the JComboBox with. This will be shown in blue
     *            with an underline and after it will be the string ": ".
     * @param searchSize the size of the search text field if <code>smode</code>
     *            is <code>SEARCH_LEFT</code> or <code>SEARCH_RIGHT</code>
     * @param smode Search mode, one of {@link #SEARCH_NONE},
     *            {@link #SEARCH_LEFT},{@link #SEARCH_RIGHT},
     *            {@link #SEARCH_EDIT}.
     * @see HackModule
     * @see MainGUI
     */
    public JLinkComboBox(final Class c, String[] items, String text,
        int searchSize, int smode)
    {
        this(c, HackModule.createComboBox(items), text, searchSize, smode);
    }

    /**
     * Main constructor for <code>JLinkComboBox</code>. All others provide
     * default values to this constructor.
     * 
     * @param c Class to link to. This must represent a class extending
     *            <code>HackModule</code>. The class must also appear in the
     *            file net/starmen/pkhack/modulelist.txt, so that
     *            <code>MainGUI</code> will have loaded it.
     * @param jcb <code>JComboBox</code> to display and get number from for
     *            link
     * @param text Text to label the JComboBox with. This will be shown in blue
     *            with an underline and after it will be the string ": ".
     * @param searchSize the size of the search text field if <code>smode</code>
     *            is <code>SEARCH_LEFT</code> or <code>SEARCH_RIGHT</code>
     * @param smode Search mode, one of {@link #SEARCH_NONE},
     *            {@link #SEARCH_LEFT},{@link #SEARCH_RIGHT},
     *            {@link #SEARCH_EDIT}.
     * @see HackModule
     * @see MainGUI
     */
    public JLinkComboBox(final Class c, JComboBox jcb, String text,
        int searchSize, int smode)
    {
        super(jcb, "<html><font color = \"blue\"><u>" + text + "</u></font>"
            + ": " + "</html>", searchSize, smode);

        this.label.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent arg0)
            {
                if (label.isEnabled() && comboBox.getSelectedIndex() != -1)
                {
                    int index = HackModule.getNumberOfString(comboBox
                        .getSelectedItem().toString());
                    JHack.main.showModule(c, new Integer(index));
                }
            }
        });
        this.label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        this.text = text;
    }

    public void setEnabled(boolean enable)
    {
        super.setEnabled(enable);
        if (enable)
        {
            label.setText("<html><font color = \"blue\"><u>" + text
                + "</u></font>" + ": " + "</html>");
        }
        else
        {
            label.setText("<html><font color = \"#"
                + HackModule.addZeros(
                    Integer.toHexString(UIManager.getColor(
                        "Label.disabledForeground").getRGB()), 8).substring(2)
                + "\"><u>" + text + "</u>" + ":</font> </html>");
        }
    }
}