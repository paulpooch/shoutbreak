package com.shoutbreak.service;

import org.json.JSONObject;

public class MessageObject {
	// Android's inter-thread messaging system allows you to pass 1 object to a thread.
	// This is the object we use pretty much everywhere.
	
	public int serviceEventCode;

	public JSONObject json;
	
	public String[] args;
	
	public Exception exception;
	
	public boolean isMasterThread = false;
	
}
