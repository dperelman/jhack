package net.starmen.pkhack;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * A wrapper for a <code>JComboBox</code> that has a text field. 
 * Whenever the text field is changed, the JComboBox is changed to reflect it;
 * and vice versa.
 * 
 * @author EBisumaru
 * @see #comboBox
 */
public class AutoSearchBox extends JComponent implements ActionListener, KeyListener
{
	/**
	 * The JComboBox this is a wrapper for.
	 */
	private JComboBox comboBox;
	private JTextField tf;
	private JLabel label;
	private boolean incTf,//include the text field
		corr = true; //correlate the text box and combo box
	private int size;
	/**
	 * Creates a new AutoSearchBox wrapper for the specified JComboBox.
	 * 
	 * @param jcb The JComboBox this is a wrapper for.
	 * @param text Text to label the JComboBox with.
	 * @param searchSize The size of the search text field
	 */
	public AutoSearchBox(JComboBox jcb, String text, int searchSize)
	{
		this(jcb, text, searchSize, true);
	}

	/**
	 * Creates a new AutoSearchBox wrapper for the specified JComboBox.
	 * 
	 * @param jcb The JComboBox this is a wrapper for.
	 * @param text Text to label the JComboBox with.
	 */
	public AutoSearchBox(JComboBox jcb, String text)
	{
		this(jcb, text, true);
	}
	
	/**
	 * Creates a new AutoSearchBox wrapper for the specified JComboBox.
	 * 
	 * @param jcb The JComboBox this is a wrapper for.
	 * @param text Text to label the JComboBox with.
	 * @param searchLeft If true, search box is left of combo box
	 */
	public AutoSearchBox(JComboBox jcb, String text, boolean searchLeft)
	{
		this(jcb, text, 10, searchLeft);
	}
	
	/**
	 * Creates a new AutoSearchBox wrapper for the specified JComboBox.
	 * 
	 * @param jcb The JComboBox this is a wrapper for.
	 * @param text Text to label the JComboBox with.
	 * @param searchSize The size of the search text field
	 * @param searchLeft If true, search box is left of combo box
	 */
	public AutoSearchBox(JComboBox jcb, String text, int searchSize, boolean searchLeft)
	{
		super();
		this.comboBox = jcb;
		comboBox.setActionCommand("effectSel");
		comboBox.addActionListener(this);
		size = searchSize;
		incTf = true;
		this.tf = new JTextField(size);
		tf.getDocument().addDocumentListener(new DocumentListener()
				{
			public void changedUpdate(DocumentEvent de)
			{
			}
			public void insertUpdate(DocumentEvent de)
			{
				tfChanged();
			}
			public void removeUpdate(DocumentEvent de)
			{
				tfChanged();
			}
		});
		this.label = new JLabel(text);
		this.initGraphics(searchLeft);
	}

	/**
	 * Makes the specified JTextField a new AutoSearchBox wrapper for the 
	 * specified JComboBox.
	 * 
	 * @param jcb The JComboBox this is a wrapper for.
	 * @param text Text to label the JComboBox with.
	 * @param searchSize The size of the search text field
	 * @param searchLeft If true, search box is left of combo box
	 */
	public AutoSearchBox(JComboBox jcb, JTextField jtf, String text, 
		boolean searchLeft, boolean incTf)
	{
		super();
		this.comboBox = jcb;
		comboBox.setActionCommand("effectSel");
		comboBox.addActionListener(this);
		this.tf = jtf;
		this.incTf = incTf;
		tf.getDocument().addDocumentListener(new DocumentListener()
				{
			public void changedUpdate(DocumentEvent de)
			{
			}
			public void insertUpdate(DocumentEvent de)
			{
				tfChanged();
			}
			public void removeUpdate(DocumentEvent de)
			{
				tfChanged();
			}
		});
		this.label = new JLabel(text);
		this.initGraphics(searchLeft);
	}

	private void initGraphics(boolean searchLeft)
	{
		setLayout(new BorderLayout());

		tf.addKeyListener(this);

		if(incTf)
			add(tf,	(searchLeft ? BorderLayout.WEST : BorderLayout.EAST));
		add(HackModule.pairComponents(label, comboBox, true),
				(searchLeft ? BorderLayout.EAST : BorderLayout.WEST));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent ae)
	{
		tf.setText(comboBox.getSelectedItem().toString());	
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
	 */
	public void keyTyped(KeyEvent ke)
	{}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
	 */
	public void keyPressed(KeyEvent ke)
	{}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
	 */
	public void keyReleased(KeyEvent ke)
	{}
	public void setEnabled(boolean enable)
	{
		super.setEnabled(enable);
		if(enable)
		{
			tf.setEnabled(true);
			comboBox.setEnabled(true);
			label.setEnabled(true);
		}
		else
		{
			tf.setEnabled(false);
			comboBox.setEnabled(false);
			label.setEnabled(false);
		}
	}
	
	public void tfChanged()
	{
		if(corr)
		{
			comboBox.removeActionListener(this);
			if (!search(tf.getText(), comboBox, true, false))
			{	
				comboBox.addItem(tf.getText() + " ???");
				comboBox.setSelectedItem(tf.getText() + " ???");
			}
			comboBox.addActionListener(this);
		}
	}
	
	public String getText()
	{
		return tf.getText();
	}
	
	public void setText(String text)
	{
		tf.setText(text);
	}
	
	public JTextField getTF()
	{
		return tf;
	}
	
	public JComboBox getCB()
	{
		return comboBox;
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
	
	public void setCorr(boolean _) {corr = _;}
	
	public boolean getCorr() {return corr;}
}


