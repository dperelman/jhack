package net.starmen.pkhack;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.filechooser.FileFilter;

/**
 * Uses {@link IPSFile} to apply a user-selected patch to the loaded ROM.
 * 
 * @author AnyoneEB
 */
public class IPSPatchApplier extends GeneralHackModule
{
	/**
     * @param rom
     * @param prefs
     */
    public IPSPatchApplier(Rom rom, XMLPreferences prefs) {
        super(rom, prefs);
    }

    protected void init()
	{}

	/**
	 * @see net.starmen.pkhack.HackModule#getVersion()
	 */
	public String getVersion()
	{
		return "0.2";
	}

	/**
	 * @see net.starmen.pkhack.HackModule#getDescription()
	 */
	public String getDescription()
	{
		return "IPS Patch Applier";
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
		if (jfc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
		{
			JFrame patchDialog = new JFrame("Patching...");
			patchDialog.invalidate();
			patchDialog.getContentPane().add(
				new JLabel("Patching, please wait..."));
			patchDialog.setSize(200, 100);
			patchDialog.setLocation(400, 400);
			patchDialog.validate();
			patchDialog.setVisible(true);
			rom.apply(IPSFile.loadIPSFile(jfc.getSelectedFile()));
			patchDialog.setVisible(false);
		}
	}

	/**
	 * @see net.starmen.pkhack.HackModule#hide()
	 */
	public void hide()
	{}

}
