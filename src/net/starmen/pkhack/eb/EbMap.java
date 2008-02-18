package net.starmen.pkhack.eb;

import java.awt.Image;
import java.io.EOFException;
import java.util.ArrayList;
import java.util.ListIterator;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.HackModule;

// Represents the whole EarthBound map and map-related data in the rom.
public class EbMap {
	public static final int[] DOOR_DEST_TYPES = { 1, -1, 0, -2, -2, 2, 2 };
	private static final int mapAddressesPtr = 0xa3db;
	private static final int tsetpalAddress = 0x17AA00;
	private static final int musicAddress = 0x1cd837;
	private static final int dPointersAddress = 0x100200;
	private static final int tsetTblAddress = 0x2F121B;
	private static final int localTsetAddress = 0x175200;
	private static final int spDataEnd = 0xf8b91;
	private static final int spAsmPointer = 0x2461;
	private static final int spDataBase = 0xf0200; // 2 byte ptr + 0xf0200
	private static final int sectorPropsAddress = 0x17b400;
	private static final int enemyLocsAddress = 0x101a80;

	private static int[] mapAddresses;
	private static ArrayList mapChanges;
	private static ArrayList[] spData;
	private static Sector[] sectorData;
	private static int[] drawingTilesets;
	private static ArrayList localTilesetChanges;
	private static ArrayList[] doorData;
	private static int[] oldDoorEntryLengths;
	private static ArrayList destData;
	//private static ArrayList destsLoaded;
	//private static ArrayList destsIndexes;
	private static ArrayList errors;
	private static Image[][][] tileImages;
	private static Image[][] spriteImages;
	private static ArrayList enemyLocChanges;

	public static void reset() {
		mapChanges = null;
		spData = null;
		sectorData = null;
		drawingTilesets = null;
		localTilesetChanges = null;
		doorData = null;
		oldDoorEntryLengths = null;
		destData = null;
		//destsLoaded = null;
		//destsIndexes = null;
		errors = null;
		tileImages = null;
		spriteImages = null;
		mapAddresses = null;
		enemyLocChanges = null;
	}

	// A shortcut for other modules to use.
	public static void loadData(HackModule hm, boolean sprites,
			boolean doors, boolean hotspots, boolean enemies,
			boolean loadMapChanges) {
		if (mapChanges == null)
			mapChanges = new ArrayList();
		if (sectorData == null)
			sectorData = new Sector[MapEditor.heightInSectors
			        				* MapEditor.widthInSectors];
		if (drawingTilesets == null) {
			drawingTilesets = new int[MapEditor.mapTsetNum];
			for (int i = 0; i < drawingTilesets.length; i++)
				drawingTilesets[i] = -1;
			loadDrawTilesets(hm.rom);
		}
		if (localTilesetChanges == null)
			localTilesetChanges = new ArrayList();
		if (errors == null)
			errors = new ArrayList();
		if (spriteImages == null)
			spriteImages = new Image[SpriteEditor.NUM_ENTRIES][8];
		if (mapAddresses == null) {
			mapAddresses = new int[8];
			for (int i = 0; i < mapAddresses.length; i++)
				mapAddresses[i] = -1;
			loadMapAddresses(hm.rom);
		}
		
		if (sprites && (spData == null)) {
			spData = new ArrayList[(MapEditor.heightInSectors / 2) * MapEditor.widthInSectors];
			TPTEditor.readFromRom(hm);
			SpriteEditor.readFromRom(hm.rom);
			loadSpriteData(hm.rom);
		}
		if (doors && (doorData == null)) {
			doorData = new ArrayList[(MapEditor.heightInSectors / 2) * MapEditor.widthInSectors];
			oldDoorEntryLengths = new int[(MapEditor.heightInSectors / 2) * MapEditor.widthInSectors];
			destData = new ArrayList();
			         		
			loadDoorData(hm.rom);
		}
		if (hotspots)
			HotspotEditor.readFromRom(hm);
		if (enemies && (enemyLocChanges == null)) {
			enemyLocChanges = new ArrayList();
			EnemyPlacementGroupsEditor.readFromRom(hm.rom);
			BattleEntryEditor.readFromRom(hm.rom);
			EnemyEditor.readFromRom(hm);
		}
		if (loadMapChanges)
			MapEventEditor.readFromRom(hm.rom);
	}

	public static void loadData(HackModule hm, boolean sprites,
			boolean doors, boolean hotspots, boolean enemies) {
		loadData(hm, sprites, doors, hotspots, false, false);
	}

	public static void loadData(HackModule hm, boolean sprites,
			boolean doors, boolean hotspots) {
		loadData(hm, sprites, doors, hotspots, false);
	}

	public static void loadMapAddresses(AbstractRom rom) {
		int address = HackModule.toRegPointer(rom.readMulti(mapAddressesPtr, 3));
		for (int i = 0; i < mapAddresses.length; i++)
			if (mapAddresses[i] == -1)
				mapAddresses[i] = HackModule.toRegPointer(rom.readMulti(address + i
						* 4, 4));
	}

	public static int getMapAddress(int y) {
		int num = y % mapAddresses.length;
		return ((y / mapAddresses.length) * 0x100) + mapAddresses[num];
	}

	public static Image getSpriteImage(int spt, int direction) {
		return spriteImages[spt][direction];
	}

	public static void loadSpriteImage(HackModule hm, int spt, int direction) {
		if (spriteImages[spt][direction] == null) {
			SpriteEditor.SpriteInfoBlock sib = SpriteEditor.sib[spt];
			spriteImages[spt][direction] = new SpriteEditor.Sprite(sib
					.getSpriteInfo(direction), hm).getImage(true);
		}
	}

	public static void resetSpriteImages() {
		spriteImages = new Image[SpriteEditor.NUM_ENTRIES][8];
	}

	private static void loadTileImage(int loadtset, int loadtile,
			int loadpalette) {
		if (tileImages == null)
			tileImages = new Image[MapEditor.drawTsetNum][1024][MapEditor.palsNum];
		
		if (tileImages[loadtset][loadtile][loadpalette] == null) {
			tileImages[loadtset][loadtile][loadpalette] = TileEditor.tilesets[loadtset]
					.getArrangementImage(loadtile, loadpalette);
		}
	}

