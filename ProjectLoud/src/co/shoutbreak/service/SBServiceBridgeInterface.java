package co.shoutbreak.service;

import co.shoutbreak.components.SBStateManager;
import co.shoutbreak.components.SBUser;

public interface SBServiceBridgeInterface {
	
	public SBUser getUser();
	public SBStateManager getStateManager();
	
}