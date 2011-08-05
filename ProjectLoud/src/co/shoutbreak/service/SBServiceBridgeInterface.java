package co.shoutbreak.service;

import co.shoutbreak.shared.StateManager;
import co.shoutbreak.shared.User;

public interface SBServiceBridgeInterface {
	
	public StateManager getStateManager();
	public User getUser();
}