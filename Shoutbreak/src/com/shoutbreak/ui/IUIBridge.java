package com.shoutbreak.ui;

import java.util.List;

import com.shoutbreak.Shout;

public interface IUIBridge {

	public void test(String s);
	public void updateInboxView(List<Shout> shoutsForDisplay);
	public void shoutSent();
	
}