	public static Image getTileImage(int loadtset, int loadtile,
			int loadpalette) {
		loadTileImage(loadtset, loadtile, loadpalette);
		return tileImages[loadtset][loadtile][loadpalette];
	}

	public static void resetTileImages() {
		tileImages = new Image[TileEditor.TILESET_NAMES.length][1024][MapEditor.palsNum];
	}

	public static void changeTile(int x, int y, int tile) {
		for (int i = 0; i < mapChanges.size(); i++) {
			MapChange change = (MapChange) mapChanges.get(i);
			if ((x == change.getX()) && (y == change.getY())) {
				change.setValue(tile);
				return;
			}
		}
		mapChanges.add(new MapChange(x, y, tile));
	}

	public static int getTile(AbstractRom rom, int x, int y) {
		for (int i = 0; i < mapChanges.size(); i++) {
			MapChange change = (MapChange) mapChanges.get(i);
			if ((x == change.getX()) && (y == change.getY()))
				return change.getValue();
		}

		return rom.read(getMapAddress(y) + x) | (getLocalTilesetROM(rom, x, y) << 8);
	}

	/*public static int[] getTiles(AbstractRom rom, int y, int x, int length) {
		int[] output = new int[length];
		for (int i = 0; i < output.length; i++)
			output[i] = getTile(rom, x + i, y);
		return output;
	}*/

	public static void writeMapChanges(AbstractRom rom) {
		int addr;
		
		for (int i = 0; i < mapChanges.size(); i++) {
			MapChange change = (MapChange) mapChanges.get(i);
			rom.write(getMapAddress(change.getY()) + change.getX(), change.getValue() & 0xff);
			
			addr = localTsetAddress + ((change.getY() / 8) * (MapEditor.width + 1)) + change.getX();
			if (((change.getY() / 4) % 2) == 1)
				addr += 0x3000;
			// To future self: Yes, you did in fact write this code! (two times!) 
			rom.write(addr, (rom.read(addr) & (~ (3 << ((change.getY() % 4) * 2)))) | (change.getValue() / 0x100) << ((change.getY() % 4) * 2));
		}
		mapChanges.clear();	
	}

	private static void loadSectorData(AbstractRom rom, int sectorX,
			int sectorY) {
		if (sectorData[sectorX + (sectorY * MapEditor.widthInSectors)] == null) {
			int sectorNum = sectorX + (sectorY * MapEditor.widthInSectors);
			if (sectorData[sectorNum] == null) {
				int address = tsetpalAddress
						+ (sectorY * ((MapEditor.width + 1) / MapEditor.sectorWidth))
						+ sectorX;
				byte tsetpal_data = rom.readByte(address);
				short music = (short) ((rom
						.read(musicAddress
								+ (sectorY * ((MapEditor.width + 1) / MapEditor.sectorWidth))
								+ sectorX) - 1) & 0xff);

				byte byte1 = rom.readByte(sectorPropsAddress + 2 * sectorNum);
				boolean canTeleport = (byte1 & 0x80) > 0;
				boolean unknown = (byte1 & 0x40) > 0;
				byte townmap = (byte) ((byte1 & 0x38) >> 3);
				byte misc = (byte) (byte1 & 0x7);
				byte item = rom
						.readByte(sectorPropsAddress + 2 * sectorNum + 1);

				sectorData[sectorNum] = new Sector(
						(byte) ((tsetpal_data & 0xf8) >> 3),
						(byte) (tsetpal_data & 0x7), music, canTeleport,
						unknown, townmap, misc, item);
			}
		}
		
	}

	public static Sector getSectorData(AbstractRom rom, int sectorX, int sectorY) {
		loadSectorData(rom, sectorX, sectorY);
		return sectorData[sectorX + (sectorY * MapEditor.widthInSectors)];
	}

	public static void setSectorData(int sectorX, int sectorY, Sector sector) {
		Sector s = sectorData[sectorX + (sectorY * MapEditor.widthInSectors)];
		s.setCantTeleport(sector.cantTeleport());
		s.setItem(sector.getItem());
		s.setMisc(sector.getMisc());
		s.setMusic(sector.getMusic());
		s.setPalette(sector.getPalette());
		s.setTileset(sector.getTileset());
		s.setTownMap(sector.getTownMap());
		s.setUnknown(sector.isUnknownEnabled());
	}

	public static void writeSectorData(AbstractRom rom) {
		for (int i = 0; i < sectorData.length; i++) {
			if (sectorData[i] != null) {
				rom.write(tsetpalAddress + i,
						(sectorData[i].getTileset() << 3)
								+ sectorData[i].getPalette());
				rom.write(musicAddress + i, sectorData[i].getMusic() + 1);
				rom.write(sectorPropsAddress + 2 * i, sectorData[i]
						.getPropsByte1());
				rom.write(sectorPropsAddress + 2 * i + 1, sectorData[i]
						.getItem());
			}
		}
	}

	public static void loadDrawTilesets(AbstractRom rom) {
		for (int i = 0; i < drawingTilesets.length; i++)
			if (drawingTilesets[i] == -1)
				drawingTilesets[i] = rom.read(EbMap.tsetTblAddress
						+ (i * 2));
	}

	public static int getDrawTileset(int mapTset) {
		return drawingTilesets[mapTset];
	}
	
	public static int getLocalTilesetROM(AbstractRom rom, int x, int y) {
		int address = localTsetAddress + ((y / 8) * (MapEditor.width + 1)) + x;
		if (((y / 4) % 2) == 1)
			address += 0x3000;
		return ((rom.read(address) >> ((y % 4) * 2)) & 3);
	}
	
