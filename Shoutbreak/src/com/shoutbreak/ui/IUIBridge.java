package com.shoutbreak.ui;

import java.util.List;

import com.shoutbreak.Shout;

public interface IUIBridge {
	
	public void updateInboxView(List<Shout> shoutsForDisplay);
	public void shoutSent();
	public void setPopulationDensity(double density);
	public void giveNoticeUI(String s);
	
}
