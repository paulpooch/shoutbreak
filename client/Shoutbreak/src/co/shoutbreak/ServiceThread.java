package co.shoutbreak;

import co.shoutbreak.Mediator.ThreadSafeMediator;
import co.shoutbreak.shared.C;
import co.shoutbreak.shared.CustomExceptionHandler;
import android.os.Handler;
import android.os.Message;

public class ServiceThread implements Runnable {

	private ThreadSafeMediator _safeM;
	private Message _message;
	private Handler _uiThreadHandler;
	
	public ServiceThread(ThreadSafeMediator threadSafeMediator, Handler uiThreadHandler, Message message) {
		_safeM = threadSafeMediator;
		_message = message;
		_uiThreadHandler = uiThreadHandler;
	}
	
	@Override
	public void run() {
		Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(C.CONFIG_CRASH_REPORT_ADDRESS));
		ProtocolGateway pg = new ProtocolGateway(_safeM, _uiThreadHandler);
		pg.go(_message);
	}
	
}
