package co.shoutbreak.service;

import co.shoutbreak.shared.StateManager;
import co.shoutbreak.shared.User;

public interface SBServiceBridgeInterface {
	
	public User getUser();
	public StateManager getStateManager();
	
}