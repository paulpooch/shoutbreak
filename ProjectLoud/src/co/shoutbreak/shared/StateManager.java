package co.shoutbreak.shared;

import java.util.Observable;

import android.widget.Toast;

import co.shoutbreak.shared.utils.SBLog;

public class StateManager extends Observable {

	// IMPORTANT THINGS TO KNOW:
	// 1. An update() observer method can never fire another event.  This would lead to an endless notifyObservers loop.
	// 2. Always set state updates when they occur.
	
	private final String TAG = "SBStateManager";
	
	private int isUIOn = -1; // -1 indicates unknown. 0 = off. 1 = on.
	private int isServiceAlive = -1;
	private int isServiceBound = -1;
	private int isPollingOn = -1;
	private int isDataAvailable = -1;
	private int isLocationAvailable = -1;
	private int isLocationTrackerUsingGPS = -1;
	private int isUserOverlayUsingGPS = -1;
	private int isInputEnabled = -1;
	private int isPowerButtonOn = -1;
	private int isPowerPrefOn = -1;
	private int isUserOverlayVisible = -1;
	
	public void fireStateEvent(StateEvent e) {
		SBLog.i(TAG, "StateManager.fireStateEvent\n" + e.toLogString());
		updateState(e);
		setChanged();
		notifyObservers(e);
	}
	
	private void updateState(StateEvent e) {
		// TODO: here is where we can update the state
		// There are more than 16 states... for example polling & service are 2 different flags, so at least 32
		// We'll let need dictate if/how this needs to be done.

	}
	
	public void shout(String text, int power) {
		StateEvent e = new StateEvent();
		e.uiJustSentShout = true;
		e.shoutText = text;
		e.shoutPower = power;
		fireStateEvent(e);
	}
	
	public boolean isAppFullyFunctional() {
		boolean result;
		// TODO: This function sucks. Mainly just here to show we need something like this.
		if (isUIOn + isServiceAlive + isServiceBound + isPollingOn + isLocationAvailable > 4) {
			result = true;
		} else {
			result = false;
		}
		
		/*
		boolean result = false;
		if (isUIOn + isServiceAlive + isServiceBound + isPollingOn + isDataAvailable + isLocationAvailable + isLocationTrackerUsingGPS + isUserOverlayUsingGPS + isInputEnabled + isPowerButtonOn  + isPowerPrefOn + isServiceBound + isUserOverlayVisible > 12) {
			result = true;
		}*/
		
		return result;
	}
	
	// Nothing below can fire Events or will have an endless cycle.
	
	private void updateLog() {
		StringBuffer sb = new StringBuffer();
		sb.append("\n/////////////////////////////////////////////////////////////\n");
		sb.append("////////////////////// STATE UPDATE /////////////////////////\n");
		sb.append("/////////////////////////////////////////////////////////////\n");
		sb.append("isUIOn = " + isUIOn + "\n");
		sb.append("isServiceAlive = " + isServiceAlive + "\n");
		sb.append("isServiceBound = " + isServiceBound + "\n");
		sb.append("isPollingOn = " + isPollingOn + "\n");
		sb.append("isDataAvailable = " + isDataAvailable + "\n");
		sb.append("isLocationAvailable = " + isLocationAvailable + "\n");
		sb.append("isLocationTrackerUsingGPS = " + isLocationTrackerUsingGPS + "\n");
		sb.append("isUserOverlayUsingGPS = " + isUserOverlayUsingGPS + "\n");
		sb.append("isInputEnabled = " + isInputEnabled + "\n");
		sb.append("isPowerButtonOn = " + isPowerButtonOn + "\n");
		sb.append("isPowerPrefOn = " + isPowerPrefOn + "\n");
		sb.append("isServiceBound = " + isServiceBound + "\n");
		sb.append("isUserOverlayVisible = " + isUserOverlayVisible + "\n");
		SBLog.i(TAG, sb.toString());
	}
	
	public boolean isLocationAvailable() {
		return isLocationAvailable == 1;
	}
	
	public void setIsUIOn(boolean b) {
		isUIOn = (b) ? 1 : 0;
		updateLog();
	}
	
	public void setIsServiceAlive(boolean b) {
		isServiceAlive = (b) ? 1 : 0;
		updateLog();
	}
	
	public void setIsServiceBound(boolean b) {
		isServiceBound = (b) ? 1 : 0;
		updateLog();
	}
	
	public void setIsPollingOn(boolean b) {
		isPollingOn = (b) ? 1 : 0;
		updateLog();
	}
	
	public void setIsDataAvailable(boolean b) {
		isDataAvailable = (b) ? 1 : 0;
		updateLog();
	}
	
	public void setIsLocationAvailable(boolean b) {
		isLocationAvailable = (b) ? 1 : 0;
		updateLog();
	}
	
	public void setIsLocationTrackerUsingGPS(boolean b) {
		isLocationTrackerUsingGPS = (b) ? 1 : 0;
		updateLog();
	}
	
	public void setIsUserOverlayUsingGPS(boolean b) {
		isUserOverlayUsingGPS = (b) ? 1 : 0;
		updateLog();
	}
	
