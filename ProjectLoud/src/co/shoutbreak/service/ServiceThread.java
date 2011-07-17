package co.shoutbreak.service;

import co.shoutbreak.shared.C;
import co.shoutbreak.shared.User;
import co.shoutbreak.shared.utils.CustomExceptionHandler;
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
		Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(C.CONFIG_CRASH_REPORT_ADDRESS));
		Logic logic = new Logic(_uiThreadHandler, _user);
		logic.go(_message);
	}
 
}
