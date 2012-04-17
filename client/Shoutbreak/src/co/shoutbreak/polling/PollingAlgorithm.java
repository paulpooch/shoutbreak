package co.shoutbreak.polling;

import java.util.Date;

import co.shoutbreak.core.C;
import co.shoutbreak.core.Mediator;
import co.shoutbreak.core.utils.SBLog;

public class PollingAlgorithm {

	private static final String TAG = "PollingAlgorithm";
	
	private static final long DELAY_MIN_SECS = 20; // 20 secs
	private static final long DELAY_MAX_SECS = 600; // 10 mins
	private static final long SECONDS_TILL_MAX_DELAY = 600; // 10 mins
	private static final long RESET_POLLING_DELAY_TO_IMMEDIATELY_TOLERANCE_MILLISEC = DELAY_MIN_SECS * 1000; // 20 seconds
	
	private static int consecutiveDroppedPackets = 0;
	
	private Date _lastActivity;
	private float _delayPerSecondElapsed;
	
	public PollingAlgorithm() {
		SBLog.constructor(TAG);
		_lastActivity = new Date();
		_delayPerSecondElapsed = (float)(DELAY_MAX_SECS - DELAY_MIN_SECS) / (float)SECONDS_TILL_MAX_DELAY;
	}
	
	public synchronized void resetPollingDelay(Mediator mediator) {
		Date now = new Date();
		if (mediator.getIsUiInForeground()) {
			long elapsedMilliseconds = now.getTime() - _lastActivity.getTime();
			if (elapsedMilliseconds > RESET_POLLING_DELAY_TO_IMMEDIATELY_TOLERANCE_MILLISEC) {
				mediator.resetPollingToNow();
			}
		}
		_lastActivity = now;
	}
	
	// returns milliseconds
	public long getPollingDelay(Mediator mediator) {
		if (mediator.getIsUiInForeground()) {
			return DELAY_MIN_SECS * 1000;
		} else {
			Date now = new Date();
			long elapsedMilliseconds = now.getTime() - _lastActivity.getTime();
			long elapsedSeconds = elapsedMilliseconds / 1000;
			long delay = DELAY_MAX_SECS;
			if (elapsedSeconds < SECONDS_TILL_MAX_DELAY) {
				delay = (long) (elapsedSeconds * _delayPerSecondElapsed); // seconds elapsed * delayPerSecondElapsed
			}
			delay = DELAY_MIN_SECS + delay;
			SBLog.polling(delay);
			return delay * 1000;
		}
	}

	public static boolean isDropCountAtLimit() {
		consecutiveDroppedPackets++;
		if (consecutiveDroppedPackets >= C.CONFIG_DROPPED_PACKET_LIMIT) {
			return true;
		} else {
			return false;
		}
	}

	public static void resetDropCount() {
		consecutiveDroppedPackets = 0;		
	}
	
}