	/*public static int getLocalTileset(AbstractRom rom, int gltx, int glty) {
		for (int i = 0; i < localTilesetChanges.size(); i++) {
			LocalTilesetChange change = (LocalTilesetChange) localTilesetChanges
					.get(i);
			if (change.x == gltx && change.y == glty)
				return change.ltset;
		}
		int address = localTsetAddress
				+ ((glty / 8) * (MapEditor.width + 1)) + gltx;
		if (((glty / 4) % 2) == 1)
			address += 0x3000;
		int local_tset = (rom.read(address) >> ((glty % 4) * 2)) & 3;

		return local_tset;
	}

	public static void setLocalTileset(AbstractRom rom, int tileX,
			int tileY, int newLtset) {
		if (newLtset != getLocalTileset(rom, tileX, tileY)) {
			for (int i = 0; i < localTilesetChanges.size(); i++) {
				LocalTilesetChange change = (LocalTilesetChange) localTilesetChanges
						.get(i);
				if (change.x == tileX && change.y == tileY) {
					change.ltset = newLtset;
					return;
				}
			}
			localTilesetChanges.add(new LocalTilesetChange(tileX, tileY,
					newLtset));
		}
	}

	public static void writeLocalTilesetChanges(AbstractRom rom) {
		for (int i = 0; i < localTilesetChanges.size(); i++) {
			LocalTilesetChange change = (LocalTilesetChange) localTilesetChanges
					.get(i);
			int tilex = change.x, tiley = change.y, newltset = change.ltset;

			int address = localTsetAddress
					+ ((tiley / 8) * (MapEditor.width + 1)) + tilex;
			if (((tiley / 4) % 2) == 1)
				address += 0x3000;
			int newLtsetData = 0, local_tset = rom.read(address), newLtset2set, localtiley = tiley
					- ((tiley / 4) * 4);
			for (int j = 0; j <= 3; j++) {
				if (j == localtiley)
					newLtset2set = newltset;
				else
					newLtset2set = (local_tset >> (j * 2)) & 3;
				newLtsetData += newLtset2set << (j * 2);
			}
			rom.write(address, newLtsetData);
		}
	}*/

	public static boolean isSpriteDataLoaded(int areaNum) {
		return (spData[areaNum] != null);
	}

	public static boolean isSpriteDataLoaded(int areaX, int areaY) {
		return isSpriteDataLoaded(areaX
				+ (areaY * MapEditor.widthInSectors));
	}

	public static int loadSpriteData(AbstractRom rom, int areaNum) {
		int spPtrsAddress = HackModule.toRegPointer(rom.readMulti(
				spAsmPointer, 3)), ptr = rom.readMulti(spPtrsAddress
				+ (areaNum * 2), 2), errorsNum = 0;
		spData[areaNum] = new ArrayList();
		if (ptr > 0) {
			int[] data = rom.read(spDataBase + ptr, (rom.read(spDataBase
					+ ptr) * 4) + 2);
			for (int j = 0; j < data[0]; j++) {
				short tpt = (short) (data[2 + (j * 4)] + (data[3 + (j * 4)] * 0x100));
				TPTEditor.TPTEntry tptEntry;
				try {
					tptEntry = TPTEditor.tptEntries[tpt];
				} catch (java.lang.ArrayIndexOutOfBoundsException e) {
					errors.add(new ErrorRecord(ErrorRecord.SPRITE_ERROR,
							"Sprite entry #" + j + " of area #" + areaNum
									+ " has a bad tpt value (0x"
									+ Integer.toHexString(tpt) + ")"));
					tpt = -1;
					tptEntry = null;
					errorsNum++;
				}

				if (tpt >= 0) {
					SpriteEditor.SpriteInfoBlock sib = SpriteEditor.sib[tptEntry
							.getSprite()];
					short spriteX = (short) (data[5 + (j * 4)] - (sib.width * 4));
					short spriteY = (short) (data[4 + (j * 4)]
							- (sib.height * 8) + 8);
					spData[areaNum].add(new SpriteLocation(tpt, spriteX,
							spriteY));
				}
			}
		}

		return errorsNum;
	}

	public static int loadSpriteData(AbstractRom rom, int areaX, int areaY) {
		return loadSpriteData(rom, areaX
				+ (areaY * MapEditor.widthInSectors));
	}

	public static void loadSpriteData(AbstractRom rom) {
		int errorsNum = 0;
		for (int i = 0; i < spData.length; i++) {
			if (!isSpriteDataLoaded(i))
				errorsNum += loadSpriteData(rom, i);
		}
		if (errorsNum > 0)
			System.out.println(errorsNum + " sprite entry error"
					+ (errorsNum > 1 ? "s" : "")
					+ " found, see Errors menu "
					+ "in Map Editor for details.");
	}

	public static SpriteLocation getSpriteLocation(int areaNum, int num) {
		return (SpriteLocation) spData[areaNum].get(num);
	}

	public static SpriteLocation getSpriteLocation(int areaX, int areaY,
			int num) {
		return getSpriteLocation(
				areaX + (areaY * MapEditor.widthInSectors), num);
	}

	public static short[][] getSpriteLocs(int areaX, int areaY) {
		int areaNum = areaX + (areaY * MapEditor.widthInSectors);

		short[][] returnValue = new short[spData[areaNum].size()][2];

		for (int i = 0; i < spData[areaNum].size(); i++) {
			returnValue[i] = new short[] {
					((SpriteLocation) spData[areaNum].get(i)).getX(),
					((SpriteLocation) spData[areaNum].get(i)).getY() };
		}
		return returnValue;
	}

	public static short[] getSpriteTpts(int areaX, int areaY) {
		int areaNum = areaX + (areaY * MapEditor.widthInSectors);

		short[] returnValue = new short[spData[areaNum].size()];

		for (int i = 0; i < spData[areaNum].size(); i++) {
			returnValue[i] = ((SpriteLocation) spData[areaNum].get(i))
					.getTpt();
		}
		return returnValue;
	}

	public static short[] getSpriteXY(int areaX, int areaY, int spNum) {
		int areaNum = areaX + (areaY * MapEditor.widthInSectors);
		SpriteLocation spLoc = (SpriteLocation) spData[areaNum].get(spNum);
		return new short[] { spLoc.getX(), spLoc.getY() };
	}

