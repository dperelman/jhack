package net.starmen.pkhack;


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
        out = new OutputStreamViewer("JHack Console");
        err = new OutputStreamViewer("JHack Error");
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
    }
}