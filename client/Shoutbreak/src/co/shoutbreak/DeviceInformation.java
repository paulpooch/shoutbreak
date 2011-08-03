package co.shoutbreak;

import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;

public class DeviceInformation implements Colleague {
	
	public static final String TAG = "DeviceInformation";
	
	private Mediator _m;
	private ShoutbreakService _service;
	private TelephonyManager _tm;
	
	public DeviceInformation(ShoutbreakService service) {
		_service = service;
		_tm = (TelephonyManager) service.getSystemService(Context.TELEPHONY_SERVICE);
	}

	@Override
	public void setMediator(Mediator mediator) {
		_m = mediator;	
	}

	@Override
	public void unsetMediator() {
		_tm = null;
		_service = null;
		_m = null;		
	}
	
	public String getDeviceId() {
		return _tm.getDeviceId();
	}

	public String getPhoneNumber() {
		return _tm.getLine1Number();
	}

	public String getNetworkOperator() {
		return _tm.getNetworkOperatorName();
	}

	public String getAndroidId() {
		return Settings.Secure.getString(_service.getContentResolver(), Settings.Secure.ANDROID_ID);
	}
}