	public static short getSpriteTpt(int areaX, int areaY, int spNum) {
		int areaNum = areaX + (areaY * MapEditor.widthInSectors);
		return ((SpriteLocation) spData[areaNum].get(spNum)).getTpt();
	}

	public static ArrayList getSpritesData(int areaNum) {
		return spData[areaNum];
	}

	public static int getSpritesNum(int areaX, int areaY) {
		int areaNum = areaX + (areaY * MapEditor.widthInSectors);
		return spData[areaNum].size();
	}

	public static void removeSprite(int areaNum, int spNum) {
		spData[areaNum].remove(spNum);
	}

	public static void removeSprite(int areaX, int areaY, int spNum) {
		removeSprite(areaX + (areaY * MapEditor.widthInSectors), spNum);
	}

	public static void removeSprite(int areaX, int areaY, short spTpt,
			byte spX, byte spY) {
		int areaNum = areaX + (areaY * MapEditor.widthInSectors);
		spData[areaNum].remove(spData[areaNum].indexOf(new SpriteLocation(
				spTpt, spX, spY)));
	}

	public static int addSprite(int areaX, int areaY, SpriteLocation spLoc) {
		spData[areaX + (areaY * MapEditor.widthInSectors)].add(spLoc);
		return spData[areaX + (areaY * MapEditor.widthInSectors)]
				.indexOf(spLoc);
	}

	public static int addSprite(int areaX, int areaY, short newX,
			short newY, short newTpt) {
		return addSprite(areaX, areaY, new SpriteLocation(newTpt, newX,
				newY));
	}

	public static int findSprite(HackModule hm, int areaX, int areaY,
			short spriteX, short spriteY) {
		int areaNum = areaX + (areaY * MapEditor.widthInSectors);
		for (int i = 0; i < spData[areaNum].size(); i++) {
			SpriteLocation spLoc = (SpriteLocation) spData[areaNum].get(i);
			TPTEditor.TPTEntry tptEntry = TPTEditor.tptEntries[spLoc
					.getTpt()];
			SpriteEditor.SpriteInfoBlock sib = SpriteEditor.sib[tptEntry
					.getSprite()];
			if (((sib.width * 8) > spriteX - spLoc.getX())
					&& (0 < spriteX - spLoc.getX())
					&& ((sib.height * 8) > spriteY - spLoc.getY())
					&& (0 < spriteY - spLoc.getY())) {
				return i;
			}
		}
		return -1;
	}
	
	public static SpriteLocation findSpriteLocation(HackModule hm, int areaX, int areaY,
			short spriteX, short spriteY) {
		int areaNum = areaX + (areaY * MapEditor.widthInSectors);
		for (int i = 0; i < spData[areaNum].size(); i++) {
			SpriteLocation spLoc = (SpriteLocation) spData[areaNum].get(i);
			TPTEditor.TPTEntry tptEntry = TPTEditor.tptEntries[spLoc
					.getTpt()];
			SpriteEditor.SpriteInfoBlock sib = SpriteEditor.sib[tptEntry
					.getSprite()];
			if (((sib.width * 8) > spriteX - spLoc.getX())
					&& (0 < spriteX - spLoc.getX())
					&& ((sib.height * 8) > spriteY - spLoc.getY())
					&& (0 < spriteY - spLoc.getY())) {
				return spLoc;
			}
		}
		return null;
	}

	public static boolean writeSprites(HackModule hm) {
		AbstractRom rom = hm.rom;
		// int spPtrsAddress = HackModule.toRegPointer(rom.readMulti(
		// spAsmPointer, 3));
		ArrayList spriteData = new ArrayList();
		byte[] pointerData = new byte[2 * spData.length];
		int whereToPut = 0xf63e7 - spDataBase;
		for (int i = 0; i < spData.length; i++) {
			if (spData[i].size() == 0) {
				pointerData[i * 2] = 0;
				pointerData[(i * 2) + 1] = 0;
			} else {
				pointerData[i * 2] = (byte) (whereToPut & 0xff);
				pointerData[(i * 2) + 1] = (byte) ((whereToPut & 0xff00) / 0x100);

				byte[] areaData = new byte[2 + (spData[i].size() * 4)];
				areaData[0] = (byte) spData[i].size();
				areaData[1] = 0;
				for (int j = 0; j < spData[i].size(); j++) {
					byte[] data = ((SpriteLocation) spData[i].get(j))
							.toByteArray();
					System.arraycopy(data, 0, areaData, (j * 4) + 2,
							data.length);
				}
				spriteData.add(areaData);
				whereToPut += areaData.length;
			}
		}

		boolean writeOK = hm.writetoFree(pointerData, spAsmPointer, 3,
				pointerData.length, pointerData.length, true);
		if (!writeOK)
			return false;

		byte[] spriteDataArray = toByteArray(spriteData);
		if (spDataBase + (0xf63e7 - spDataBase) + spriteDataArray.length >= spDataEnd)
			return false;
		rom.write(spDataBase + (0xf63e7 - spDataBase), spriteDataArray);

		return true;
	}

	private static byte[] toByteArray(ArrayList list) {
		ListIterator iter = list.listIterator();
		// get the total size of all elements (flattened size)
		int size = 0;
		while (iter.hasNext()) {
			size += ((byte[]) iter.next()).length;
		}

		// now build the flat array
		byte[] retVal = new byte[size];
		iter = list.listIterator();
		int idx = 0; // placeholder
		while (iter.hasNext()) {
			byte[] thisArray = (byte[]) iter.next();

			for (int i = 0; i < thisArray.length; i++) {
				retVal[idx] = thisArray[i];
				idx++;
			}
		}

		return retVal;
	}
	
