package co.shoutbreak.storage;

public class CellDensity {

	public int cellX;
	public int cellY;
	public double density;
	public boolean isSet;
	public String lastUpdated;
	
	public CellDensity() {
		cellX = -1;
		cellY = -1;
		density = -1;
		isSet = false;
	}
	
}