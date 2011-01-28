package com.shoutbreak;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.shoutbreak.service.CellDensity;
import com.shoutbreak.service.ErrorManager;
import com.shoutbreak.service.User;
import com.shoutbreak.ui.CustomMapView;
import com.shoutbreak.ui.InboxListViewAdapter;
import com.shoutbreak.ui.UserLocationOverlay;
import com.shoutbreak.ui.InboxViewHolder;

public class ShoutbreakUI extends MapActivity {
	// TODO: bundle saved instance... restore state from that
	
	protected User _user;
	protected Intent _serviceIntent;
	protected IShoutbreakService _service;
	protected ShoutbreakServiceConnection _serviceConn;	
	
	// keyboard
	protected InputMethodManager _inputMM;
	
	// UI controls
	protected ImageButton _cShoutsButton;
	protected ImageButton _cInboxButton;
	protected ImageButton _cSettingsButton;
	protected Button _cOnButton;
	protected Button _cOffButton;
	protected ImageButton _cShoutButton;
	protected CustomMapView _cMapView;
	protected EditText _cShoutText;
	protected ListView _cInboxListView;
	protected LinearLayout _cRow1;
	protected LinearLayout _cRow2;
	protected RelativeLayout _cRow3;
	protected RelativeLayout _cRow4;
	protected RelativeLayout _cRow6;
	protected RelativeLayout _cNoticeBox;
	protected RelativeLayout _cNoticeBoxInbox;
	protected TextView _cNoticeText;
	protected TextView _cNoticeTextInbox;
	protected Animation _animNoticeExpand;
	protected Animation _animNoticeShowText;
	
	protected UserLocationOverlay _userLocationOverlay;
	protected MapController _mapController;
	protected InboxListViewAdapter _inboxListViewAdapter;

	///////////////////////////////////////////////////////////////////////////
	// LIFECYCLE METHODS
	///////////////////////////////////////////////////////////////////////////
	
	/** Called when the activity is first created. */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		Log.e("### UI ###", "UI constructor");
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		// this fixes the "android title bar bug" - it also CAUSES the keyboard not sliding up the editText bug
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		_serviceIntent = new Intent();
		_serviceIntent.setClassName("com.shoutbreak", "com.shoutbreak.ShoutbreakService");
		_inputMM = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);		
		_animNoticeExpand = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.notice_expand);
		_animNoticeShowText = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.notice_show_text);
		_cShoutsButton = (ImageButton) findViewById(R.id.btnShouts);
		_cInboxButton = (ImageButton) findViewById(R.id.btnInbox);
		_cSettingsButton = (ImageButton) findViewById(R.id.btnSettings);
		_cOnButton = (Button) findViewById(R.id.btnOn);
		_cOffButton = (Button) findViewById(R.id.btnOff);
		_cShoutButton = (ImageButton) findViewById(R.id.btnShout);
		_cMapView = (CustomMapView)findViewById(R.id.cmvMap);
		_cShoutText = (EditText) findViewById(R.id.etShoutText);	
		_cNoticeBox = (RelativeLayout) findViewById(R.id.rlNotice);
		_cNoticeText = (TextView) findViewById(R.id.tvNotice);
		_cNoticeBoxInbox = (RelativeLayout) findViewById(R.id.r6Notice);
		_cNoticeTextInbox = (TextView) findViewById(R.id.tvNoticeInbox);
		_cInboxListView = (ListView)findViewById(R.id.lvInbox);
		
		_cRow1 = (LinearLayout) findViewById(R.id.llRow1);
		_cRow2 = (LinearLayout) findViewById(R.id.llRow2);
		_cRow3 = (RelativeLayout) findViewById(R.id.rlRow3);
		_cRow4 = (RelativeLayout) findViewById(R.id.rlRow4);
		_cRow6 = (RelativeLayout) findViewById(R.id.llRow6);
		
		_mapController = _cMapView.getController();
		_cShoutsButton.setImageResource(R.drawable.tab_on); // start in shouts tab
		
		// Setup User
		ShoutbreakApplication app = (ShoutbreakApplication)this.getApplication();
		app.setUIReference(this);
		_user = app.getUser();
		
		_cShoutsButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {	
				_cShoutsButton.setImageResource(R.drawable.tab_on);
				_cInboxButton.setImageResource(R.drawable.tab_button_states);
				_cSettingsButton.setImageResource(R.drawable.tab_button_states);
				_cRow1.setVisibility(View.VISIBLE);
				_cRow2.setVisibility(View.VISIBLE);
				_cRow3.setVisibility(View.VISIBLE);
				_cRow4.setVisibility(View.VISIBLE);
				_cRow6.setVisibility(View.GONE);
			}
		});
		
		_cInboxButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				goToInbox();				
			}
		});
		
		_cSettingsButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				_cSettingsButton.setImageResource(R.drawable.tab_on);
				_cShoutsButton.setImageResource(R.drawable.tab_button_states);
				_cInboxButton.setImageResource(R.drawable.tab_button_states);
				giveNotice("TEST Test Tdada!");
