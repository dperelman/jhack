/*
 * Created on Mar 30, 2003
 */
package net.starmen.pkhack;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;

/**
 * A wrapper for a <code>JComboBox</code> that has a text field and a search
 * button.
 * 
 * @author AnyoneEB
 * @see #comboBox
 */
public class JSearchableComboBox
	extends JComponent
	implements ActionListener, KeyListener
{
	/**
	 * The JComboBox this is a wrapper for.
	 */
	public JComboBox comboBox;
	private JTextField tf;
	private JButton findb = new JButton("Find");
	private JLabel label;
	/**
	 * Creates a new JSearchableComboBox wrapper for the specified JComboBox.
	 * 
	 * @param jcb The JComboBox this is a wrapper for.
	 * @param text Text to label the JComboBox with.
	 * @param searchSize The size of the search text field
	 */
	public JSearchableComboBox(JComboBox jcb, String text, int searchSize)
	{
		this(jcb, text, searchSize, true);
	}

	/**
	 * Creates a new JSearchableComboBox wrapper for the specified JComboBox.
	 * 
	 * @param jcb The JComboBox this is a wrapper for.
	 * @param text Text to label the JComboBox with.
	 */
	public JSearchableComboBox(JComboBox jcb, String text)
	{
		this(jcb, text, true);
	}
	
	/**
	 * Creates a new JSearchableComboBox wrapper for the specified JComboBox.
	 * 
	 * @param jcb The JComboBox this is a wrapper for.
	 * @param text Text to label the JComboBox with.
	 * @param searchLeft If true, search box is left of combo box
	 */
	public JSearchableComboBox(JComboBox jcb, String text, boolean searchLeft)
	{
		this(jcb, text, 10, searchLeft);
	}
	
	/**
	 * Creates a new JSearchableComboBox wrapper for the specified JComboBox.
	 * 
	 * @param jcb The JComboBox this is a wrapper for.
	 * @param text Text to label the JComboBox with.
	 * @param searchSize The size of the search text field
	 * @param searchLeft If true, search box is left of combo box
	 */
	public JSearchableComboBox(JComboBox jcb, String text, int searchSize, boolean searchLeft)
	{
		super();
		this.comboBox = jcb;
		this.tf = new JTextField(searchSize);
		this.label = new JLabel(text);
		this.initGraphics(true);
	}

	private void initGraphics(boolean searchLeft)
	{
		setLayout(new BorderLayout());

		findb.addActionListener(this);
		tf.addKeyListener(this);

		add(
			HackModule.pairComponents(tf, findb, true),
			(searchLeft ? BorderLayout.WEST : BorderLayout.EAST));
		add(
			HackModule.pairComponents(label, comboBox, true),
			(searchLeft ? BorderLayout.EAST : BorderLayout.WEST));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent ae)
	{
		HackModule.search(tf.getText().toLowerCase(), comboBox);
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
	{
		if (ke.getKeyCode() == KeyEvent.VK_ENTER)
		{
			HackModule.search(tf.getText(), comboBox);
		}
	}
	public void setEnabled(boolean enable)
	{
	    super.setEnabled(enable);
	    if(enable)
	    {
			tf.setEnabled(true);
			findb.setEnabled(true);
			comboBox.setEnabled(true);
			label.setEnabled(true);
	    }
	    else
	    {
			tf.setEnabled(false);
			findb.setEnabled(false);
			comboBox.setEnabled(false);
			label.setEnabled(false);
	    }
	}

}
