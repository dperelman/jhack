package net.starmen.pkhack;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileFilter;

/**
 * Main GUI class of JHack. Shows the main window with the list of module
 * buttons. This is called by {@link JHack#main(String[])}.
 * 
 * @author AnyoneEB
 * @version 0.4.5
 */
//Made by AnyoneEB.
//Code released under the GPL - http://www.gnu.org/licenses/gpl.txt
public class MainGUI implements ActionListener, WindowListener
{
    private JFrame mainWindow = new JFrame();
    private Rom rom, orgRom = null;
    private ArrayList moduleList = new ArrayList();

    private boolean showload = false;
    private JFrame loadingDialog;
    private JProgressBar loadingBar;

    private JMenu revertMenu;
    private JMenuItem[] recentLoadsItems = new JMenuItem[5];

    private XMLPreferences prefs;

    private ArrayList recentLoads = new ArrayList();

    private final static String DATE_FORMAT = "yyyyMMddHHmmss";

    private Box buttons;
    private JButton[] modButtons;

    private String[] addToArr(String[] in, String arg)
    {
        String[] out = new String[in.length + 1];
        for (int i = 0; i < in.length; i++)
        {
            out[i] = in[i];
        }
        out[in.length] = arg;
        return out;
    }

    private int getModuleCount()
    {
        return moduleList.size();
    }

    private HackModule getModuleAt(int i)
    {
        return (HackModule) moduleList.get(i);
    }

    /**
     * Inits HackModules.
     * 
     * @return String[] indicating after which modules there should be
     *         separators and their titles.
     */
    private String[] initModules()
    {
        //HackModule.initBigArrays();
        String[] moduleNames = new String[0]; //list of module class names

        try
        {
            moduleNames = new CommentedLineNumberReader(
                new InputStreamReader(
                    ClassLoader
                        .getSystemResourceAsStream("net/starmen/pkhack/modulelist.txt")))
                .readUsedLines();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            moduleNames = new String[]{"DontCare"};
        }

        if (showload)
        {
            loadingBar.setMaximum(moduleNames.length);
            loadingBar.setStringPainted(true);
        }

        String[] spacingList = new String[moduleNames.length + 1];
        Arrays.fill(spacingList, null);
        String tmp;
        //modules = new HackModule[moduleList.length];
        moduleList.ensureCapacity(moduleNames.length);
        for (int i = 0; i < moduleNames.length; i++)
        {
            if (showload)
            {
                loadingBar.setValue(i);
            }
            try
            {
                /*
                 * If moduleName is like ------- add a separator after last
                 * module added
                 */
                if (moduleNames[i].startsWith("-"))
                {
                    try
                    {
                        spacingList[moduleList.size()] = new StringTokenizer(
                            moduleNames[i], "-", false).nextToken();
                    }
                    catch (NoSuchElementException nsee)
                    {
                        spacingList[moduleList.size()] = "";
                    }
                }
                else
                    moduleList.add((HackModule) Class.forName(
                        "net.starmen.pkhack." + moduleNames[i]).getConstructor(
                        new Class[]{Rom.class, XMLPreferences.class})
                        .newInstance(new Object[]{rom, prefs}));
            }
            catch (InstantiationException e)
            {
                e.printStackTrace();
            }
            catch (IllegalAccessException e)
            {
                e.printStackTrace();
            }
            catch (ClassNotFoundException e)
            {
                e.printStackTrace();
            }
            catch (IllegalArgumentException e)
            {
                e.printStackTrace();
            }
            catch (SecurityException e)
            {
                e.printStackTrace();
            }
            catch (InvocationTargetException e)
            {
                e.printStackTrace();
            }
            catch (NoSuchMethodException e)
            {
                e.printStackTrace();
            }
        }
        moduleList.trimToSize();

        return spacingList;
    }

