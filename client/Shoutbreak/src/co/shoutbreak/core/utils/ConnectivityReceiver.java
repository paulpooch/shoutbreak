package co.shoutbreak.core.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

// http://code.google.com/p/krvarma-android-samples/source/browse/trunk/ConnectionTest/src/com/varma/samples/conntest/MainActivity.java
// http://developer.motorola.com/docstools/library/detecting-and-using-lte-networks/
public class ConnectivityReceiver extends BroadcastReceiver {
	
	private DataListener _dataListener; 
	
	public ConnectivityReceiver(DataListener dataListener) {
		_dataListener = dataListener;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            //NetworkInfo netInfo = (NetworkInfo)intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            //int netType = netInfo.getType();
            //int netSubtype = netInfo.getSubtype();
            //boolean isConnected = netInfo.isConnected();
        	_dataListener.checkDataStateAndPushEvents();
        }
	}
}