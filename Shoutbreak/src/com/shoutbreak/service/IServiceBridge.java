package com.shoutbreak.service;

import com.shoutbreak.ui.IUIBridge;

public interface IServiceBridge {
	public void registerUIBridge(IUIBridge bridge);
	public void unRegisterUIBridge();
	public void runServiceFromUI();
	public void activateLocationTracker();
	public void disableLocationTracker();
	public CellDensity getCurrentCellDensity();
	public void markShoutAsRead(String shoutID);
	public void shout(String text, int power);
	public void vote(String shoutID, int vote);
	public void deleteShout(String shoutID);
}
