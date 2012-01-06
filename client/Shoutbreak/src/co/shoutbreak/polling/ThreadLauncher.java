package co.shoutbreak.polling;

import java.util.UUID;

import co.shoutbreak.core.C;
import co.shoutbreak.core.Colleague;
import co.shoutbreak.core.Mediator;
import co.shoutbreak.core.utils.SBLog;
import android.os.Handler;
import android.os.Message;

public class ThreadLauncher implements Colleague {

	private static final String TAG = "ThreadLauncher";
	
	private Mediator _m;
	private Handler _uiThreadHandler;
	private PollingThread _loopingThread; // we need to track this so we can un-post it if app turns off....
									      // The problem case is:
									      // postDelayed a ping loop, location dies, ping tries to run, latitude is null, problem
	private UUID _currentKeyForLife;
	
	public ThreadLauncher(Mediator mediator) {
		SBLog.constructor(TAG);
		_m = mediator;
		_uiThreadHandler = new Handler() {
			@Override
			public void handleMessage(Message message) {				
				_m.handlePollingResponse(message);
			}
		};
		
	}
	
	@Override
	public void unsetMediator() {
		_m = null;		
	}
	
	public void spawnNextPollingThread(Message message) {
		
		CrossThreadPacket oldXPacket = (CrossThreadPacket) message.obj;
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
		
			if (message.what == C.STATE_IDLE && oldXPacket.keyForLife.compareTo(_currentKeyForLife) != 0) {
				return;
			}
		
		}
		newMessage.obj = newXPacket;

		// Do we return to Polling?
		switch (oldThreadPurpose) {
			case C.PURPOSE_LOOP_FROM_UI: {
				launchPollingThread(newMessage, false);
				break;
			}
			case C.PURPOSE_LOOP_FROM_UI_DELAYED: {
				launchPollingThread(newMessage, true);
				break;
			}
			default:
				break;
		}
	}
	
	public void launchPollingThread(Message message, boolean delayed) {
		launchPollingThread(message, delayed, UUID.randomUUID());
	}
	
	public void launchPollingThread(Message message, boolean delayed, UUID keyForLife) {
		PollingThread thread = new PollingThread(_m.getAThreadSafeMediator(), _uiThreadHandler, message, keyForLife);
		if (delayed) {
			_loopingThread = thread;
			_uiThreadHandler.postDelayed(thread, _m.getPollingDelay());
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
		_currentKeyForLife = UUID.randomUUID();
		launchPollingThread(message, false, _currentKeyForLife);
	}
	
	public void stopLaunchingPollingThreads() {
		if (_loopingThread != null) {
			// Remove any threads that are about to be run because they were postDelayed.
			// We no longer have all requirements to run service, so they shouldn't run.
			_uiThreadHandler.removeCallbacks(_loopingThread);
		}
	}

	public void resetPollingToNow() {
		// How do we do this without creating a bunch of polling threads simultaneously?
		// Should only launch if one is postDelayed and we can kill it.
		stopLaunchingPollingThreads();
		startPolling();		
	}	
	
	public void handleShoutStart(String text, int power) {
		Message message = new Message();
		CrossThreadPacket xPacket = new CrossThreadPacket();
		xPacket.purpose = C.PURPOSE_DEATH;
		xPacket.sArgs = new String[] { text };
		xPacket.iArgs = new int[] { power };
		message.obj = xPacket;
		message.what = C.STATE_SHOUT;
		launchPollingThread(message, false);
	}
	
	public void handleVoteStart(String shoutId, int vote) {
		Message message = new Message();
		CrossThreadPacket xPacket = new CrossThreadPacket();
		xPacket.purpose = C.PURPOSE_DEATH;
		xPacket.sArgs = new String[] { shoutId };
		xPacket.iArgs = new int[] { vote };
		message.obj = xPacket;
		message.what = C.STATE_VOTE;
		launchPollingThread(message, false);
	}

}