    private void initGraphics()
    {
        //show loading dialog first
        if (showload)
        {
            loadingDialog = new JFrame("Loading...");
            loadingBar = new JProgressBar();
            loadingDialog.getContentPane().add(loadingBar);
            loadingDialog.setVisible(true);
            loadingDialog.invalidate();
            loadingDialog.setSize(200, 60);
            loadingDialog.setResizable(false);
            loadingDialog.validate();
        }

        loadPrefs();

        //main init stuff
        mainWindow.setTitle(MainGUI.getDescription() + " "
            + MainGUI.getVersion());

        mainWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mainWindow.addWindowListener(this);

        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('f');
        fileMenu.add(HackModule.createJMenuItem("Open ROM...", 'o', "ctrl O",
            "load", this));
        fileMenu.add(HackModule.createJMenuItem("Close ROM", 'c', "ctrl F4",
            "closeROM", this));
        fileMenu.add(HackModule.createJMenuItem("Save ROM", 's', "ctrl S",
            "save", this));
        fileMenu.add(HackModule.createJMenuItem("Save ROM As...", 'a',
            "ctrl A", "saveAs", this));
        fileMenu.add(new JSeparator());
        revertMenu = new JMenu("Revert");
        revertMenu.setMnemonic('r');
        fileMenu.add(revertMenu);
        refreshRevertMenu();
        fileMenu.add(new JSeparator());

        for (int i = 0; i < recentLoadsItems.length; i++)
        {
            recentLoadsItems[i] = new JMenuItem();
            recentLoadsItems[i].setMnemonic(Integer.toString(i + 1).charAt(0));
            recentLoadsItems[i].setActionCommand("recentLoad" + i);
            recentLoadsItems[i].addActionListener(this);
            recentLoadsItems[i].setVisible(false);
            fileMenu.add(recentLoadsItems[i]);
        }
        refreshRecentLoads();

        fileMenu.add(new JSeparator());
        fileMenu.add(HackModule.createJMenuItem("Exit", 'x', "alt F4", "exit",
            this));
        menuBar.add(fileMenu);

        JMenu optionsMenu = new JMenu("Options");
        optionsMenu.setMnemonic('o');
        optionsMenu.add(new PrefsCheckBox("Use Hex Numbers", prefs,
            "useHexNumbers", true, 'h'));
        optionsMenu.add(new PrefsCheckBox("Use Direct File IO", prefs,
            "useDirectFileIO", false, 'd', null, "directio", this));
        optionsMenu.add(new PrefsCheckBox("Auto Check for Updates", prefs,
            "checkVersion", true, 'u'));
        optionsMenu.add(HackModule.createJMenuItem("Change Default ROM...",
            'c', null, "selectDefROM", this));

        JMenu toolkitMenu = new JMenu("Look & Feel");
        toolkitMenu.setMnemonic('t');

        UIManager.LookAndFeelInfo[] lafi = UIManager.getInstalledLookAndFeels();
        ButtonGroup lafbg = new ButtonGroup();
        for (int i = 0; i < lafi.length; i++)
        {
            JRadioButtonMenuItem laf = new JRadioButtonMenuItem(lafi[i]
                .getName());
            laf.setSelected(UIManager.getLookAndFeel().getName().equals(
                lafi[i].getName()));
            laf.setActionCommand("LAF_" + lafi[i].getClassName());
            laf.addActionListener(this);
            lafbg.add(laf);
            toolkitMenu.add(laf);
        }

        optionsMenu.add(toolkitMenu);

        menuBar.add(optionsMenu);

        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic('h');
        helpMenu.add(HackModule.createJMenuItem("About...", 'a', null, "about",
            this));
        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(helpMenu);

        mainWindow.setJMenuBar(menuBar);

        //add buttons
        String[] spList = initModules();
        /*
         * int spaces = 0; for (int i = 0; i < spList.length; i++) if
         * (spList[i])
         */
        buttons = new Box(BoxLayout.Y_AXIS);
        Box currBox = null;
        HeadingLabel prevLabel = null;
        //buttons.setLayout(new GridLayout(getModuleCount() + spaces, 1));
        JScrollPane scroll = new JScrollPane(buttons,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        modButtons = new JButton[getModuleCount()];
        for (int i = 0; i < getModuleCount(); i++)
        {
            if (spList[i] != null)
            {
                if (currBox != null)
                {
                    buttons.add(currBox);
                    currBox.setVisible(false);
                    if (prevLabel != null)
                        prevLabel.setCollapsableArea(currBox);
                }
                currBox = new Box(BoxLayout.Y_AXIS);
                if (spList[i] == "")
                    buttons.add(Box.createVerticalStrut(16));
                else
                {
                    HeadingLabel t = new HeadingLabel(spList[i],
                        SwingConstants.CENTER);
                    t.setMaximumSize(new Dimension(274, 16));
                    buttons.add(t);
                    prevLabel = t;
                }
            }

            modButtons[i] = new JButton(getModuleAt(i).getDescription());
            if (getModuleAt(i).getIcon() != null)
            {
                modButtons[i].setIcon(getModuleAt(i).getIcon());
            }
            modButtons[i].setActionCommand("module" + i);
            modButtons[i].addActionListener(this);
            modButtons[i].setMaximumSize(new Dimension(274, 26));
            currBox.add(modButtons[i]);
        }
        if (currBox != null)
        {
            currBox.setVisible(false);
            buttons.add(currBox);
            if (prevLabel != null) prevLabel.setCollapsableArea(currBox);
        }
        mainWindow.getContentPane().add(scroll);

        if (showload)
        {
            loadingDialog.setVisible(false);
        }
        mainWindow.setVisible(true);
        mainWindow.invalidate();
        mainWindow.setSize(300, 400);
        mainWindow.validate();
        mainWindow.setResizable(false);
    }

    private class HeadingLabel extends JLabel
    {
        private JComponent ca;
        private Color regCol, hoverCol;

        public HeadingLabel(String label, int align, Color col, Color hover)
        {
            super(label, align);

            this.regCol = col;
            this.hoverCol = hover;

            this.setForeground(col);

            this.addMouseListener(new MouseListener()
            {

                public void mouseClicked(MouseEvent arg0)
                {
                    if (ca != null) ca.setVisible(!ca.isShowing());
                    if (ca.isVisible())
                        setFont(getFont().deriveFont(Font.BOLD));
                    else
                        setFont(getFont().deriveFont(0));
                    buttons.doLayout();
                    buttons.repaint();
                    ca.doLayout();
                    ((JComponent) mainWindow.getContentPane().getComponents()[0])
                        .updateUI();
                }

                public void mouseEntered(MouseEvent arg0)
                {
                    if (ca != null) setForeground(hoverCol);
                }

                public void mouseExited(MouseEvent arg0)
                {
                    if (ca != null) setForeground(regCol);
                }

                public void mousePressed(MouseEvent arg0)
                {}

                public void mouseReleased(MouseEvent arg0)
                {}
            });
        }

        public HeadingLabel(String label, int align)
        {
            this(label, align, Color.BLUE, Color.RED);
        }

        public void setCollapsableArea(JComponent ca)
        {
            this.ca = ca;
            this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

    /**
     * Creates a new <code>MainGUI</code> and shows its main window.
     */
    public MainGUI()
    {
        initGraphics();
    }

    /**
     * Returns the name of this program ("JHack").
     * 
     * @return "JHack".
     */
    public static String getDescription() //Return one line description of
    // class
    {
        return "JHack";
    }

    /**
     * Returns the version of JHack as a <code>String</code>. Can have any
     * number of numbers and dots ex. "0.3.3.5".
     * 
     * @return The version of this class.
     */
    public static String getVersion()
    {
        return "0.4.5";
    }

    /**
     * Returns the credits for this class.
     * 
     * @return The credits for this class.
     */
    public static String getCredits() //Return who made it
    {
        return "Written by AnyoneEB\n" + "Various lists from PK Hack website\n"
            + "Icons from PK Hack source";
    }

    private void exit()
    {
        if (rom.isLoaded && !rom.isDirectFileIO())
        {
            int ques = JOptionPane.showConfirmDialog(null,
                "Do you want to save before exiting?", "Save?",
                JOptionPane.YES_NO_CANCEL_OPTION);
            if (ques == JOptionPane.CANCEL_OPTION)
            {
                return;
            }
            else if (ques == JOptionPane.YES_OPTION)
            {
                rom.saveRom();
            }
        }
        doBackup();
        savePrefs();

        System.exit(0);
    }

    /**
     * Saves the preferences to JHack.xml.
     */
    private void savePrefs()
    {
        this.getPrefs().save();
    }

    /**
     * Loads the preferences from JHack.ini. Case-insensive.
     */
    private void loadPrefs()
    {
        rom = new Rom();
        File jhackDir = new File(System.getProperty("user.home")
            + File.separator + ".jhack");
        if (!jhackDir.exists()) jhackDir.mkdir();
        File xmlFile = new File(jhackDir.toString() + File.separator
            + "JHackPrefs.xml");
        HashMap iniPrefs = new HashMap();
        if (!xmlFile.exists())
        {
            try
            {
                String[] input = new CommentedLineNumberReader("JHack.ini")
                    .readUsedLines();
                for (int i = 0; i < input.length; i++)
                {
                    String[] split = input[i].split("=");
                    split[0] = split[0].trim();
                    split[1] = split[1].trim();
                    iniPrefs.put(split[0], split[1]);
                }
            }
            catch (FileNotFoundException e)
            {
                iniPrefs.put("useHexNumbers", Boolean.toString(true));
                iniPrefs.put("useDirectFileIO", Boolean.toString(false));
            }
            catch (IOException e)
            {
                e.printStackTrace();
                iniPrefs.put("useHexNumbers", Boolean.toString(true));
                iniPrefs.put("useDirectFileIO", Boolean.toString(false));
            }
        }
        this.prefs = new XMLPreferences(xmlFile, iniPrefs);
        try
        {
            if (this.getPrefs().getValueAsBoolean("useDirectFileIO"))
            {
                rom = new RomFileIO();
            }
        }
        catch (NullPointerException e)
        {}
        try
        {
            UIManager.setLookAndFeel(prefs.getValue("laf"));
        }
        catch (NullPointerException e)
        {
            System.out
                .println("No look and feel preference found, using default.");
        }
        catch (Exception e)
        {
            System.out.println("Error loading look and feel preference.");
            e.printStackTrace();
        }
        if (this.getPrefs().getValue("checkVersion") == null)
        {
            int ques = JOptionPane.showConfirmDialog(null,
                "Do you want JHack to check for updates\n"
                    + "automatically on startup?\n"
                    + "NOTE: This accesses AnyoneEB's server and\n"
                    + "could (but won't) be used to gather usage\n"
                    + "statistics if enabled.",
                "Check for updates automatically?", JOptionPane.YES_NO_OPTION);
            this.getPrefs().setValueAsBoolean("checkVersion",
                ques == JOptionPane.YES_OPTION);
        }
        //convert expRomPath and orgRomPath to game specific names
        if (this.getPrefs().hasValue("expRomPath"))
        {
            this.getPrefs().setValue("Earthbound.expRomPath",
                getPrefs().getValue("expRomPath"));
            this.getPrefs().removeValue("expRomPath");
        }
        if (this.getPrefs().hasValue("orgRomPath"))
        {
            this.getPrefs().setValue("Earthbound.orgRomPath",
                getPrefs().getValue("orgRomPath"));
            this.getPrefs().removeValue("orgRomPath");
        }
        if (this.getPrefs().getValueAsBoolean("checkVersion"))
        {
            askToUpdate();
        }
        loadRecentLoadsPref();

        //delete old prefs
        new File("JHack.ini").delete();
        new File("JHackPrefs.xml").delete();
        //new File("preferences.dtd").delete();
    }

    private String getModuleCredits()
    {
        String returnValue = "\n\n" + Rom.getDescription() + " "
            + Rom.getVersion() + "\n" + Rom.getCredits();
        for (int i = 0; i < getModuleCount(); i++)
        {
            returnValue += "\n\n" + getModuleAt(i).getDescription() + " "
                + getModuleAt(i).getVersion() + "\n"
                + getModuleAt(i).getCredits();
        }
        return returnValue;
    }

    private String getFullCredits()
    {
        return MainGUI.getDescription() + " " + MainGUI.getVersion() + "\n"
            + MainGUI.getCredits() + this.getModuleCredits();
    }

    private JScrollPane createScollingLabel(String text)
    {
        int emptyLine = new JLabel("newline").getPreferredSize().height;
        JPanel labels = new JPanel();
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));

        text = text.replaceAll("\n\n", "\nnewline\n");

        StringTokenizer st = new StringTokenizer(text, "\n");
        JLabel temp;
        while (st.hasMoreTokens())
        {
            temp = new JLabel(st.nextToken());
            if (temp.getText().equals("newline"))
            {
                labels.add(Box.createVerticalStrut(emptyLine));
            }
            else
            {
                labels.add(temp);
            }
        }

        JScrollPane out = new JScrollPane(labels,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        out.setPreferredSize(new Dimension(out.getPreferredSize().width, 200));

        return out;
    }

    public void windowClosed(WindowEvent we)
    {
        exit();
    }

    public void windowActivated(WindowEvent we)
    {}

    public void windowClosing(WindowEvent we)
    {
        exit();
    }

    public void windowDeactivated(WindowEvent we)
    {}

    public void windowDeiconified(WindowEvent we)
    {}

    public void windowIconified(WindowEvent we)
    {}

    public void windowOpened(WindowEvent we)
    {}

    private static File getFileOfSize(final long size1, final long size2,
        String msg, boolean repeat)
    {
        JFileChooser jfc = new JFileChooser(Rom.getDefaultDir());
        jfc.setFileFilter(new FileFilter()
        {
            public boolean accept(File f)
            {
                if ((((f.getAbsolutePath().toLowerCase().endsWith(".smc")
                    || f.getAbsolutePath().toLowerCase().endsWith(".sfc") || f
                    .getAbsolutePath().toLowerCase().endsWith(".fig")) && (f
                    .length() == size1 || f.length() == size2)) || f
                    .isDirectory())
                    && f.exists()) { return true; }
                return false;
            }

            public String getDescription()
            {
                return "SNES ROMs (*.smc, *.sfc, *.fig)";
            }
        });
        do
        {
            JOptionPane.showMessageDialog(null, msg, "Where is this file?",
                JOptionPane.QUESTION_MESSAGE);
        }
        while (jfc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION
            && repeat);
        return jfc.getSelectedFile();
    }

    private static File getFile(String msg, boolean repeat)
    {
        JFileChooser jfc = new JFileChooser(Rom.getDefaultDir());
        jfc.setFileFilter(new FileFilter()
        {
            public boolean accept(File f)
            {
                if ((((f.getAbsolutePath().toLowerCase().endsWith(".smc")
                    || f.getAbsolutePath().toLowerCase().endsWith(".sfc") || f
                    .getAbsolutePath().toLowerCase().endsWith(".fig"))) || f
                    .isDirectory())
                    && f.exists()) { return true; }
                return false;
            }

            public String getDescription()
            {
                return "SNES ROMs (*.smc, *.sfc, *.fig)";
            }
        });
        do
        {
            JOptionPane.showMessageDialog(null, msg, "Where is this file?",
                JOptionPane.QUESTION_MESSAGE);
        }
        while (jfc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION
            && repeat);
        return jfc.getSelectedFile();
    }

    /**
     * Creates a exp'd ROM from the org ROM pref and sets the exp ROM pref.
     * 
     * @return expanded Rom object.
     */
    private Rom orgRomToExp(String romType)
    {
        String orgRom = this.getPrefs().getValue(romType + ".orgRomPath");
        String expRom = orgRom.substring(0, orgRom.length() - 3) + "expanded."
            + orgRom.substring(orgRom.length() - 3);

        String dd = Rom.getDefaultDir();

        Rom r = new Rom();
        r.loadRom(new File(orgRom));
        r.expand();
        r.saveRom(new File(expRom));

        this.getPrefs().setValue(romType + ".expRomPath", expRom);

        Rom.setDefaultDir(dd);

        return r;
    }

    /**
     * Asks the user for the location of a unmodified organial or expanded ROM.
     * Puts this value in "expRomPath" preferences, after copying and expanding
     * the copy, if nessacary. It checks that and the "orgRomPath" values and
     * does not ask if they are already set with valid paths to ROMs of the
     * correct length.
     * 
     * @return An Rom with path of ExpRomPath if just expanded, null if
     *         ExpRomPath pref set.
     */
    public Rom requestOrgRomFile(String romType)
    {
        String tmp;
        File tmpf;
        if ((tmp = this.getPrefs().getValue(romType + ".expRomPath")) == null
            || !(tmpf = new File(tmp)).exists())
        {
            //if orgRomPath from old version set, use it
            if ((tmp = this.getPrefs().getValue(romType + ".orgRomPath")) != null
                && (tmpf = new File(tmp)).exists()) { return orgRomToExp(romType); }
            this.getPrefs().setValue(
                romType + ".expRomPath",
                (tmpf = getFile("Select a unmodified or expanded " + romType
                    + " ROM.", true)).toString());
            if (romType == "Earthbound"
                && tmpf.length() == Rom.EB_ROM_SIZE_REGULAR)
            {
                this.getPrefs().setValue(romType + ".orgRomPath",
                    tmpf.toString());
                return orgRomToExp(romType);
            }
        }
        //null means expRomPath is set to something valid
        return null;
    }

    /**
     * Returns the orginal ROM file's path. Will always get an expanded ROM. If
     * there is not a good recorded value, it will ask the user for the
     * location. Sets private orgRom var to the returned value.
     * 
     * @return The path to an unmodified ROM.
     */
    public Rom getOrginalRomFile(String romType)
    {
        if (orgRom != null && orgRom.isLoaded) return orgRom;
        Rom out = requestOrgRomFile(romType);
        if (out != null)
            return orgRom = out;
        else
        {
            out = new Rom();
            String dd = Rom.getDefaultDir();
            out.loadRom(new File(this.getPrefs().getValue(
                romType + ".expRomPath")));
            Rom.setDefaultDir(dd);
            return orgRom = out;
        }
    }

    private File getBackupDir()
    {
        return new File(rom.getFilePath().getParent() + File.separator
            + "backup");
    }

    private void writeBackup() throws FileNotFoundException, IOException
    {
        if (rom.isLoaded)
        {
            File backupDir = getBackupDir();
            backupDir.mkdir();
            getOrginalRomFile(rom.getRomType());
            //			byte[] orgRomArr = new byte[rom.length()];
            //			new FileInputStream(getOrginalRomFile()).read(orgRomArr);
            System.out.println("About to write backup .ips...");
            IPSFile ips = rom.createIPS(orgRom);
            //			FileOutputStream out =
            //				new FileOutputStream(
            File f = new File(backupDir.toString()
                + File.separator
                + rom.getFilePath().getName()
                + "."
                + new SimpleDateFormat(DATE_FORMAT)
                    .format(new java.util.Date()) + ".bak.ips");
            ips.saveIPSFile(f);
            //			for (int i = 0; i < ips.length(); i++)
            //			{
            //				out.write(ips.charAt(i));
            //			}
            //			out.close();
            System.out.println("Completed writing backup .ips...");

            this.refreshRevertMenu();
        }
    }

    /**
     * Call to make a backup. Called on exit, restore from backup, and the menu
     * options save, save as, load, and close.
     */
    private void doBackup()
    {
        try
        {
            writeBackup();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void readBackup(String date) throws FileNotFoundException,
        IOException
    {
        if (rom.isLoaded)
        {
            File backupDir = getBackupDir();
            if (backupDir.exists())
            {
                File backupFile = new File(backupDir.toString()
                    + File.separator + rom.getFilePath().getName() + "." + date
                    + ".bak.ips");
                //backup current rom path
                File romPath = rom.getFilePath();
                //load orginal ROM and patch it
                rom.loadRom(getOrginalRomFile(rom.getRomType()).path);
                if (rom.isDirectFileIO()) rom.saveRom(romPath);
                rom.apply(IPSFile.loadIPSFile(backupFile));
                //change path back so it saves in the right place
                rom.path = romPath;
                Rom.setDefaultDir(romPath.getParent());
                resetModules();
            }
        }
    }

    private void restoreFromBackup(String date)
    {
        doBackup();
        try
        {
            System.out.println("About to restore from backup .ips");
            readBackup(date);
            System.out.println("Completed restored from backup .ips");
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void refreshRevertMenu()
    {
        revertMenu.removeAll();
        if (rom.isLoaded)
        {
            File backupDir = getBackupDir();
            if (backupDir.exists())
            {
                //get list of backed up files
                String[] backups = backupDir.list(new FilenameFilter()
                {
                    public boolean accept(File dir, String name)
                    {
                        if (name.startsWith(rom.getFilePath().getName())
                            && name.endsWith(".bak.ips")) return true;
                        return false;
                    }
                });
                //sort list so it will be ordered by date
                Arrays.sort(backups);
                JMenu currMenu = revertMenu;
                int c = 0;
                //look through list and make menu options from it.
                for (int i = backups.length - 1; i >= 0; i--)
                {
                    //show 20 entries then a more menu
                    c++;
                    if (c > 20)
                    {
                        c = 0;
                        currMenu.add(currMenu = new JMenu("More"));
                    }
                    String date = backups[i].substring(rom.getFilePath()
                        .getName().length() + 1, rom.getFilePath().getName()
                        .length()
                        + 1 + DATE_FORMAT.length());
                    String formatedDate;
                    try
                    {
                        formatedDate = new SimpleDateFormat(DATE_FORMAT).parse(
                            date).toString();
                    }
                    catch (ParseException e)
                    {
                        formatedDate = date;
                    }
                    JMenuItem item = new JMenuItem(formatedDate);
                    item.setActionCommand("revert-" + date);
                    item.addActionListener(this);
                    currMenu.add(item);
                }
            }
        }
    }

    private void saveRecentLoadsPref()
    {
        String out = "";
        String[] tmpRecent = (String[]) recentLoads.toArray(new String[0]), recent = new String[Math
            .min(5, tmpRecent.length)];
        for (int i = 0; i < recent.length; i++)
        {
            recent[i] = tmpRecent[tmpRecent.length - (i + 1)];
            out += recent[i];
            if (i != recent.length - 1) out += File.pathSeparatorChar;
        }
        this.getPrefs().setValue("recentLoads", out);
        loadRecentLoadsPref(); //force down to 5
    }

    private void loadRecentLoadsPref()
    {
        try
        {
            String[] recent = this.getPrefs().getValue("recentLoads").split(
                File.pathSeparator);
            recentLoads.clear();
            for (int i = recent.length - 1; i >= 0; i--)
            {
                recentLoads.add(recent[i]);
            }
        }
        catch (NullPointerException e)
        {
            recentLoads.clear();
            System.out.println("Unable to load recent loads.");
        }
    }

    private void refreshRecentLoads()
    {
        String[] tmpRecent = (String[]) recentLoads.toArray(new String[0]), recent = new String[Math
            .min(5, tmpRecent.length)];
        int i = 0;
        for (; i < recent.length; i++)
        {
            recent[i] = tmpRecent[tmpRecent.length - (i + 1)];
            recentLoadsItems[i].setText((i + 1) + " " + recent[i]);
            recentLoadsItems[i].setVisible(true);
        }
        for (; i < 5; i++)
        {
            recentLoadsItems[i].setVisible(false);
        }
    }

    private void addRecentLoad(String path)
    {
        for (Iterator i = recentLoads.iterator(); i.hasNext();)
            if (new File((String) i.next()).equals(new File(path))) i.remove();
        recentLoads.add(path);
        saveRecentLoadsPref();
    }

    private void closeRom()
    {
        if (rom.isLoaded && !(rom.isDirectFileIO()))
        {
            int ques = JOptionPane.showConfirmDialog(null,
                "Do you want to save your changes?", "Save?",
                JOptionPane.YES_NO_CANCEL_OPTION);
            if (ques == JOptionPane.CANCEL_OPTION)
            {
                return;
            }
            else if (ques == JOptionPane.YES_OPTION)
            {
                rom.saveRom();
            }
        }
        doBackup();

        rom.isLoaded = false;
        mainWindow.setTitle(MainGUI.getDescription() + " "
            + MainGUI.getVersion());
        this.refreshRevertMenu();
    }

    private void resetModules()
    {
        for (int i = 0; i < getModuleCount(); i++)
        {
            try
            {
                if (getModuleAt(i).isRomSupported())
                {
                    //                    System.out.println(loc.getName() + " is a "
                    //                        + rom.getRomType() + " ROM and is usable by "
                    //                        + modButtons[i].getText() + ".");
                    modButtons[i].setVisible(true);
                    getModuleAt(i).reset();
                }
                else
                {
                    //                    System.out.println(loc.getName() + " is a "
                    //                        + rom.getRomType() + " ROM and is not usable by "
                    //                        + modButtons[i].getText() + ".");
                    modButtons[i].setVisible(false);
                }
                getModuleAt(i).hide();
            }
            catch (NullPointerException npe)
            {
                //If not module inited yet
            }
        }
    }

    private void loadRom(File loc)
    {
        if (loc == null)
        {
            if (!rom.loadRom()) return;
        }
        else if (!rom.loadRom(loc)) return;
        //make sure spt names are read for other resets
        //SpriteEditor.initSptNames(rom.getPath());
        resetModules();
        mainWindow.setTitle(MainGUI.getDescription() + " "
            + MainGUI.getVersion() + " - " + rom.getPath());
        addRecentLoad(rom.getPath());
        refreshRecentLoads();
        doBackup();
    }

    private void loadRom()
    {
        loadRom(null);
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
     */
    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equalsIgnoreCase("load"))
        {
            loadRom(null);
        }
        else if (ae.getActionCommand().equalsIgnoreCase("save"))
        {
            rom.saveRom();
            doBackup();
        }
        else if (ae.getActionCommand().equalsIgnoreCase("saveAs"))
        {
            rom.saveRomAs();
            doBackup();
        }
        else if (ae.getActionCommand().equalsIgnoreCase("closeRom"))
        {
            closeRom();
        }
        else if (ae.getActionCommand().startsWith("revert-"))
        {
            this.restoreFromBackup(ae.getActionCommand().substring(7));
        }
        else if (ae.getActionCommand().startsWith("recentLoad"))
        {
            loadRom(new File(recentLoadsItems[Integer.parseInt(ae
                .getActionCommand().substring(10))].getText().substring(2)));
        }
        else if (ae.getActionCommand().equalsIgnoreCase("exit"))
        {
            this.exit();
        }
        else if (ae.getActionCommand().equalsIgnoreCase("about"))
        {
            JOptionPane.showMessageDialog(null, this.createScollingLabel(this
                .getFullCredits()), "About" + MainGUI.getDescription() + " "
                + MainGUI.getVersion(), JOptionPane.INFORMATION_MESSAGE);
        }
        else if (ae.getActionCommand().equalsIgnoreCase("directio"))
        {
            //            this.getPrefs().setValueAsBoolean(
            //                "useDirectFileIO",
            //                !rom.isDirectFileIO());
            if (rom.isLoaded && !(rom.isDirectFileIO()))
            {
                int ques = JOptionPane.showConfirmDialog(null,
                    "Do you want to save your changes?", "Save?",
                    JOptionPane.YES_NO_CANCEL_OPTION);
                if (ques == JOptionPane.CANCEL_OPTION)
                {
                    return;
                }
                else if (ques == JOptionPane.YES_OPTION)
                {
                    rom.saveRom();
                }
            }

            File rompath = null;
            if (rom.isLoaded) rompath = rom.getFilePath();

            if (!rom.isDirectFileIO())
            {
                rom = new RomFileIO();
            }
            else
            {
                rom = new Rom();
            }

            //            HackModule.rom = rom;
            if (rompath != null) rom.loadRom(rompath);
            for (int i = 0; i < getModuleCount(); i++)
            {
                try
                {
                    getModuleAt(i).rom = rom;
                    getModuleAt(i).hide();
                    getModuleAt(i).reset();
                }
                catch (NullPointerException npe)
                {
                    //If not module inited yet
                }
            }
            System.gc();

            //System.out.println("Debug: rom classname: " +
            // rom.getClass().getName());
        }
        else if (ae.getActionCommand().startsWith("LAF_"))
        {
            String laf = ae.getActionCommand().substring(4);
            prefs.setValue("laf", laf);
            try
            {
                if (!UIManager.getLookAndFeel().getClass().getName()
                    .equals(laf))
                {
                    UIManager.setLookAndFeel(laf);
                    SwingUtilities.updateComponentTreeUI(SwingUtilities
                        .getRoot(mainWindow));

                    for (Iterator i = moduleList.iterator(); i.hasNext();)
                        ((HackModule) i.next()).updateUI();
                }
            }
            catch (ClassNotFoundException e)
            {
                e.printStackTrace();
            }
            catch (InstantiationException e)
            {
                e.printStackTrace();
            }
            catch (IllegalAccessException e)
            {
                e.printStackTrace();
            }
            catch (UnsupportedLookAndFeelException e)
            {
                e.printStackTrace();
            }
        }
        else if (ae.getActionCommand().equals("selectDefROM"))
        {
            String romType;
            if (rom.isLoaded)
            {
                romType = rom.getRomType();
            }
            else
            {
                //JDialog typeDia = new JDialog(mainWindow, "Select or Enter
                // ROM Type", true);
                //typeDia.getContentPane().setLayout(new BorderLayout());

                JComboBox typeSel = new JComboBox(RomTypeFinder
                    .getRomTypeNames());
                typeSel.setEditable(true);
                typeSel.setSelectedIndex(0);
                //typeDia.add(typeSel, BorderLayout.NORTH);
                if (JOptionPane.showConfirmDialog(mainWindow, typeSel,
                    "Select or Enter ROM Type", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE) != JOptionPane.OK_OPTION)
                    return;

                romType = typeSel.getSelectedItem().toString();
            }
            getPrefs().removeValue(romType + ".orgRomPath");
            getPrefs().removeValue(romType + ".expRomPath");
            requestOrgRomFile(romType);
        }
        else if (ae.getActionCommand().startsWith("module"))
        {
            if (rom.isLoaded)
            {
                getModuleAt(
                    Integer.parseInt(ae.getActionCommand().substring(6)))
                    .show();
            }
            else
            {
                JOptionPane.showMessageDialog(null,
                    "You must load a ROM first!", "Error!",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Shows the specified module with the given input. Calls
     * {@link HackModule#show(Object)}on the module.
     * 
     * @see #showModule(String, Object)
     * @param modClass Class you want to show, must extend HackModule.
     * @param input Input to send to class's {@link HackModule#show(Object)}.
     * @return True if successful, false if module not loaded or input invalid.
     */
    public boolean showModule(Class modClass, Object input)
    {
        for (int i = 0; i < getModuleCount(); i++)
        {
            if (getModuleAt(i).getClass().equals(modClass))
            {
                try
                {
                    getModuleAt(i).show(input);
                    return true;
                }
                catch (IllegalArgumentException e)
                {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Shows the specified module with the given input. Calls
     * {@link HackModule#show(Object)}on the module.
     * 
     * @see #showModule(Class, Object)
     * @param modClass Name of the class you want to show, must extend
     *            HackModule.
     * @param input Input to send to class's {@link HackModule#show(Object)}.
     * @return True if successful, false if module not loaded or input invalid.
     * @throws ClassNotFoundException On named class not existing.
     */
    public boolean showModule(String modClass, Object input)
        throws ClassNotFoundException
    {
        return showModule(Class.forName(modClass), input);
    }

    /**
     * Returns the XMLPreferences instance. Use this to get and set preferences
     * that should be presistant across sessions.
     * 
     * @return the XMLPreferences instance.
     */
    public XMLPreferences getPrefs()
    {
        return this.prefs;
    }

    /**
     * Returns the latest version number or null if current version is latest.
     * Also returns null on failure. Uses the site <a href =
     * "http://anyoneeb.ath.cx:83/jhack/checkversion?ver=0.3.6.5">
     * http://anyoneeb.ath.cx:83/jhack/checkversion?ver=0.3.6.5 </a> to check.
     * 
     * @see #getChangeLog(String)
     * @see #askToUpdate()
     * @return The lastest JHack version number or null if current is latest.
     */
    public static String checkVersion()
    {
        try
        {
            URL checkSite = new URL(
                "http://anyoneeb.ath.cx:83/jhack/checkversion?ver="
                    + MainGUI.getVersion());
            String ver = new String();
            InputStreamReader in = new InputStreamReader(checkSite.openStream());
            char[] cbuf = new char[10];
            while (in.read(cbuf) != -1)
                ver += new String(cbuf);
            ver = ver.trim();
            if (ver.equals("NO UPDATE"))
                return null;
            else
                return ver;
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns the changelog entry for the specified version. Returns null on
     * failure. Uses the website <a href =
     * "http://anyoneeb.ath.cx:83/jhack/changelog?ver=0.3.6.5">
     * http://anyoneeb.ath.cx:83/jhack/changelog?ver=0.3.6.5 </a> for info.
     * 
     * @see #getVersion()
     * @see #checkVersion()
     * @see #askToUpdate()
     * @param version Version to get changelog for.
     * @return Changelog entry for specified version or null on failure.
     */
    public static String getChangeLog(String version)
    {
        try
        {
            URL clogSite = new URL(
                "http://anyoneeb.ath.cx:83/jhack/changelog?ver=" + version);
            String out = new String();
            InputStreamReader in = new InputStreamReader(clogSite.openStream());
            char[] cbuf = new char[1024];
            while (in.read(cbuf) != -1)
                out += new String(cbuf);
            out = out.trim();
            return out;
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Checks for updates and if one is found, asks the user if it should
     * download it. This should be called every time JHack starts if the
     * "checkVersion" pref is set to true.
     * 
     * @see #getChangeLog(String)
     * @see #checkVersion()
     * @return True if new version downloaded, false if not.
     */
    public static boolean askToUpdate()
    {
        try
        {
            String ver;
            if ((ver = checkVersion()) != null)
            {
                JEditorPane clogDisplay = new JEditorPane(new URL(
                    "http://anyoneeb.ath.cx:83/jhack/changelog?ver=" + ver));
                clogDisplay.setEditable(false);
                JScrollPane clogScroll = new JScrollPane(clogDisplay);
                clogScroll.setPreferredSize(new Dimension(250, 150));
                String[] dispverarr = ver.split("\\.");
                String dispver = new String();
                for (int i = 0; i < dispverarr.length; i++)
                {
                    if (i > 0) dispver += ".";
                    dispver += Integer.toString(Integer.parseInt(dispverarr[i],
                        36));
                }
                int ques = JOptionPane.showConfirmDialog(null, clogScroll,
                    "Download JHack v" + dispver + "?",
                    JOptionPane.YES_NO_OPTION);
                if (ques == JOptionPane.YES_OPTION)
                {
                    String[] fileverarr = ver.split("\\.");
                    String filever = new String();
                    for (int i = 0; i < fileverarr.length; i++)
                        filever += fileverarr[i];
                    URL dl = new URL("http://anyoneeb.ath.cx:83/jhack/JHack."
                        + filever + ".jar.zip");
                    InputStream in = dl.openStream();
                    FileOutputStream out = new FileOutputStream(new File(
                        "JHack." + filever + ".jar.zip"));
                    byte[] b = new byte[50000];
                    int tmp;
                    while ((tmp = in.read(b)) != -1)
                    {
                        out.write(b, 0, tmp);
                    }
                    out.close();
                    return true;
                }
            }
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return false;
    }
}