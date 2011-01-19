package com.shoutbreak.service;

import com.shoutbreak.R;
import com.shoutbreak.Vars;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
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