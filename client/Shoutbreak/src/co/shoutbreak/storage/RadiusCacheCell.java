package co.shoutbreak.storage;

public class RadiusCacheCell {

	public int cellX;
	public int cellY;
	public int level;
	public long radius;
	public boolean isSet;
	public String lastUpdated;
	
	public RadiusCacheCell() {
		cellX = -1;
		cellY = -1;
		level = -1;
		radius = -1;		
		isSet = false;
	}
	
}
