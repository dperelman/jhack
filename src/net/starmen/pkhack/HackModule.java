package net.starmen.pkhack;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.filechooser.FileFilter;

/**
 * Abstract class for a hacking module. All hacking modules must extend this
 * class. <br>
 * IMPORTANT: The class name of your module must be in
 * net/starmen/pkhack/modulelist.txt or it will not be loaded.
 * 
 * @author AnyoneEB
 */
//Made by AnyoneEB.
//Code released under the GPL - http://www.gnu.org/licenses/gpl.txt
public abstract class HackModule
{
    /**
     * Common instance of AbstractRom
     * 
     * @see net.starmen.pkhack.AbstractRom
     */
    public AbstractRom rom;
    /**
     * Preferences.
     */
    public XMLPreferences prefs;

    /**
     * If true createJComboBoxFromArray() and getNumberedString() default to hex
     * numbers. If false, they use decimal. Defaults to true.
     * 
     * @see #getNumberedString(String, int)
     * @see #createJComboBoxFromArray(Object[])
     * @see #createJComboBoxFromArray(Object[], JComboBox)
     */
    public static boolean getUseHexNumbers()
    {
        return JHack.main.getPrefs().getValueAsBoolean("useHexNumbers");
    }

    /**
     * If true createJComboBoxFromArray() and getNumberedString() default to hex
     * numbers. If false, they use decimal. Defaults to true.
     * 
     * @see #getNumberedString(String, int)
     * @see #createJComboBoxFromArray(Object[])
     * @see #createJComboBoxFromArray(Object[], JComboBox)
     * @param useHexNumbers If true, functions output hex strings.
     */
    public static void setUseHexNumbers(boolean useHexNumbers)
    {
        JHack.main.getPrefs().setValueAsBoolean("useHexNumbers", useHexNumbers);
    }

    /**
     * If true init() has been called.
     * 
     * @see #init()
     */
    public boolean isInited = false;

    /**
     * Main window of the module. Modules may or may not use this. If they do
     * not they may need to override {@link #updateUI()}.
     */
    protected JFrame mainWindow;

    /**
     * Called the first time this HackModule is <code>show()</code> 'd.
     * 
     * @see #show()
     */
    protected abstract void init();

    /**
     * Returns the version of this HackModule as a <code>String</code>. Can
     * have any number of numbers and dots ex. "0.3.3.5".
     * 
     * @return Version of this HackModule.
     */
    public abstract String getVersion();

    /**
     * Returns a short, human readable description of this HackModule.
     * 
     * @return A short (one-line) description of this HackModule
     */
    public abstract String getDescription();

    /**
     * Returns the credits for this HackModule. Will probably contain "Written
     * by..." and the discoverer of the information this HackModule uses.
     * 
     * @return Credits for this HackModule. Can be multiple lines.
     */
    public abstract String getCredits();

    /**
     * Shows the main window of the module. Calls <code>init()</code> the
     * first time it is called.
     */
    public void show()
    {
        if (!isInited)
        {
            isInited = true;
            init();
        }
    }

    /**
     * Shows the main window of the module and then responds to input. The
     * response to the input depends on the extending class. For example, an
     * input type of Integer could make the class go directly to a specific
     * entry number. By default there is no response to the input.
     * 
     * @param in Input, type/format defined by extending class.
     * @throws IllegalArgumentException If incorrect type of input.
     */
    public void show(Object in) throws IllegalArgumentException
    {
        show();
    }

    /**
     * Called when a new rom is loaded. It should reload any information from
     * the ROM that isn't reloaded every time the module <code>show()</code>
     * 'd
     */
    public void reset()
    {}

    /**
     * Any module with a GUI should implement this to call
     * <code>updateUI()</code> on all of its components.
     * 
     * @see javax.swing.SwingUtilities#updateComponentTreeUI(java.awt.Component)
     */
    public void updateUI()
    {
        if (mainWindow != null)
        {
            SwingUtilities.updateComponentTreeUI(SwingUtilities
                .getRoot(mainWindow));
            mainWindow.pack();
        }
    }

    /**
     * Hides all GUI components of the module. Might want to ask the user if
     * they want to save changes.
     */
    public abstract void hide();

    /**
     * Override this for an icon on the module's buttons.
     * 
     * @return Icon to be used to represent this <code>HackModule</code>
     */
    public Icon getIcon()
    {
        return null;
    }

    /**
     * Constructor that takes a AbstractRom and preferences object.
     * 
     * @param rom ROM object for I/O
     * @param prefs prefrences access
     */
    public HackModule(AbstractRom rom, XMLPreferences prefs)
    {
        this.rom = rom;
        this.prefs = prefs;
    }

    protected HackModule()
    {}

    /**
     * Returns true if this instance of <code>HackModule</code> supports the
     * currently loaded ROM,
     * 
     * @return if this supports the currently loaded ROM
     */
    public abstract boolean isRomSupported();

    //common GUI functions
    /**
     * Wraps a component in a <code>JPanel</code> with a <code>JLabel</code>
     * and that component. This is the same as calling
     * <code>pairComponents(new JLabel(label), comp, true)</code>, except
     * this sets {@link JLabel#setLabelFor(java.awt.Component)}to
     * <code>comp</code>
     * 
     * @param label <code>String</code> to label component with
     * @param comp Component to label
     * @return A <code>JPanel</code> containing a <code>JLabel</code> and
     *         the component.
     * @see #pairComponents(JComponent, JComponent, boolean)
     */
    public static JPanel getLabeledComponent(String label, JComponent comp) //useful
    // for
    // making
    // layouts
    {
        return getLabeledComponent(label, comp, null);
    }

    /**
     * Wraps a component in a <code>JPanel</code> with a <code>JLabel</code>
     * and that component both with a tooltip. This is the same as calling
     * <code>pairComponents(new JLabel(label), comp, true, tooltip, tooltip)</code>,
     * except this sets {@link JLabel#setLabelFor(java.awt.Component)}to
     * <code>comp</code>
     * 
     * @param label <code>String</code> to label component with
     * @param comp Component to label
     * @param tooltip Tooltip to use. Set for both the label and the component.
     * @return A <code>JPanel</code> containing a <code>JLabel</code> and
     *         the component both with a tooltip.
     * @see #pairComponents(JComponent, JComponent, boolean, String)
     */
    public static JPanel getLabeledComponent(String label, JComponent comp,
        String tooltip) //useful for making layouts
    {
        /*
         * return pairComponents(new JLabel(), pairComponents(new JLabel(label),
         * comp, true, tooltip), false);
         */
        JLabel l = new JLabel(label);
        l.setLabelFor(comp);
        return pairComponents(l, comp, true, tooltip);
    }

    /**
     * Wraps two components into a single <code>JPanel</code>. The
     * <code>JPanel</code> uses a <code>BorderLayout</code>.
     * 
     * @param comp1 Component to put in west or north.
     * @param comp2 Component to put in east, south, or center
     * @param isHorizontal If true west and east are used, if false north and
     *            south are used.
     * @param stretch If true center is used for <code>comp2</code> instead of
     *            east or south.
     * @param tooltip1 Tooltip to set for <code>comp1</code>. If
     *            <code>null</code> no tooltip is set.
     * @param tooltip2 Tooltip to set for <code>comp2</code>. If
     *            <code>null</code> no tooltip is set.
     * @return A <code>JPanel</code> containing the two components using a
     *         <code>BorderLayout</code>
     */
    public static JPanel pairComponents(JComponent comp1, JComponent comp2,
        boolean isHorizontal, boolean stretch, String tooltip1, String tooltip2)
    {
        JPanel out = new JPanel();
        out.setLayout(new BorderLayout());
        if (comp1 == null)
            comp1 = new JLabel();
        if (comp2 == null)
            comp2 = new JLabel();
        comp1.setMaximumSize(comp1.getPreferredSize());
        if (tooltip1 != null)
        {
            comp1.setToolTipText(tooltip1);
        }
        out.add(comp1, (isHorizontal ? BorderLayout.WEST : BorderLayout.NORTH));
        comp2.setMaximumSize(comp2.getPreferredSize());
        if (tooltip2 != null)
        {
            comp2.setToolTipText(tooltip2);
        }
        out.add(comp2, (stretch ? BorderLayout.CENTER : (isHorizontal
            ? BorderLayout.EAST
            : BorderLayout.SOUTH)));
        return out;
    }

    /**
     * Wraps two components into a single <code>JPanel</code>. The
     * <code>JPanel</code> uses a <code>BorderLayout</code>.
     * 
     * @param comp1 Component to put in west or north.
     * @param comp2 Component to put in east, south, or center
     * @param isHorizontal If true west and east are used, if false north and
     *            south are used.
     * @param stretch If true center is used for <code>comp2</code> instead of
     *            east or south.
     * @return A <code>JPanel</code> containing the two components using a
     *         <code>BorderLayout</code>
     */
    public static JPanel pairComponents(JComponent comp1, JComponent comp2,
        boolean isHorizontal, boolean stretch)
    {
        return pairComponents(comp1, comp2, isHorizontal, stretch, null, null);
    }

    /**
     * Wraps two components into a single <code>JPanel</code>. The
     * <code>JPanel</code> uses a <code>BorderLayout</code>.
     * 
     * @param comp1 Component to put in west or north.
     * @param comp2 Component to put in east, south, or center
     * @param isHorizontal If true west and east are used, if false north and
     *            south are used.
     * @param tooltip Tooltip to set for <code>comp1</code> and
     *            <code>comp2</code>. If <code>null</code> no tooltip is
     *            set.
     * @return A <code>JPanel</code> containing the two components using a
     *         <code>BorderLayout</code>
     * @see #pairComponents(JComponent, JComponent, boolean, boolean, String,
     *      String)
     */
    public static JPanel pairComponents(JComponent comp1, JComponent comp2,
        boolean isHorizontal, String tooltip)
    {
        return pairComponents(comp1, comp2, isHorizontal, false, tooltip,
            tooltip);
    }

    /**
     * Wraps two components into a single <code>JPanel</code>. The
     * <code>JPanel</code> uses a <code>BorderLayout</code>.
     * 
     * @param comp1 Component to put in west or north.
     * @param comp2 Component to put in east, south, or center
     * @param isHorizontal If true west and east are used, if false north and
     *            south are used.
     * @return A <code>JPanel</code> containing the two components using a
     *         <code>BorderLayout</code>
     * @see #pairComponents(JComponent, JComponent, boolean, boolean, String,
     *      String)
     */
    public static JPanel pairComponents(JComponent comp1, JComponent comp2,
        boolean isHorizontal)
    {
        return pairComponents(comp1, comp2, isHorizontal, false, null, null);
    }

    /**
     * Returns the input <code>String</code> with the number in []'s before
     * it. If <code>useHexNumbers</code> is true then the number is in hex,
     * otherwise it's in decimal. If the hex number comes out to a single digit,
     * a zero will be added before it. This was written to be used for making
     * lists for <code>JComboBox</code> 's.
     * 
     * @param in <code>String</code> to add number to
     * @param num Number to add to String
     * @param useHexNumbers If true a hex number is put in, otherwise decimal is
     *            used.
     * @return A new <code>String</code> in the form "["+num+"] "+in
     * @see #getNumberedString(String, int)
     * @see #createJComboBoxFromArray(Object[])
     */
    public static String getNumberedString(String in, int num,
        boolean useHexNumbers)
    {
        return "["
            + (useHexNumbers
                ? (Integer.toHexString(num).length() == 1 ? "0"
                    + Integer.toHexString(num) : Integer.toHexString(num))
                : Integer.toString(num)) + "] " + in.trim();
    }

    /**
     * Returns the input <code>String</code> with the number in []'s before
     * it. If useHexNumbers pref is true then the number is in hex, otherwise
     * it's in decimal. If the hex number comes out to a single digit, a zero
     * will be added before it. This was written to be used for making lists for
     * <code>JComboBox</code> 's.
     * 
     * @param in String to add number to
     * @param num Number to add to String
     * @return A new <code>String</code> in the form "["+num+"] "+in
     * @see #getNumberedString(String, int, boolean)
     * @see #createJComboBoxFromArray(Object[])
     */
    public static String getNumberedString(String in, int num)
    {
        return getNumberedString(in, num, getUseHexNumbers());
    }

    /**
     * Returns the number added to the given String by
     * {@link #getNumberedString(String, int, boolean)}.
     * 
     * @param in String created by
     *            {@link #getNumberedString(String, int, boolean)}.
     * @param useHexNumbers If true number is read as hex, if false decimal.
     * @return The number in a numbered string. Negtive if fails.
     */
    public static int getNumberOfString(String in, boolean useHexNumbers)
    {
        try
        {
            return Integer.parseInt(new StringTokenizer(in, "[]", false)
                .nextToken(), useHexNumbers ? 16 : 10);
        }
        catch (NumberFormatException e)
        {
            return -1;
        }
        catch (NullPointerException e)
        {
            return -1;
        }
    }

    /**
     * Returns the number added to the given String by
     * {@link #getNumberedString(String, int)}.
     * 
     * @param in String created by {@link #getNumberedString(String, int)}.
     * @return The number in a numbered string.
     */
    public static int getNumberOfString(String in)
    {
        return getNumberOfString(in, getUseHexNumbers());
    }

    /**
     * Creates a new <code>JFrame</code> with "Apply Changes" and "Close"
     * buttons. The <code>JFrame</code>'s content pane has a
     * <code>BorderLayout</code> with the south used. In the south there are
     * two buttons: an "Apply Changes" button with the action command "apply"
     * and a "Close" button with the action command "close". Both
     * <code>addActionListener(al)</code>.
     * 
     * @param al <code>ActionListener</code> the buttons register.
     * @return A <code>JFrame</code> with "Apply Changes" and "Close" buttons.
     */
    public static JFrame createBaseWindow(ActionListener al)
    {
        JFrame out = new JFrame();
        out.setLocationRelativeTo(JHack.main.getMainWindow());
        out.getContentPane().setLayout(new BorderLayout());

        JPanel buttons = new JPanel();
        buttons.setLayout(new FlowLayout());

        JButton apply = new JButton("Apply Changes");
        apply.setActionCommand("apply");
        apply.addActionListener(al);
        buttons.add(apply);
        JButton close = new JButton("Close");
        close.setActionCommand("close");
        close.addActionListener(al);
        buttons.add(close);

        out.getContentPane().add(buttons, BorderLayout.SOUTH);

        return out;
    }

    /**
     * Creates a <code>JTextField</code> with a maximum length. This
     * <code>JTextField</code> uses a
     * {@link net.starmen.pkhack.MaxLengthDocument}or a
     * {@link net.starmen.pkhack.NumericMaxLengthDocument}.
     * 
     * @param len Maximum length for the <code>JTextField</code>
     * @param numbersOnly if true, a <code>NumericMaxLengthDocument</code> is
     *            used to allow only numbers to be typed
     * @param hex True for hex numbers, false for decimal
     * @return A new <code>JTextField</code>, which limits the number of
     *         input characters to <code>len</code>
     * @see net.starmen.pkhack.MaxLengthDocument
     * @see NumericMaxLengthDocument
     */
    public static JTextField createSizedJTextField(int len,
        boolean numbersOnly, boolean hex)
    {
        return new JTextField(numbersOnly ? new NumericMaxLengthDocument(len,
            hex ? "[^0-9a-fA-F]" : null) : new MaxLengthDocument(len), "", len);
    }

