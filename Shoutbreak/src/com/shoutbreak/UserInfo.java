package com.shoutbreak;

import java.util.List;

import com.shoutbreak.service.CellDensity;

// This is all temporary crap for the UI.
// The real data is stored in Database through User methods.
// Use this just for UI stuff.
public class UserInfo {

	// Flags indicate if the data should be looked at
	public boolean flagLevel;
	public boolean flagPopulationDensity;
	public boolean flagInbox;
	
	// Is user's location enabled?
	public boolean isLocationEnabled;
	
	// Did a level up just occur?
	public boolean flagLevelUp;
	
	private int _level;
	private int _points;
	private int _nextLevelAt;
	private CellDensity _cellDensity;
	private List<Shout> _displayInbox;
	
	public UserInfo() {
	
		flagLevel = false;
		flagLevelUp = false;
		flagPopulationDensity = false;
		flagInbox = false;
		isLocationEnabled = false;
		
		_level = 0;
		_points = 0;
		_nextLevelAt = 0;
		_cellDensity = null;
		_displayInbox = null;

	}
	
	public void setLevel(int i) {
		_level = i;
	}
	
	public void setPoints(int i) {
		_points = i;
	}
	
	public void setNextLevelAt(int i) {
		_nextLevelAt = i;
	}
	
	public int getLevel() {
		return _level;
	}
	
	public int getPoints() {
		return _points;
	}
	
	public int getNextLevelAt() {
		return _nextLevelAt;
	}
	
	public void setCellDensity(CellDensity c) {
		_cellDensity = c;
	}
	
	public CellDensity getCellDensity() {
		return _cellDensity;
	}
	
	public void setDisplayInbox(List<Shout> l) {
		_displayInbox = l;
	}
	
	public List<Shout> getDisplayInbox() {
		return _displayInbox;
	}
	
}