	public void setIsUserOverlayVisible(boolean b) {
		isUserOverlayVisible = (b) ? 1 : 0;
		updateLog();
	}
	
	public void setIsPowerButtonOn(boolean b) {
		isPowerButtonOn = (b) ? 1 : 0;
		updateLog();
	}
	
	public void setIsPowerPrefOn(boolean b) {
		isPowerPrefOn = (b) ? 1 : 0;
		updateLog();
	}

	
	/*
	// events
	public static final int UPDATE_DENSITY = -1;
	
	// state changes
	public static final int ENABLE_UI = 1;
	public static final int DISABLE_UI = 2;
	
	// app state
	public boolean isUIEnabled;
	
	// command handler
	public void call(int cmd) {

		// handle state change
		if (cmd > 0) {
			
			// don't do anything if already enabled
			switch (cmd) {
	
				case ENABLE_UI:
					if (isUIEnabled) return;
					break;
	
				case DISABLE_UI:
					if (!isUIEnabled) return;
					break;
			}
			
			setChanged(); // ignore. this must be called.
			notifyObservers(cmd); // send the cmd to observers
			
			// change the state after the observers receive the cmd
			switch (cmd) {
				case ENABLE_UI: case DISABLE_UI:
					isUIEnabled = !isUIEnabled;
					break;
			}
			
		// handle event
		} else {
			setChanged();
			notifyObservers(cmd); // send the event to observers
		}
	}
}
*/
	
	
	
	
	
//	private final int DATA_ON = 1;
//	private final int LOCATION_ON = 2;
//	private final int SERVICE_ON = 4;
//	private final int UI_ON = 8;
//	
//	public static final int DISABLE_POLLING = 0;
//	public static final int ENABLE_POLLING = 1;
//	public static final int DISABLE_UI = 2;
//	public static final int ENABLE_UI = 3;
//		
//	private boolean _isUIOn = false;
//	private boolean _isServiceOn = false;
//	private boolean _isLocationOn = false;
//	private boolean _isDataOn = false;
//	
//	private int _state;
//	
//	public int getState() {
//		return _state;
//	}
//	
//	private void setState(int newState) {
//		boolean isValidState;
//		if (newState != _state) {
//			SBLog.i(TAG, "changeState(" + newState + ")");
//			isValidState = true;
//			_state = newState;
//		
//			switch (newState) {
//		
//				/* APP OFF */
//				case 0:	case 1:	case 2:	case 3:
//					SBLog.i(TAG, "APP OFF");
//				break;
//				
//				/* SERVICE WAITING */
//				case 4:	case 5:	case 6:
//					SBLog.i(TAG, "SERVICE WAITING");
//				break;
//				
//				/* SERVICE RUNNING */
//				case 7:
//					SBLog.i(TAG, "SERVICE RUNNING");
//				break;
//				
//				/* POWER OFF, BOTH WARNINGS */
//				case 8:
//					SBLog.i(TAG, "POWER OFF, BOTH WARNINGS");
//				break;
//					
//				/* POWER OFF, LOCATION WARNING */
//				case 9:
//					SBLog.i(TAG, "POWER OFF, LOCATION WARNING");
//				break;
//				
//				/* POWER OFF, DATA WARNING */
//				case 10:
//					SBLog.i(TAG, "POWER OFF, DATA WARNING");
//				break;
//				
//				/* POWER OFF, BOTH ON */
//				case 11:
//					SBLog.i(TAG, "POWER OFF, BOTH ON");
//				break;
//				
//				/* POWER ON, DISABLED */
//				case 12: case 13: case 14:
//					SBLog.i(TAG, "APP OFF");
//				break;
//				
//				/* POWER ON, FUNCTIONAL */
//				case 15:
//					SBLog.i(TAG, "APP OFF");
//				break;
//				
//				/* INVALID STATE */
//				default:
//					SBLog.e(TAG, "Invalid state change");
//					isValidState = false;
//				break;
//			}
//			
//			// notify observers of state change
//			if (isValidState) {
//				setChanged();
//				notifyObservers();
//			}
//		}
//	}
//	
//	public void enableUI() {
//		setState(_state | UI_ON);
//		_isUIOn = true;
//	}
//	
//	public void disableUI() {
//		setState(_state - UI_ON);
//		_isUIOn = false;
//	}
//	
//	public void enableService() {
//		setState(_state | SERVICE_ON);
//		_isServiceOn = true;
//	}
//	
//	public void disableService() {
//		setState(_state - SERVICE_ON);
//		_isServiceOn = false;
//	}
//	
//	public void enableLocation() {
//		setState(_state | LOCATION_ON);
//		_isLocationOn = true;
//	}
//	
//	public void disableLocation() {
//		setState(_state - LOCATION_ON);
//		_isLocationOn = false;
//	}
//	
//	public void enableData() {
//		setState(_state | DATA_ON);
//		_isDataOn = true;
//	}
//	
//	public void disableData() {
//		setState(_state - DATA_ON);
//		_isDataOn = false;
//	}
	
}
