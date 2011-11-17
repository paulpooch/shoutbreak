package co.shoutbreak.polling;

import java.util.Date;

import co.shoutbreak.core.utils.SBLog;

public class PollingAlgorithm {

	private static final String TAG = "PollingAlgorithm";
	
	private static final int DELAY_MIN_SECS = 20; // 20 secs
	private static final int DELAY_MAX_SECS = 1200; // 20 mins
	private static final int SECONDS_TILL_MAX_DELAY = 600; // 10 mins
	
	private Date _lastActivity;
	private float _delayPerSecondElapsed;
	
	public PollingAlgorithm() {
		SBLog.constructor(TAG);
		_lastActivity = new Date();
		_delayPerSecondElapsed = (float)(DELAY_MAX_SECS - DELAY_MIN_SECS) / (float)SECONDS_TILL_MAX_DELAY;
	}
	
	public synchronized void resetPollingDelay() {
		_lastActivity = new Date();
	}
	
	// returns milliseconds
	public long getPollingDelay() {
		Date now = new Date();
		long elapsedMilliseconds = now.getTime() - _lastActivity.getTime();
		long delay = (long) ((elapsedMilliseconds / 1000) * _delayPerSecondElapsed); // seconds elapsed * delayPerSecondElapsed
		delay = DELAY_MIN_SECS + delay;
		return delay * 1000;
	}
}
