package co.shoutbreak;

import co.shoutbreak.Mediator.ThreadSafeMediator;
import co.shoutbreak.shared.C;
import android.os.Handler;
import android.os.Message;

public class PollingThreadLauncher implements Colleague {

	private Mediator _m;
	private Handler _uiThreadHandler;
	
	public PollingThreadLauncher() {
		
		_uiThreadHandler = new Handler() {
			@Override
			public void handleMessage(Message message) {				
				_m.handlePollingResponse(message);
			}
		};
		
	}
	
	public void spawnNextPollingThread(Message message) {
		
		CrossThreadPacket oldXPacket = (CrossThreadPacket) message.obj;
		int oldUiCode = oldXPacket.uiCode;
		int oldThreadPurpose = oldXPacket.purpose;

		// We need to re-create a new Message object. The old one falls out of scope.
		// This is probably good for dumping stale data anyway.
		Message newMessage = new Message();
		newMessage.what = message.what;
		CrossThreadPacket newXPacket = new CrossThreadPacket();
		// Sometimes JSON needs to carry between states.
		newXPacket.json = oldXPacket.json;	
		// Only logic can decide if we are allowed to delay. 
		// A lot of times we need immediate follow-up loop.
		if (oldThreadPurpose == C.PURPOSE_LOOP_FROM_UI || oldThreadPurpose == C.PURPOSE_LOOP_FROM_UI_DELAYED) {
			newXPacket.purpose = C.PURPOSE_LOOP_FROM_UI;
		}
		newMessage.obj = newXPacket;

		// This should probably not exist when we're done.
		// Anything to do on UI?
		switch (oldUiCode) {
			/*
			case C.UI_RECEIVE_SHOUTS: {
				int newShouts = _user.getShoutsJustReceived();
				if (newShouts > 0) {
					String pluralShout = "shout" + (newShouts > 1 ? "s" : "");
					giveStatusBarNotification(newShouts + " " + pluralShout + " received", "Shoutbreak", "you have " + newShouts + " new " + pluralShout);
				}
				break;
			}
			*/
		}

		// Do we return to Logic?
		switch (oldThreadPurpose) {
			case C.PURPOSE_LOOP_FROM_UI: {
				launchPollingThread(newMessage, false);
				break;
			}
			case C.PURPOSE_LOOP_FROM_UI_DELAYED: {
				launchPollingThread(newMessage, true);
				break;
			}
		}
	}

	public void launchPollingThread(Message message, boolean delayed) {
		PollingThread thread = new PollingThread(_uiThreadHandler, message);
		if (delayed) {
			_uiThreadHandler.postDelayed(thread, C.CONFIG_IDLE_LOOP_TIME_WITH_UI_OPEN);
		} else {
			_uiThreadHandler.post(thread);
		}
	}
	
	public void startPolling() {
		Message message = new Message();
		CrossThreadPacket xPacket = new CrossThreadPacket();
		xPacket.purpose = C.PURPOSE_LOOP_FROM_UI;
		message.obj = xPacket;
		message.what = C.STATE_IDLE;
		launchPollingThread(message, false);
	}
	
	public void stopPolling() {
		// I don't think there's anything to do here.
	}
	
	public class PollingThread implements Runnable {

		private Message _message;
		private Handler _uiThreadHandler;
		
		public PollingThread(Handler uiThreadHandler, Message message) {
			_message = message;
			_uiThreadHandler = uiThreadHandler;
		}
		
		public void run() {
			// Beware concurrency issues ahead.
			Thread.setDefaultUncaughtExceptionHandler(new CrashReportingExceptionHandler(C.CONFIG_CRASH_REPORT_ADDRESS));
			ThreadSafeMediator safeMediator = _m.getAThreadSafeMediator();
			ProtocolGateway gateway = new ProtocolGateway(safeMediator, _uiThreadHandler);
			gateway.go(_message);
		}
		
	}

	@Override
	public void setMediator(Mediator mediator) {
		_m = mediator;		
	}

	@Override
	public void unsetMediator() {
		_m = null;		
	}
	
}
