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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.Date;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Creates a window to graphicly display the output of a
 * <code>OutputStream</code>. Intended for making a window to show a user the
 * console output (STD_OUT/STD_ERR).
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
    private boolean error = false, send = false, sendi;
    private JDialog einfodia;

    /**
     * @return Returns the PrintStream.
     */
    public PrintStream getPrintStream()
    {
        return errps;
    }

    public OutputStreamViewer(final String title, final String date,
        final String ext)
    {
        OutputStream errout;
        final PipedInputStream in;
        try
        {
            final DateFormat dfi = DateFormat.getInstance();
            String logdir = new StringBuffer().append(
                JHack.JHACK_DIR.toString()).append(File.separator).append(
                "logs").toString();
            File logdirf = new File(logdir);
            if (!logdirf.exists())
                logdirf.mkdir();
            String logfile = new StringBuffer().append(logdir).append(
                File.separator).append(date).append(".").append(ext).append(
                ".log").toString();
            FileWriter l = null;
            try
            {
                l = new FileWriter(logfile);
            }
            catch (IOException fioe)
            {
                JOptionPane.showMessageDialog(null, "Unable to open file "
                    + logfile + "\n for writing.", "Logging error",
                    JOptionPane.WARNING_MESSAGE);
            }
            final FileWriter log = (l == null ? null : l);
            //            PipedInputStream tmp;
            //            in = new InputStreamReader(tmp = new PipedInputStream());
            errout = new PipedOutputStream(in = new PipedInputStream());
            errps = new PrintStream(errout);

            errDia = new JFrame(title);
            errArea = new JTextArea(20, 60);
            errArea.setEditable(false);
            errArea.append(dfi.format(new Date()) + ": ");
            errDia.getContentPane().setLayout(new BorderLayout());
            errDia.getContentPane().add(new JScrollPane(errArea),
                BorderLayout.CENTER);
            JButton close = new JButton("Close");
            errDia.addWindowListener(new WindowAdapter()
            {
                public void windowClosing(WindowEvent e)
                {
                    errArea.append("\n\n------------------------\n"
                        + "Window closed." + "\n------------------------\n\n");
                }
            });
            close.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    errDia.setVisible(false);
                    errArea.append("\n\n------------------------\n"
                        + "Window closed." + "\n------------------------\n\n");
                }
            });
            errDia.getContentPane().add(close, BorderLayout.SOUTH);
            errDia.pack();

            t = new Thread()
            {
                StringBuffer sb = new StringBuffer();
                int wait = 0;

                private void appendToErrArea()
                {
                    String sbs = sb.toString();
                    errArea.append(sbs);
                    if (log != null)
                    {
                        try
                        {
                            log.write(sbs);
                            log.flush();
                        }
                        catch (IOException e)
                        {
                            System.out.println("An error occured writing "
                                + "to log file.");
                        }
                    }
                    sb = new StringBuffer();
                    if (!errDia.isVisible() && isEnabled())
                    {
                        errDia.setVisible(true);
                        Rectangle r = errArea.getBounds();
                        errArea.scrollRectToVisible(new Rectangle(0, r.height,
                            1, 1));
                    }
                }

                public void run()
                {
                    while (run)
                    {
                        try
                        {
                            if (in.available() > 0)
                            {
                                wait = 0;
                                char c = (char) in.read();
                                sb.append(Character.toString(c));
                                if (c == '\n')
                                {
                                    sb.append(dfi.format(new Date()) + ": ");
                                    send = error; //send if this is error
                                    // output
                                    appendToErrArea();
                                }
                            }
                            else
                            {
                                if (sb.length() > 0)
                                    appendToErrArea();
                                if (send)
                                {
                                    wait++;
                                    /*
                                     * Output has to stop for 4 seconds before
                                     * it is considered finished.
                                     */
                                    if (wait > 2)
                                    {
                                        sendError();
                                        send = false;
                                        wait = 0;
                                    }
                                }
                                sleep(2000);
                            }
                        }
                        catch (Exception e)
                        {
                            //e.printStackTrace(System.err);
                        }
                    }
                    try
                    {
                        log.close();
                    }
                    catch (IOException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
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

    private void sendError()
    {
        try
        {
            URL url = new URL("http://anyoneeb.ath.cx:83/jhack/reporterror");
            URLConnection connection = url.openConnection();
            connection.setDoOutput(true);

            PrintWriter out = new PrintWriter(connection.getOutputStream());
            out.print("version="
                + URLEncoder.encode(MainGUI.getVersion(), "UTF-8"));
            out.print("&outlog="
                + URLEncoder.encode(JHack.out.getText(), "UTF-8"));
            out.print("&errlog="
                + URLEncoder.encode(JHack.err.getText(), "UTF-8"));
            out.close();

            BufferedReader in = new BufferedReader(new InputStreamReader(
                connection.getInputStream()));
            String inputLine;
            StringBuffer input = new StringBuffer();

            while ((inputLine = in.readLine()) != null)
                input.append(inputLine).append('\n');

            int il = input.length();
            if (il > 0)
                input.deleteCharAt(il - 1);
            String instr = input.toString();

            in.close();

            try
            {
                int id;
                try
                {
                    id = Integer.parseInt(instr);
                }
                catch (NumberFormatException nfe)
                {
                    System.out
                        .println("WARNING: Reading integer failed first time, "
                            + "trying to remove whitespace.");
                    id = Integer.parseInt(instr.trim());
                }
                einfodia = new JDialog(JHack.main.getMainWindow(),
                    "Error Report", true);
                JButton sendb = new JButton("Send"), nsendb = new JButton(
                    "Don't Send");
                sendi = true;
                sendb.addActionListener(new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        sendi = true;
                        einfodia.hide();
                    }
                });
                nsendb.addActionListener(new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        sendi = false;
                        einfodia.hide();
                    }
                });
                JTextField usertf = new JTextField(20);
                JTextArea comment = new JTextArea(5, 70);

                einfodia.getContentPane().setLayout(new BorderLayout());
                einfodia.getContentPane().add(
                    HackModule.createFlowLayout(new JButton[]{sendb, nsendb}),
                    BorderLayout.SOUTH);

                Box entry = new Box(BoxLayout.Y_AXIS);
                entry.add(HackModule.createFlowLayout(new JLabel(
                    "An error has occured in JHack. "
                        + "Please enter your username and any "
                        + "information related to the error.",
                    SwingConstants.CENTER)));
                entry.add(HackModule.getLabeledComponent("Username: ", usertf));
                entry.add(HackModule.getLabeledComponent("Comment: ",
                    new JScrollPane(comment)));

                einfodia.getContentPane().add(entry, BorderLayout.CENTER);
                einfodia.pack();
                einfodia.show();
                if (sendi)
                {
                    URL urlu = new URL(
                        "http://anyoneeb.ath.cx:83/jhack/updateerror");
                    URLConnection connectionu = urlu.openConnection();
                    connectionu.setDoOutput(true);

                    PrintWriter outu = new PrintWriter(connectionu
                        .getOutputStream());
                    outu.print("id="
                        + URLEncoder.encode(Integer.toString(id), "UTF-8"));
                    outu.print("&user="
                        + URLEncoder.encode(usertf.getText(), "UTF-8"));
                    outu.print("&comment="
                        + URLEncoder.encode(comment.getText(), "UTF-8"));
                    outu.close();

                    BufferedReader inu = new BufferedReader(
                        new InputStreamReader(connectionu.getInputStream()));
                    input = new StringBuffer();

                    while ((inputLine = inu.readLine()) != null)
                        input.append(inputLine).append('\n');

                    il = input.length();
                    if (il > 0)
                        input.deleteCharAt(il - 1);
                    String uerrstr = input.toString();

                    inu.close();

                    if (uerrstr.trim().length() > 0)
                    {
                        JTextArea jta = new JTextArea("An error occured while "
                            + "reporting an error:\n" + uerrstr);
                        jta.setEditable(false);
                        JOptionPane.showMessageDialog(null, jta,
                            "Error Reporting Error",
                            JOptionPane.WARNING_MESSAGE);
                    }
                }
            }
            catch (NumberFormatException nfe)
            {
                JTextArea jta = new JTextArea("An error occured while "
                    + "reporting an error:\n" + instr);
                jta.setEditable(false);
                JOptionPane.showMessageDialog(null, jta,
                    "Error Reporting Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        catch (IOException ioe)
        {
            JTextArea jta = new JTextArea("An error occured while "
                + "reporting an error:\n" + ioe.getClass() + ": "
                + ioe.getMessage());
            jta.setEditable(false);
            JOptionPane.showMessageDialog(null, jta, "Error Reporting Error",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
    }

    public void setError(boolean err)
    {
        error = true;
    }

    public boolean isError()
    {
        return error;
    }

    public String getText()
    {
        return errArea.getText();
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