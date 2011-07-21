package co.shoutbreak;

import co.shoutbreak.shared.SBLog;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

public class Shoutbreak extends Activity implements Colleague {
	
	private static String TAG = "Shoutbreak";
	
	private Mediator _m;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
		ImageButton composeTab;
		ImageButton inboxTab;
		ImageButton profileTab;
		ImageButton powerButton;
    	
    	SBLog.i(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
		// register button listeners
		composeTab = (ImageButton) findViewById(R.id.composeTab);
		composeTab.setOnClickListener(_composeTabListener);
		inboxTab = (ImageButton) findViewById(R.id.inboxTab);
		inboxTab.setOnClickListener(_inboxTabListener);
		profileTab = (ImageButton) findViewById(R.id.profileTab);
		profileTab.setOnClickListener(_profileTabListener);
		powerButton = (ImageButton) findViewById(R.id.powerButton);
		powerButton.setOnClickListener(_powerButtonListener);
		
        _m.setUIOn();
        _m.bindUIToService();
    }

	@Override
	public void setMediator(Mediator mediator) {
		SBLog.i(TAG, "setMediator()");
		_m = mediator;
	}

	@Override
	public void unSetMediator() {
		SBLog.i(TAG, "unSetMediator()");
		_m = null;		
	}

	/* Button Listeners */
	
	private OnClickListener _composeTabListener = new OnClickListener() {
		public void onClick(View v) {

		}
	};
	
	private OnClickListener _inboxTabListener = new OnClickListener() {
		public void onClick(View v) {

		}
	};
	
	private OnClickListener _profileTabListener = new OnClickListener() {
		public void onClick(View v) {

		}
	};
	
	private OnClickListener _powerButtonListener = new OnClickListener() {
		public void onClick(View v) {

		}
	};
	
}

