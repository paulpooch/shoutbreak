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
	private int _threadPurpose;
	
	public PollingThread(ThreadSafeMediator threadSafeMediator, Handler uiThreadHandler, int threadPurpose, UUID keyForLife, Message message) {
		SBLog.constructor(TAG);
		_safeM = threadSafeMediator;
		_uiThreadHandler = uiThreadHandler;
		_threadPurpose = threadPurpose;
		_keyForLife = keyForLife;
		_message = message;
	}
	
	public void run() {
		// Beware concurrency issues ahead.
		Polling gateway = new Polling(_safeM, _uiThreadHandler, _threadPurpose, _keyForLife);
		gateway.go(_message);
	}
	
}	