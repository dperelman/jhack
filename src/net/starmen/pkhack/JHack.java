package net.starmen.pkhack;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Executable of JHack. Run this to start JHack.
 * 
 * @author AnyoneEB
 */
//Made by AnyoneEB.
//Code released under the GPL - http://www.gnu.org/licenses/gpl.txt
public class JHack
{
    public static MainGUI main;
    public static OutputStreamViewer out, err;

    public static void main(String[] args)
    {
        System.setOut((out = new OutputStreamViewer("JHack Console"))
            .getPrintStream());
        System.setErr((err = new OutputStreamViewer("JHack Error"))
            .getPrintStream());

        main = new MainGUI();
    }
}