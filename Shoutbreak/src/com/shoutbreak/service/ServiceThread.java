package com.shoutbreak.service;

import com.shoutbreak.CustomExceptionHandler;
import com.shoutbreak.Vars;

import android.os.Handler;
import android.os.Message;

// this thread executes something in the state engine then dies
public class ServiceThread implements Runnable {
	
	private StateEngine _stateEngine;
	private Message _message;
	
	public ServiceThread(Handler uiThreadHandler, User user, Message message) {
		Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(Vars.CRASH_REPORT_ADDRESS));
		_stateEngine = new StateEngine(uiThreadHandler, user);
		_message = message;
	}
	
	public void run() {
		_stateEngine.goToState(_message);
	}
	
}