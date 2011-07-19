package co.shoutbreak.service;

import co.shoutbreak.shared.C;
import co.shoutbreak.shared.User;
import co.shoutbreak.shared.utils.CustomExceptionHandler;
import android.os.Handler;
import android.os.Message;

public class ServiceThread implements Runnable {
	
	private ShoutbreakService _service;
	private Message _message;
	private Handler _uiThreadHandler;
	private User _user;
	
	public ServiceThread(ShoutbreakService service, Handler uiThreadHandler, Message message, User user) {
		_service = service;
		_message = message;
		_uiThreadHandler = uiThreadHandler;
		_user = user;
	}
	
	public void run() {
		Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(C.CONFIG_CRASH_REPORT_ADDRESS));
		Logic logic = new Logic(_service, _uiThreadHandler, _user);
		logic.go(_message);
	}
 
}