    /**
     * Creates a <code>JTextField</code> with a maximum length. This
     * <code>JTextField</code> uses a
     * {@link net.starmen.pkhack.MaxLengthDocument}or a
     * {@link net.starmen.pkhack.NumericMaxLengthDocument}.
     * 
     * @param len Maximum length for the <code>JTextField</code>
     * @param numbersOnly if true, a <code>NumericMaxLengthDocument</code> is
     *            used to allow only numbers to be typed
     * @return A new <code>JTextField</code>, which limits the number of
     *         input characters to <code>len</code>
     * @see net.starmen.pkhack.MaxLengthDocument
     * @see NumericMaxLengthDocument
     */
    public static JTextField createSizedJTextField(int len, boolean numbersOnly)
    {
        return createSizedJTextField(len, numbersOnly, false);
    }

    /**
     * Creates a <code>JTextField</code> with a maximum length. This
     * <code>JTextField</code> uses a
     * {@link net.starmen.pkhack.MaxLengthDocument}.
     * 
     * @param len Maximum length for the <code>JTextField</code>
     * @return A new <code>JTextField</code>, which limits the number of
     *         input characters to <code>len</code>
     * @see net.starmen.pkhack.MaxLengthDocument
     */
    public static JTextField createSizedJTextField(int len)
    {
        return createSizedJTextField(len, false);
    }

    protected static class NumberedComboBoxModel extends SimpleComboBoxModel
    {
        private Object obj[];

        public NumberedComboBoxModel(Object[] in)
        {
            obj = in;
        }

        public Object getElementAt(int i)
        {
            try
            {
                if (obj[i].toString().startsWith("["))
                {
                    return obj[i];
                }
                else
                {
                    return HackModule.getNumberedString(obj[i].toString(), i);
                }
            }
            catch (NullPointerException e)
            {
                return HackModule.getNumberedString("", i);
            }
            catch (ArrayIndexOutOfBoundsException e)
            {
                return getElementAt(0);
            }
        }

        public int getSize()
        {
            return obj.length;
        }
    }

    /**
     * Sets up a <code>JComboBox</code> from an <code>Object[]</code>.
     * Removes all items from the <code>JComboBox</code> first. Calls
     * {@link #getNumberedString(String, int, boolean)}before adding each item
     * unless the item starts with "[". The boolean value is the
     * <code>useHexNumbers<code>
     * parameter passed to this method.
     * 
     * @param in <code>Object[]</code> to add to the <code>JComboBox</code>
     * @param out The <code>JComboBox</code> to add the items to.
     * @param useHexNumbers If true hex is used for the numbers, else decimal is used.
     * @return <code>out<code> with all the items from <code>in</code> added to it.
     * @see #getNumberedString(String, int, boolean)
     */
    public static JComboBox createJComboBoxFromArray(Object[] in,
        JComboBox out, boolean useHexNumbers)
    {
        out.removeAllItems();
        for (int i = 0; i < in.length; i++)
        {
            if (in[i] == null)
            {
                out.addItem(getNumberedString("", i, useHexNumbers));
            }
            else if (in[i].toString().startsWith("["))
            {
                out.addItem(in[i]);
            }
            else
            {
                out.addItem(getNumberedString(in[i].toString(), i,
                    useHexNumbers));
            }
        }
        return out;
    }

    /**
     * Sets up a <code>JComboBox</code> from an <code>Object[]</code>.
     * Removes all items from the <code>JComboBox</code> first. Calls
     * {@link #getNumberedString(String, int, boolean)}before adding each item
     * unless the item starts with "[". The boolean value is the useHexNumbers
     * pref.
     * 
     * @param in <code>Object[]</code> to add to the <code>JComboBox</code>
     * @param out The <code>JComboBox</code> to add the items to.
     * @return <code>out<code> with all the items from <code>in</code> added to it.
     * @see #getNumberedString(String, int, boolean)
     * @see #createJComboBoxFromArray(Object[], JComboBox, boolean)
     */
    public static JComboBox createJComboBoxFromArray(Object[] in, JComboBox out)
    {
        //return createJComboBoxFromArray(in, out, getUseHexNumbers());
        out.setModel(new NumberedComboBoxModel(in));
        return out;
    }

    /**
     * Creates and sets up a <code>JComboBox</code> from an
     * <code>Object[]</code>. Calls
     * {@link #getNumberedString(String, int, boolean)}before adding each item
     * unless the item starts with "[". The boolean value is the
     * <code>useHexNumbers<code>
     * parameter passed to this method.
     * 
     * @param in <code>Object[]</code> to add to the <code>JComboBox</code>
     * @param useHexNumbers If true hex is used for the numbers, else decimal is used.
     * @return A new <code>JComboBox</code> with all the items from <code>in</code> added to it.
     * @see #getNumberedString(String, int, boolean)
     * @see #createJComboBoxFromArray(Object[], JComboBox, boolean)
     */
    public static JComboBox createJComboBoxFromArray(Object[] in,
        boolean useHexNumbers)
    {
        JComboBox out = new JComboBox();
        return createJComboBoxFromArray(in, out, useHexNumbers);
    }

    /**
     * Creates and sets up a <code>JComboBox</code> from an
     * <code>Object[]</code>. Calls
     * {@link #getNumberedString(String, int, boolean)}before adding each item
     * unless the item starts with "[". The boolean value is the useHexNumbers
     * pref.
     * 
     * @param in <code>Object[]</code> to add to the <code>JComboBox</code>
     * @return A new <code>JComboBox</code> with all the items from
     *         <code>in</code> added to it.
     * @see #getNumberedString(String, int, boolean)
     * @see #createJComboBoxFromArray(Object[], JComboBox, boolean)
     */
    public static JComboBox createJComboBoxFromArray(Object[] in)
    {
        //return createJComboBoxFromArray(in, getUseHexNumbers());
        return new JComboBox(new NumberedComboBoxModel(in));
    }

    /**
     * Searches for a <code>String</code> in a <code>JComboBox</code>. If
     * the <code>String</code> is found inside an item in the
     * <code>JComboBox</code> then the item is selected on the JComboBox and
     * true is returned. The search starts at the item after the currently
     * selected item or the first item if the last item is currently selected.
     * If the search <code>String</code> is not found, then an error dialog
     * pops-up and the function returns false.
     * 
     * @param text <code>String</code> to seach for. Can be anywhere inside
     *            any item.
     * @param selector <code>JComboBox</code> to get list from, and set if
     *            search is found.
     * @param beginFromStart If true, search starts at first item.
     * @param displayError If false, the error dialog will never be displayed
     * @return Wether the search <code>String</code> is found.
     */
    public static boolean search(String text, JComboBox selector,
        boolean beginFromStart, boolean displayError)
    {
        if (text == null || selector == null || selector.getItemCount() == 0)
            return false;
        text = text.toLowerCase();
        for (int i = (selector.getSelectedIndex() + 1 != selector
            .getItemCount()
            && !beginFromStart ? selector.getSelectedIndex() + 1 : 0); i < selector
            .getItemCount(); i++)
        {
            if (selector.getItemAt(i).toString().toLowerCase().indexOf(text) != -1)
            {
                if (selector.getSelectedIndex() == -1 && selector.isEditable())
                {
                    selector.setEditable(false);
                    selector.setSelectedIndex(i == 0 ? 1 : 0);
                    selector.setSelectedIndex(i);
                    selector.setEditable(true);
                }
                selector.setSelectedIndex(i);
                selector.repaint();
                return true;
            }
        }

        if (beginFromStart)
        {
            if (displayError)
                JOptionPane.showMessageDialog(null,
                    "Sorry, your search was not found.", "Not found!",
                    JOptionPane.ERROR_MESSAGE);
            else
                System.out.println("Search not found.");
            return false;
        }
        else
        {
            return search(text, selector, true, displayError);
        }
    }

    /**
     * Searches for a <code>String</code> in a <code>JComboBox</code>. If
     * the <code>String</code> is found inside an item in the
     * <code>JComboBox</code> then the item is selected on the JComboBox and
     * true is returned. The search starts at the item after the currently
     * selected item or the first item if the last item is currently selected.
     * If the search <code>String</code> is not found, then an error dialog
     * pops-up and the function returns false.
     * 
     * @param text <code>String</code> to seach for. Can be anywhere inside
     *            any item.
     * @param selector <code>JComboBox</code> to get list from, and set if
     *            search is found.
     * @param beginFromStart If true, search starts at first item.
     * @return Wether the search <code>String</code> is found.
     */
    public static boolean search(String text, JComboBox selector,
        boolean beginFromStart)
    {
        return search(text, selector, beginFromStart, true);
    }

    /**
     * Searches for a <code>String</code> in a <code>JComboBox</code>. If
     * the <code>String</code> is found inside an item in the
     * <code>JComboBox</code> then the item is selected on the JComboBox and
     * true is returned. The search starts at the item after the currently
     * selected item or the first item if the last item is currently selected.
     * If the search <code>String</code> is not found, then an error dialog
     * pops-up and the function returns false.
     * 
     * @param text <code>String</code> to seach for. Can be anywhere inside
     *            any item.
     * @param selector <code>JComboBox</code> to get list from, and set if
     *            search is found.
     * @return Wether the search <code>String</code> is found.
     */
    public static boolean search(String text, JComboBox selector)
    {
        return search(text, selector, false);
    }

    /**
     * Returns a new JPanel with the specified components added to it using a
     * FlowLayout.
     * 
     * @see #createFlowLayout(Component)
     * @param comps JComponent[]
     * @return A new JPanel with the specified components added to it.
     */
    public static JComponent createFlowLayout(Component[] comps)
    {
        JComponent out = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        for (int i = 0; i < comps.length; i++)
        {
            out.add(comps[i]);
        }
        return out;
    }

    /**
     * Returns a new JPanel with the specified component added to it using a
     * FlowLayout.
     * 
     * @see #createFlowLayout(Component[])
     * @param comp JComponent
     * @return A new JPanel with the specified component added to it.
     */
    public static JComponent createFlowLayout(Component comp)
    {
        return createFlowLayout(new Component[]{comp});
    }

    /**
     * Creates a <code>JMenuItem</code>. Useful for creating menus.
     * 
     * @param name Text diplayed on the menu item
     * @param m Character to press to get to this menu item when you have its
     *            menu selected
     * @param key String representation of key sequence to press to use this
     *            menu item from anywhere on the same window as its menu
     * @param ac action command
     * @param al <code>ActionListener</code> the menu item reports to.
     * @return a new <code>JMenuItem</code>
     */
    public static JMenuItem createJMenuItem(String name, char m, String key,
        String ac, ActionListener al)
    {
        JMenuItem j = new JMenuItem(name);
        j.setMnemonic(m);
        if (key != null)
            j.setAccelerator(KeyStroke.getKeyStroke(key));
        j.setActionCommand(ac);
        j.addActionListener(al);
        return j;
    }

    /**
     * Creates a standard edit menu. Menu includes undo if requested and cut,
     * copy. paste, and delete. Action commands are "undo" "cut" "copy" "paste"
     * "delete".
     * 
     * @param al <code>ActionListener</code> to send <code>ActionEvents</code>
     *            to
     * @param includeUndo if true an undo menu item is included, if false it is
     *            not included
     * @return a edit menu
     */
    public static JMenu createEditMenu(ActionListener al, boolean includeUndo)
    {
        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic('e');

        if (includeUndo)
        {
            editMenu.add(HackModule.createJMenuItem("Undo", 'u', "ctrl Z",
                "undo", al));
            editMenu.addSeparator();
        }
        editMenu.add(HackModule
            .createJMenuItem("Cut", 't', "ctrl X", "cut", al));
        editMenu.add(HackModule.createJMenuItem("Copy", 'c', "ctrl C", "copy",
            al));
        editMenu.add(HackModule.createJMenuItem("Paste", 'p', "ctrl V",
            "paste", al));
        editMenu.add(HackModule.createJMenuItem("Delete", 'd', "DELETE",
            "delete", al));

        return editMenu;
    }

    private static class SimpleFileFilter extends FileFilter
    {
        private boolean save;
        private String ext, desc;

        public SimpleFileFilter(boolean save, String ext, String desc)
        {
            this.save = save;
            this.ext = ext;
            this.desc = desc;
        }

        public boolean accept(File f)
        {
            if ((f.getAbsolutePath().toLowerCase().endsWith("." + ext) || f
                .isDirectory())
                && (f.exists() || save))
            {
                return true;
            }
            return false;
        }

        public String getDescription()
        {
            return desc + " (*." + ext + ")";
        }

        public String getDesc()
        {
            return desc;
        }

        public String getExt()
        {
            return ext;
        }
    }

    /**
     * Asks the user to select a file. The extension is forced for saving.
     * 
     * @param save if true, shows a save dialog, if false shows an open dialog
     * @param ext extension of file without the dot before it
     * @param desc description of file type
     * @return the user-selected <code>File</code> or null if canceled.
     * @see #openFile(String[], String[])
     */
    public static File getFile(final boolean save, final String ext,
        final String desc)
    {
        JFileChooser jfc = new JFileChooser(AbstractRom.getDefaultDir());
        jfc.setFileFilter(new SimpleFileFilter(save, ext, desc));
        if ((save ? jfc.showSaveDialog(null) : jfc.showOpenDialog(null)) == JFileChooser.APPROVE_OPTION)
        {
            File out = jfc.getSelectedFile();
            //extension is forced on save.
            if (!out.getPath().endsWith("." + ext) && save)
                out = new File(out.getPath() + "." + ext);
            AbstractRom.setDefaultDir(out.getParent());
            return out;
        }
        else
        {
            return null;
        }
    }

    /**
     * Asks the user to select a file of one of multiple types. The extension
     * selected by the user is forced for saving.
     * 
     * @param save if true, shows a save dialog, if false shows an open dialog
     * @param ext lower case extension of file without the dot before it
     * @param desc description of file type
     * @return the user-selected <code>File</code> or null if canceled.
     * @see #getFile(boolean, String, String)
     */
    public static File getFile(final boolean save, final String[] ext,
        final String[] desc)
    {
        if (ext.length != desc.length)
        {
            System.err.println("Invalid call to getFile("
                + "boolean,String[],String[]): "
                + "String[] lengths must be equal.");
            return null;
        }
        JFileChooser jfc;
        try
        {
            jfc = new JFileChooser(AbstractRom.getDefaultDir());
        }
        catch (RuntimeException e)
        {
            e.printStackTrace(System.out);
            return null;
        }
        for (int i = 0; i < ext.length; i++)
            jfc.addChoosableFileFilter(new SimpleFileFilter(save, ext[i],
                desc[i]));
        if ((save ? jfc.showSaveDialog(null) : jfc.showOpenDialog(null)) == JFileChooser.APPROVE_OPTION)
        {
            File out = jfc.getSelectedFile();
            //extension is forced on save.
            String exts = ((SimpleFileFilter) jfc.getFileFilter()).getExt();
            if (!out.getPath().endsWith("." + exts) && save)
                out = new File(out.getPath() + "." + exts);
            AbstractRom.setDefaultDir(out.getParent());
            return out;
        }
        else
        {
            return null;
        }
    }

    //common other functions
    /**
     * Simple conversion from a regular <code>char</code> to an EarthBound
     * <code>char</code>. Adds 0x30 to the character value. Simple because it
     * doesn't handle control codes or compression. Works fine for the scattered
     * strings not accessable by the text editor.
     * 
     * @param regChr A regular <code>char</code>
     * @return An EarthBound <code>char</code>
     * @see #simpToGameString(char[])
     * @see #simpToRegChar(char)
     * @see #simpToRegString(char[])
     */
    public abstract char simpToGameChar(char regChr);

