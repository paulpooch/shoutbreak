package co.shoutbreak.polling;

import java.util.UUID;

import android.os.Handler;
import android.os.Message;
import co.shoutbreak.core.Mediator.ThreadSafeMediator;
import co.shoutbreak.core.utils.SBLog;

public class PollingThread implements Runnable {

	private static final String TAG = "PollingThread";
	
	private ThreadSafeMediator _safeM;
	private Message _message;
	private Handler _uiThreadHandler;
	private UUID _keyForLife;
	
	public PollingThread(ThreadSafeMediator threadSafeMediator, Handler uiThreadHandler, Message message, UUID keyForLife) {
		SBLog.constructor(TAG);
		_safeM = threadSafeMediator;
		_message = message;
		_uiThreadHandler = uiThreadHandler;
		_keyForLife = keyForLife;
	}
	
	public void run() {
		// Beware concurrency issues ahead.
		Polling gateway = new Polling(_safeM, _uiThreadHandler, _keyForLife);
		gateway.go(_message);
	}
	
}	