//				_cRow1.setVisibility(View.GONE);
//				_cRow2.setVisibility(View.GONE);
//				_cRow3.setVisibility(View.GONE);
//				_cRow4.setVisibility(View.GONE);
//				_cRow6.setVisibility(View.GONE);
			}
		});
		
		_cShoutButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String text = _cShoutText.getText().toString().trim();
				try {
					_service.shout(text);
					hideKeyboard();
				} catch (RemoteException ex) {
					ErrorManager.manage(ex);
				}
			}
		});
		
		_cShoutText.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
			}
		});
		
		_cShoutText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
				} else {
					hideKeyboard();
				}	
			}
		});
		
		_cOnButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				turnServiceOn();
			}
		});

		_cOffButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				turnServiceOff();
			}
		});
		
		initInboxListView();
		initMap();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		_user.getInbox().refresh();
		if (_user.getBooleanPreference(Vars.PREF_APP_ON_OFF_STATUS, true)) {
			turnEverythingOn();
		} else {
			turnEverythingOff();
		}
		// this gets extras from notification if app not running
		Bundle extras = getIntent().getExtras();
		handleExtras(extras);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		// we disable/enable to be nice to user battery
		_userLocationOverlay.disableMyLocation();
		_user.getLocationTracker().stopListeningToLocation();
		
	}
	
	@Override
	protected void onStop() {
		super.onStop();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		releaseService();
		ShoutbreakApplication app = (ShoutbreakApplication)this.getApplication();
		app.getUIReference().clear();
		Log.d(getClass().getSimpleName(), "onDestroy");
		Log.e("### UI ###", "UI destroy");
	}
	
	///////////////////////////////////////////////////////////////////////////
	// END LIFECYCLE METHODS
	///////////////////////////////////////////////////////////////////////////
	
	protected void turnEverythingOn() {
		
		// we disable/enable to be nice to user battery
		_userLocationOverlay.enableMyLocation();
		_user.getLocationTracker().startListeningToLocation();
		
		CellDensity cellDensity = _user.getCellDensity();
		if (cellDensity.isSet) {	
			_userLocationOverlay.setPopulationDensity(cellDensity.density);
		}	
		turnServiceOn();
	}
	
	protected void turnEverythingOff() {
		turnServiceOff(); // this is probably redundant but we'll be totally sure service is dead with this
	}
	
	// this gets extras from notification if app already running
	public void onNewIntent(Intent intent){
		Bundle extras = intent.getExtras();
		handleExtras(extras);		
    }
	
	protected void handleExtras(Bundle extras) {
		if (extras != null) {
			if (extras.containsKey(Vars.EXTRA_REFERRED_FROM_NOTIFICATION) && extras.getBoolean(Vars.EXTRA_REFERRED_FROM_NOTIFICATION)) {
				goToInbox();
			}
		}
		
	}
	
	protected void goToInbox() {
		_cInboxButton.setImageResource(R.drawable.tab_on);
		_cShoutsButton.setImageResource(R.drawable.tab_button_states);
		_cSettingsButton.setImageResource(R.drawable.tab_button_states);
		_cRow1.setVisibility(View.GONE);
		_cRow2.setVisibility(View.GONE);
		_cRow3.setVisibility(View.GONE);
		_cRow4.setVisibility(View.GONE);
		_cRow6.setVisibility(View.VISIBLE);
	}
	
	public InboxListViewAdapter getInboxListViewAdapter() {
		return _inboxListViewAdapter;
	}
	
	protected void initInboxListView() {
		
		_inboxListViewAdapter = new InboxListViewAdapter(this);
		_cInboxListView.setAdapter(_inboxListViewAdapter);
		_cInboxListView.setItemsCanFocus(false);
        
        // item expand listener
		_cInboxListView.setOnItemClickListener(new ListView.OnItemClickListener() {        
			public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
				// TODO Auto-generated method stub
				InboxViewHolder holder = (InboxViewHolder) view.getTag();
				String shoutID = holder.shoutID;
				holder.collapsed.setVisibility(View.GONE);
		        holder.expanded.setVisibility(View.VISIBLE);
				
		        // TODO: is this hacky?
		        if (((Shout)_inboxListViewAdapter.getItem(position)).state_flag == Vars.SHOUT_STATE_NEW) {
		        	_user.getInbox().markShoutAsRead(shoutID);
		        	_inboxListViewAdapter.notifyDataSetChanged();
		    	}
		        
		        _inboxListViewAdapter.getCacheExpandState().put(shoutID, true);
			}
        });
		
	}
	
	protected void initMap() {
		_userLocationOverlay = new UserLocationOverlay(this, _cMapView);
		_mapController.setZoom(Vars.DEFAULT_ZOOM_LEVEL);
		_cMapView.setClickable(true);
		_cMapView.setEnabled(true);
		_cMapView.setUI(this);
		_cMapView.setUserLocationOverlay(_userLocationOverlay);
		_cMapView.getOverlays().add(_userLocationOverlay);
		_cMapView.postInvalidate();
		_userLocationOverlay.runOnFirstFix(new Runnable() {
			public void run() {
				_mapController.animateTo(_userLocationOverlay.getMyLocation());
			}
		});
	}
	
	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}	
	
	public User getUser() {
		return _user;
	}
	
	public void giveNotice(String noticeText) {
		// map notice
		_cNoticeText.setText(noticeText);
		_cNoticeBox.startAnimation(_animNoticeExpand);
		_cNoticeText.setTextColor(Color.WHITE);
		_cNoticeText.startAnimation(_animNoticeShowText);
		// inbox notice
		_cNoticeTextInbox.setText(noticeText);
		_cNoticeBoxInbox.startAnimation(_animNoticeExpand);
		_cNoticeTextInbox.setTextColor(Color.WHITE);
		_cNoticeTextInbox.startAnimation(_animNoticeShowText);
	}
	
	public IShoutbreakService getService() {
		return _service;
	}
	
	public void hideKeyboard() {
		_inputMM.hideSoftInputFromWindow(_cShoutText.getWindowToken(), 0);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
	}
	
	protected void turnServiceOn() {
		if (_serviceConn == null) {
			startService(_serviceIntent);
			_serviceConn = new ShoutbreakServiceConnection();
			bindService(_serviceIntent, _serviceConn, Context.BIND_AUTO_CREATE);
		}
		_user.setBooleanPreference(Vars.PREF_APP_ON_OFF_STATUS, true);
	}
	
	protected void turnServiceOff() {
		releaseService();
		stopService(_serviceIntent);
		_user.setBooleanPreference(Vars.PREF_APP_ON_OFF_STATUS, false);	
	}

	protected void releaseService() {
		if (_serviceConn != null) {
			unbindService(_serviceConn);
			_serviceConn = null;
		}
	}
	
	/**
	 * This implementation is used to receive callbacks from the remote service.
	 */
	protected IShoutbreakServiceCallback _shoutbreakServiceCallback = new IShoutbreakServiceCallback.Stub() {
		/**
		 * This is called by the remote service regularly to tell us about new
		 * values. Note that IPC calls are dispatched through a thread pool
		 * running in each process, so the code executing here will NOT be
		 * running in our main thread like most other things -- so, to update
		 * the UI, we need to use a Handler to hop over there.
		 */		
		public void serviceEventComplete(int serviceEventCode) {
			switch (serviceEventCode) {
				case Vars.SEC_UI_RECONNECT_COMPLETE: {
					// we can do stuff here if we want
					// only called if UI is launched when service was already running
					break;
				}
				case Vars.SEC_SHOUT_SENT: {
					giveNotice("shout sent");
					_cShoutText.setText("");
					break;
				}
				case Vars.SEC_RECEIVE_SHOUTS: {
					CellDensity cellDensity = _user.getCellDensity();
					_userLocationOverlay.setPopulationDensity(cellDensity.density);
					int newShouts = _user.getShoutsJustReceived();
					if (newShouts > 0) {
						String notice = "just heard " + newShouts + " new shout";
						if (newShouts > 1) {
							notice += "s"; // plural is dumb
						}
						giveNotice(notice);
						_user.getInbox().refresh();
					} else if (_user.getScoresJustReceived()) {
						_user.getInbox().refresh();
					}
					break;
				}
				case Vars.SEC_VOTE_COMPLETED: {
					_user.getInbox().refresh();
					break;
				}				
			}
		}
	};

	class ShoutbreakServiceConnection implements ServiceConnection {

		public void onServiceConnected(ComponentName className, IBinder boundService) {
			
			_service = IShoutbreakService.Stub.asInterface((IBinder) boundService);

			// We want to monitor the service for as long as we are
			// connected to it.
			try {
				_service.registerCallback(_shoutbreakServiceCallback);
			} catch (RemoteException e) {
				// In this case the service has crashed before we could even
				// do anything with it; we can count on soon being
				// disconnected (and then reconnected if it can be restarted)
				// so there is no need to do anything here.
			}
			Log.d(getClass().getSimpleName(), "onServiceConnected()");

		}

		public void onServiceDisconnected(ComponentName className) {
			_service = null;
			Log.d(getClass().getSimpleName(), "onServiceDisconnected");
		}

	}

}