    /**
     * Simple conversion from a regular <code>char[]</code> to an EarthBound
     * <code>char[]</code>. Adds 0x30 to the character value. Simple because
     * it doesn't handle control codes or compression. Works fine for the
     * scattered strings not accessable by the text editor. Calls
     * {@link #simpToGameChar(char)}on every <code>char</code> in the array.
     * 
     * @param string An array of regular <code>char</code>'s
     * @return An array of EarthBound <code>char</code>'s
     * @see #simpToGameChar(char)
     * @see #simpToRegChar(char)
     * @see #simpToRegString(char[])
     */
    public abstract char[] simpToGameString(char[] string);

    /**
     * Simple conversion from an EarthBound <code>char</code> to a regular
     * <code>char</code>. Subracts 0x30 from the character value. Simple
     * because it doesn't handle control codes or compression. Works fine for
     * the scattered strings not accessable by the text editor.
     * 
     * @param gameChr An EarthBound <code>char</code>
     * @return A regular <code>char</code>
     * @see #simpToGameChar(char)
     * @see #simpToGameString(char[])
     * @see #simpToRegString(char[])
     */
    public abstract char simpToRegChar(char gameChr);

    /**
     * Simple conversion from an EarthBound <code>char</code> to a regular
     * <code>char</code>. Subracts 0x30 from the character value. Simple
     * because it doesn't handle control codes or compression. Works fine for
     * the scattered strings not accessable by the text editor. Calls
     * {@link #simpToRegChar(char)}on every <code>char</code> in the array.
     * 
     * @param string An array of EarthBound <code>char</code>'s
     * @return An arry of regular <code>char</code>'s
     * @see #simpToGameChar(char)
     * @see #simpToGameString(char[])
     * @see #simpToRegString(char[])
     */
    public abstract char[] simpToRegString(char[] string);

    /**
     * Reads a regular string from the ROM.
     * 
     * @param offset Where to read from.
     * @param len Number of bytes to read.
     * @return a regular string
     * @see #simpToRegChar(char)
     * @see AbstractRom#readChar(int, int)
     * @see AbstractRom#read(int)
     */
    public String readRegString(int offset, int len)
    {
        char[] c = rom.readChar(offset, len);
        for (int i = 0; i < len; i++)
        {
            c[i] = simpToRegChar(c[i]);
            if (c[i] == 0)
                return new String(c, 0, i);
        }
        return new String(c);
    }

    /**
     * Reads a regular null-terminated string from the ROM.
     * 
     * @param offset Where to read from.
     * @return a regular string
     * @see #simpToRegChar(char)
     * @see AbstractRom#readChar(int, int)
     * @see AbstractRom#read(int)
     */
    public String readRegString(int offset)
    {
        char c;
        String str = new String();
        while ((c = rom.readChar(offset++)) != 0)
            str += simpToRegChar(c);
        return str;
    }

    /**
     * Reads a regular string from the ROM at <code>seekOffset</code>.
     * 
     * @param len Number of bytes to read.
     * @return a regular string
     * @see #simpToRegChar(char)
     * @see AbstractRom#seek(int)
     * @see AbstractRom#readCharSeek(int)
     * @see AbstractRom#readSeek()
     */
    public String readSeekRegString(int len)
    {
        char[] c = rom.readCharSeek(len);
        for (int i = 0; i < len; i++)
        {
            c[i] = simpToRegChar(c[i]);
            if (c[i] == 0)
                return new String(c, 0, i);
        }
        return new String(c);
    }

    /**
     * Write a regular string to the ROM.
     * 
     * @param offset Where in the ROM to write the value. Counting starts at
     *            zero, as normal.
     * @param len number of bytes (characters) to write
     * @param str regular striing to write at <code>offset</code>.
     * @see #simpToGameChar(char)
     * @see AbstractRom#write(int, char[])
     * @see AbstractRom#writeSeek(int)
     * @see #readRegString(int, int)
     */
    public void writeRegString(int offset, int len, String str)
    {
        char[] c = new char[len];
        for (int i = 0; i < len; i++)
        {
            c[i] = (i < str.length() ? simpToGameChar(str.charAt(i)) : 0);
        }
        rom.write(offset, c);
    }

    /**
     * Write a regular string to the ROM at <code>seekOffset</code>.
     * 
     * @param len number of bytes (characters) to write
     * @param str regular string to write at <code>seekOffset</code>.
     * @see #simpToGameChar(char)
     * @see AbstractRom#seek(int)
     * @see AbstractRom#writeSeek(char[])
     * @see AbstractRom#writeSeek(int)
     * @see #readSeekRegString(int)
     */
    public void writeSeekRegString(int len, String str)
    {
        char[] c = new char[len];
        for (int i = 0; i < len; i++)
        {
            c[i] = (i < str.length() ? simpToGameChar(str.charAt(i)) : 0);
        }
        rom.writeSeek(c);
    }

    /**
     * Draws the image in the specified <code>int[][]</code> with the
     * specified palette. Assumes that pixel (x, y) of the image is the color
     * <code>palette(image[x][y])</code>.
     * 
     * @param image <code>int[][]</code> of color numbers in
     *            <code>int[x][y] form
     * @param palette <code>Color[]</code> for converting color numbers to colors
     * @return An <code>Image</code> drawn in the specified palette.
     */
    public static BufferedImage drawImage(int[][] image, Color[] palette)
    {
        //        BufferedImage out = new BufferedImage(image.length, image[0].length,
        //            BufferedImage.TYPE_INT_ARGB);
        //        Graphics g = out.getGraphics();
        //        for (int x = 0; x < image.length; x++)
        //        {
        //            for (int y = 0; y < image[0].length; y++)
        //            {
        //                g.setColor(palette[image[x][y]]);
        //                g.drawLine(x, y, x, y);
        //            }
        //        }
        //        return out;
        return drawImage(image, palette, false, false);
    }

    /**
     * Draws the image in the specified <code>int[][]</code> with the
     * specified palette. Assumes that pixel (x, y) of the image is the color
     * <code>palette(image[x][y])</code>.
     * 
     * @param image <code>int[][]</code> of color numbers in
     *            <code>int[x][y] form
     * @param palette <code>Color[]</code> for converting color numbers to colors
     * @param hFlip if true, output will be horizontally flipped
     * @param vFlip if true, output will be vertically flipped
     * @return An <code>Image</code> drawn in the specified palette.
     */
    public static BufferedImage drawImage(int[][] image, Color[] palette,
        boolean hFlip, boolean vFlip)
    {
        BufferedImage out = new BufferedImage(image.length, image[0].length,
            BufferedImage.TYPE_INT_ARGB);
        Graphics g = out.getGraphics();
        for (int x = 0; x < image.length; x++)
        {
            for (int y = 0; y < image[0].length; y++)
            {
                g
                    .setColor(palette[image[hFlip ? image.length - x - 1 : x][vFlip
                        ? image[0].length - y - 1
                        : y]]);
                g.drawLine(x, y, x, y);
            }
        }
        return out;
    }

    /**
     * Draws the image in the specified <code>byte[][]</code> with the
     * specified palette. Assumes that pixel (x, y) of the image is the color
     * <code>palette(image[x][y])</code>.
     * 
     * @param image <code>byte[][]</code> of color numbers in
     *            <code>int[x][y] form
     * @param palette <code>Color[]</code> for converting color numbers to colors
     * @return An <code>Image</code> drawn in the specified palette.
     */
    public static BufferedImage drawImage(byte[][] image, Color[] palette)
    {
        //        BufferedImage out = new BufferedImage(image.length, image[0].length,
        //            BufferedImage.TYPE_INT_ARGB);
        //        Graphics g = out.getGraphics();
        //        for (int x = 0; x < image.length; x++)
        //        {
        //            for (int y = 0; y < image[0].length; y++)
        //            {
        //                g.setColor(palette[image[x][y]]);
        //                g.drawLine(x, y, x, y);
        //            }
        //        }
        //        return out;
        return drawImage(image, palette, false, false);
    }

    /**
     * Draws the image in the specified <code>byte[][]</code> with the
     * specified palette. Assumes that pixel (x, y) of the image is the color
     * <code>palette(image[x][y])</code>.
     * 
     * @param image <code>byte[][]</code> of color numbers in
     *            <code>int[x][y] form
     * @param palette <code>Color[]</code> for converting color numbers to colors
     * @param hFlip if true, output will be horizontally flipped
     * @param vFlip if true, output will be vertically flipped
     * @return An <code>Image</code> drawn in the specified palette.
     */
    public static BufferedImage drawImage(byte[][] image, Color[] palette,
        boolean hFlip, boolean vFlip)
    {
        BufferedImage out = new BufferedImage(image.length, image[0].length,
            BufferedImage.TYPE_INT_ARGB);
        Graphics g = out.getGraphics();
        for (int x = 0; x < image.length; x++)
        {
            for (int y = 0; y < image[0].length; y++)
            {
                g
                    .setColor(palette[image[hFlip ? image.length - x - 1 : x][vFlip
                        ? image[0].length - y - 1
                        : y] & 0xff]);
                g.drawLine(x, y, x, y);
            }
        }
        return out;
    }

