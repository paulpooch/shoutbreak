package com.shoutbreak.service;

import com.shoutbreak.UserInfo;
import com.shoutbreak.ui.IUIBridge;

public interface IServiceBridge {
	
	public void registerUIBridge(IUIBridge bridge);
	public void unRegisterUIBridge();
	public void runServiceFromUI();
	public void toggleLocationTracker(boolean turnOn);
	public void stopServiceFromUI();
	public UserInfo pullUserInfo();
	
	// Shout stuff.
	public void markShoutAsRead(String shoutID);
	public void shout(String text, int power);
	public void vote(String shoutID, int vote);
	public void deleteShout(String shoutID);
	
}
