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
     * Directory where all JHack settings and logs should be stored.
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
        boolean justPatching = false;
        String rom = null, patch = null;
        int size = 0;
        for (int i = 0; i < args.length; i++)
            if (args[i].equals("-c") || args[i].equals("--console"))
                useConsole = true;
            else if ((args[i].equals("-p") || args[i].equals("--patch"))
                && (args.length >= i + 3))
            {
                justPatching = true;
                useConsole = true;
                patch = args[i + 1];
                rom = args[i + 2];
                size = Integer.parseInt(args[i + 3]);
            }
        String date = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        out = new OutputStreamViewer("JHack Console", date, "out");
        err = new OutputStreamViewer("JHack Error", date, "err");
        if (!useConsole)
        {
            System.setOut(out.getPrintStream());
            err.setError(true);
            System.setErr(err.getPrintStream());
        }
        else
        {
            out.stop();
            err.stop();

            String logbasestr = JHACK_DIR.toString() + File.separator + "logs"
                + File.separator + date;
            File outlog = new File(logbasestr + ".out.log"), errlog = new File(
                logbasestr + ".err.log");

            if (outlog.exists())
                while (!outlog.delete())
                    ;
            if (errlog.exists())
                while (!errlog.delete())
                    ;
        }

        main = new MainGUI(!justPatching);

        if (justPatching)
        {
            AbstractRom r = new RomMem();
            if (!r.loadRom(new File(rom)))
            {
                System.exit(-1);
            }
            if (size == 32)
                r.expand();
            else if (size == 48)
                r.expandEx();
            r.apply(IPSFile.loadIPSFile(new File(patch)));
            r.saveRom();
            System.exit(0);
        }
        else
        {

            if (!main.getPrefs().hasValue("autoLoad")
                || main.getPrefs().getValueAsBoolean("autoLoad"))
                main.loadLastRom();
        }
    }
}