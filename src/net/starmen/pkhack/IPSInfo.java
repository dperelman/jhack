/*
 * Created on Jun 22, 2003
 */
package net.starmen.pkhack;


/**
 * Thrown together code to get info on a .ips file, requires source changes to work.
 * May be rewritten as a module at some point if I have reason to.
 * 
 * @author AnyoneEB
 */
public class IPSInfo
{
//	public static void main(ResetButton rb)
//	{
//		JFileChooser jfc = new JFileChooser(AbstractRom.defaultDir);
//		jfc.setFileFilter(new FileFilter()
//		{
//			public boolean accept(File f)
//			{
//				if (f.getAbsolutePath().toLowerCase().endsWith(".ips")
//					|| f.isDirectory())
//				{
//					return true;
//				}
//				return false;
//			}
//			public String getDescription()
//			{
//				return "IPS Files (*.ips)";
//			}
//		});
//		if (jfc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
//		{
//			IPSFile ips = IPSFile.LoadIPSFile(jfc.getSelectedFile());
//			try
//			{
//				FileWriter output =
//					new FileWriter(jfc.getSelectedFile().getName() + ".nfo");
//				for (int i = 0; i < ips.getRecordCount(); i++)
//				{
//					output.write(
//						(Integer.toHexString(ips.getRecord(i).offset)
//							+ " - "
//							+ rb.getRangeName(ips.getRecord(i).offset))
//							+ "\n");
//					System.out.println(ips.getRecordCount() - i);
//				}
//				output.close();
//			}
//			catch (IOException e)
//			{
//				e.printStackTrace();
//			}
//		}
//		else
//		{
//			System.out.println("You must select a .ips file.");
//			System.exit(-1);
//		}
//	}
}
