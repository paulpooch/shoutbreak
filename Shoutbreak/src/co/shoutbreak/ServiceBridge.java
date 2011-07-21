package co.shoutbreak;

import co.shoutbreak.shared.StateManager;
import co.shoutbreak.shared.User;

public interface ServiceBridge {
	
	public StateManager getStateManager();
	public User getUser();
}