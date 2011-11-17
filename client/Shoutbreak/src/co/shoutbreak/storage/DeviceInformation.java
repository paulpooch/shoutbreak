package co.shoutbreak.storage;

import co.shoutbreak.core.ShoutbreakService;
import co.shoutbreak.core.utils.SBLog;
import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;

public class DeviceInformation {
	
	public static final String TAG = "DeviceInformation";

	private TelephonyManager _tm;
	private String _androidId;
	
	public DeviceInformation(ShoutbreakService service) {
		SBLog.constructor(TAG);
		_tm = (TelephonyManager) service.getSystemService(Context.TELEPHONY_SERVICE);
		_androidId = Settings.Secure.getString(service.getContentResolver(), Settings.Secure.ANDROID_ID);
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
