package co.shoutbreak.polling;

import android.os.Handler;
import android.os.Message;
import co.shoutbreak.core.Mediator.ThreadSafeMediator;
import co.shoutbreak.core.utils.SBLog;

public class PollingThread implements Runnable {

	private static final String TAG = "PollingThread";
	
	private ThreadSafeMediator _safeM;
	private Message _message;
	private Handler _uiThreadHandler;
	
	public PollingThread(ThreadSafeMediator threadSafeMediator, Handler uiThreadHandler, Message message) {
		SBLog.constructor(TAG);
		_safeM = threadSafeMediator;
		_message = message;
		_uiThreadHandler = uiThreadHandler;
	}
	
	public void run() {
		// Beware concurrency issues ahead.
		Polling gateway = new Polling(_safeM, _uiThreadHandler);
		gateway.go(_message);
	}
	
}	