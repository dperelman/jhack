package net.starmen.pkhack.eb;

/**
 * @author Mr. Tenda
 *
 * TODO Write javadoc for this interface
 */
public interface MapGraphicsListener {
	void changedMode(int newMode, int oldMode);
	void changedSector(boolean knowsSector, int sectorX, int sectorY);
	void enableUndo(boolean enable);
}