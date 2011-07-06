package co.shoutbreak.ui.views;

import java.util.Observable;
import java.util.Observer;

import android.os.AsyncTask;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

import co.shoutbreak.R;
import co.shoutbreak.components.SBStateManager;
import co.shoutbreak.misc.SBLog;
import co.shoutbreak.ui.SBContext;

public class ComposeView extends SBView implements Observer {
	
	private final String TAG = "ComposeView.java";
	private final String POWER_STATE_PREF = "POWER_STATE";
	
	private ImageButton _PowerButton;
	
	private boolean _isPowerOn;
	
	/* Do NOT store any SBContext parameters, will cause service leak */
	
	public ComposeView(SBContext context, String name, int resourceId, int notificationId) {
		super(context, name, resourceId, notificationId);
		_Context.getStateManager().addObserver(this);
		
		_PowerButton = (ImageButton) context.findViewById(R.id.powerButton);
		_PowerButton.setOnClickListener(_powerButtonListener);
		setPowerState(_Context.getPreferenceManager().getBoolean(POWER_STATE_PREF, false));
	}
	
	/* LIFECYCLE METHODS */
	
	@Override
	void onShow() {
		SBLog.i(TAG, "onShow()");
		// inflate map view
		// TODO Auto-generated method stub	
	}
	
	@Override
	void onHide() {
		SBLog.i(TAG, "onHide()");
		// remove map view
		// TODO Auto-generated method stub
	}

	@Override
	void onDestroy() {
		SBLog.i(TAG, "onDestroy()");
		_Context.getStateManager().deleteObserver(this);		
	}
	
	/* LISTENERS: use AsyncTasks for expensive shit */
	
	private OnClickListener _powerButtonListener = new OnClickListener() {
		public void onClick(View v) {
			setPowerState(!_isPowerOn);
		}
	};
	
	/* OBSERVER METHODS */

	public void update(Observable observable, Object data) {
		/* TODO: this design is getting ugly */
		SBStateManager stateManager = (SBStateManager) observable;
	}
	
	/* TODO: also kind of ugly */
	private class PowerStateChangeTask extends AsyncTask<SBContext, Integer, Integer> {

		@Override
		protected Integer doInBackground(SBContext... params) {
			return null;
		}
		
	}
	
	private void setPowerState(boolean state) {
		if (state) {
			_isPowerOn = true;
			_PowerButton.setImageResource(R.drawable.power_button_on);
			_Context.getPreferenceManager().putBoolean(POWER_STATE_PREF, true);
			_Context.getStateManager().enableService();
		} else {
			_isPowerOn = false;
			_PowerButton.setImageResource(R.drawable.power_button_off);
			_Context.getPreferenceManager().putBoolean(POWER_STATE_PREF, false);
			_Context.getStateManager().disableService();
		}
	}
}