package co.shoutbreak.service;

import co.shoutbreak.components.SBStateManager;
import co.shoutbreak.components.SBUser;

public interface ServiceBridgeInterface {
	
	public SBUser getUser();
	public SBStateManager getStateManager();
	
}