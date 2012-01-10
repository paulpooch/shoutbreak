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
		// Make sure we pass on the keyForLife... or polling thread will not be allowed to continue relaunching.
		UUID oldKeyForLife = oldXPacket.keyForLife;
		int oldThreadPurpose = oldXPacket.purpose;
		int newPurpose = C.PURPOSE_DEATH;
		Message idleMessage = _uiThreadHandler.obtainMessage(C.STATE_IDLE);
		
		// Only logic can decide if we are allowed to delay. 
		// A lot of times we need immediate follow-up loop.
		if (oldThreadPurpose == C.PURPOSE_LOOP_FROM_UI || oldThreadPurpose == C.PURPOSE_LOOP_FROM_UI_DELAYED) {
			newPurpose = C.PURPOSE_LOOP_FROM_UI;
			
			if (message.what == C.STATE_IDLE && oldKeyForLife.compareTo(_currentKeyForLife) == 0) {
				// Do we return to Polling?
				switch (oldThreadPurpose) {
					case C.PURPOSE_LOOP_FROM_UI: {
						launchPollingThread(newPurpose, oldKeyForLife, false, idleMessage);
						break;
					}
					case C.PURPOSE_LOOP_FROM_UI_DELAYED: {
						launchPollingThread(newPurpose, oldKeyForLife, true, idleMessage);
						break;
					}
					default:
						break;
				}
			}
		}	
	}
	
	// Used by one-time trips to polling thread (shout, vote, etc.)
	public void launchDisposableThread(Message message, boolean delayed) {
		launchPollingThread(C.PURPOSE_DEATH, UUID.randomUUID(), delayed, message);
	}
	
	public void launchPollingThread(int threadPurpose, UUID keyForLife, boolean delayed, Message message) {
		PollingThread thread = new PollingThread(_m.getAThreadSafeMediator(), _uiThreadHandler, threadPurpose, keyForLife, message);
		if (delayed) {
			_loopingThread = thread;
			_uiThreadHandler.postDelayed(thread, _m.getPollingDelay());
		} else {
			_uiThreadHandler.post(thread);
		}
	}
	
	public void startPolling() {
		_currentKeyForLife = UUID.randomUUID();
		Message idleMessage = _uiThreadHandler.obtainMessage(C.STATE_IDLE);
		launchPollingThread(C.PURPOSE_LOOP_FROM_UI, _currentKeyForLife, false, idleMessage);
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
		CrossThreadPacket xPacket = new CrossThreadPacket();
		xPacket.sArgs = new String[] { text };
		xPacket.iArgs = new int[] { power };
		Message message = _uiThreadHandler.obtainMessage(C.STATE_SHOUT, xPacket);
		launchDisposableThread(message, false);
	}
	
	public void handleVoteStart(String shoutId, int vote) {
		CrossThreadPacket xPacket = new CrossThreadPacket();
		xPacket.sArgs = new String[] { shoutId };
		xPacket.iArgs = new int[] { vote };
		Message message = _uiThreadHandler.obtainMessage(C.STATE_VOTE, xPacket);
		launchDisposableThread(message, false);
	}

}