	public static void nullMapData(AbstractRom rom, boolean nullMap, boolean nullSectors) {
		if (nullMap)
			mapChanges.clear();	
		
		int addr;
		for (int i = 0; i <= MapEditor.height; i++)
			for (int j = 0; j <= MapEditor.width; j++) {
				if (nullSectors && (i % MapEditor.sectorHeight == 0) && (j % MapEditor.sectorWidth == 0)) {
					// sectorData[(i / MapEditor.sectorHeight) * MapEditor.widthInSectors + (j / MapEditor.sectorWidth)].clear();
				}
				if (nullMap && (i % 8 == 0)) {
					addr = localTsetAddress + ((i / 8) * (MapEditor.width + 1)) + j + ((i / 4) % 2 == 1 ? 0x3000 : 0);
					rom.write(addr, 0);
				}
					
				if (nullMap) {
					rom.write(getMapAddress(i) + j, 0);
				}
			}
	}

	public static void nullSpriteData() {
		for (int i = 0; i < spData.length; i++)
			spData[i] = new ArrayList();
	}
	
	public static void nullDoorData() {
		for (int i = 0; i < doorData.length; i++)
			doorData[i] = new ArrayList();
	}
	
	public static void nullEnemyData() {
		for (int i = 0; i < MapEditor.height / 2; i++)
			for (int j = 0; j < MapEditor.width / 2; j++)
				changeEnemyLoc(i, j, (byte) 0);
	}

	public static void loadDoorData(AbstractRom rom) {
		int errors = 0;
		for (int i = 0; i < (MapEditor.heightInSectors / 2)
				* MapEditor.widthInSectors; i++)
			if (doorData[i] == null)
				errors += loadDoorData(rom, i);
		
		// If there are no destinations loaded, create a dummy one to satisfy the Door Editor
		if (destData.size() == 0)
			addNewDestination();
		
		if (errors > 0)
			System.out.println(errors + " door entry error"
					+ (errors > 1 ? "s" : "") + " found"
					+ (errors == 9 ? " (this is normal for clean ROMs)"
							: ", see Errors menu in Map Editor for details."));
	}

