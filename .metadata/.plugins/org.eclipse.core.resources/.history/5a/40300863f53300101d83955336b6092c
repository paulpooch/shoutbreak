package com.shoutbreak.service;

import android.os.Handler;
import android.os.Message;

public class ServiceThread implements Runnable {

	private Message _message;
	private Handler _uiThreadHandler;
	private User _user;
	
	public ServiceThread(Handler uiThreadHandler, Message message, User user) {
		_message = message;
		_uiThreadHandler = uiThreadHandler;
		_user = user;
	}
	
	public void run() {
		Logic logic = new Logic(_uiThreadHandler, _user);
		logic.go(_message);
	}
	
}
