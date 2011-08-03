package co.shoutbreak;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import co.shoutbreak.shared.SBLog;

public class DataListener implements Colleague {

	private static final String TAG = "DataListener";
	
	private Mediator _m;
	private TelephonyManager _telephonyManager;
	
	public DataListener() {
		SBLog.i(TAG, "new DataListener()");
		_telephonyManager = (TelephonyManager) _m.getSystemService(Context.TELEPHONY_SERVICE);
		_telephonyManager.listen(_phoneStateListener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
	}
	
	@Override
	public void setMediator(Mediator mediator) {
		SBLog.i(TAG, "setMediator()");
		_m = mediator;
	}

	@Override
	public void unsetMediator() {
		SBLog.i(TAG, "unsetMediator()");
		_m = null;	
	}
	
	public boolean isDataEnabled() {
		SBLog.i(TAG, "isDataEnabled");
		return _telephonyManager.getDataState() == TelephonyManager.DATA_CONNECTED;
	}
	
	private PhoneStateListener _phoneStateListener = new PhoneStateListener() {
		@Override
		public void onDataConnectionStateChanged(int state) {
			SBLog.i(TAG, "onDataConnectionStateChanged()");
			if (state == TelephonyManager.DATA_CONNECTED) {
				_m.onDataEnabled();
			} else if (state == TelephonyManager.DATA_SUSPENDED || state == TelephonyManager.DATA_DISCONNECTED) {
				_m.onDataDisabled();
			}
		}
	};
}
