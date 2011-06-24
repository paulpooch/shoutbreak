package co.shoutbreak.components;

import java.util.Observable;

import co.shoutbreak.misc.SBLog;

public class SBStateManager extends Observable {

	private final String TAG = "SBStateManager";
		
	private final int UI_ON = 8;
	private final int SERVICE_ON = 4;
	private final int LOCATION_ON = 2;
	private final int DATA_ON = 1;
	private final int ALL_ON = 15;
	
	private int _state;
	
	public int getState() {
		return _state;
	}
	
	private void setState(int newState) {
		boolean isValidState;
		if (newState != _state) {
			SBLog.i(TAG, "changeState(" + newState + ")");
			isValidState = true;
			_state = newState;
		
			switch(newState) {
		
				/* APP OFF */
				case 0:	case 1:	case 2:	case 3:
					SBLog.i(TAG, "APP OFF");
				break;
				
				/* SERVICE WAITING */
				case 4:	case 5:	case 6:
					SBLog.i(TAG, "SERVICE WAITING");
				break;
				
				/* SERVICE RUNNING */
				case 7:
					SBLog.i(TAG, "SERVICE RUNNING");
				break;
				
				/* POWER OFF, BOTH WARNINGS */
				case 8:
					SBLog.i(TAG, "POWER OFF, BOTH WARNINGS");
				break;
					
				/* POWER OFF, LOCATION WARNING */
				case 9:
					SBLog.i(TAG, "POWER OFF, LOCATION WARNING");
				break;
				
				/* POWER OFF, DATA WARNING */
				case 10:
					SBLog.i(TAG, "POWER OFF, DATA WARNING");
				break;
				
				/* POWER OFF, BOTH ON */
				case 11:
					SBLog.i(TAG, "POWER OFF, BOTH ON");
				break;
				
				/* POWER ON, DISABLED */
				case 12: case 13: case 14:
					SBLog.i(TAG, "APP OFF");
				break;
				
				/* POWER ON, FUNCTIONAL */
				case 15:
					SBLog.i(TAG, "APP OFF");
				break;
				
				/* INVALID STATE */
				default:
					SBLog.e(TAG, "Invalid state change");
					isValidState = false;
				break;
			}
			
			// notify observers of state change
			if (isValidState) {
				notifyObservers();
			}
		}
	}
	
	public void enableUI() {
		setState(_state | UI_ON);
	}
	
	public void disableUI() {
		setState(_state & (ALL_ON - UI_ON));
	}
	
	public void enableService() {
		setState(_state | SERVICE_ON);
	}
	
	public void disableService() {
		setState(_state & (ALL_ON - SERVICE_ON));
	}
	
	public void enableLocation() {
		setState(_state | LOCATION_ON);
	}
	
	public void disableLocation() {
		setState(_state & (ALL_ON - LOCATION_ON));
	}
	
	public void enableData() {
		setState(_state | DATA_ON);
	}
	
	public void disableData() {
		setState(_state & (ALL_ON - DATA_ON));
	}
	
}
