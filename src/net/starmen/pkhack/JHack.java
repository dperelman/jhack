package net.starmen.pkhack;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Executable of JHack. Run this to start JHack.
 * 
 * @author AnyoneEB
 */
//Made by AnyoneEB.
//Code released under the GPL - http://www.gnu.org/licenses/gpl.txt
public class JHack
{
    /**
     * Directory where all JHack 
     */
    public static final File JHACK_DIR = new File(System
        .getProperty("user.home")
        + File.separator + ".jhack");
    static
    {
        if (!JHACK_DIR.exists())
            JHACK_DIR.mkdir();
    }

    public static MainGUI main;
    public static OutputStreamViewer out, err;
    protected static boolean useConsole = false;

    /**
     * @return Returns the useConsole.
     */
    public static boolean isUseConsole()
    {
        return useConsole;
    }

    public static void main(String[] args)
    {
        for (int i = 0; i < args.length; i++)
            if (args[i].equals("-c") || args[i].equals("--console"))
                useConsole = true;
        String date = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        out = new OutputStreamViewer("JHack Console", date, "out");
        err = new OutputStreamViewer("JHack Error", date, "err");
        if (!useConsole)
        {
            System.setOut(out.getPrintStream());
            System.setErr(err.getPrintStream());
        }
        else
        {
            out.stop();
            err.stop();
        }

        main = new MainGUI();
        main.loadLastRom();
    }
}