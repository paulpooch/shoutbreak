package co.shoutbreak.core.utils;

import co.shoutbreak.core.Colleague;
import co.shoutbreak.core.Mediator;
import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class DataListener implements Colleague {

	private static final String TAG = "DataListener";
	
	private Mediator _m;
	private TelephonyManager _telephonyManager;
	
	public DataListener(Mediator mediator) {
    	SBLog.constructor(TAG);
		_m = mediator;
		_telephonyManager = (TelephonyManager) _m.getSystemService(Context.TELEPHONY_SERVICE);
		_telephonyManager.listen(_phoneStateListener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
	}

	@Override
	public void unsetMediator() {
		SBLog.lifecycle(TAG, "unsetMediator()");
		_m = null;	
	}
	
	public boolean isDataEnabled() {
		SBLog.method(TAG, "isDataEnabled");
		return _telephonyManager.getDataState() == TelephonyManager.DATA_CONNECTED;
	}
	
	private PhoneStateListener _phoneStateListener = new PhoneStateListener() {
		@Override
		public void onDataConnectionStateChanged(int state) {
			SBLog.method(TAG, "onDataConnectionStateChanged()");
			if (state == TelephonyManager.DATA_CONNECTED) {
				_m.onDataEnabled();
			} else if (state == TelephonyManager.DATA_SUSPENDED || state == TelephonyManager.DATA_DISCONNECTED) {
				_m.onDataDisabled();
			}
		}
	};
}
