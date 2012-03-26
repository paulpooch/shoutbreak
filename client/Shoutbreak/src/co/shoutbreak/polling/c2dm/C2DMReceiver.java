
package co.shoutbreak.polling.c2dm;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import co.shoutbreak.core.C;

import com.google.android.c2dm.C2DMBaseReceiver;

// https://github.com/commonsguy/cw-advandroid/tree/eb63063053077899adc05884f247d22f4360141b/Push/C2DM
public class C2DMReceiver extends C2DMBaseReceiver {
	public C2DMReceiver() {
		super(C.CONFIG_C2DM_ACCOUNT);
	}

	@Override
	public void onRegistrered(Context context, String registrationId) {
		Log.w("C2DMReceiver-onRegistered", registrationId);
	}

	@Override
	public void onUnregistered(Context context) {
		Log.w("C2DMReceiver-onUnregistered", "got here!");
	}

	@Override
	public void onError(Context context, String errorId) {
		Log.w("C2DMReceiver-onError", errorId);
	}

	@Override
	protected void onMessage(Context context, Intent intent) {
		Log.w("C2DMReceiver", intent.getStringExtra("payload"));
	}
}