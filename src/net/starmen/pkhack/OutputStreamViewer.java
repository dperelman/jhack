/*
 * Created on Apr 29, 2004
 */
package net.starmen.pkhack;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.text.DateFormat;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * TODO Write javadoc for this class
 * 
 * @author AnyoneEB
 */
public class OutputStreamViewer
{
    private PrintStream errps;
    private JFrame errDia;
    private JTextArea errArea;
    private Thread t;
    private boolean enabled = true;
    private boolean run = true;

    /**
     * @return Returns the PrintStream.
     */
    public PrintStream getPrintStream()
    {
        return errps;
    }

    public OutputStreamViewer(final String title)
    {
        OutputStream errout;
        final PipedInputStream in;
        try
        {
            //            PipedInputStream tmp;
            //            in = new InputStreamReader(tmp = new PipedInputStream());
            errout = new PipedOutputStream(in = new PipedInputStream());
            errps = new PrintStream(errout);

            
            errDia = new JFrame(title);
            errArea = new JTextArea(20, 60);
            errArea.setEditable(false);
            errArea.append(DateFormat.getInstance().format(new Date())
                + ": ");
            errDia.getContentPane().setLayout(new BorderLayout());
            errDia.getContentPane().add(new JScrollPane(errArea),
                BorderLayout.CENTER);
            JButton close = new JButton("Close");
            errDia.addWindowListener(new WindowAdapter()
            {
                public void windowClosing(WindowEvent e)
                {
                    errArea.append("\n\n------------------------\n"
                        + "Window closed."
                        + "\n------------------------\n\n");
                }
            });
            close.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    errDia.setVisible(false);
                    errArea.append("\n\n------------------------\n"
                        + "Window closed."
                        + "\n------------------------\n\n");
                }
            });
            errDia.getContentPane().add(close, BorderLayout.SOUTH);
            errDia.pack();
            
            
            t = new Thread()
            {
                public void run()
                {
                    while (run)
                    {
                        try
                        {
                            if (in.available() > 0)
                            {
                                char c = (char) in.read();
                                errArea.append(Character.toString(c));
                                if (c == '\n')
                                    errArea.append(DateFormat.getInstance()
                                        .format(new Date())
                                        + ": ");
                                if (!errDia.isVisible() && isEnabled())
                                {
                                    errDia.setVisible(true);
                                    Rectangle r = errArea.getBounds();
                                    errArea.scrollRectToVisible(new Rectangle(
                                        0, r.height, 1, 1));
                                }
                            }
                            else
                            {
                                sleep(2000);
                            }
                        }
                        catch (Exception e)
                        {
                            //e.printStackTrace(System.err);
                        }
                    }
                }
            };
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();
        }
        catch (IOException e)
        {
            JOptionPane.showMessageDialog(null, "Unable to init " + title + "."
                + "Subsequent messages will only be displayed to the console.",
                "Error Initing Display", JOptionPane.WARNING_MESSAGE);
            e.printStackTrace();
        }
    }

    public void updateUI()
    {
        SwingUtilities.updateComponentTreeUI(SwingUtilities.getRoot(errDia));
    }

    public void disable()
    {
        setEnabled(false);
    }

    public void enable()
    {
        setEnabled(true);
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
        errDia.setVisible(enabled);
    }

    public boolean isEnabled()
    {
        return enabled;
    }
    
    /** Stops the thread so it cannot be restarted. */
    public void stop()
    {
        run = false;
    }
}