/*
 * Created on Sep 26, 2003
 */
package net.starmen.pkhack;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * TODO Write javadoc for this class Reads
 * /net/starmen/pkhack/ips/ipslisting.xml and displays a JTable of IPS's with
 * the columns: Name | Author | Short Description | Screenshot.
 * 
 * @author AnyoneEB
 */
public class IPSDatabase extends GeneralHackModule implements ActionListener
{
    /**
     * @param rom
     * @param prefs
     */
    public IPSDatabase(Rom rom, XMLPreferences prefs)
    {
        super(rom, prefs);
    }

    public static class IPSDatabaseEntry
    {
        private IPSFile ips;
        private String name, author, sdesc, romType;
        private ImageIcon ss;
        private boolean applied;

        protected IPSDatabaseEntry(String filename, int filesize, String name,
            String author, String sdesc, String ss, int sssize, String romType)
            throws IOException
        {
            byte[] bips = new byte[filesize + 1], bss = new byte[sssize + 1];
            //			ClassLoader
            //				.getSystemResourceAsStream("net/starmen/pkhack/ips/" + filename)
            //				.read(bips);
            readAll(bips,
                ClassLoader.getSystemResourceAsStream("net/starmen/pkhack/ips/"
                    + filename));
            this.ips = new IPSFile(bips);
            //this.checkApplied(rom);
            this.name = name;
            this.author = author;
            this.sdesc = sdesc;
            if (sssize == 0)
                this.ss = null;
            else
            {
                //				ClassLoader
                //					.getSystemResourceAsStream("net/starmen/pkhack/ips/" + ss)
                //					.read(bss);
                readAll(bss, ClassLoader
                    .getSystemResourceAsStream("net/starmen/pkhack/ips/" + ss));
                this.ss = new ImageIcon(bss);
            }
            this.romType = romType;
        }

        protected IPSDatabaseEntry(Element e) throws NumberFormatException,
            IOException
        {
            this(e.getAttribute("src"), Integer
                .parseInt(e.getAttribute("size")), e.getAttribute("name"), e
                .getAttribute("author"), e.getAttribute("sdesc"), e
                .getAttribute("sshot"), Integer.parseInt(e
                .getAttribute("sssize")), e.getAttribute("romtype"));
        }

        /**
         * Returns true if this patch has been applied.
         * 
         * @return true if this has been applied on the current ROM already
         */
        public boolean isApplied()
        {
            return applied;
        }

        /**
         * Rechecks if this has been applied.
         */
        public void checkApplied(Rom rom)
        {
            this.applied = rom.check(ips);
        }

        /**
         * Applies this patch on {@link HackModule#rom}.
         * 
         * @return false if unable to patch because ROM is not expanded
         */
        public boolean apply(Rom rom)
        {
            boolean out = true;
            if (!this.applied)
            {
                out = rom.apply(this.ips);
                checkAllApplied(rom);
            }
            return out;
        }

        /**
         * Unapplies this patch from {@link HackModule#rom}.
         * 
         * @return false if unable to unpatch because ROM is not expanded or if
         *         ROM not patched
         */
        public boolean unapply(Rom rom, Rom orgRom)
        {
            boolean out = true;
            if (this.applied)
            {
                out = rom.unapply(this.ips, orgRom);
                checkAllApplied(rom);
            }
            else
                return false;
            return out;
        }

        /**
         * Checks if this patch is intended for the specified ROM type.
         * 
         * @param romType name of game to check if this can be applied to
         * @return true if this patch is intended for a ROM of the specified
         *         game
         */
        public boolean forRom(String romType)
        {
            return romType.equalsIgnoreCase(this.romType);
        }
    }

    public static void checkAllApplied(Rom rom)
    {
        for (int i = 0; i < entries.size(); i++)
        {
            ((IPSDatabaseEntry) entries.get(i)).checkApplied(rom);
        }
    }

    public void reset()
    {
        readXML(rom);
    }

    private static void readAll(byte[] b, InputStream in) throws IOException
    {
        int i = 0;
        int tmp;
        while ((tmp = in.read(b, i, b.length - i)) != -1)
            i += tmp;
    }

    private static ArrayList entries = new ArrayList();

