/*
 * Created on Sep 26, 2003
 */
package net.starmen.pkhack;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
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
    public IPSDatabase(AbstractRom rom, XMLPreferences prefs)
    {
        super(rom, prefs);

        try
        {
            Class[] c = new Class[]{byte[].class, HackModule.class};
            registerExtension("ips",
                IPSDatabase.class.getMethod("applyIPS", c), IPSDatabase.class
                    .getMethod("unapplyIPS", c), IPSDatabase.class.getMethod(
                    "checkAppliedIPS", c), this);
        }
        catch (SecurityException e)
        {
            //no security model, shouldn't have to worry about this
            e.printStackTrace();
        }
        catch (NoSuchMethodException e)
        {
            //I just wrote the methods, I'm pretty sure they exist ^_^
            e.printStackTrace();
        }
    }

    public static class DatabaseEntry
    {
        protected byte[] file;
        private String filename, name, author, sdesc, romType;
        private ImageIcon ss;
        protected boolean applied;

        protected DatabaseEntry(String filename, int filesize, String name,
            String author, String sdesc, String ss, int sssize, String romType)
            throws IOException
        {
            this.filename = filename;
            file = new byte[filesize + 1];
            byte[] bss = new byte[sssize + 1];
            //			ClassLoader
            //				.getSystemResourceAsStream("net/starmen/pkhack/ips/" + filename)
            //				.read(bips);
            readAll(file,
                ClassLoader.getSystemResourceAsStream("net/starmen/pkhack/ips/"
                    + filename));
            //this.checkApplied(rom);
            this.name = name;
            this.author = author;
            this.sdesc = sdesc;
            if (sssize == 0)
                this.ss = null;
            else
            {
                readAll(bss, ClassLoader
                    .getSystemResourceAsStream("net/starmen/pkhack/ips/" + ss));
                this.ss = new ImageIcon(bss);
            }
            this.romType = romType;
        }

        protected DatabaseEntry(Element e) throws NumberFormatException,
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
        public void checkApplied(AbstractRom rom)
        {
            applied = forRom(rom.getRomType())
                && checkAppliedFile(filename, file);
        }

        /**
         * Applies this patch on {@link HackModule#rom}.
         * 
         * @return false if unable to patch because ROM is not expanded
         */
        public boolean apply()
        {
            return applyFile(filename, file);
        }

        /**
         * Unapplies this patch from {@link HackModule#rom}.
         * 
         * @return false if unable to unpatch because ROM is not expanded or if
         *         ROM not patched
         */
        public boolean unapply()
        {
            return unapplyFile(filename, file);
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

        /**
         * @return Returns the author.
         */
        public String getAuthor()
        {
            return author;
        }

        /**
         * @return Returns the name.
         */
        public String getName()
        {
            return name;
        }

        /**
         * @return Returns the sdesc.
         */
        public String getSdesc()
        {
            return sdesc;
        }

        /**
         * @return Returns the ss.
         */
        public ImageIcon getSs()
        {
            return ss;
        }

        /**
         * @return Returns the romType.
         */
        public String getRomType()
        {
            return romType;
        }
    }

    public static void checkAllApplied(AbstractRom rom)
    {
        readXML();
        for (int i = 0; i < entries.size(); i++)
        {
            ((DatabaseEntry) entries.get(i)).checkApplied(rom);
        }
    }

    /** If false, readXML(rom) needs to be called. */
    private static boolean inited = false;

    public void reset()
    {
        inited = false;
    }

    private static void readAll(byte[] b, InputStream in) throws IOException
    {
        int i = 0;
        int tmp;
        while ((tmp = in.read(b, i, b.length - i)) != -1)
            i += tmp;
    }

    private static List entries = new ArrayList();

    private static boolean xmlInited = false;

    public static void readXML()
    {
        if (xmlInited)
            return;
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
                    DatabaseEntry de = new DatabaseEntry((Element) nl.item(i));
                    entries.add(de);
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
        xmlInited = true;
    }
    private JTable ipsTable;

    protected void init()
    {
        readXML();

        mainWindow = new JFrame(this.getDescription());
        mainWindow.setLocationRelativeTo(JHack.main.getMainWindow());
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
                    if (((DatabaseEntry) entries.get(i)).forRom(rom
                        .getRomType()))
                        o++;
                }
                return o;
            }

            public DatabaseEntry getRow(int row)
            {
                int r = 0;
                for (int i = 0; i < entries.size(); i++)
                {
                    if (((DatabaseEntry) entries.get(i)).forRom(rom
                        .getRomType()))
                    {
                        if (r == row)
                            return (DatabaseEntry) entries.get(r);
                        r++;
                    }
                }
                throw new IndexOutOfBoundsException();
            }

            public Object getValueAt(int row, int col)
            {
                try
                {
                    DatabaseEntry e = getRow(row);
                    //(IPSDatabaseEntry) entries.get(row);
                    switch (col)
                    {
                        case 0:
                            return e.getName();
                        case 1:
                            return e.getAuthor();
                        case 2:
                            return e.getSdesc();
                        case 3:
                            return e.getSs();
                        case 4:
                            return Boolean.valueOf(e.isApplied());
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
        return "0.5";
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
            if (sxe.getException() != null)
                x = sxe.getException();
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

    /**
     * Reads from the ROM and checks all patches for if they have been applied.
     * 
     * @param rom AbstractRom to check against
     * @param force If true, checking is done even if it has already been done
     *            on this AbstractRom. (The AbstractRom could have been changed
     *            by JHack modules since it was loaded.)
     */
    public static void readFromRom(AbstractRom rom, boolean force)
    {
        readXML();
        if (!inited || force)
            checkAllApplied(rom);
        inited = true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.HackModule#show()
     */
    public void show()
    {
        super.show();

        readFromRom(rom, true);
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
    public static DatabaseEntry getPatch(String patchName)
    {
        readXML();
        DatabaseEntry e;
        for (Iterator i = entries.iterator(); i.hasNext();)
        {
            if ((e = ((DatabaseEntry) i.next())).getName().equalsIgnoreCase(
                patchName))
            {
                return e;
            }
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
        if (ae.getActionCommand().equals("close"))
        {
            hide();
        }
        if (ipsTable.getSelectedRow() == -1)
            return;
        if (ae.getActionCommand().equals("apply"))
        {
            DatabaseEntry de = (DatabaseEntry) entries.get(ipsTable
                .getSelectedRow());
            //make sure ROM is expanded
            rom.expand();
            de.apply();
            checkAllApplied(rom);
            ipsTable.repaint();
        }
        else if (ae.getActionCommand().equals("unapply"))
        {
            DatabaseEntry de = (DatabaseEntry) entries.get(ipsTable
                .getSelectedRow());
            //make sure ROM is expanded
            rom.expand();
            de.unapply();
            checkAllApplied(rom);
            ipsTable.repaint();
        }
    }

    /**
     * Used to cache <code>IPSFile</code> objects. Accessed by and written to
     * by <code>getIPSFileFor()</code>.
     * 
     * @see IPSFile
     * @see #getIPSFileFor(byte[])
     */
    protected static HashMap ipsFiles = new HashMap();

    /**
     * Creates an <code>IPSFile</code> object for the specified
     * <code>byte[]</code> or returns a cached one. This uses
     * {@link #ipsFiles}to store the cache of <code>IPSFile</code> objects.
     * Use this method instead of <code>new IPSFile(byte[])</code> in order to
     * make sure the <code>IPSFile</code> only gets created once.
     * 
     * @param file <code>byte[]</code> containing an .ips file.
     * @return an <code>IPSFile</code> object based on <code>file</code>
     */
    protected static IPSFile getIPSFileFor(byte[] file)
    {
        IPSFile ips = (IPSFile) ipsFiles.get(file);
        if (ips == null)
        {
            ips = new IPSFile(file);
            ipsFiles.put(file, ips);
        }

        return ips;
    }

    public static boolean checkAppliedIPS(byte[] file, HackModule hm)
    {
        return hm.rom.check(getIPSFileFor(file));
    }

    public static boolean applyIPS(byte[] file, HackModule hm)
    {
        return hm.rom.apply(getIPSFileFor(file));
    }

    public static boolean unapplyIPS(byte[] file, HackModule hm)
    {
        IPSFile ips = getIPSFileFor(file);
        if (hm.rom.check(ips))
            return hm.rom.unapply(ips, JHack.main.getOrginalRomFile(hm.rom
                .getRomType()));
        else
            return false;
    }

    /**
     * <code>HashMap</code> used by <code>registerExtension()</code> to
     * store methods to use for imnporting files for different extensions.
     * 
     * @see #registerExtension(String, Method, Method, Method, Object)
     * @see #applyFile(String, byte[], int)
     */
    protected static HashMap exts = new HashMap();
    /**
     * Indicates the apply method.
     * 
     * @see #registerExtension(String, Method, Method, Method, Object)
     * @see #applyFile(String, byte[], int)
     * @see #applyFile(String, byte[])
     */
    public static final int METHOD_APPLY = 0;
    /**
     * Indicates the unapply method.
     * 
     * @see #registerExtension(String, Method, Method, Method, Object)
     * @see #applyFile(String, byte[], int)
     * @see #unapplyFile(String, byte[])
     */
    public static final int METHOD_UNAPPLY = 1;
    /**
     * Indicates the check applied method.
     * 
     * @see #registerExtension(String, Method, Method, Method, Object)
     * @see #applyFile(String, byte[], int)
     * @see #checkAppliedFile(String, byte[])
     */
    public static final int METHOD_CHECK = 2;
    /**
     * Indicates the object stored in the {@link #exts}<code>HashMap</code>.
     * 
     * @see #registerExtension(String, Method, Method, Method, Object)
     * @see #applyFile(String, byte[], int)
     */
    protected static final int METHOD_OBJ = 3;

    /**
     * Registers a static method which can import files with the specified
     * extension, optionally another which can undo the changes made by the
     * first, and also a method which can check if the first has been done to
     * the current ROM. These should be called from a static block from any
     * class that has import abilities. Either method may require user
     * intervention.
     * 
     * @param ext extension given method can read; this is considered to be what
     *            is <em>after</em> the last period (.) in the file name.
     * @param apply method that takes two arguments of a <code>byte[]</code>
     *            which is the contents of a file with the extension
     *            <code>ext</code> and any <code>Object</code> for other
     *            use. This method should import the data in the
     *            <code>byte[]</code> into the ROM.
     * @param remove method that takes two arguments of a <code>byte[]</code>
     *            which is the contents of a file with the extension
     *            <code>ext</code> and any <code>Object</code> for other
     *            use. This method should look at the <code>byte[]</code> and
     *            try to undo what calling the <code>apply</code> would do
     *            with that data. This arugment may be null if such a method is
     *            unavailable and/or does not make sense in the context.
     * @param check method that takes two arguments of a <code>byte[]</code>
     *            which is the contents of a file with the extension
     *            <code>ext</code> and any <code>Object</code> for other
     *            use. This method should look at the <code>byte[]</code> and
     *            return a <code>Boolean</code> or a <code>boolean</code>
     *            with a <code>true</code> or <code>Boolean.TRUE</code>
     *            value if the exported data has already been put into the
     *            current ROM and a <code>false</code> or
     *            <code>Boolean.FALSE</code> value otherwise.
     * @param obj parameter to send to method along with file. This most likely
     *            will be an instance of <code>HackModule</code> for
     *            reading/writting
     * @see #applyFile(String, byte[], boolean)
     * @see #applyFile(String, byte[])
     * @see #unapplyFile(String, byte[])
     * @see #checkAppliedFile(String, byte[])
     */
    public static void registerExtension(String ext, Method apply,
        Method remove, Method check, Object obj)
    {
        if (apply == null)
            throw (new IllegalArgumentException(
                "IPSDatabase.registerExtension(): \"apply\" must be non-null"));

        exts.put(ext, new Object[]{apply, remove, check, obj});
    }

    /**
     * Method not final, defination subject to change. Imports data from the
     * specified file. Uses <code>filename</code> to identify what the
     * extension is. Once the extension has been read, the apply or remove
     * method registered with
     * {@link #registerExtension(String, Method, Method, Object)}will be used
     * to process the data based on the <code>apply</code> argument. That
     * method may prompt the user for options, but may or may not allow the user
     * to cancel.
     * 
     * @param filename filename to get extension from
     * @param file <code>byte[]</code> with the exported data
     * @param apply Indicates which method to call. Value must be one of the
     *            <code>METHOD_*</code> constants. May throw
     *            <code>IllegalArgumentException</code> if unapply is not
     *            supported for the file type.
     * @return <code>null</code> for apply or unapply methods. For check
     *         method <code>Boolean.TRUE</code> if file already applied,
     *         <code>Boolean.FALSE</code> if not.
     * @see #applyFile(String, byte[])
     * @see #unapplyFile(String, byte[])
     * @see #checkAppliedFile(String, byte[])
     * @see #registerExtension(String, Method, Method, Object)
     * @see #METHOD_APPLY
     * @see #METHOD_UNAPPLY
     * @see #METHOD_CHECK
     * @see #METHOD_OBJ
     */
    protected static Boolean applyFile(String filename, byte[] file, int method)
    {
        Object out = null;
        try
        {
            String ext = filename.substring(filename.lastIndexOf(".") + 1);
            Object[] obj = (Object[]) exts.get(ext);
            if (obj == null)
            {
                JOptionPane.showMessageDialog(null,
                    "Error performing specified action in IPS Database:\n"
                        + "File type ." + ext + " is not supported.",
                    "Unsupported file type", JOptionPane.ERROR_MESSAGE);
                return null;
            }
            Method m = (Method) obj[method];
            if (m == null)
            {
                final String[] methodNames = new String[]{"Apply", "Unapply",
                    "Check"};
                throw (new IllegalArgumentException((methodNames[method]
                    + " not supported for ." + ext + " files.")));
            }
            Object o = obj[METHOD_OBJ];
            out = m.invoke(null, new Object[]{file, o});
        }
        catch (Exception e)
        {
            JOptionPane.showMessageDialog(null,
                "Unexpected error performing specified action in IPS Database.\n"
                    + "See error console for details.", "Unknown error",
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        if (out instanceof Boolean)
            return (Boolean) out;
        else
            return null;
    }

    /**
     * Imports data from the specified file. Uses <code>filename</code> to
     * identify what the extension is. Once the extension has been read, the
     * apply method registered with
     * {@link #registerExtension(String, Method, Method, Object)}will be used
     * to process the data. That method may prompt the user for options, but may
     * or may not allow the user to cancel.
     * 
     * @param filename filename to get extension from
     * @param file <code>byte[]</code> with the exported data
     * @return <code>true</code> if successful
     * @see #registerExtension(String, Method, Object)
     * @see #applyFile(String, byte[], boolean)
     * @see #unapplyFile(String, byte[])
     * @see #checkAppliedFile(String, byte[])
     * @see #METHOD_APPLY
     */
    public static boolean applyFile(String filename, byte[] file)
    {
        Object out = applyFile(filename, file, METHOD_APPLY);
        return out != null ? out.equals(Boolean.TRUE) : false;
    }

    /**
     * Unimports data from the specified file. Uses <code>filename</code> to
     * identify what the extension is. Once the extension has been read, the
     * remove method registered with
     * {@link #registerExtension(String, Method, Method, Object)}will be used
     * to process the data. That method may prompt the user for options, but may
     * or may not allow the user to cancel.
     * 
     * @param filename filename to get extension from
     * @param file <code>byte[]</code> with the exported data
     * @return <code>true</code> if successful; <code>false</code> could be
     *         caused by an unapply method not being available for the file
     *         type.
     * @see #registerExtension(String, Method, Object)
     * @see #applyFile(String, byte[], boolean)
     * @see #applyFile(String, byte[])
     * @see #checkAppliedFile(String, byte[])
     * @see #METHOD_UNAPPLY
     */
    public static boolean unapplyFile(String filename, byte[] file)
    {
        Object out = applyFile(filename, file, METHOD_UNAPPLY);
        return out != null ? out.equals(Boolean.TRUE) : false;
    }

    /**
     * Checks if data has been imported from the specified file. Uses
     * <code>filename</code> to identify what the extension is. Once the
     * extension has been read, the remove method registered with
     * {@link #registerExtension(String, Method, Method, Object)}will be used
     * to process the data.
     * 
     * @param filename filename to get extension from
     * @param file <code>byte[]</code> with the exported data
     * @return <code>true</code> if specified <code>file</code> has already
     *         been applied to the current ROM.
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @see #registerExtension(String, Method, Object)
     * @see #applyFile(String, byte[], boolean)
     * @see #applyFile(String, byte[])
     * @see #unapplyFile(String, byte[])
     * @see #METHOD_CHECK
     */
    public static boolean checkAppliedFile(String filename, byte[] file)
    {
        return applyFile(filename, file, METHOD_CHECK).equals(Boolean.TRUE);
    }
}