    /**
     * Asks user which type of expansion they wish to use. If the current ROM is
     * already expanded, this does nothing. Otherwise, it will ask the user if
     * they want to expand to 4 or 6 megabytes. If they try to cancel, the ROM
     * is expanded to 4 megabytes. Note that ROM expansion is currently for
     * Earthbound ROMs only.
     * 
     * @see AbstractRom#expand()
     * @see AbstractRom#expandEx()
     */
    public void askExpandType()
    {
        if (rom.length() == 0x300200)
        {
            Object[] options = new String[]{"4 megabytes", "6 megabytes"};
            int ans = JOptionPane.showOptionDialog(null,
                "An action you have performed requires an expanded ROM.\n"
                    + "A normal ROM is 3 megabytes and may be expanded to\n"
                    + "4 or 6 megabytes (32 or 48 megabits). If you\n"
                    + "choose to expand to 4 megabytes, you may use the\n"
                    + "ROM expander to expand to 6 megabytes at a later time.",
                "Expand to...?", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (ans == 1)
                rom.expandEx();
            else
                rom.expand();
        }
    }

    /**
     * Simple spacefinder function. Finds the highest occurrence of a data-free
     * block of size [length]. Checks for normal expanded area data (i.e., 255
     * [00]s, one [02], and so on). If it finds an interrupt in the pattern, it
     * will skip that area and search just beyond it, thus leaving any
     * user-modified data intact (hopefully).
     * 
     * @param startAt last byte in ROM to look at
     * @param length length of safe area to find.
     * @return offset of free area at least <code>length</code> bytes long
     * @throws EOFException if entire ROM has been searched and nothing found
     */
    public abstract int findFreeRange(int startAt, int length)
        throws EOFException;

    /**
     * Sets the specifed range in the ROM to null. In the expanded meg null may
     * be [00] or [02] otherwise it is always [00].
     * 
     * @param address offset to start at
     * @param len number of bytes to set to null
     */
    public abstract void nullifyArea(int address, int len);

    /**
     * Writes the specified data into a free spot in the ROM and nulls the
     * previous copy. If a free spot large enough cannot be found, false will be
     * returned and the ROM will not be changed.
     * 
     * @param data data to write
     * @param pointerLoc location of pointers to data, will be set to point to
     *            the location <code>data</code> is written to
     * @param pointerBase the base SNES (NOT HEX!) address of the pointer
     * @param pointerLen length of the pointer at PointerLoc in bytes
     * @param oldLen length of orginal data; that many bytes starting at the old
     *            pointer at pointerLoc will be set to null
     * @param newLen how many bytes to read from <code>data</code>
     * @param beginAt the last byte that can be changed
     * @param mustBeInExpanded whether the data must be written to the expanded
     *            area
     * @return true on success, false on failure (no change will be made to the
     *         ROM on failure)
     * @see #findFreeRange(int, int)
     */

    public boolean writetoFree(byte[] rawData, int[] pointerLoc,
        int pointerBase, int pointerLen, int oldLen, int newLen, int beginAt,
        boolean mustBeInExpanded)
    {
        if (rawData == null || pointerLoc == null)
            return false;
        int pointerDelay = 0;
        int orgNewLen = newLen;
        byte[] data;
        // 0xff shielding if the new data starts or ends with 0
        if ((rawData[0] == 0) && (rawData[rawData.length - 1] == 0))
        {
            data = new byte[rawData.length + 2];
            data[0] = (byte) 0xff;
            data[data.length - 1] = (byte) 0xff;
            System.arraycopy(rawData, 0, data, 1, rawData.length);
            newLen += 2;
            pointerDelay = 1;
        }
        else if (rawData[0] == 0)
        {
            data = new byte[rawData.length + 1];
            data[0] = (byte) 0xff;
            System.arraycopy(rawData, 0, data, 1, rawData.length);
            newLen++;
            pointerDelay = 1;
        }
        else if (rawData[rawData.length - 1] == 0)
        {
            data = new byte[rawData.length + 1];
            data[data.length - 1] = (byte) 0xff;
            System.arraycopy(rawData, 0, data, 0, rawData.length);
            newLen++;
        }
        else
            data = rawData;

        //make sure ROM is expanded if needed
        if ((newLen > oldLen) || mustBeInExpanded)
            askExpandType();

        //store old pointer for use later
        int oldPointer;
        if (pointerLen < 0)
            oldPointer = rom.readRegAsmPointer(pointerLoc[0]);
        else
            oldPointer = toRegPointer(rom.readMulti(pointerLoc[0], pointerLen)
                + pointerBase);
        //do not bother with shielding before 0x300200.
        if (((!(mustBeInExpanded && (oldPointer < 0x300200)) && (orgNewLen <= oldLen)) || (newLen <= oldLen))
            && (oldPointer + oldLen <= beginAt))
        {
            //if it fits in the same place, then write there
            nullifyArea(oldPointer, oldLen);
            if (oldPointer < 0x300200)
                rom.write(oldPointer, rawData, orgNewLen);
            else
            {
                if (pointerLen < 0)
                    for (int i = 0; i < pointerLoc.length; i++)
                        rom.writeRegAsmPointer(pointerLoc[i], oldPointer
                            + pointerDelay);
                else
                    for (int i = 0; i < pointerLoc.length; i++)
                        rom.write(pointerLoc[i], toSnesPointer(oldPointer
                            + pointerDelay), pointerLen);
                rom.write(oldPointer, data, newLen);
            }
            return true;
        }
        else
        {
            //if it's too big to fit in the same place...
            //back-up old data in case there isn't enough space
            byte[] oldData = rom.readByte(oldPointer, oldLen);
            //delete old data from ROM, it may be part of the empty space
            // found
            nullifyArea(oldPointer, oldLen);
            try
            {
                //look for space...
                int newPointer = findFreeRange(beginAt, newLen);
                //write data there
                rom.write(newPointer, data, newLen);
                //change pointer
                newPointer = newPointer - pointerBase + pointerDelay;
                //write pointer
                if (pointerLen > 0)
                    for (int i = 0; i < pointerLoc.length; i++)
                        rom.write(pointerLoc[i], toSnesPointer(newPointer),
                            pointerLen);
                else
                    for (int i = 0; i < pointerLoc.length; i++)
                        rom.writeRegAsmPointer(pointerLoc[i], newPointer);
                //success!
                return true;
            }
            catch (EOFException e)
            {
                //if there isn't space, rewrite old data, and return failure
                rom.write(oldPointer, oldData);
                return false;
            }
        }
    }

    /**
     * Writes the specified data into a free spot in the ROM and nulls the
     * previous copy. If a free spot large enough cannot be found, false will be
     * returned and the ROM will not be changed.
     * 
     * @param data data to write
     * @param pointerLoc location of pointer to data, will be set to point to
     *            the location <code>data</code> is written to
     * @param pointerBase the base SNES (NOT HEX!) address of the pointer
     * @param pointerLen length of the pointer at PointerLoc in bytes
     * @param oldLen length of orginal data; that many bytes starting at the old
     *            pointer at pointerLoc will be set to null
     * @param newLen how many bytes to read from <code>data</code>
     * @param beginAt the last byte that can be changed
     * @param mustBeInExpanded whether the data must be written to the expanded
     *            area
     * @return true on success, false on failure (no change will be made to the
     *         ROM on failure)
     * @see #findFreeRange(int, int)
     */

    public boolean writetoFree(byte[] rawData, int pointerLoc, int pointerBase,
        int pointerLen, int oldLen, int newLen, int beginAt,
        boolean mustBeInExpanded)
    {
        return writetoFree(rawData, new int[]{pointerLoc}, pointerBase,
            pointerLen, oldLen, newLen, beginAt, mustBeInExpanded);
    }

    /**
     * Writes the specified data into a free spot in the ROM and nulls the
     * previous copy. If a free spot large enough cannot be found, false will be
     * returned and the ROM will not be changed.
     * 
     * @param data data to write
     * @param pointerLoc location of pointer to data, will be set to point to
     *            the location <code>data</code> is written to
     * @param pointerBase the base SNES (NOT HEX!) address of the pointer
     * @param pointerLen length of the pointer at PointerLoc in bytes
     * @param oldLen length of orginal data; that many bytes starting at the old
     *            pointer at pointerLoc will be set to null
     * @param newLen how many bytes to read from <code>data</code>
     * @return true on success, false on failure (no change will be made to the
     *         ROM on failure)
     * @see #findFreeRange(int, int)
     */

    public boolean writeToFree(byte[] data, int pointerLoc, int pointerBase,
        int pointerLen, int oldLen, int newLen)
    {
        int beginAt = pointerBase;
        for (int i = 0; i < pointerLen; i++)
            beginAt += 0xff * (Math.pow(0x10, i * 2));
        return writetoFree(data, pointerLoc, pointerBase, pointerLen, oldLen,
            newLen, beginAt, false);
    }

    /**
     * Writes the specified data into a free spot in the ROM and nulls the
     * previous copy. If a free spot large enough cannot be found, false will be
     * returned and the ROM will not be changed.
     * 
     * @param data data to write
     * @param pointerLoc location of pointer to data, will be set to point to
     *            the location <code>data</code> is written to
     * @param pointerLen length of the pointer at PointerLoc in bytes
     * @param oldLen length of orginal data; that many bytes starting at the old
     *            pointer at pointerLoc will be set to null
     * @param newLen how many bytes to read from <code>data</code>
     * @param beginAt the last byte that can be changed
     * @param mustBeInExpanded whether the data must be written to the expanded
     *            area
     * @return true on success, false on failure (no change will be made to the
     *         ROM on failure)
     * @see #findFreeRange(int, int)
     */

    public boolean writetoFree(byte[] data, int pointerLoc, int pointerLen,
        int oldLen, int newLen, int beginAt, boolean mustBeInExpanded)
    {
        return writetoFree(data, pointerLoc, 0, pointerLen, oldLen, newLen,
            beginAt, mustBeInExpanded);
    }

    /**
     * aaWrites the specified data into a free spot in the ROM and nulls the
     * previous copy. If a free spot large enough cannot be found, false will be
     * returned and the ROM will not be changed.
     * 
     * @param data data to write
     * @param pointerLoc location of pointer to data, will be set to point to
     *            the location <code>data</code> is written to
     * @param pointerLen length of the pointer at PointerLoc in bytes
     * @param oldLen length of orginal data; that many bytes starting at the old
     *            pointer at pointerLoc will be set to null
     * @param newLen how many bytes to read from <code>data</code>
     * @param mustBeInExpanded whether the data must be written to the expanded
     *            area
     * @return true on success, false on failure (no change will be made to the
     *         ROM on failure)
     * @see #findFreeRange(int, int)
     */

    public boolean writetoFree(byte[] data, int pointerLoc, int pointerLen,
        int oldLen, int newLen, boolean mustBeInExpanded)
    {
        if ((newLen > oldLen) || mustBeInExpanded)
            askExpandType();
        return writetoFree(data, pointerLoc, pointerLen, oldLen, newLen, rom
            .length(), mustBeInExpanded);
    }

    /**
     * Writes the specified data into a free spot in the ROM and nulls the
     * previous copy. If a free spot large enough cannot be found, false will be
     * returned and the ROM will not be changed.
     * 
     * @param data data to write
     * @param pointerLoc location of pointer to data, will be set to point to
     *            the location <code>data</code> is written to
     * @param oldLen length of orginal data; that many bytes starting at the old
     *            pointer at pointerLoc will be set to null
     * @param newLen how many bytes to read from <code>data</code>
     * @return true on success, false on failure (no change will be made to the
     *         ROM on failure)
     * @see #findFreeRange(int, int)
     */

    public boolean writeToFree(byte[] data, int pointerLoc, int oldLen,
        int newLen)
    {
        return writetoFree(data, pointerLoc, 4, oldLen, newLen, false);
    }

    /**
     * Writes the specified data into a free spot in the ROM and nulls the
     * previous copy. If a free spot large enough cannot be found, false will be
     * returned and the ROM will not be changed.
     * 
     * @param data data to write
     * @param pointerLoc location of ASM links to data, all will be set to point
     *            to the location <code>data</code> is written to
     * @param oldLen length of orginal data; that many bytes starting at the old
     *            pointer at pointerLoc will be set to null
     * @param newLen how many bytes to read from <code>data</code>
     * @param beginAt the last byte that can be changed
     * @param mustBeInExpanded if true, data will always be written to the
     *            expanded area, never to where it originally was
     * @return true on success, false on failure (no change will be made to the
     *         ROM on failure)
     * @see #findFreeRange(int, int)
     * @see AbstractRom#writeAsmPointer(int, int)
     */
    public boolean writeToFreeASMLink(byte[] data, int[] pointerLoc,
        int oldLen, int newLen, int beginAt, boolean mustBeInExpanded)
    {
        return writetoFree(data, pointerLoc, 0, -1, oldLen, newLen, beginAt,
            mustBeInExpanded);
    }

    /**
     * Writes the specified data into a free spot in the ROM and nulls the
     * previous copy. If a free spot large enough cannot be found, false will be
     * returned and the ROM will not be changed.
     * 
     * @param data data to write
     * @param pointerLoc location of an ASM link to data, it will be set to
     *            point to the location <code>data</code> is written to
     * @param oldLen length of orginal data; that many bytes starting at the old
     *            pointer at pointerLoc will be set to null
     * @param newLen how many bytes to read from <code>data</code>
     * @param beginAt the last byte that can be changed
     * @param mustBeInExpanded if true, data will always be written to the
     *            expanded area, never to where it originally was
     * @return true on success, false on failure (no change will be made to the
     *         ROM on failure)
     * @see #findFreeRange(int, int)
     * @see AbstractRom#writeAsmPointer(int, int)
     */
    public boolean writeToFreeASMLink(byte[] data, int pointerLoc, int oldLen,
        int newLen, int beginAt, boolean mustBeInExpanded)
    {
        return writeToFreeASMLink(data, new int[]{pointerLoc}, oldLen, newLen,
            beginAt, mustBeInExpanded);
    }

    /**
     * Writes the specified data into a free spot in the ROM and nulls the
     * previous copy. If a free spot large enough cannot be found, false will be
     * returned and the ROM will not be changed.
     * 
     * @param data data to write
     * @param pointerLoc location of ASM links to data, all will be set to point
     *            to the location <code>data</code> is written to
     * @param oldLen length of orginal data; that many bytes starting at the old
     *            pointer at pointerLoc will be set to null
     * @param newLen how many bytes to read from <code>data</code>
     * @param mustBeInExpanded if true, data will always be written to the
     *            expanded area, never to where it originally was
     * @return true on success, false on failure (no change will be made to the
     *         ROM on failure)
     * @see #findFreeRange(int, int)
     * @see AbstractRom#writeAsmPointer(int, int)
     */
    public boolean writeToFreeASMLink(byte[] data, int[] pointerLoc,
        int oldLen, int newLen, boolean mustBeInExpanded)
    {
        return writeToFreeASMLink(data, pointerLoc, oldLen, newLen, rom
            .length(), mustBeInExpanded);
    }

    /**
     * Writes the specified data into a free spot in the ROM and nulls the
     * previous copy. If a free spot large enough cannot be found, false will be
     * returned and the ROM will not be changed.
     * 
     * @param data data to write
     * @param pointerLoc location of ASM links to data, all will be set to point
     *            to the location <code>data</code> is written to
     * @param oldLen length of orginal data; that many bytes starting at the old
     *            pointer at pointerLoc will be set to null
     * @param newLen how many bytes to read from <code>data</code>
     * @return true on success, false on failure (no change will be made to the
     *         ROM on failure)
     * @see #findFreeRange(int, int)
     * @see AbstractRom#writeAsmPointer(int, int)
     */
    public boolean writeToFreeASMLink(byte[] data, int[] pointerLoc,
        int oldLen, int newLen)
    {
        return writeToFreeASMLink(data, pointerLoc, oldLen, newLen, false);
    }

    /**
     * Writes the specified data into a free spot in the ROM and nulls the
     * previous copy. If a free spot large enough cannot be found, false will be
     * returned and the ROM will not be changed.
     * 
     * @param data data to write
     * @param pointerLoc location of ASM link to data, will be set to point to
     *            the location <code>data</code> is written to
     * @param oldLen length of orginal data; that many bytes starting at the old
     *            pointer at pointerLoc will be set to null
     * @param newLen how many bytes to read from <code>data</code>
     * @param mustBeInExpanded if true, data will always be written to the
     *            expanded area, never to where it originally was
     * @return true on success, false on failure (no change will be made to the
     *         ROM on failure)
     * @see #findFreeRange(int, int)
     * @see AbstractRom#writeAsmPointer(int, int)
     */
    public boolean writeToFreeASMLink(byte[] data, int pointerLoc, int oldLen,
        int newLen, boolean mustBeInExpanded)
    {
        return writeToFreeASMLink(data, new int[]{pointerLoc}, oldLen, newLen,
            mustBeInExpanded);
    }

    /**
     * Writes the specified data into a free spot in the ROM and nulls the
     * previous copy. If a free spot large enough cannot be found, false will be
     * returned and the ROM will not be changed.
     * 
     * @param data data to write
     * @param pointerLoc location of ASM link to data, will be set to point to
     *            the location <code>data</code> is written to
     * @param oldLen length of orginal data; that many bytes starting at the old
     *            pointer at pointerLoc will be set to null
     * @param newLen how many bytes to read from <code>data</code>
     * @return true on success, false on failure (no change will be made to the
     *         ROM on failure)
     * @see #findFreeRange(int, int)
     * @see AbstractRom#writeAsmPointer(int, int)
     */
    public boolean writeToFreeASMLink(byte[] data, int pointerLoc, int oldLen,
        int newLen)
    {
        return writeToFreeASMLink(data, new int[]{pointerLoc}, oldLen, newLen,
            false);
    }

    //1BPP
    /**
     * Reads a one bit per pixel (1BPP) area from the ROM and writes the data to
     * the specified area on the given byte array. This area represents a 8x
     * <code>h</code> black and white image. Note that the size of a 1BPP
     * image is <code>h</code> bytes.
     * 
     * @param target byte array to write to
     * @param address address of image in ROM
     * @param h height of image in ROM
     * @param x x-offset on image to write to
     * @param y y-offset on image to write to
     * @return number of bytes read
     * @see #read1BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write1BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write1BPPArea(byte[][], int, int, int, int)
     */
    public int read1BPPArea(byte[][] target, int address, int h, int x, int y)
    {
        byte[] b = rom.readByte(address, h);
        int len = read1BPPArea(target, b, 0, h, x, y);

        return len;
    }

    /**
     * Reads a one bit per pixel (1BPP) area from a
     * <code>byte[]<code> and writes the data
     * to the specified area on the given byte array. This area represents a 8x
     * <code>h</code> black and white image. Note that the size of a 1BPP image is <code>h</code> bytes.
     * 
     * @param target byte array to write to
     * @param source reading buffer
     * @param off index to start reading from
     * @param h height of image in ROM
     * @param x x-offset on image to write to
     * @param y y-offset on image to write to
     * @return number of bytes read
     * @see #read1BPPArea(byte[][], int, int, int, int)
     * @see #write1BPPArea(byte[][], int, int, int, int)
     * @see #write1BPPArea(byte[][], byte[], int, int, int, int)
     */
    public static int read1BPPArea(byte[][] target, byte[] source, int off,
        int h, int x, int y)
    {
        int offset = off;
        for (int i = 0; i < h; i++)
        {
            byte b = source[offset++];

            for (int j = 0; j < 8; j++)
                target[(7 - j) + x][i + y] = (byte) ((b & (1 << j)) >> j);
        }
        return offset - off;
    }

    /**
     * Writes a one bit per pixel (1BPP) area into the ROM from the specified
     * area on the given byte array. This area represents a 8x <code>h</code>
     * black and white image. Note that the size of a 1BPP image is
     * <code>h</code> bytes.
     * 
     * @param source byte array to read from
     * @param address address of image to write in ROM
     * @param h height of image to write to ROM
     * @param x x-offset on image to read from
     * @param y y-offset on image to read from
     * @return number of bytes written
     * @see #read1BPPArea(byte[][], int, int, int, int)
     * @see #read1BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write1BPPArea(byte[][], byte[], int, int, int, int)
     */
    public int write1BPPArea(byte[][] source, int address, int h, int x, int y)
    {
        byte[] b = new byte[h];
        int len = write1BPPArea(source, b, 0, h, x, y);
        rom.write(address, b);

        return len;
    }

    /**
     * Writes a one bit per pixel (1BPP) area into the given <code>byte[]</code>
     * from the specified area on the given byte array. This area represents a
     * 8x <code>h</code> black and white image. Note that the size of a 1BPP
     * image is <code>h</code> bytes.
     * 
     * @param source byte array to read from
     * @param target writting buffer
     * @param off index to start writting to
     * @param h height of image to write to ROM
     * @param x x-offset on image to read from
     * @param y y-offset on image to read from
     * @return number of bytes written
     * @see #read1BPPArea(byte[][], int, int, int, int)
     * @see #read1BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write1BPPArea(byte[][], int, int, int, int)
     */
    public static int write1BPPArea(byte[][] source, byte[] target, int off,
        int h, int x, int y)
    {
        int offset = off;
        for (int i = 0; i < h; i++)
        {
            byte b = 0;

            for (int j = 0; j < 8; j++)
                b |= (source[(7 - j) + x][i + y] & 1) << j;

            target[offset++] = b;
        }
        return offset - off;
    }

    //2BPP
    /**
     * Writes a two bit per pixel (2BPP) area into the ROM from the specified
     * area on the given byte array. This area represents a 8x8 four color
     * image. Note that the size of a 2BPP image is 16 bytes.
     * 
     * @param target byte array to write to
     * @param address address of image in ROM
     * @param x x-offset on image to write to
     * @param y y-offset on image to write to
     * @param bitOffset number of bits to left-shift <code>target</code>
     * @return number of bytes read
     * @see #read1BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write1BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write1BPPArea(byte[][], int, int, int, int)
     * @see #read2BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write2BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write2BPPArea(byte[][], int, int, int, int)
     */
    public int read2BPPArea(byte[][] target, int address, int x, int y,
        int bitOffset)
    {
        byte[] b = rom.readByte(address, 16);
        int len = read2BPPArea(target, b, 0, x, y, bitOffset);

        return len;
    }

    /**
     * Writes a two bit per pixel (2BPP) area into the ROM from the specified
     * area on the given byte array. This area represents a 8x8 four color
     * image. Note that the size of a 2BPP image is 16 bytes.
     * 
     * @param target byte array to write to
     * @param address address of image in ROM
     * @param x x-offset on image to write to
     * @param y y-offset on image to write to
     * @return number of bytes read
     * @see #read1BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write1BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write1BPPArea(byte[][], int, int, int, int)
     * @see #read2BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write2BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write2BPPArea(byte[][], int, int, int, int)
     */
    public int read2BPPArea(byte[][] target, int address, int x, int y)
    {
        return read2BPPArea(target, address, x, y, -1);
    }

    /**
     * Reads a two bit per pixel (2BPP) area from <code>source</code> to the
     * given byte array. This area represents a 8x8 four color image. Note that
     * the size of a 2BPP image is 16 bytes.
     * 
     * @param target byte array to write to
     * @param source reading buffer
     * @param off index to start reading from
     * @param x x-offset on image to write to
     * @param y y-offset on image to write to
     * @param bitOffset number of bits to left-shift <code>target</code>
     * @return number of bytes read
     * @see #read1BPPArea(byte[][], int, int, int, int)
     * @see #write1BPPArea(byte[][], int, int, int, int)
     * @see #write1BPPArea(byte[][], byte[], int, int, int, int)
     * @see #read2BPPArea(byte[][], int, int, int, int)
     * @see #write2BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write2BPPArea(byte[][], int, int, int, int)
     */
    public static int read2BPPArea(byte[][] target, byte[] source, int off,
        int x, int y, int bitOffset)
    {
        if (bitOffset < 0)
            bitOffset = 0;
        int offset = off;
        for (int i = 0; i < 8; i++)
        {
            for (int k = 0; k < 2; k++)
            {
                byte b = source[offset++];
                for (int j = 0; j < 8; j++)
                {
                    target[(7 - j) + x][i + y] |= (byte) (((b & (1 << j)) >> j) << (k + bitOffset));
                }
            }
        }
        return offset - off;
    }

    /**
     * Reads a two bit per pixel (2BPP) area from <code>source</code> to the
     * given byte array. This area represents a 8x8 four color image. Note that
     * the size of a 2BPP image is 16 bytes.
     * 
     * @param target byte array to write to
     * @param source reading buffer
     * @param off index to start reading from
     * @param x x-offset on image to write to
     * @param y y-offset on image to write to
     * @return number of bytes read
     * @see #read1BPPArea(byte[][], int, int, int, int)
     * @see #write1BPPArea(byte[][], int, int, int, int)
     * @see #write1BPPArea(byte[][], byte[], int, int, int, int)
     * @see #read2BPPArea(byte[][], int, int, int, int)
     * @see #write2BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write2BPPArea(byte[][], int, int, int, int)
     */
    public static int read2BPPArea(byte[][] target, byte[] source, int off,
        int x, int y)
    {
        return read2BPPArea(target, source, off, x, y, -1);
    }

    /**
     * Writes a two bit per pixel (2BPP) area into the ROM from the specified
     * area on the given byte array. This area represents a 8x8 four color
     * image. Note that the size of a 2BPP image is 16 bytes.
     * 
     * @param source byte array to read from
     * @param address address of image to write in ROM
     * @param x x-offset on image to read from
     * @param y y-offset on image to read from
     * @return number of bytes written
     * @see #read1BPPArea(byte[][], int, int, int, int)
     * @see #read1BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write1BPPArea(byte[][], byte[], int, int, int, int)
     * @see #read2BPPArea(byte[][], int, int, int, int)
     * @see #read2BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write2BPPArea(byte[][], byte[], int, int, int, int)
     */
    public int write2BPPArea(byte[][] source, int address, int x, int y,
        int bitOffset)
    {
        byte[] b = new byte[16];
        int len = write2BPPArea(source, b, 0, x, y, bitOffset);
        rom.write(address, b);

        return len;
    }

    /**
     * Writes a two bit per pixel (2BPP) area into the ROM from the specified
     * area on the given byte array. This area represents a 8x8 four color
     * image. Note that the size of a 2BPP image is 16 bytes.
     * 
     * @param source byte array to read from
     * @param address address of image to write in ROM
     * @param x x-offset on image to read from
     * @param y y-offset on image to read from
     * @return number of bytes written
     * @see #read1BPPArea(byte[][], int, int, int, int)
     * @see #read1BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write1BPPArea(byte[][], byte[], int, int, int, int)
     * @see #read2BPPArea(byte[][], int, int, int, int)
     * @see #read2BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write2BPPArea(byte[][], byte[], int, int, int, int)
     */
    public int write2BPPArea(byte[][] source, int address, int x, int y)
    {
        return write2BPPArea(source, address, x, y, 0);
    }

    /**
     * Writes a two bit per pixel (2BPP) area into <code>target</code> from
     * the specified area on the given byte array. This area represents a 8x8
     * four color image. Note that the size of a 2BPP image is 16 bytes.
     * 
     * @param source byte array to read from
     * @param target writting buffer
     * @param off index to start writting to
     * @param x x-offset on image to read from
     * @param y y-offset on image to read from
     * @param bitOffset number of bits to the left to shift <code>source</code>
     * @return number of bytes written
     * @see #read1BPPArea(byte[][], int, int, int, int)
     * @see #read1BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write1BPPArea(byte[][], int, int, int, int)
     * @see #read2BPPArea(byte[][], int, int, int, int)
     * @see #read2BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write2BPPArea(byte[][], int, int, int, int)
     */
    public static int write2BPPArea(byte[][] source, byte[] target, int off,
        int x, int y, int bitOffset)
    {
        if (bitOffset < 0)
            bitOffset = 0;
        int offset = off;
        for (int i = 0; i < 8; i++)
        {
            for (int k = 0; k < 2; k++)
            {
                byte b = 0;
                for (int j = 0; j < 8; j++)
                    b |= ((source[(7 - j) + x][i + y] & (1 << (k + bitOffset))) >> (k + bitOffset)) << j;
                target[offset++] = b;
            }
        }
        return offset - off;
    }

    /**
     * Writes a two bit per pixel (2BPP) area into <code>target</code> from
     * the specified area on the given byte array. This area represents a 8x8
     * four color image. Note that the size of a 2BPP image is 16 bytes.
     * 
     * @param source byte array to read from
     * @param target writting buffer
     * @param off index to start writting to
     * @param x x-offset on image to read from
     * @param y y-offset on image to read from
     * @return number of bytes written
     * @see #read1BPPArea(byte[][], int, int, int, int)
     * @see #read1BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write1BPPArea(byte[][], int, int, int, int)
     * @see #read2BPPArea(byte[][], int, int, int, int)
     * @see #read2BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write2BPPArea(byte[][], int, int, int, int)
     */
    public static int write2BPPArea(byte[][] source, byte[] target, int off,
        int x, int y)
    {
        return write2BPPArea(source, target, off, x, y, 0);
    }

    //4BPP
    /**
     * Writes a four bit per pixel (4BPP) area into the ROM from the specified
     * area on the given byte array. This area represents a 8x8 four color
     * image. Note that the size of a 4BPP image is 32 bytes.
     * 
     * @param target byte array to write to
     * @param address address of image in ROM
     * @param x x-offset on image to write to
     * @param y y-offset on image to write to
     * @param bitOffset number of bits to left-shift <code>target</code>
     * @return number of bytes read
     * @see #read1BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write1BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write1BPPArea(byte[][], int, int, int, int)
     * @see #read2BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write2BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write2BPPArea(byte[][], int, int, int, int)
     */
    public int read4BPPArea(byte[][] target, int address, int x, int y,
        int bitOffset)
    {
        byte[] b = rom.readByte(address, 32);
        int len = read4BPPArea(target, b, 0, x, y, bitOffset);

        return len;
    }

    /**
     * Writes a four bit per pixel (4BPP) area into the ROM from the specified
     * area on the given byte array. This area represents a 8x8 four color
     * image. Note that the size of a 4BPP image is 32 bytes.
     * 
     * @param target byte array to write to
     * @param address address of image in ROM
     * @param x x-offset on image to write to
     * @param y y-offset on image to write to
     * @return number of bytes read
     * @see #read1BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write1BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write1BPPArea(byte[][], int, int, int, int)
     * @see #read2BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write2BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write2BPPArea(byte[][], int, int, int, int)
     */
    public int read4BPPArea(byte[][] target, int address, int x, int y)
    {
        return read4BPPArea(target, address, x, y, -1);
    }

    /**
     * Reads a four bit per pixel (4BPP) area from <code>source</code> to the
     * given byte array. This area represents a 8x8 four color image. Note that
     * the size of a 4BPP image is 32 bytes.
     * 
     * @param target byte array to write to
     * @param source reading buffer
     * @param off index to start reading from
     * @param x x-offset on image to write to
     * @param y y-offset on image to write to
     * @param bitOffset number of bits to left-shift <code>target</code>
     * @return number of bytes read
     * @see #read1BPPArea(byte[][], int, int, int, int)
     * @see #write1BPPArea(byte[][], int, int, int, int)
     * @see #write1BPPArea(byte[][], byte[], int, int, int, int)
     * @see #read2BPPArea(byte[][], int, int, int, int)
     * @see #write2BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write2BPPArea(byte[][], int, int, int, int)
     */
    public static int read4BPPArea(byte[][] target, byte[] source, int off,
        int x, int y, int bitOffset)
    {
        if (bitOffset < 0)
            bitOffset = 0;
        read2BPPArea(target, source, off, x, y, bitOffset);
        read2BPPArea(target, source, off + 16, x, y, bitOffset + 2);
        return 32;
    }

    /**
     * Reads a four bit per pixel (4BPP) area from <code>source</code> to the
     * given byte array. This area represents a 8x8 four color image. Note that
     * the size of a 4BPP image is 32 bytes.
     * 
     * @param target byte array to write to
     * @param source reading buffer
     * @param off index to start reading from
     * @param x x-offset on image to write to
     * @param y y-offset on image to write to
     * @return number of bytes read
     * @see #read1BPPArea(byte[][], int, int, int, int)
     * @see #write1BPPArea(byte[][], int, int, int, int)
     * @see #write1BPPArea(byte[][], byte[], int, int, int, int)
     * @see #read2BPPArea(byte[][], int, int, int, int)
     * @see #write2BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write2BPPArea(byte[][], int, int, int, int)
     */
    public static int read4BPPArea(byte[][] target, byte[] source, int off,
        int x, int y)
    {
        return read4BPPArea(target, source, off, x, y, -1);
    }

    /**
     * Writes a four bit per pixel (4BPP) area into the ROM from the specified
     * area on the given byte array. This area represents a 8x8 four color
     * image. Note that the size of a 4BPP image is 32 bytes.
     * 
     * @param source byte array to read from
     * @param address address of image to write in ROM
     * @param x x-offset on image to read from
     * @param y y-offset on image to read from
     * @return number of bytes written
     * @see #read1BPPArea(byte[][], int, int, int, int)
     * @see #read1BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write1BPPArea(byte[][], byte[], int, int, int, int)
     * @see #read2BPPArea(byte[][], int, int, int, int)
     * @see #read2BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write2BPPArea(byte[][], byte[], int, int, int, int)
     */
    public int write4BPPArea(byte[][] source, int address, int x, int y,
        int bitOffset)
    {
        byte[] b = new byte[32];
        int len = write4BPPArea(source, b, 0, x, y, bitOffset);
        rom.write(address, b);

        return len;
    }

    /**
     * Writes a four bit per pixel (4BPP) area into the ROM from the specified
     * area on the given byte array. This area represents a 8x8 four color
     * image. Note that the size of a 4BPP image is 32 bytes.
     * 
     * @param source byte array to read from
     * @param address address of image to write in ROM
     * @param x x-offset on image to read from
     * @param y y-offset on image to read from
     * @return number of bytes written
     * @see #read1BPPArea(byte[][], int, int, int, int)
     * @see #read1BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write1BPPArea(byte[][], byte[], int, int, int, int)
     * @see #read2BPPArea(byte[][], int, int, int, int)
     * @see #read2BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write2BPPArea(byte[][], byte[], int, int, int, int)
     */
    public int write4BPPArea(byte[][] source, int address, int x, int y)
    {
        return write4BPPArea(source, address, x, y, 0);
    }

    /**
     * Writes a four bit per pixel (4BPP) area into <code>target</code> from
     * the specified area on the given byte array. This area represents a 8x8
     * sixteen color image. Note that the size of a 4BPP image is 32 bytes.
     * 
     * @param source byte array to read from
     * @param target writting buffer
     * @param off index to start writting to
     * @param x x-offset on image to read from
     * @param y y-offset on image to read from
     * @param bitOffset number of bits to the left to shift <code>source</code>
     * @return number of bytes written
     * @see #read1BPPArea(byte[][], int, int, int, int)
     * @see #read1BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write1BPPArea(byte[][], int, int, int, int)
     * @see #read2BPPArea(byte[][], int, int, int, int)
     * @see #read2BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write2BPPArea(byte[][], int, int, int, int)
     */
    public static int write4BPPArea(byte[][] source, byte[] target, int off,
        int x, int y, int bitOffset)
    {
        if (bitOffset < 0)
            bitOffset = 0;
        write2BPPArea(source, target, off, x, y, bitOffset);
        write2BPPArea(source, target, off + 16, x, y, bitOffset + 2);
        return 32;
    }

    /**
     * Writes a four bit per pixel (4BPP) area into <code>target</code> from
     * the specified area on the given byte array. This area represents a 8x8
     * sixteen color image. Note that the size of a 4BPP image is 32 bytes.
     * 
     * @param source byte array to read from
     * @param target writting buffer
     * @param off index to start writting to
     * @param x x-offset on image to read from
     * @param y y-offset on image to read from
     * @return number of bytes written
     * @see #read1BPPArea(byte[][], int, int, int, int)
     * @see #read1BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write1BPPArea(byte[][], int, int, int, int)
     * @see #read2BPPArea(byte[][], int, int, int, int)
     * @see #read2BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write2BPPArea(byte[][], int, int, int, int)
     */
    public static int write4BPPArea(byte[][] source, byte[] target, int off,
        int x, int y)
    {
        return write4BPPArea(source, target, off, x, y, 0);
    }

    //8BPP
    /**
     * Reads an eight bit per pixel (8BPP) area from <code>source</code> to
     * the given byte array. This area represents a 8x8 256 color image. Note
     * that the size of a 8BPP image is 64 bytes.
     * 
     * @param target byte array to write to
     * @param source reading buffer
     * @param off index to start reading from
     * @param x x-offset on image to write to
     * @param y y-offset on image to write to
     * @return number of bytes read
     * @see #read1BPPArea(byte[][], int, int, int, int)
     * @see #write1BPPArea(byte[][], int, int, int, int)
     * @see #write1BPPArea(byte[][], byte[], int, int, int, int)
     * @see #read2BPPArea(byte[][], int, int, int, int)
     * @see #write2BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write2BPPArea(byte[][], int, int, int, int)
     */
    public static int read8BPPArea(byte[][] target, byte[] source, int off,
        int x, int y)
    {
        for (int i = 0; i < 4; i++)
            read2BPPArea(target, source, off + 16 * i, x, y, 2 * i);
        return 64;
    }

    /**
     * Writes an eight bit per pixel (8BPP) area into <code>target</code> from
     * the specified area on the given byte array. This area represents a 8x8
     * 256 color image. Note that the size of a 8BPP image is 64 bytes.
     * 
     * @param source byte array to read from
     * @param target writting buffer
     * @param off index to start writting to
     * @param x x-offset on image to read from
     * @param y y-offset on image to read from
     * @return number of bytes written
     * @see #read1BPPArea(byte[][], int, int, int, int)
     * @see #read1BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write1BPPArea(byte[][], int, int, int, int)
     * @see #read2BPPArea(byte[][], int, int, int, int)
     * @see #read2BPPArea(byte[][], byte[], int, int, int, int)
     * @see #write2BPPArea(byte[][], int, int, int, int)
     */
    public static int write8BPPArea(byte[][] source, byte[] target, int off,
        int x, int y)
    {
        for (int i = 0; i < 4; i++)
            write2BPPArea(source, target, off + 16 * i, x, y, 2 * i);
        return 64;
    }

    //read palette from byte[]
    /**
     * Reads an SNES format palette color from the specificed place in a
     * <code>byte[]</code>. This reads one color of a palette, which is two
     * bytes long. SNES palettes are made up of 16-bit little endian color
     * entries. 5 bits each are used for (from lowest order to highest order
     * bits) red, green, and blue, and one bit is left unused.
     * 
     * @param b <code>byte[]</code> palette is stored in
     * @param offset offset in <code>byte[]</code> palette color is at; note
     *            that <code>b[offset]</code> and <code>b[offset + 1]</code>
     *            will be read
     * @return a {@link Color}that is equivalent to the specified SNES color
     * @see #readPalette(byte[])
     * @see #readPalette(byte[], Color[])
     * @see #readPalette(byte[], int, Color[])
     * @see #readPalette(byte[], int, int)
     * @see #writePalette(byte[], int, Color)
     * @see #writePalette(byte[], int, Color[])
     * @see #writePalette(Color)
     * @see #writePalette(Color[])
     */
    public static Color readPalette(byte[] b, int offset)
    {
        int bgrBlock = ((b[offset] & 0xff) | ((b[offset + 1] & 0xff) << 8)) & 0x7FFF;
        return new Color((bgrBlock & 0x001f) * 8,
            ((bgrBlock & 0x03e0) >> 5) * 8, (bgrBlock >> 10) * 8);
    }

    /**
     * Reads an SNES format palette color from the start of a
     * <code>byte[]</code>. This reads one color of a palette, which is two
     * bytes long. SNES palettes are made up of 16-bit little endian color
     * entries. 5 bits each are used for (from lowest order to highest order
     * bits) red, green, and blue, and one bit is left unused.
     * 
     * @param b <code>byte[]</code> palette is stored in
     * @return a {@link Color}that is equivalent to the specified SNES color
     * @see #readPalette(byte[], int)
     * @see #readPalette(byte[], Color[])
     * @see #readPalette(byte[], int, Color[])
     * @see #readPalette(byte[], int, int)
     * @see #writePalette(byte[], int, Color)
     * @see #writePalette(byte[], int, Color[])
     * @see #writePalette(Color)
     * @see #writePalette(Color[])
     */
    public static Color readPalette(byte[] b)
    {
        return readPalette(b, 0);
    }

    /**
     * Reads an SNES format palette from the specificed place in a
     * <code>byte[]</code>. This reads as many colors of the palette as the
     * <code>Color[]<code> array is long;
     * each color is two bytes long. SNES
     * palettes are made up of 16-bit little endian color entries. 5 bits each
     * are used for (from lowest order to highest order bits) red, green, and
     * blue, and one bit is left unused.
     * 
     * @param b <code>byte[]</code> palette is stored in
     * @param offset offset in <code>byte[]</code> palette color is at; note
     *            that <code>c.length() * 2</code> bytes
     *            will be read
     * @param c <code>Color[]</code> to read {@link Color}'s into, which are equivalent to the SNES colors 
     * @see #readPalette(byte[])
     * @see #readPalette(byte[], int)
     * @see #readPalette(byte[], Color[])
     * @see #readPalette(byte[], int, int)
     * @see #writePalette(byte[], int, Color)
     * @see #writePalette(byte[], int, Color[])
     * @see #writePalette(Color)
     * @see #writePalette(Color[])
     */
    public static void readPalette(byte[] b, int offset, Color[] c)
    {
        for (int i = 0; i < c.length; i++)
            c[i] = readPalette(b, offset + (i * 2));
    }

    /**
     * Reads an SNES format palette from the start of a <code>byte[]</code>.
     * This reads as many colors of the palette as the
     * <code>Color[]<code> array is long;
     * each color is two bytes long. SNES
     * palettes are made up of 16-bit little endian color entries. 5 bits each
     * are used for (from lowest order to highest order bits) red, green, and
     * blue, and one bit is left unused.
     * 
     * @param b <code>byte[]</code> palette is stored in
     * @param c <code>Color[]</code> to read {@link Color}'s into, which are equivalent to the SNES colors 
     * @see #readPalette(byte[])
     * @see #readPalette(byte[], int)
     * @see #readPalette(byte[], int, Color[])
     * @see #readPalette(byte[], int, int)
     * @see #writePalette(byte[], int, Color)
     * @see #writePalette(byte[], int, Color[])
     * @see #writePalette(Color)
     * @see #writePalette(Color[])
     */
    public static void readPalette(byte[] b, Color[] c)
    {
        readPalette(b, 0, c);
    }

    /**
     * Reads an SNES format palette from the specificed place in a
     * <code>byte[]</code>. This reads <code>size</code> colors of the
     * palette; each color is two bytes long. SNES palettes are made up of
     * 16-bit little endian color entries. 5 bits each are used for (from lowest
     * order to highest order bits) red, green, and blue, and one bit is left
     * unused.
     * 
     * @param b <code>byte[]</code> palette is stored in
     * @param offset offset in <code>byte[]</code> palette color is at; note
     *            that <code>size * 2</code> bytes will be read
     * @param size number of colors to read, <code>size * 2</code> equals the
     *            number of bytes to read
     * @return a <code>Color[]</code> of {@link Color}'s that are equivalent
     *         to the SNES colors
     * @see #readPalette(byte[])
     * @see #readPalette(byte[], int)
     * @see #readPalette(byte[], Color[])
     * @see #readPalette(byte[], int, Color[])
     * @see #writePalette(byte[], int, Color)
     * @see #writePalette(byte[], int, Color[])
     * @see #writePalette(Color)
     * @see #writePalette(Color[])
     */
    public static Color[] readPalette(byte[] b, int offset, int size)
    {
        Color[] c = new Color[size];
        readPalette(b, offset, c);
        return c;
    }

    //write palette to byte[]
    /**
     * Writes an SNES format palette color to the specificed place in a
     * <code>byte[]</code>. This writes one color of a palette, which is two
     * bytes long. SNES palettes are made up of 16-bit little endian color
     * entries. 5 bits each are used for (from lowest order to highest order
     * bits) red, green, and blue, and one bit is left unused.
     * 
     * @param b <code>byte[]</code> palette will be stored in
     * @param offset offset in <code>byte[]</code> palette color will be
     *            written at at; note that <code>b[offset]</code> and
     *            <code>b[offset + 1]</code> will be written to
     * @param c {@link Color}to write as an SNES color; note that
     *            <code>Color</code> supports up to 8 bits per color component
     *            (red, green, blue) as well as an alpha channel and SNES colors
     *            have only 5 bits per color component and no alpha support
     * @see #readPalette(byte[])
     * @see #readPalette(byte[], int)
     * @see #readPalette(byte[], Color[])
     * @see #readPalette(byte[], int, Color[])
     * @see #readPalette(byte[], int, int)
     * @see #writePalette(byte[], int, Color[])
     * @see #writePalette(Color)
     * @see #writePalette(Color[])
     */
    public static void writePalette(byte[] b, int offset, Color c)
    {
        int bgrBlock = (c.getRed() >> 3) & 0x1f;
        bgrBlock += ((c.getGreen() >> 3) & 0x1f) << 5;
        bgrBlock += ((c.getBlue() >> 3) & 0x1f) << 10;
        bgrBlock &= 0x7FFF;

        b[offset] = (byte) (bgrBlock & 0xff);
        b[offset + 1] = (byte) ((bgrBlock >> 8) & 0xff);
    }

    /**
     * Writes an SNES format palette to the specificed place in a
     * <code>byte[]</code>. This writes all <code>Color</code>'s in
     * <code>c</code> to a palette; each color is two bytes long. SNES
     * palettes are made up of 16-bit little endian color entries. 5 bits each
     * are used for (from lowest order to highest order bits) red, green, and
     * blue, and one bit is left unused.
     * 
     * @param b <code>byte[]</code> palette will be stored in
     * @param offset offset in <code>byte[]</code> palette color will be
     *            written at at; note that <code>b[offset]</code> and
     *            <code>b[offset + 1]</code> will be written to
     * @param c {@link Color}[] to write as an SNES palette; note that
     *            <code>Color</code> supports up to 8 bits per color component
     *            (red, green, blue) as well as an alpha channel and SNES colors
     *            have only 5 bits per color component and no alpha support
     * @see #readPalette(byte[])
     * @see #readPalette(byte[], int)
     * @see #readPalette(byte[], Color[])
     * @see #readPalette(byte[], int, Color[])
     * @see #readPalette(byte[], int, int)
     * @see #writePalette(byte[], int, Color)
     * @see #writePalette(Color)
     * @see #writePalette(Color[])
     */
    public static void writePalette(byte[] b, int offset, Color[] c)
    {
        for (int i = 0; i < c.length; i++)
            writePalette(b, offset + (i * 2), c[i]);
    }

    /**
     * Writes an SNES format palette color to the start of a new
     * <code>byte[]</code>. This writes one color of a palette, which is two
     * bytes long. SNES palettes are made up of 16-bit little endian color
     * entries. 5 bits each are used for (from lowest order to highest order
     * bits) red, green, and blue, and one bit is left unused.
     * 
     * @param c {@link Color}to write as an SNES color; note that
     *            <code>Color</code> supports up to 8 bits per color component
     *            (red, green, blue) as well as an alpha channel and SNES colors
     *            have only 5 bits per color component and no alpha support
     * @return a <code>byte[]</code> with two elements containing the SNES
     *         format of the specified <code>Color</code>.
     * @see #readPalette(byte[])
     * @see #readPalette(byte[], int)
     * @see #readPalette(byte[], Color[])
     * @see #readPalette(byte[], int, Color[])
     * @see #readPalette(byte[], int, int)
     * @see #writePalette(byte[], int, Color)
     * @see #writePalette(byte[], int, Color[])
     * @see #writePalette(Color[])
     */
    public static byte[] writePalette(Color c)
    {
        byte[] b = new byte[2];
        writePalette(b, 0, c);
        return b;
    }

    /**
     * Writes an SNES format palette to the start of a new <code>byte[]</code>.
     * This writes all <code>Color</code>'s in <code>c</code> to a palette;
     * each color is two bytes long. SNES palettes are made up of 16-bit little
     * endian color entries. 5 bits each are used for (from lowest order to
     * highest order bits) red, green, and blue, and one bit is left unused.
     * 
     * @param c {@link Color}[] to write as an SNES palette; note that
     *            <code>Color</code> supports up to 8 bits per color component
     *            (red, green, blue) as well as an alpha channel and SNES colors
     *            have only 5 bits per color component and no alpha support
     * @return a <code>byte[]</code> with <code>2 * c.length</code> elements
     *         containing the SNES format of the specified <code>Color[]</code>.
     * @see #readPalette(byte[])
     * @see #readPalette(byte[], int)
     * @see #readPalette(byte[], Color[])
     * @see #readPalette(byte[], int, Color[])
     * @see #readPalette(byte[], int, int)
     * @see #writePalette(byte[], int, Color)
     * @see #writePalette(byte[], int, Color[])
     * @see #writePalette(Color)
     */
    public static byte[] writePalette(Color[] c)
    {
        byte[] b = new byte[2 * c.length];
        writePalette(b, 0, c);
        return b;
    }

    /**
     * Pads the given String on the left with the specified char.
     * 
     * @param in String to pad
     * @param maxLen length to pad to
     * @param chr char to pad with
     * @return padded String
     */
    public static String padString(String in, int maxLen, char chr)
    {
        while (in.length() < maxLen)
        {
            in = chr + in;
        }
        return in;
    }

    /**
     * Adds zeros to the start of a <code>String</code> up to
     * <code>maxLen</code>. This for making hex values look nice. If
     * <code>in</code> is already as long, or longer, than <code>maxLen</code>
     * <code>in</code>
     * will be returned.
     * 
     * @param in <code>String</code> to add zeros to
     * @return A new <code>String</code> with length >=<code>maxLen</code>
     *         by adding "0"'s to the start of <code>in</code>
     */
    public static String addZeros(String in, int maxLen)
    {
        return padString(in, maxLen, '0');
    }

    /**
     * Converts a regular pointer to a SNES pointer. This is done by subtracting
     * 0x200 for the header and then, if before 4 MB, adding 0xC00000.
     * 
     * @param in A regular pointer
     * @return An SNES pointer
     */
    public static int toSnesPointer(int in)
    {
        in -= 0x200;
        if (in >= 0x400000)
        {
            return in;
        }
        else
            return in + 0xC00000;
    }

    /**
     * Converts a SNES pointer to a regular pointer. This is done by adding
     * 0x200 for the header and then, if at least 0xC00000, subtracting
     * 0xC00000.
     * 
     * @param in An SNES pointer
     * @return A regular pointer
     */
    public static int toRegPointer(int in)
    {
        in += 0x200;
        if (in >= 0xC00200)
            return in - 0xC00000;
        else
            return in;
    }

    /**
     * Removes <b>all </b> spaces from a <code>String</code>. Use
     * <code>String.trim()</code> if you want to remove spaces from the start
     * and end of a <code>String</code>.
     * 
     * @param in <code>String</code> to remove spaces from.
     * @return A new <code>String</code> with the contents of <code>in</code>
     *         without any spaces.
     */
    public static String killSpaces(String in) //removes _ALL_ spaces from a
    // string
    {
        String out = new String();
        StringTokenizer st = new StringTokenizer(in, " ");
        while (st.hasMoreTokens())
        {
            out += st.nextToken();
        }
        return out;
    }

    /**
     * Adds the specified element to a <code>String[]</code> by adding 1 to
     * its length.
     * 
     * @param in Array to be added to
     * @param arg <code>String</code> to add to the array
     * @return A <code>String[]</code> with a length one greater.
     * @deprecated Use <code>ArrayList</code>
     */
    public static String[] addToArr(String[] in, String arg)
    {
        String[] out = new String[in.length + 1];
        for (int i = 0; i < in.length; i++)
        {
            out[i] = in[i];
        }
        out[in.length] = arg;
        return out;
    }

    /**
     * Abstract <code>ComboBoxModel</code> for convenance use for creating
     * simple <code>ComboBoxModel</code>'s simply. This is used by the
     * <code>createComboBoxModel()</code> and <code>createComboBox</code>
     * methods for that purpose.
     * 
     * @author AnyoneEB
     * @see HackModule#createComboBoxModel(Object[], boolean, String)
     * @see HackModule#createComboBox(Object[], boolean, String, ActionListener)
     */
    protected static abstract class SimpleComboBoxModel extends
        DefaultComboBoxModel implements ListDataListener
    {
        protected ArrayList listDataListeners = new ArrayList();
        protected Object selectedItem = null;
        /** Offset of this list from the array it represents. */
        protected int offset = 0; //

        /**
         * @param offset The offset to set.
         */
        public void setOffset(int offset)
        {
            this.offset = offset;
        }

        /**
         * @return Returns the offset.
         */
        public int getOffset()
        {
            return offset;
        }

        /**
         *  
         */
        public SimpleComboBoxModel()
        {}

        public void contentsChanged(ListDataEvent lde)
        {
            this.fireContentsChanged(lde.getSource(), lde.getIndex0(), lde
                .getIndex1());
            this.notifyListDataListeners(new ListDataEvent(this, lde.getType(),
                lde.getIndex0() + offset, lde.getIndex1() + offset));
        }

        public void intervalAdded(ListDataEvent arg0)
        {}

        public void intervalRemoved(ListDataEvent arg0)
        {}

        public Object getSelectedItem()
        {
            return selectedItem;
        }

        public void setSelectedItem(Object obj)
        {
            this.selectedItem = obj;
        }

        public void addListDataListener(ListDataListener ldl)
        {
            listDataListeners.add(ldl);
        }

        public void removeListDataListener(ListDataListener ldl)
        {
            listDataListeners.remove(ldl);
        }

        public void notifyListDataListeners(ListDataEvent lde)
        {
            for (Iterator i = listDataListeners.iterator(); i.hasNext();)
            {
                ((ListDataListener) i.next()).contentsChanged(lde);
            }
        }
    }

    /**
     * Creates a <code>ComboBoxModel</code> for an array. The
     * <code>toString()</code> method will be called on all elements of the
     * array, so only <code>String</code>'s will be seen as elements. This
     * calls {@link #addDataListener(Object[], ListDataListener)}so that
     * {@link #notifyDataListeners(Object[], ListDataEvent)}can be used to
     * notify it of updates. If <code>zeroBased</code> is true, the array
     * values will be pushed up by one (to offsets <code>1</code> to
     * <code>array.length</code>). Offset <code>0</code> will be set to
     * <code>zeroString</code>.
     * 
     * @param array <code>Object</code>'s whose <code>.toString()</code>
     *            method will give the <code>String</code> to use for the
     *            corresponding position in the <code>ComboBoxModel</code>.
     * @param zeroBased If false, the values of <code>array</code> are pushed
     *            up one and <code>zeroString</code> is used for the zero
     *            value.
     * @param zeroString <code>String</code> to use as element zero if
     *            <code>zeroBased</code> is false.
     * @return a <code>ComboBoxModel</code> for the specified array
     * @see SimpleComboBoxModel
     * @see #createComboBox(Object[], boolean, String, ActionListener)
     * @see #createComboBoxModel(Object[], String)
     */
    public static SimpleComboBoxModel createComboBoxModel(final Object[] array,
        final boolean zeroBased, final String zeroString)
    {
        SimpleComboBoxModel out = new SimpleComboBoxModel()
        {
            boolean zb = zeroBased;

            public int getSize()
            {
                return array.length + offset;
            }

            public Object getElementAt(int i)
            {
                String out;
                try
                {
                    if (i == 0 && !zb)
                        out = zeroString;
                    else
                        out = array[i - offset].toString();
                }
                catch (NullPointerException e)
                {
                    out = zeroString;
                }
                return HackModule.getNumberedString(out, i);
            }
        };
        addDataListener(array, out);
        out.setOffset(zeroBased ? 0 : 1);

        return out;
    }

    /**
     * Creates a <code>ComboBoxModel</code> for an array. The
     * <code>toString()</code> method will be called on all elements of the
     * array, so only <code>String</code>'s will be seen as elements. This
     * calls {@link #addDataListener(Object[], ListDataListener)}so that
     * {@link #notifyDataListeners(Object[], ListDataEvent)}can be used to
     * notify it of updates. If <code>zeroString</code> is not null, the array
     * values will be pushed up by one (to offsets <code>1</code> to
     * <code>array.length</code>). Offset <code>0</code> will be set to
     * <code>zeroString</code>.
     * 
     * @param array <code>Object</code>'s whose <code>.toString()</code>
     *            method will give the <code>String</code> to use for the
     *            corresponding position in the <code>ComboBoxModel</code>.
     * @param zeroString <code>null</code> if array should start at element
     *            zero, otherwise <code>String</code> to use as element zero.
     * @return a <code>ComboBoxModel</code> for the specified array
     * @see SimpleComboBoxModel
     * @see #createComboBox(Object[], boolean, String, ActionListener)
     * @see #createComboBoxModel(Object[], boolean, String)
     */
    public static SimpleComboBoxModel createComboBoxModel(final Object[] array,
        final String zeroString)
    {
        return createComboBoxModel(array, zeroString == null, zeroString);
    }
    /**
     * <code>ListDataListener<code>'s used to notify <code>ComboBoxModel</code>'s
     * created by <code>createComboBoxModel()</code> of changes.
     * 
     * @see HackModule.SimpleComboBoxModel
     * @see #createComboBox(Object[], boolean, String, ActionListener)
     * @see #createComboBoxModel(Object[], boolean, String)
     * @see #addDataListener(Object[], ListDataListener)
     * @see #removeDataListener(Object[], ListDataListener)
     * @see #notifyDataListeners(Object[], ListDataEvent)
     */
    private static Hashtable comboBoxListeners = new Hashtable();

    /**
     * Retrives the <code>List</code> containing the
     * <code>ListDataListener</code>'s for the specified array. If it does
     * not exist, it will be created and added to {@link #comboBoxListeners}.
     * Since this is an object, modifying (by adding elements to) the returned
     * value will modify the value in <code>comboBoxListeners</code>.
     * 
     * @param array array to get listeners for
     * @return <code>List</code> of the <code>ListDataListeners</code> for
     *         <code>array</code>
     */
    private static List getListeners(Object[] array)
    {
        Object obj = comboBoxListeners.get(array);
        if (obj == null)
        {
            obj = new ArrayList();
            comboBoxListeners.put(array, obj);
        }
        return (List) obj;
    }

    /**
     * Adds a <code>ListDataListener</code> to <code>array</code>. This
     * should be called by a <code>ComboBoxModel</code> representing
     * <code>array</code> so it can be notified of changes to
     * <code>array</code> when <code>notifyDataListeners()</code> is called.
     * 
     * @param array array to add listener for
     * @param ldl <code>ListDataListener</code> to listen to changes in
     *            <code>array</code>
     * @see ListDataListener
     * @see #removeDataListener(Object[], ListDataListener)
     * @see #notifyDataListeners(Object[], ListDataEvent)
     */
    protected static void addDataListener(Object[] array, ListDataListener ldl)
    {
        getListeners(array).add(ldl);
    }

    /**
     * Removes a <code>ListDataListener</code> from <code>array</code>.
     * 
     * @param array array to remove listener from
     * @param ldl <code>ListDataListener</code> which was listening to changes
     *            in <code>array</code>
     * @see ListDataListener
     * @see #addDataListener(Object[], ListDataListener)
     * @see #notifyDataListeners(Object[], ListDataEvent)
     */
    protected static void removeDataListener(Object[] array,
        ListDataListener ldl)
    {
        getListeners(array).remove(ldl);
    }

    /**
     * Notifies all listeners of <code>array</code> that a change has occured.
     * This should be called any time an array which has combo boxes displaying
     * it is modified. <code>lde</code> should include the details as
     * described in {@link ListDataEvent}. It is suggested that the other
     * <code>notifyDataListeners()</code> methods are used for convenance.
     * 
     * @param array array change occured in
     * @param lde <code>ListDataEvent</code> containing specifics of change
     * @see ListDataEvent
     * @see #notifyDataListeners(Object[], Object, int, int)
     * @see #notifyDataListeners(Object[], Object, int)
     * @see #addDataListener(Object[], ListDataListener)
     * @see #removeDataListener(Object[], ListDataListener)
     */
    public static void notifyDataListeners(Object[] array, ListDataEvent lde)
    {
        for (Iterator i = getListeners(array).iterator(); i.hasNext();)
        {
            ((ListDataListener) i.next()).contentsChanged(lde);
        }
    }

    /**
     * Notifies all listeners of <code>array</code> that a change has occured
     * in the element range from <code>start</code> to <code>end</code>.
     * This should be called any time an array which has combo boxes displaying
     * it is modified.
     * 
     * @param array array change occured in
     * @param source <code>Object</code> which caused the change
     * @param start index of first element of <code>array</code> that changed
     * @param end index of last element of <code>array</code> that changed
     * @see #notifyDataListeners(Object[], ListDataEvent)
     * @see #notifyDataListeners(Object[], Object, int)
     * @see #addDataListener(Object[], ListDataListener)
     * @see #removeDataListener(Object[], ListDataListener)
     */
    public static void notifyDataListeners(Object[] array, Object source,
        int start, int end)
    {
        notifyDataListeners(array, new ListDataEvent(source,
            ListDataEvent.CONTENTS_CHANGED, start, end));
    }

    /**
     * Notifies all listeners of <code>array</code> that a change has occured
     * in the element <code>num</code>. This should be called any time an
     * array which has combo boxes displaying it is modified.
     * 
     * @param array array change occured in
     * @param source <code>Object</code> which caused the change
     * @param num index of the element of <code>array</code> that changed
     * @see #notifyDataListeners(Object[], ListDataEvent)
     * @see #notifyDataListeners(Object[], Object, int, int)
     * @see #addDataListener(Object[], ListDataListener)
     * @see #removeDataListener(Object[], ListDataListener)
     */
    public static void notifyDataListeners(Object[] array, Object source,
        int num)
    {
        notifyDataListeners(array, source, num, num);
    }

    /**
     * Creates a <code>JComboBox</code> showing the elements of
     * <code>array</code> as numbered <code>String</code>s. Since this adds
     * a <code>ListDataListener</code> using
     * {@link HackModule#addDataListener(Object[], ListDataListener)}, the
     * {@link #notifyDataListeners(Object[], ListDataEvent)}methods can be used
     * to force it to be redrawn. If <code>zeroBased</code> is false, the
     * array values will be pushed up by one (to offsets <code>1</code> to
     * <code>array.length</code>). Offset <code>0</code> will be set to
     * <code>zeroString</code>. If not null, <code>al</code> will be added
     * as an <code>ActionListener</code> to the return value.
     * 
     * @param array <code>Object</code>'s whose <code>.toString()</code>
     *            method will give the <code>String</code> to use for the
     *            corresponding position in the <code>JComboBox</code>.
     * @param zeroBased If false, the values of <code>array</code> are pushed
     *            up one and <code>zeroString</code> is used for the zero
     *            value.
     * @param zeroString <code>String</code> to use as element zero if
     *            <code>zeroBased</code> is false.
     * @param al If not null, <code>JComboBox.addActionListener(al);</code>
     *            will be called.
     * @return a <code>JComboBox</code> displaying <code>array</code> as
     *         <code>String</code>'s with any offset determined by
     *         <code>zeroBased</code> and <code>al</code> added as its
     *         <code>ActionListener</code>
     * @see #createComboBoxModel(Object[], boolean, String)
     * @see #getNumberedString(String, int)
     * @see #addDataListener(Object[], ListDataListener)
     * @see #notifyDataListeners(Object[], ListDataEvent)
     * @see #createComboBox(Object[], String, ActionListener)
     * @see #createComboBox(Object[], boolean, ActionListener)
     * @see #createComboBox(Object[], ActionListener)
     * @see #createComboBox(Object[], String)
     * @see #createComboBox(Object[])
     */
    public static JComboBox createComboBox(Object[] array, boolean zeroBased,
        String zeroString, final ActionListener al)
    {
        SimpleComboBoxModel model = createComboBoxModel(array, zeroBased,
            zeroString);
        final JComboBox out = new JComboBox(model);
        if (al != null)
            out.addActionListener(al);
        ListDataListener ldl = new ListDataListener()
        {

            public void contentsChanged(ListDataEvent lde)
            {
                if (out.getSelectedIndex() == -1)
                {
                    out.removeActionListener(al);
                    out.setSelectedIndex(lde.getIndex0());
                    out.addActionListener(al);
                }
            }

            public void intervalAdded(ListDataEvent arg0)
            {}

            public void intervalRemoved(ListDataEvent arg0)
            {}
        };
        model.addListDataListener(ldl);

        return out;
    }

    /**
     * Creates a <code>JComboBox</code> showing the elements of
     * <code>array</code> as numbered <code>String</code>s. Since this adds
     * a <code>ListDataListener</code> using
     * {@link HackModule#addDataListener(Object[], ListDataListener)}, the
     * {@link #notifyDataListeners(Object[], ListDataEvent)}methods can be used
     * to force it to be redrawn. If <code>zeroBased</code> is false, the
     * array values will be pushed up by one (to offsets <code>1</code> to
     * <code>array.length</code>). Offset <code>0</code> will be set to
     * "Nothing". If not null, <code>al</code> will be added as an
     * <code>ActionListener</code> to the return value.
     * 
     * @param array <code>Object</code>'s whose <code>.toString()</code>
     *            method will give the <code>String</code> to use for the
     *            corresponding position in the <code>JComboBox</code>.
     * @param zeroBased If false, the values of <code>array</code> are pushed
     *            up one and "Nothing" is used for the zero value.
     * @param al If not null, <code>JComboBox.addActionListener(al);</code>
     *            will be called.
     * @return a <code>JComboBox</code> displaying <code>array</code> as
     *         <code>String</code>'s with any offset determined by
     *         <code>zeroBased</code> and <code>al</code> added as its
     *         <code>ActionListener</code>
     * @see #createComboBoxModel(Object[], boolean, String)
     * @see #getNumberedString(String, int)
     * @see #addDataListener(Object[], ListDataListener)
     * @see #notifyDataListeners(Object[], ListDataEvent)
     * @see #createComboBox(Object[], boolean, String, ActionListener)
     * @see #createComboBox(Object[], String, ActionListener)
     * @see #createComboBox(Object[], ActionListener)
     * @see #createComboBox(Object[], String)
     * @see #createComboBox(Object[])
     */
    public static JComboBox createComboBox(Object[] array, boolean zeroBased,
        final ActionListener al)
    {
        return createComboBox(array, zeroBased, "Nothing", al);
    }

    /**
     * Creates a <code>JComboBox</code> showing the elements of
     * <code>array</code> as numbered <code>String</code>s. Since this adds
     * a <code>ListDataListener</code> using
     * {@link HackModule#addDataListener(Object[], ListDataListener)}, the
     * {@link #notifyDataListeners(Object[], ListDataEvent)}methods can be used
     * to force it to be redrawn. If <code>zeroString</code> is not null, the
     * array values will be pushed up by one (to offsets <code>1</code> to
     * <code>array.length</code>). Offset <code>0</code> will be set to
     * <code>zeroString</code>. If not null, <code>al</code> will be added
     * as an <code>ActionListener</code> to the return value.
     * 
     * @param array <code>Object</code>'s whose <code>.toString()</code>
     *            method will give the <code>String</code> to use for the
     *            corresponding position in the <code>JComboBox</code>.
     * @param zeroString <code>String</code> to use as element zero if it is
     *            not null.
     * @param al If not null, <code>JComboBox.addActionListener(al);</code>
     *            will be called.
     * @return a <code>JComboBox</code> displaying <code>array</code> as
     *         <code>String</code>'s with any offset determined by
     *         <code>zeroString</code> and <code>al</code> added as its
     *         <code>ActionListener</code>
     * @see #createComboBoxModel(Object[], boolean, String)
     * @see #createComboBoxModel(Object[], String)
     * @see #getNumberedString(String, int)
     * @see #addDataListener(Object[], ListDataListener)
     * @see #notifyDataListeners(Object[], ListDataEvent)
     * @see #createComboBox(Object[], boolean, String, ActionListener)
     * @see #createComboBox(Object[], boolean, ActionListener)
     * @see #createComboBox(Object[], ActionListener)
     * @see #createComboBox(Object[], String)
     * @see #createComboBox(Object[])
     */
    public static JComboBox createComboBox(Object[] array, String zeroString,
        final ActionListener al)
    {
        return createComboBox(array, zeroString == null, zeroString, al);
    }

    /**
     * Creates a <code>JComboBox</code> showing the elements of
     * <code>array</code> as numbered <code>String</code>s. Since this adds
     * a <code>ListDataListener</code> using
     * {@link HackModule#addDataListener(Object[], ListDataListener)}, the
     * {@link #notifyDataListeners(Object[], ListDataEvent)}methods can be used
     * to force it to be redrawn. If <code>zeroString</code> is not null, the
     * array values will be pushed up by one (to offsets <code>1</code> to
     * <code>array.length</code>). Offset <code>0</code> will be set to
     * <code>zeroString</code>.
     * 
     * @param array <code>Object</code>'s whose <code>.toString()</code>
     *            method will give the <code>String</code> to use for the
     *            corresponding position in the <code>JComboBox</code>.
     * @param zeroString <code>String</code> to use as element zero if
     *            <code>zeroBased</code> is false.
     * @return a <code>JComboBox</code> displaying <code>array</code> as
     *         <code>String</code>'s with any offset determined by
     *         <code>zeroString</code>
     * @see #createComboBoxModel(Object[], boolean, String)
     * @see #createComboBoxModel(Object[], String)
     * @see #getNumberedString(String, int)
     * @see #addDataListener(Object[], ListDataListener)
     * @see #notifyDataListeners(Object[], ListDataEvent)
     * @see #createComboBox(Object[], boolean, String, ActionListener)
     * @see #createComboBox(Object[], boolean, ActionListener)
     * @see #createComboBox(Object[], String, ActionListener)
     * @see #createComboBox(Object[], ActionListener)
     * @see #createComboBox(Object[])
     */
    public static JComboBox createComboBox(Object[] array, String zeroString)
    {
        return createComboBox(array, zeroString, null);
    }

    /**
     * Creates a <code>JComboBox</code> showing the elements of
     * <code>array</code> as numbered <code>String</code>s. Since this adds
     * a <code>ListDataListener</code> using
     * {@link HackModule#addDataListener(Object[], ListDataListener)}, the
     * {@link #notifyDataListeners(Object[], ListDataEvent)}methods can be used
     * to force it to be redrawn. If not null, <code>al</code> will be added
     * as an <code>ActionListener</code> to the return value.
     * 
     * @param array <code>Object</code>'s whose <code>.toString()</code>
     *            method will give the <code>String</code> to use for the
     *            corresponding position in the <code>JComboBox</code>.
     * @param al If not null, <code>JComboBox.addActionListener(al);</code>
     *            will be called.
     * @return a <code>JComboBox</code> displaying <code>array</code> as
     *         <code>String</code>'s with <code>al</code> added as its
     *         <code>ActionListener</code>
     * @see #createComboBoxModel(Object[], boolean, String)
     * @see #getNumberedString(String, int)
     * @see #addDataListener(Object[], ListDataListener)
     * @see #notifyDataListeners(Object[], ListDataEvent)
     * @see #createComboBox(Object[], boolean, String, ActionListener)
     * @see #createComboBox(Object[], String, ActionListener)
     * @see #createComboBox(Object[], boolean, ActionListener)
     * @see #createComboBox(Object[], String)
     * @see #createComboBox(Object[])
     */
    public static JComboBox createComboBox(Object[] array,
        final ActionListener al)
    {
        return createComboBox(array, true, null, al);
    }

    /**
     * Creates a <code>JComboBox</code> showing the elements of
     * <code>array</code> as numbered <code>String</code>s. Since this adds
     * a <code>ListDataListener</code> using
     * {@link HackModule#addDataListener(Object[], ListDataListener)}, the
     * {@link #notifyDataListeners(Object[], ListDataEvent)}methods can be used
     * to force it to be redrawn.
     * 
     * @param array <code>Object</code>'s whose <code>.toString()</code>
     *            method will give the <code>String</code> to use for the
     *            corresponding position in the <code>JComboBox</code>.
     * @return a <code>JComboBox</code> displaying <code>array</code> as
     *         <code>String</code>'s
     * @see #createComboBoxModel(Object[], boolean, String)
     * @see #getNumberedString(String, int)
     * @see #addDataListener(Object[], ListDataListener)
     * @see #notifyDataListeners(Object[], ListDataEvent)
     * @see #createComboBox(Object[], boolean, String, ActionListener)
     * @see #createComboBox(Object[], String, ActionListener)
     * @see #createComboBox(Object[], boolean, ActionListener)
     * @see #createComboBox(Object[], String)
     * @see #createComboBox(Object[], ActionListener)
     */
    public static JComboBox createComboBox(Object[] array)
    {
        return createComboBox(array, (ActionListener) null);
    }

    //big array init
    /**
     * Returns the base directory most files are located at. Ends with a
     * separator.
     */
    public String getDefaultBaseDir()
    {
        return "net" + File.separator + "starmen" + File.separator + "pkhack"
            + File.separator;
    }

    /**
     * Reads a file into an array. The existance of a ROM-specific file will be
     * checked before reading the default file. First
     * <code>rompath.filename</code> will be searched for a ROM-specific file.
     * If there is no ROM specific file, the default file at net/starmen/pkhack/
     * <code>filename</code> will be used. The array should be in the format
     * <code>EntryNum - Entry\n</code>. All whitespace is ignored (spaces are
     * allowed within <code>Entry</code>.<code>EntryNum</code> will be
     * read as decimal or hexidecimal depending on <code>hexNum</code>.
     * 
     * @param filename where to look for the file. (rompath.filename or
     *            /net/starmen/pkhack/filename will be used)
     * @param hexNum if true entry numbers in the file will be interpreted as
     *            hex
     * @param out array data will be read into, this same array will be returned
     * @param size size of array to create if <code>out</code> is null
     * @see #readArray(String, boolean, int)
     * @see #readArray(String, boolean, String[])
     * @see #writeArray(String, boolean, String[])
     */
    public static void readArray(String baseDir, String filename,
        String romPath, boolean hexNum, String[] out, int size)
    {
        if (out == null)
            out = new String[size];
        //if the first one is null, the rest are
        if (out[0] == null)
            Arrays.fill(out, new String());
        try
        {
            String[] list;
            try
            {
                if (romPath == null)
                    throw new NullPointerException(
                        "You will never see this text.");
                list = new CommentedLineNumberReader(new FileReader((romPath
                    + "." + filename))).readUsedLines();
            }
            catch (Exception e1)
            {
                //If file in directory of ROM doesn't exist,
                //then read the default file.
                System.out.println("No ROM specific " + filename
                    + " file was found, using default (" + baseDir + filename
                    + ").");
                list = new CommentedLineNumberReader(new InputStreamReader(
                    ClassLoader.getSystemResourceAsStream(baseDir + filename)))
                    .readUsedLines();
            }
            String[] tempStr;
            for (int i = 0; i < list.length; i++)
            {
                if (list[i].substring(0, Math.min(7, list[i].length()))
                    .matches("[^#]*-[^#]*.*"))
                {
                    tempStr = list[i].split("-", 2);
                    int num = Integer.parseInt(tempStr[0].trim(), (hexNum
                        ? 16
                        : 10));
                    out[num] = tempStr[1].trim();
                }
                else
                    out[i] = list[i].trim();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Reads a file into an array. See
     * {@link #readArray(String, String, String, boolean, String[], int)}for
     * more information.
     * 
     * @param filename where to look for the file. (rompath.filename or
     *            /net/starmen/pkhack/filename will be used)
     * @param hexNum if true entry numbers in the file will be interpreted as
     *            hex
     * @param out array data will be read into, this same array will be returned
     * @see #readArray(String, boolean, int)
     * @see #readArray(String, String, String, boolean, String[], int)
     * @see #writeArray(String, boolean, String[])
     */
    public static void readArray(String baseDir, String filename,
        String romPath, boolean hexNum, String[] out)
    {
        readArray(baseDir, filename, romPath, hexNum, out, out.length);
    }

    /**
     * Reads a file into an array. See
     * {@link #readArray(String, String, String, boolean, String[], int)}for
     * more information.
     * 
     * @param baseDir directory to look in for file
     * @param filename where to look for the file. (<code>baseDir</code>+
     *            <code>filename</code> will be used)
     * @param hexNum if true entry numbers in the file will be interpreted as
     *            hex
     * @param out array data will be read into, this same array will be returned
     * @see #readArray(String, String, boolean, int)
     * @see #readArray(String, String, String, boolean, String[], int)
     * @see #writeArray(String, boolean, String[])
     */
    public static void readArray(String baseDir, String filename,
        boolean hexNum, String[] out)
    {
        readArray(baseDir, filename, null, hexNum, out, out.length);
    }

    /**
     * Reads a file into an array. See
     * {@link #readArray(String, String, String, boolean, String[], int)}for
     * more information.
     * 
     * @param filename where to look for the file. (rompath.filename or
     *            /net/starmen/pkhack/filename will be used)
     * @param hexNum if true entry numbers in the file will be interpreted as
     *            hex
     * @param out array data will be read into, this same array will be returned
     * @see #readArray(String, boolean, int)
     * @see #readArray(String, String, String, boolean, String[], int)
     * @see #writeArray(String, boolean, String[])
     */
    public void readArray(String filename, boolean hexNum, String[] out)
    {
        readArray(getDefaultBaseDir(), filename, null, hexNum, out, out.length);
    }

    /**
     * Reads a file into an array. Creates a new array with length
     * <code>size</code> to read data into. See
     * {@link #readArray(String, String, String, boolean, String[], int)}for
     * more information.
     * 
     * @param filename where to look for the file. (rompath.filename or
     *            /net/starmen/pkhack/filename will be used)
     * @param hexNum if true entry numbers in the file will be interpreted as
     *            hex
     * @param size size of array to create to read data into
     * @return <code>String[size]</code> containing information from file
     * @see #readArray(String, boolean, String[])
     * @see #readArray(String, String, String, boolean, String[], int)
     * @see #writeArray(String, boolean, String[])
     */
    public static String[] readArray(String baseDir, String filename,
        String romPath, boolean hexNum, int size)
    {
        String[] out = new String[size];
        readArray(baseDir, filename, romPath, hexNum, out, size);
        return out;
    }

    /**
     * Reads a file into an array. Creates a new array with length
     * <code>size</code> to read data into. See
     * {@link #readArray(String, String, String, boolean, String[], int)}for
     * more information.
     * 
     * @param baseDir directory to look in for file
     * @param filename where to look for the file. (<code>baseDir</code>+
     *            <code>filename</code> will be used)
     * @param hexNum if true entry numbers in the file will be interpreted as
     *            hex
     * @param size size of array to create to read data into
     * @return <code>String[size]</code> containing information from file
     * @see #readArray(String, String, boolean, String[])
     * @see #readArray(String, String, String, boolean, String[], int)
     * @see #writeArray(String, boolean, String[])
     */
    public static String[] readArray(String baseDir, String filename,
        boolean hexNum, int size)
    {
        String[] out = new String[size];
        readArray(baseDir, filename, null, hexNum, out, size);
        return out;
    }

    /**
     * Reads a file into an array. Creates a new array with length
     * <code>size</code> to read data into. See
     * {@link #readArray(String, String, String, boolean, String[], int)}for
     * more information.
     * 
     * @param filename where to look for the file. (
     *            <code>getDefaultBaseDir()</code>+<code>filename</code>
     *            will be used)
     * @param hexNum if true entry numbers in the file will be interpreted as
     *            hex
     * @param size size of array to create to read data into
     * @return <code>String[size]</code> containing information from file
     * @see #readArray(String, String, String, boolean, String[])
     * @see #readArray(String, String, String, boolean, String[], int)
     * @see #writeArray(String, boolean, String[])
     * @see #getDefaultBaseDir()
     */
    public String[] readArray(String filename, boolean hexNum, int size)
    {
        String[] out = new String[size];
        readArray(getDefaultBaseDir(), filename, null, hexNum, out, size);
        return out;
    }

    /**
     * Writes an array to a ROM-specific file. File will be written to
     * <code>rompath.filename</code>. Format is same as that which
     * {@link #readArray(String, String, String, boolean, String[], int)}reads.
     * 
     * @param filename base filename to write to. File will actually be written
     *            to <code>rompath.filename</code>.
     * @param hexNum if true entry numbers will be written in hexidecimal, if
     *            false they will be in decimal
     * @param in data to write to file
     * @see #readArray(String, String, String, boolean, int)
     * @see #readArray(String, String, String, boolean, String[])
     * @see #readArray(String, String, String, boolean, String[], int)
     */
    public void writeArray(String filename, boolean hexNum, String[] in)
    {
        String output = new String();
        for (int i = 0; i < in.length; i++)
        {
            output += (in[i].toString().length() > 0 ? padString(Integer
                .toString(i, (hexNum ? 16 : 10)), 3, ' ')
                + " - " + in[i] + "\n" : "");
        }

        try
        {
            FileWriter out = new FileWriter(rom.getPath() + "." + filename);
            out.write(output);
            out.close();
        }
        catch (FileNotFoundException e)
        {
            System.out.println("Error: File not saved: File not found.");
            e.printStackTrace();
            return;
        }
        catch (IOException e)
        {
            System.out.println("Error: File not saved: Could write file.");
            e.printStackTrace();
            return;
        }
    }

    public static int numberize(String ib)
    {
        String in = ib;
        //remove preceding spaces
        for (int i = 0; i < in.length(); i++)
        {
            if ((in.charAt(i) < '9') && (in.charAt(i) > '0'))
            {
                in = in.substring(i);
                break;
            }
        }
        //get number
        for (int i = 0; i < in.length(); i++)
        {
            if (!((in.charAt(i) < '9') && (in.charAt(i) > '0')))
            {
                if (i == 0)
                {
                    return 0;
                }
                return Integer.parseInt(in.substring(0, i));
            }
        }
        if (in.equals(new String("")))
            return 0;
        else
            return Integer.parseInt(in);
    }
}