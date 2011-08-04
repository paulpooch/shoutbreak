package co.shoutbreak;

import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;

public class DeviceInformation implements Colleague {
	
	public static final String TAG = "DeviceInformation";
	
	private Mediator _m;
	private TelephonyManager _tm;
	private String _androidId;
	
	public DeviceInformation(Mediator mediator, ShoutbreakService service) {
		_m = mediator;
		_tm = (TelephonyManager) service.getSystemService(Context.TELEPHONY_SERVICE);
		_androidId = Settings.Secure.getString(service.getContentResolver(), Settings.Secure.ANDROID_ID);
	}

	@Override
	public void unsetMediator() {
		_tm = null;
		_m = null;		
	}
	
	public final String getDeviceId() {
		return _tm.getDeviceId();
	}

	public final String getPhoneNumber() {
		return _tm.getLine1Number();
	}

	public final String getNetworkOperator() {
		return _tm.getNetworkOperatorName();
	}

	public final String getAndroidId() {
		return _androidId;
	}
}
