package net.starmen.pkhack;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

/**
 * Uses {@link IPSFile} to make a patch based on the loaded ROM and another user-selected file.
 * 
 * @author AnyoneEB
 */
public class IPSPatchMaker extends GeneralHackModule
{
	/**
     * @param rom
     * @param prefs
     */
    public IPSPatchMaker(Rom rom, XMLPreferences prefs) {
        super(rom, prefs);
    }

    Rom orgRom = new Rom(); //ROM to base patch off of

	protected void init()
	{}
	/**
	 * @see net.starmen.pkhack.HackModule#getVersion()
	 */
	public String getVersion()
	{
		return "0.6";
	}

	/**
	 * @see net.starmen.pkhack.HackModule#getDescription()
	 */
	public String getDescription()
	{
		return "IPS Patch Maker";
	}

	/**
	 * @see net.starmen.pkhack.HackModule#getCredits()
	 */
	public String getCredits()
	{
		return "Written by AnyoneEB";
	}

	/**
	 * @see net.starmen.pkhack.HackModule#show()
	 */
	public void show()
	{
		JOptionPane.showMessageDialog(
			null,
			"Select a base ROM for the IPS patch.",
			"Select a base ROM",
			JOptionPane.INFORMATION_MESSAGE);
		if (!orgRom.loadRom())
		{
			return;
		}
		if (rom.length() != orgRom.length())
		{
			JOptionPane.showMessageDialog(
				null,
				"Cannot create patch. ROMs are different sizes.",
				"Error!",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		IPSFile ips = rom.createIPS(orgRom);
		if (ips.getRecordCount() == 0)
		{
			JOptionPane.showMessageDialog(
				null,
				"Cannot create patch. ROMs are identical.",
				"Error!",
				JOptionPane.ERROR_MESSAGE);
		}
		else
		{
			JFileChooser jfc = new JFileChooser(Rom.getDefaultDir());
			jfc.setFileFilter(new FileFilter()
			{
				public boolean accept(File f)
				{
					if (f.getAbsolutePath().toLowerCase().endsWith(".ips")
						|| f.isDirectory())
					{
						return true;
					}
					return false;
				}
				public String getDescription()
				{
					return "IPS Files (*.ips)";
				}
			});
			if (jfc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION)
			{
				ips.saveIPSFile(jfc.getSelectedFile());
/*				try
				{
					FileOutputStream out =
						new FileOutputStream(jfc.getSelectedFile());
					for (int i = 0; i < ips.length(); i++)
					{
						out.write(ips.charAt(i));
					}
					out.close();
				}
				catch (FileNotFoundException e)
				{
					System.out.println(
						"Error: File not saved: File not found.");
					e.printStackTrace();
				}
				catch (IOException e)
				{
					System.out.println(
						"Error: File not saved: Could write file.");
					e.printStackTrace();
				}*/
			}
		}
	}

	/**
	 * @see net.starmen.pkhack.HackModule#hide()
	 */
	public void hide()
	{}
}
