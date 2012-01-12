package co.shoutbreak.core.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import co.shoutbreak.core.Colleague;
import co.shoutbreak.core.Mediator;

public class DataChangeListener implements Colleague {

	private static final String TAG = "DataListener";
	
	private Mediator _m;
	private TelephonyManager _telephonyManager;
	private ConnectivityManager _connectivityManager;
	
	public DataChangeListener(Mediator mediator) {
    	SBLog.constructor(TAG);
		_m = mediator;
		_telephonyManager = (TelephonyManager) _m.getSystemService(Context.TELEPHONY_SERVICE);
		_connectivityManager = (ConnectivityManager) _m.getSystemService(Context.CONNECTIVITY_SERVICE);
		_telephonyManager.listen(_phoneStateListener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
	}

	@Override
	public void unsetMediator() {
		SBLog.lifecycle(TAG, "unsetMediator()");
		_m = null;	
	}
	
	public boolean isDataEnabled() {
		SBLog.method(TAG, "isDataEnabled");
		NetworkInfo info = _connectivityManager.getActiveNetworkInfo();
		boolean cellData = (_telephonyManager.getDataState() == TelephonyManager.DATA_CONNECTED);
		boolean wifiWimaxData = (info != null && info.isConnected());
		return (cellData || wifiWimaxData);
	}
	
	public void checkDataStateAndPushEvents() {
		if (isDataEnabled()) {
			_m.onDataEnabled();
		} else {
			_m.onDataDisabled();
		}
	}
	
	private PhoneStateListener _phoneStateListener = new PhoneStateListener() {
		@Override
		public void onDataConnectionStateChanged(int state) {
			SBLog.method(TAG, "onDataConnectionStateChanged()");
			checkDataStateAndPushEvents();
		}
	};
	
}