    public static void readXML(Rom rom)
    {
        //don't do this twice
        if (entries.size() > 0) return;
        //This is the slow way, the fast way (SAX) looks like too much work.
        Document dom;
        if ((dom = parseFile(ClassLoader
            .getSystemResourceAsStream("net/starmen/pkhack/ips/ipslisting.xml"))) == null)
        {
            System.out.println("Error reading ipslisting.xml!");
        }
        else
        {
            //put the xml elements as java objects into entries ArrayList
            NodeList nl = dom.getElementsByTagName("ips");
            for (int i = 0; i < nl.getLength(); i++)
            {
                try
                {
                    IPSDatabaseEntry ide = new IPSDatabaseEntry((Element) nl
                        .item(i));
                    ide.checkApplied(rom);
                    entries.add(ide);

                }
                catch (NumberFormatException e)
                {
                    e.printStackTrace();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
    private JTable ipsTable;

    protected void init()
    {
        readXML(rom);

        mainWindow = new JFrame(this.getDescription());
        mainWindow.getContentPane().setLayout(new BorderLayout());

        JPanel buttons = new JPanel();
        buttons.setLayout(new FlowLayout());

        JButton apply = new JButton("Apply Patch");
        apply.setActionCommand("apply");
        apply.addActionListener(this);
        buttons.add(apply);

        JButton unapply = new JButton("Unapply Patch");
        unapply.setActionCommand("unapply");
        unapply.addActionListener(this);
        buttons.add(unapply);

        JButton close = new JButton("Close");
        close.setActionCommand("close");
        close.addActionListener(this);
        buttons.add(close);

        mainWindow.getContentPane().add(buttons, BorderLayout.SOUTH);

        final String[] columnNames = {"Name", "Author", "Description",
            "Screenshot", "Applied?"};
        TableModel ipsModel = new AbstractTableModel()
        {
            public int getColumnCount()
            {
                return 5;
            }

            public int getRowCount()
            {
                int o = 0;
                for (int i = 0; i < entries.size(); i++)
                {
                    if (((IPSDatabaseEntry) entries.get(i)).forRom(rom
                        .getRomType())) o++;
                }
                return o;
            }

            public IPSDatabaseEntry getRow(int row)
            {
                int r = 0;
                for (int i = 0; i < entries.size(); i++)
                {
                    if (((IPSDatabaseEntry) entries.get(i)).forRom(rom
                        .getRomType()))
                    {
                        if (r == row) return (IPSDatabaseEntry) entries.get(r);
                        r++;
                    }
                }
                throw new IndexOutOfBoundsException();
            }

            public Object getValueAt(int row, int col)
            {
                try
                {
                    IPSDatabaseEntry e = getRow(row);
                    //(IPSDatabaseEntry) entries.get(row);
                    switch (col)
                    {
                        case 0:
                            return e.name;
                        case 1:
                            return e.author;
                        case 2:
                            return e.sdesc;
                        case 3:
                            return e.ss;
                        case 4:
                            return new Boolean(e.isApplied());
                    }
                }
                catch (IndexOutOfBoundsException e)
                {}
                return null;
            }

            public String getColumnName(int col)
            {
                return columnNames[col];
            }

            public Class getColumnClass(int c)
            {
                return getValueAt(0, c).getClass();
            }

            public boolean isCellEditable(int row, int col)
            {
                return false;
            }
        };
        ipsTable = new JTable(ipsModel);
        ipsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ipsTable.setRowHeight(224);
        ipsTable.getColumn(columnNames[0]).setPreferredWidth(90);
        ipsTable.getColumn(columnNames[1]).setPreferredWidth(50);
        ipsTable.getColumn(columnNames[2]).setPreferredWidth(250);
        ipsTable.getColumn(columnNames[3]).setPreferredWidth(256);
        ipsTable.getColumn(columnNames[4]).setPreferredWidth(17);
        ipsTable.setPreferredScrollableViewportSize(new Dimension(800, 500));
        ipsTable.doLayout();
        JScrollPane scrollPane = new JScrollPane(ipsTable);

        mainWindow.getContentPane().add(scrollPane, BorderLayout.CENTER);

        mainWindow.pack();
    }

    public String getVersion()
    {
        return "0.3";
    }

    public String getDescription()
    {
        return "IPS Database";
    }

    public String getCredits()
    {
        return "Written by AnyoneEB\n"
            + "IPS patches made by indicated authors";
    }

    public void hide()
    {
        mainWindow.setVisible(false);
    }

    private static Document parseFile(InputStream f)
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(true);
        //factory.setNamespaceAware(true);
        try
        {
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(f);
        }
        catch (SAXException sxe)
        {
            // Error generated during parsing)
            Exception x = sxe;
            if (sxe.getException() != null) x = sxe.getException();
            x.printStackTrace();
            return null;
        }
        catch (ParserConfigurationException pce)
        {
            // Parser with specified options can't be built
            pce.printStackTrace();
            return null;
        }
        catch (IOException ioe)
        { // I/O error
            ioe.printStackTrace();
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.HackModule#show()
     */
    public void show()
    {
        super.show();

        reset();
        ipsTable.revalidate();

        mainWindow.setVisible(true);
    }

    /**
     * Returns the patch with the specified name to the ROM. Returns null if the
     * patch is not found.
     * 
     * @param patchName name of patch to apply as named in the IPSDatabase list
     * @return a {@link IPSDatabaseEntry}or null
     */
    public static IPSDatabaseEntry getPatch(String patchName)
    {
        IPSDatabaseEntry e;
        for (Iterator i = entries.iterator(); i.hasNext();)
        {
            if ((e = ((IPSDatabaseEntry) i.next())).name
                .equalsIgnoreCase(patchName)) { return e; }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equals("apply"))
        {
            if (!((IPSDatabaseEntry) entries.get(ipsTable.getSelectedRow()))
                .isApplied())
            {
                if (!((IPSDatabaseEntry) entries.get(ipsTable.getSelectedRow()))
                    .apply(rom))
                {
                    rom.expand();
                    ((IPSDatabaseEntry) entries.get(ipsTable.getSelectedRow()))
                        .apply(rom);
                }
                ipsTable.repaint();
            }
        }
        else if (ae.getActionCommand().equals("unapply"))
        {
            if (((IPSDatabaseEntry) entries.get(ipsTable.getSelectedRow()))
                .isApplied())
            {
                if (!((IPSDatabaseEntry) entries.get(ipsTable.getSelectedRow()))
                    .unapply(rom, JHack.main
                        .getOrginalRomFile(rom.getRomType())))
                {
                    rom.expand();
                    ((IPSDatabaseEntry) entries.get(ipsTable.getSelectedRow()))
                        .unapply(rom, JHack.main.getOrginalRomFile(rom
                            .getRomType()));
                }
                ipsTable.repaint();
            }
        }
        else if (ae.getActionCommand().equals("close"))
        {
            hide();
        }
    }

    protected static HashMap exts = new HashMap();

    /**
     * Method not final, defination subject to change. Registers a static method
     * which can import files with the specified extension. This should be
     * called from a static block from any class that has import abilities.
     * 
     * @param ext extension given method can read; this is considered to be what
     *            is <em>after</em> the last period (.) in the file name.
     * @param m method that takes two arguments of a <code>byte[]</code> which
     *            is the contents of a file with the extension <code>ext</code>
     *            and any <code>Object</code> for other use
     * @param obj parameter to send to method along with file. This most likely
     *            will be an instance of <code>HackModule</code> for
     *            reading/writting
     * @see #applyFile(String, byte[])
     */
    public static void registerExtension(String ext, Method m, Object obj)
    {
        exts.put(ext, new Object[]{m, obj});
    }

    /**
     * Method not final, defination subject to change. Imports data from the
     * specified file. Uses <code>filename</code> to identify what the
     * extension is. Once the extension has been read, the method registered
     * with {@link #registerExtension(String, Method, Object)}will be used to
     * process the data. That method may prompt the user for options, but may or
     * may not allow the user to cancel.
     * 
     * @param filename filename to get extension from
     * @param file <code>byte[]</code> with the exported data
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @see #registerExtension(String, Method, Object)
     */
    public static void applyFile(String filename, byte[] file)
        throws IllegalArgumentException, IllegalAccessException,
        InvocationTargetException
    {
        Object[] obj = (Object[]) exts.get(filename.substring(filename
            .lastIndexOf(".")));
        Method m = (Method) obj[0];
        Object o = (HackModule) obj[1];
        m.invoke(null, new Object[]{file, o});
    }
}