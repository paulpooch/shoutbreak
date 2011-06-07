package com.shoutbreak.service;

public interface IServiceBridge {
	
	public void registerUserListener(UserListener listener);
	public void pullUserInfo();
	public void runServiceFromUI();
	public void toggleLocationTracker(boolean turnOn);
	public void stopServiceFromUI();
	
	// Shout stuff.
	public void markShoutAsRead(String shoutID);
	public void shout(String text, int power);
	public void vote(String shoutID, int vote);
	public void deleteShout(String shoutID);
	
}