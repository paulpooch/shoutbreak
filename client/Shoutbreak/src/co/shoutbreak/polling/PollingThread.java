package co.shoutbreak.polling;

import android.os.Handler;
import android.os.Message;
import co.shoutbreak.core.C;
import co.shoutbreak.core.Mediator.ThreadSafeMediator;
import co.shoutbreak.core.utils.CrashReportingExceptionHandler;

public class PollingThread implements Runnable {

	private ThreadSafeMediator _safeM;
	private Message _message;
	private Handler _uiThreadHandler;
	
	public PollingThread(ThreadSafeMediator threadSafeMediator, Handler uiThreadHandler, Message message) {
		_safeM = threadSafeMediator;
		_message = message;
		_uiThreadHandler = uiThreadHandler;
	}
	
	public void run() {
		// Beware concurrency issues ahead.
		Thread.setDefaultUncaughtExceptionHandler(new CrashReportingExceptionHandler(C.CONFIG_CRASH_REPORT_ADDRESS));
		Polling gateway = new Polling(_safeM, _uiThreadHandler);
		gateway.go(_message);
	}
	
}	