	// returns how many errors were encountered
	private static int loadDoorData(AbstractRom rom, int areaNum) {
		int ptr = HackModule.toRegPointer(rom.readMulti(dPointersAddress
				+ (areaNum * 4), 4)), numErrors = 0;
		doorData[areaNum] = new ArrayList();
		oldDoorEntryLengths[areaNum] = rom.read(ptr);
		short doorX, doorY, doorPtr;
		byte doorType;
		for (int i = 0; i < rom.readByte(ptr); i++) {
			doorX = (short) rom.read(ptr + 3 + (i * 5));
			doorY = (short) rom.read(ptr + 2 + (i * 5));
			doorType = rom.readByte(ptr + 4 + (i * 5));
			doorPtr = (short) rom.readMulti(ptr + 5 + (i * 5), 2);
			try {
				if (DOOR_DEST_TYPES[doorType] < 0)
					doorData[areaNum].add(new DoorLocation(doorX, doorY,
							doorType, doorPtr));
				else {
					doorData[areaNum].add(new DoorLocation(doorX, doorY,
							doorType, doorPtr, loadDestData(rom, 0xf0200 + doorPtr, doorType)));
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				errors.add(new ErrorRecord(ErrorRecord.DOOR_ERROR,
						"Could not load door entry #" + i + " of area #"
								+ areaNum + " at 0x"
								+ Integer.toHexString(ptr + (i * 5))
								+ " because it has an invalid type ("
								+ doorType + ")."));
				numErrors++;
			}
		}

		return numErrors;
	}

	public static ArrayList getErrors() {
		return errors;
	}

	public static int getDoorsNum(int areaNum) {
		return doorData[areaNum].size();
	}

	public static int getDoorsNum(int areaX, int areaY) {
		return getDoorsNum(areaX + (areaY * MapEditor.widthInSectors));
	}

	public static short[] getDoorXY(int areaX, int areaY, int doorNum) {
		int areaNum = areaX + (areaY * MapEditor.widthInSectors);
		DoorLocation dLoc = (DoorLocation) doorData[areaNum].get(doorNum);
		return new short[] { dLoc.getX(), dLoc.getY() };
	}

	public static int addDoor(int areaX, int areaY, DoorLocation dLoc) {
		int areaNum = areaX + (areaY * MapEditor.widthInSectors);
		doorData[areaNum].add(dLoc);
		return doorData[areaNum].indexOf(dLoc);
	}

	public static int addDoor(int areaX, int areaY, short doorX,
			short doorY, byte doorType, short doorPtr) {
		return addDoor(areaX, areaY, new DoorLocation(doorX, doorY,
				doorType, doorPtr, (Destination) destData.get(0)));
	}

	public static int addDoor(int areaX, int areaY, short doorX,
			short doorY, byte doorType, short doorPtr, int destNum) {
		return addDoor(areaX, areaY, new DoorLocation(doorX, doorY,
				doorType, doorPtr, (Destination) destData.get(destNum)));
	}

	public static void removeDoor(int areaX, int areaY, int num) {
		int areaNum = areaX + (areaY * MapEditor.widthInSectors);
		doorData[areaNum].remove(num);
	}

	public static DoorLocation getDoorLocation(int areaNum, int num) {
		return (DoorLocation) doorData[areaNum].get(num);
	}

	public static DoorLocation getDoorLocation(int areaX, int areaY, int num) {
		return getDoorLocation(areaX + (areaY * MapEditor.widthInSectors), num);
	}

	public static int findDoor(int areaX, int areaY, short doorX, short doorY) {
		int areaNum = areaX + (areaY * MapEditor.widthInSectors);
		for (int i = 0; i < doorData[areaNum].size(); i++) {
			DoorLocation doorLocation = (DoorLocation) doorData[areaNum]
					.get(i);
			if ((8 >= doorX - (doorLocation.getX() * 8))
					&& (0 <= doorX - (doorLocation.getX() * 8))
					&& (8 >= doorY - (doorLocation.getY() * 8))
					&& (0 <= doorY - (doorLocation.getY() * 8))) {
				return i;
			}
		}
		return -1;
	}
	
	public static int[] findDoor(Destination dest) {
		DoorLocation door;
		for (int i = 0; i < doorData.length; i++) {
			for (int j = 0; j < doorData[i].size(); j++) {
				door = (DoorLocation) doorData[i].get(j);
				if ((door.getDestination() != null) && door.getDestination().equals(dest))
					return new int[] { i-((i/MapEditor.widthInSectors)*MapEditor.widthInSectors), i/MapEditor.widthInSectors, j };
			}
		}
		return null;
	}
	
	public static int getDestIndex(Destination dest) {
		return destData.indexOf(dest);
	}

	public static boolean writeDoors(HackModule hm, boolean oldCompatability) {
		AbstractRom rom = hm.rom;

		ArrayList destPointers = new ArrayList();
		int address = 0;
		Destination dest;
		byte[] oldDestData = rom.readByte(0xf0200, 0xf5ae3-0xf0200);
		byte[] destBytes;
		for (int i = 0; i < destData.size(); i++) {
			dest = (Destination) destData.get(i);
			if (findDoor((Destination) destData.get(i)) == null) {
				// Remove the unused destinations from the arraylist
				destData.remove(i);
				i--;
			} else {
				destBytes = dest.toByteArray();
				
				// Do not let destination data overflow into the next block
				// (The next block is map/music event data)
				if (0xf0200 + address + destBytes.length > 0xf5ae3) {
					rom.write(0xf0200, oldDestData);
					System.out.println("Could not write door data. Your destination data is too large to fit into the ROM.");
					return false;
				}
				
				rom.write(0xf0200 + address, destBytes);
				destPointers.add(new Integer(address));
				address += destBytes.length;
				if (oldCompatability)
					rom.write(0x2e9610 + i, dest.getType());
			}
		}
		// Make sure the editors have at least one destination to work with
		if (destData.size() == 0)
			addNewDestination();
		
		if (oldCompatability) {
			rom.write(0x2e9430, new int[] { 0, 0x96, 0x2e, 0x00, 0xe1, 0x03 });
			rom.write(0x2e9600, destData.size(), 2);
		}

		byte[][] allRawDoorData = new byte[doorData.length][];
		int totalLength = 0;
		DoorLocation doorLocation;
		for (int i = 0; i < doorData.length; i++) {
			// Only bother clearing the old door data if it was in the expanded area
			// After all, if it's at 0xf284f-0xf5ae3, then it could be new destination data
			if (HackModule.toRegPointer(rom.readMulti(dPointersAddress + (i * 4),4)) >= 0x300200)
				hm.nullifyArea(HackModule.toRegPointer(rom.readMulti(dPointersAddress + (i * 4),4)), (oldDoorEntryLengths[i] * 5) + 2);
			
			if (doorData[i].size() > 0) {
				oldDoorEntryLengths[i] = doorData[i].size();
				allRawDoorData[i] = new byte[(doorData[i].size() * 5) + 2];
				totalLength += allRawDoorData[i].length;
				allRawDoorData[i][0] = (byte) doorData[i].size();
				allRawDoorData[i][1] = 0;
				for (int j = 0; j < doorData[i].size(); j++) {
					doorLocation = (DoorLocation) doorData[i].get(j);
					allRawDoorData[i][2 + (j * 5)] = (byte) doorLocation.getY();
					allRawDoorData[i][3 + (j * 5)] = (byte) doorLocation.getX();
					allRawDoorData[i][4 + (j * 5)] = doorLocation.getType();
					if (DOOR_DEST_TYPES[doorLocation.getType()] < 0) {
						short data = (short) (doorLocation.getMisc() & 0xffff);
						allRawDoorData[i][5 + (j * 5)] = (byte) (data & 0xff);
						allRawDoorData[i][6 + (j * 5)] = (byte) (((data & 0xff00) / 0x100) & 0xff);
					} else {
						int destAddress = ((Integer) destPointers.get(destData.indexOf(doorLocation.getDestination()))).intValue();
						allRawDoorData[i][5 + (j * 5)] = (byte) (destAddress & 0xff);
						allRawDoorData[i][6 + (j * 5)] = (byte) ((destAddress & 0xff00) / 0x100);
					}
				}
			} else {
				oldDoorEntryLengths[i] = 0;
				// TODO check if someone hasn't written to 0xA012F...
				rom.write(dPointersAddress + (i * 4), HackModule.toSnesPointer(0xA012F), 4);
			}
		}

		try {
			address = hm.findFreeRange(hm.rom.length(), totalLength);
			for (int i = 0; i < allRawDoorData.length; i++)
				if (allRawDoorData[i] != null) {
					rom.write(address, allRawDoorData[i]);
					rom.write(dPointersAddress + (i * 4), HackModule.toSnesPointer(address), 4);
					address += allRawDoorData[i].length;
				}
			return true;
		} catch (EOFException e) {
			System.out.println("ERROR: findFreeRange: Not enough space found in ROM to write doors!");
			e.printStackTrace();
			return false;
		}

	}

	private static Destination loadDestData(AbstractRom rom, int address, int type) {
		for (int i = 0; i < destData.size(); i++) {
			if (((Destination) destData.get(i)).getAddress() == address)
				return (Destination) destData.get(i);
		}
		Destination dest = null;
		if (DOOR_DEST_TYPES[type] == 0) {
			int pointer = rom.readMulti(address, 4);
			int flag = rom.readMulti(address + 4, 2);
			boolean flagReversed;
			if (flag > 0x8000) {
				flag -= 0x8000;
				flagReversed = true;
			} else
				flagReversed = false;
			short yCoord = (short) rom.read(address + 6);
			yCoord += (short) ((rom.read(address + 7) & 0x3F) << 8);
			short xCoord = (short) (rom.readMulti(address + 8, 2));
			byte style = rom.readByte(address + 10);
			byte direction = (byte) ((rom.read(address + 7) & 0xC0) >> 6);
			dest = new Destination(address, pointer, (short) flag, flagReversed,
					xCoord, yCoord, style, direction);
		} else if (DOOR_DEST_TYPES[type] == 1) {
			short flag = (short) (rom.readMulti(address, 2));
			boolean flagReversed;
			if (flag > 0x8000) {
				flag -= 0x8000;
				flagReversed = true;
			} else
				flagReversed = false;
			int pointer = rom.readMulti(address + 2, 4);
			dest = new Destination(address, flag, flagReversed, pointer);
		} else if (DOOR_DEST_TYPES[type] == 2) {
			int pointer = rom.readMulti(address, 4);
			dest = new Destination(address, pointer);
		}

		if (dest != null) {
			destData.add(dest);
			//System.out.println("Destination #" + Integer.toHexString(destData.size()-1) + ": 0x" + Integer.toHexString(address));
			return dest;
		} else
			return null;
	}

	public static Destination getDestination(int index) {
		return (Destination) destData.get(index);
	}
	
	public static int getNumDests() {
		return destData.size();
	}
	
	public static void removeDestination(int i) {
		if ((i >= 0) && (i < destData.size()))
				destData.remove(i);
	}
	
	public static void addNewDestination() {
		destData.add(new Destination(0,0));
	}

	public static void changeEnemyLoc(int x, int y, byte enemy) {
		for (int i = 0; i < enemyLocChanges.size(); i++) {
			MapChange change = (MapChange) enemyLocChanges.get(i);
			if ((x == change.getX()) && (y == change.getY())) {
				change.setValue(enemy);
				return;
			}
		}
		enemyLocChanges.add(new MapChange(x, y, enemy));
	}

	public static int getEnemyLoc(AbstractRom rom, int x, int y) {
		MapChange change;
		for (int i = 0; i < enemyLocChanges.size(); i++) {
			change = (MapChange) enemyLocChanges.get(i);
			if ((x == change.getX()) && (y == change.getY()))
				return change.getValue() & 0xff;
		}
		return rom.read(enemyLocsAddress + (y * (MapEditor.width + 1) / 2 + x) * 2);
	}

	public static void writeEnemyLocs(AbstractRom rom) {
		MapChange change;
		for (int i = 0; i < enemyLocChanges.size(); i++) {
			change = (MapChange) enemyLocChanges.get(i);
			rom.write(enemyLocsAddress
					+ (change.getY() * (MapEditor.width + 1) / 2 + change.getX()) * 2, change.getValue());
		}
		enemyLocChanges.clear();
	}

	public static class MapChange {
		private int x, y, value;

		public MapChange(int x, int y, int value) {
			this.x = x;
			this.y = y;
			this.value = value;
		}

		public int getX() {
			return x;
		}

		public int getY() {
			return y;
		}

		public int getValue() {
			return value;
		}

		public void setValue(int value) {
			this.value = value;
		}
	}

	/*public static class LocalTilesetChange {
		public int x, y, ltset;

		public LocalTilesetChange(int x, int y, int ltset) {
			this.x = x;
			this.y = y;
			this.ltset = ltset;
		}
	}*/

	public static class SpriteLocation {
		private short x, y, tpt;

		public SpriteLocation(short tpt, short x, short y) {
			this.tpt = tpt;
			this.x = x;
			this.y = y;
		}

		public void setTpt(short tpt) {
			this.tpt = tpt;
		}

		public short getTpt() {
			return tpt;
		}

		public void setX(short x) {
			this.x = x;
		}

		public void setY(short y) {
			this.y = y;
		}

		public short getX() {
			return this.x;
		}

		public short getY() {
			return this.y;
		}

		public byte[] toByteArray() {
			byte[] byteArray = new byte[4];
			TPTEditor.TPTEntry tptEntry = TPTEditor.tptEntries[getTpt()];
			SpriteEditor.SpriteInfoBlock sib = SpriteEditor.sib[tptEntry
					.getSprite()];
			byteArray[0] = (byte) (getTpt() & 0xff);
			byteArray[1] = (byte) (getTpt() / 0x100);
			byteArray[2] = (byte) (getY()
					+ ((short) ((sib.height * 8) & 0xffff)) - 8);
			// - (sib.height * 8) + 8
			byteArray[3] = (byte) (getX() + ((short) ((sib.width * 4) & 0xffff)));

			return byteArray;
		}
	}

	public static class DoorLocation {
		private Destination dest;
		private byte type;
		private short x, y, pointer, misc;

		public DoorLocation(short x, short y, byte type, short pointer, Destination dest) {
			this.x = x;
			this.y = y;
			this.type = type;
			this.pointer = pointer;
			this.dest = dest;
		}

		public DoorLocation(short x, short y, byte type, short misc) {
			this.x = x;
			this.y = y;
			this.type = type;
			this.misc = misc;
			this.dest = null;
		}

		public void setX(short x) {
			this.x = x;
		}

		public short getX() {
			return x;
		}

		public void setY(short y) {
			this.y = y;
		}

		public short getY() {
			return y;
		}

		public void setType(byte type) {
			this.type = type;
		}

		public byte getType() {
			return type;
		}

		public void setPointer(short pointer) {
			this.pointer = pointer;
		}

		public short getPointer() {
			return pointer;
		}

		public int getMiscDirection() {
			if ((misc & 0xffff) == 0x8000)
				return 4;
			else
				return misc / 0x100;
		}

		public void setMiscDirection(int dir) {
			if (dir == 4)
				misc = (short) 0x8000;
			else
				misc = (short) (dir * 0x100);
		}

		public boolean isMiscRope() {
			return (misc & 0xffff) == 0x8000;
		}

		public void setMiscRope(boolean rope) {
			if (rope)
				this.misc = (short) 0x8000;
			else
				this.misc = 0;
		}

		public short getMisc() {
			return misc;
		}

		public Destination getDestination() {
			return dest;
		}

		public void setDestination(Destination dest) {
			this.dest = dest;
		}
	}

	public static class Sector {
		private byte tileset, palette, townmap, misc, item;

		private short music;

		private boolean cantTeleport, unknown;

		public Sector(byte tileset, byte palette, short music,
				boolean cantTeleport, boolean unknown, byte townmap,
				byte misc, byte item) {
			this.tileset = tileset;
			this.palette = palette;
			this.music = music;
			this.cantTeleport = cantTeleport;
			this.unknown = unknown;
			this.townmap = townmap;
			this.misc = misc;
			this.item = item;
		}
		
		public void clear() {
			this.tileset = 0;
			this.palette = 0;
			this.music = 0;
			this.cantTeleport = false;
			this.unknown = false;
			this.townmap = 0;
			this.misc = 0;
			this.item = 0;
		}

		public void setTileset(byte tileset) {
			this.tileset = tileset;
		}

		public byte getTileset() {
			return tileset;
		}

		public void setPalette(byte palette) {
			this.palette = palette;
		}

		public byte getPalette() {
			return palette;
		}

		public void setMusic(short music) {
			this.music = music;
		}

		public short getMusic() {
			return music;
		}

		public boolean cantTeleport() {
			return cantTeleport;
		}

		public void setCantTeleport(boolean cantTeleport) {
			this.cantTeleport = cantTeleport;
		}

		public boolean isUnknownEnabled() {
			return unknown;
		}

		public void setUnknown(boolean unknown) {
			this.unknown = unknown;
		}

		public byte getTownMap() {
			return townmap;
		}

		public void setTownMap(byte townmap) {
			this.townmap = townmap;
		}

		public byte getMisc() {
			return misc;
		}

		public void setMisc(byte misc) {
			this.misc = misc;
		}

		public byte getItem() {
			return item;
		}

		public void setItem(byte item) {
			this.item = item;
		}

		public byte getPropsByte1() {
			byte out = 0;
			out += (cantTeleport ? 1 : 0) << 7;
			out += (unknown ? 1 : 0) << 6;
			out += townmap << 3;
			out += misc;
			return out;
		}
	}

	public static class Destination {
		private int address, pointer;
		private short flag, yCoord, xCoord;
		private byte style, direction, type;
		private boolean flagReversed;

		public Destination(int address, int pointer, short flag, boolean flagReversed,
				short xCoord, short yCoord, byte style, byte direction) {
			this.address = address;
			this.pointer = pointer;
			this.flag = flag;
			this.flagReversed = flagReversed;
			this.xCoord = xCoord;
			this.yCoord = yCoord;
			this.style = style;
			this.direction = direction;
			this.type = 0;
		}

		public Destination(int address, short flag, boolean flagReversed, int pointer) {
			this.address = address;
			this.flag = flag;
			this.flagReversed = flagReversed;
			this.pointer = pointer;
			this.type = 1;
		}

		public Destination(int address, int pointer) {
			this.address = address;
			this.pointer = pointer;
			this.type = 2;
		}
		
		public int getAddress() {
			return address;
		}

		public void setType(byte type) {
			this.type = type;
		}

		public byte getType() {
			return type;
		}
		
		public int getPointer() {
			return pointer;
		}

		public void setPointer(int pointer) {
			this.pointer = pointer;
		}

		public int getFlag() {
			return flag;
		}

		public void setFlag(short flag) {
			this.flag = flag;
		}

		public short getY() {
			return yCoord;
		}

		public void setY(short yCoord) {
			this.yCoord = yCoord;
		}

		public short getX() {
			return xCoord;
		}

		public void setX(short xCoord) {
			this.xCoord = xCoord;
		}

		public byte getStyle() {
			return style;
		}

		public void setStyle(byte style) {
			this.style = style;
		}

		public byte getDirection() {
			return direction;
		}

		public void setDirection(byte direction) {
			this.direction = direction;
		}

		public boolean isFlagReversed() {
			return flagReversed;
		}

		public void setFlagReversed(boolean flagReversed) {
			this.flagReversed = flagReversed;
		}
		
		public String toString() {
			if (type == 0)
				return "Door - (" + xCoord + "," + yCoord + ")";
			else if (type == 1)
				return "Switch - 0x" + HackModule.addZeros(Integer.toHexString(flag + (flagReversed ? 0x8000 : 0)), 4);
			else if (type == 2)
				return "Object - $" + Integer.toHexString(pointer);
			else
				return "Invalid Type";
		}

		public byte[] toByteArray() {
			byte[] byteArray;
			if (type == 0) {
				byteArray = new byte[11];
				byteArray[0] = (byte) (pointer & 0xff);
				byteArray[1] = (byte) ((pointer & 0xff00) / 0x100);
				byteArray[2] = (byte) ((pointer & 0xff0000) / 0x10000);
				byteArray[3] = 0;
				byteArray[4] = (byte) (flag & 0xff);
				byteArray[5] = (byte) ((flag & 0xff00) / 0x100);
				if (flagReversed)
					byteArray[5] += 0x80;
				byteArray[6] = (byte) (yCoord & 0xff);
				byteArray[7] = (byte) (((yCoord & 0xff00) / 0x100) + (direction << 6));
				byteArray[8] = (byte) (xCoord & 0xff);
				byteArray[9] = (byte) ((xCoord & 0xff00) / 0x100);
				byteArray[10] = style;
			} else if (type == 1) {
				byteArray = new byte[6];
				byteArray[0] = (byte) (flag & 0xff);
				byteArray[1] = (byte) ((flag & 0xff00) / 0x100);
				if (flagReversed)
					byteArray[1] += 0x80;
				byteArray[2] = (byte) (pointer & 0xff);
				byteArray[3] = (byte) ((pointer & 0xff00) / 0x100);
				byteArray[4] = (byte) ((pointer & 0xff0000) / 0x10000);
				byteArray[5] = 0;
			} else if (type == 2) {
				byteArray = new byte[4];
				byteArray[0] = (byte) (pointer & 0xff);
				byteArray[1] = (byte) ((pointer & 0xff00) / 0x100);
				byteArray[2] = (byte) ((pointer & 0xff0000) / 0x10000);
				byteArray[3] = 0;
			} else {
				return null;
			}
			return byteArray;
		}
	}

	public static class ErrorRecord {
		public static final int SPRITE_ERROR = 0;

		public static final int DOOR_ERROR = 1;

		private int type;

		private String message;

		public ErrorRecord(int type, String message) {
			this.type = type;
			this.message = message;
		}

		public int getType() {
			return type;
		}

		public String getMessage() {
			return message;
		}
	}
}