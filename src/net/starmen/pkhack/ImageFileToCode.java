package net.starmen.pkhack;

import java.awt.Image;
import java.awt.image.PixelGrabber;

import javax.swing.ImageIcon;

/**
 * Converts an image file into Java code to draw that image file.
 * Used because <code>new ImageIcon(File)</code> doesn't seem to work on Macs.
 * Assumes that a <code>Graphics</code> object named <code>g</code> is already
 * made with the right <code>getGraphics()</code>.
 * 
 * @author AnyoneEB
 */
public class ImageFileToCode
{
//	public static void main(String[] args)
//	{
//		String dir = "net/starmen/pkhack/",
//			file[] = {"blackenedNess"};
//		for (int i = 0; i < file.length; i++)
//		{
//			imageToCode(dir + file[i] + ".gif");
//		}
//	}
	
	/**
	 * Outputs to screen graphics code based the image at <code>filename</code>.
	 * 
	 * @param filename Filename of the image.
	 */
	public static void imageToCode(String filename)
	{
		System.out.println("//Created by ImageFileToCode from " + filename);
		imageToCode(new ImageIcon(filename).getImage());
	}
	/**
	 * Outputs to screen graphics code based <code>img</code>.
	 * 
	 * @param img Image
	 */
	public static void imageToCode(Image img)
	{
		int w = 16, h = 16;
		int[] pixels = new int[w * h];
		PixelGrabber pg = new PixelGrabber(img, 0, 0, w, h, pixels, 0, w);
		try
		{
			pg.grabPixels();
		}
		catch (InterruptedException e)
		{
			System.err.println("interrupted waiting for pixels!");
			return;
		}
		int[] curCol = toColor(pixels[0]), newCol;
		System.out.println(
			"g.setColor(new Color("
				+ curCol[0]
				+ ", "
				+ curCol[1]
				+ ", "
				+ curCol[2]
				+ "));");
		int pixel;
		for (int j = 0; j < h; j++)
		{
			for (int i = 0; i < w; i++)
			{
				pixel = pixels[j * w + i];
				newCol = toColor(pixel);
				if (newCol[3] != 0) //if not transparent
				{
					if (!(newCol[0] == curCol[0]
						&& newCol[1] == curCol[1]
						&& newCol[2] == curCol[2]
						&& newCol[3] == curCol[3]))
					{
						curCol = toColor(pixel);
						System.out.println(
							"g.setColor(new Color("
								+ curCol[0]
								+ ", "
								+ curCol[1]
								+ ", "
								+ curCol[2]
								+ "));");
					}
					System.out.println(
						"g.drawLine(" + i + "," + j + "," + i + "," + j + ");");
				}
			}
		}
		System.out.println();
	}
	private static int[] toColor(int pixel)
	{
		int[] out = new int[4];
		out[3] = (pixel >> 24) & 0xff;
		out[0] = (pixel >> 16) & 0xff;
		out[1] = (pixel >> 8) & 0xff;
		out[2] = (pixel) & 0xff;

		return out;
	}

}
