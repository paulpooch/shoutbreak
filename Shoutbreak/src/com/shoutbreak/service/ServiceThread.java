package com.shoutbreak.service;

import android.os.Message;

// this thread executes something in the state engine then dies
public class ServiceThread implements Runnable {
	
	private StateEngine _stateEngine;
	private Message _message;
	
	public ServiceThread(StateEngine stateEngine, Message message) {
		_stateEngine = stateEngine;	
		_message = message;
	}
	
	public void run() {
		_stateEngine.goToState(_message);
	}
	
}