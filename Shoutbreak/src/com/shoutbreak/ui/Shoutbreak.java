package com.shoutbreak.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.shoutbreak.C;
import com.shoutbreak.CustomExceptionHandler;
import com.shoutbreak.ErrorManager;
import com.shoutbreak.R;
import com.shoutbreak.Shout;
import com.shoutbreak.service.IServiceBridge;
import com.shoutbreak.service.ShoutbreakService;
import com.shoutbreak.service.User;
import com.shoutbreak.service.UserEvent;
import com.shoutbreak.service.UserListener;

import com.shoutbreak.ui.map.CustomMapView;
import com.shoutbreak.ui.map.UserLocationOverlay;

public class Shoutbreak extends MapActivity implements UserListener {

	private Shoutbreak _ui;
	private Intent _serviceIntent;
	private ShoutbreakServiceConnection _serviceConn;
	private IServiceBridge _serviceBridge; // This is how we access the service.
	private InboxListViewAdapter _inboxListViewAdapter;
	private UserLocationOverlay _userLocationOverlay;
	protected MapController _mapController;
	private DialogBuilder _dialogBuilder;
	private boolean _isPowerOn;

	// keyboard
	protected InputMethodManager _inputMM;

	private CustomMapView _cMapView;
	private RelativeLayout _cRlMap;
	private RelativeLayout _cRlShoutInput;
	private RelativeLayout _cRlInbox;
	private RelativeLayout _cRlUser;
	private ImageButton _cBtnPower;
	private ImageButton _cBtnShout;
	private ImageButton _cBtnShoutingTab;
	private ImageButton _cBtnInboxTab;
	private ImageButton _cBtnUserTab;
	private TextView _cTvTitleBar;
	private TextView _cTvUserLevel;
	private EditText _cShoutText;
	protected ListView _cInboxListView;

	protected RelativeLayout _cNoticeBoxShouting;
	protected RelativeLayout _cNoticeBoxInbox;
	protected RelativeLayout _cNoticeBoxUser;
	protected TextView _cNoticeTextShouting;
	protected TextView _cNoticeTextInbox;
	protected TextView _cNoticeTextUser;
	protected Animation _animNoticeExpand;
	protected Animation _animNoticeShowText;

	// LIFECYCLE //////////////////////////////////////////////////////////////

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(C.CONFIG_CRASH_REPORT_ADDRESS));

		_ui = this;
		//_userInfo = new UserInfo();
		_serviceIntent = new Intent(Shoutbreak.this, ShoutbreakService.class);
		_inboxListViewAdapter = new InboxListViewAdapter(this);
		_inputMM = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		_dialogBuilder = new DialogBuilder(_ui);

		_cRlMap = (RelativeLayout) findViewById(R.id.rlMap);
		_cRlShoutInput = (RelativeLayout) findViewById(R.id.rlShoutInput);
		_cRlInbox = (RelativeLayout) findViewById(R.id.rlInbox);
		_cRlUser = (RelativeLayout) findViewById(R.id.rlUser);
		_cBtnPower = (ImageButton) findViewById(R.id.btnPower);
		_cBtnShout = (ImageButton) findViewById(R.id.btnShout);
		_cBtnShoutingTab = (ImageButton) findViewById(R.id.btnShoutingTab);
		_cBtnInboxTab = (ImageButton) findViewById(R.id.btnInboxTab);
		_cBtnUserTab = (ImageButton) findViewById(R.id.btnUserTab);
		_cTvTitleBar = (TextView) findViewById(R.id.tvTitleBar);
		_cTvUserLevel = (TextView) findViewById(R.id.tvUserLevel);
		_cShoutText = (EditText) findViewById(R.id.etShoutText);
		_cInboxListView = (ListView) findViewById(R.id.lvInbox);

		_cNoticeBoxShouting = (RelativeLayout) findViewById(R.id.rlNoticeShouting);
		_cNoticeTextShouting = (TextView) findViewById(R.id.tvNoticeShouting);
		_cNoticeBoxInbox = (RelativeLayout) findViewById(R.id.rlNoticeInbox);
		_cNoticeTextInbox = (TextView) findViewById(R.id.tvNoticeInbox);
		_cNoticeBoxUser = (RelativeLayout) findViewById(R.id.rlNoticeUser);
		_cNoticeTextUser = (TextView) findViewById(R.id.tvNoticeUser);

		_animNoticeExpand = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.notice_expand);
		_animNoticeShowText = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.notice_show_text);

		_cBtnPower.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (_isPowerOn) {
					serviceOffUIOn();
				} else {
					serviceOnUIOn();
				}
			}
		});

		_cBtnShout.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String text = _cShoutText.getText().toString().trim();
				int power = _userLocationOverlay.getCurrentPower();
				boolean valid = true;
				if (text.equals("")) {
					ErrorManager.warnUser(_ui, "cannot shout blanks");
					valid = false;
				}
				if (valid && power < 1) {
					ErrorManager.warnUser(_ui, "a shout with zero power won't reach won't reach anybody");
					valid = false;
				}
				if (valid) {
					if (_serviceBridge != null) {
						_serviceBridge.shout(text, power);
					} else {
						ErrorManager.warnUser(_ui, "cannot shout with service offline");
					}
				}
				hideKeyboard();
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

		_cBtnShoutingTab.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				_cBtnShoutingTab.setImageResource(R.drawable.tab_shouting_on);
				_cBtnInboxTab.setImageResource(R.drawable.tab_inbox);
				_cBtnUserTab.setImageResource(R.drawable.tab_user);
				_cRlMap.setVisibility(View.VISIBLE);
				_cRlShoutInput.setVisibility(View.VISIBLE);
				_cRlInbox.setVisibility(View.GONE);
				_cRlUser.setVisibility(View.GONE);
			}
		});

		_cBtnInboxTab.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				goToInbox();
			}
		});

		_cBtnUserTab.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				_cBtnUserTab.setImageResource(R.drawable.tab_user_on);
				_cBtnShoutingTab.setImageResource(R.drawable.tab_shouting);
				_cBtnInboxTab.setImageResource(R.drawable.tab_inbox);
				_cRlMap.setVisibility(View.GONE);
				_cRlShoutInput.setVisibility(View.GONE);
				_cRlInbox.setVisibility(View.GONE);
				_cRlUser.setVisibility(View.VISIBLE);
			}
		});

		// Start in shouting tab.
		_cBtnShoutingTab.setImageResource(R.drawable.tab_shouting_on);
		
		initMap();
		initInboxListView();
		disableInputs();
		
	}

	@Override
	protected void onStart() {
		super.onStart();
	}
   
	@Override
	protected void onResume() {
		super.onResume();
		figureOutService();
		// this gets extras from notification if app not running
		Bundle extras = getIntent().getExtras();
		handleExtras(extras);
	}

	@Override
	protected void onPause() {
		super.onPause();
		serviceOnUIOff();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	// this gets extras from notification if app already running
	public void onNewIntent(Intent intent) {
		Bundle extras = intent.getExtras();
		handleExtras(extras);
	}

	///////////////////////////////////////////////////////////////////////////
	
	// TODO: Optimize this.  We shouldn't refresh inbox 3 times on 1 ping for instance.
	public void handleUserEvent(UserEvent e) {
		User user = e.getUser();
		
		if (e.type == UserEvent.LOCATION_SERVICES_CHANGE) {	
			if (user.getLocationTracker().isLocationEnabled()) {
				// run service from here?
				toggleGPSUsage(true);
				_serviceBridge.runServiceFromUI();
				enableInputs();
			} else {
				serviceOffUIOn();
				_dialogBuilder.showDialog(DialogBuilder.DIALOG_LOCATION_DISABLED);
			}
		
		} else if (e.type == UserEvent.INBOX_CHANGE) {
			_inboxListViewAdapter.updateDisplay(user.getInbox().getShoutsForUI());
		
		} else if (e.type == UserEvent.SHOUT_SENT) {
			giveNotice("shout sent");
			_cShoutText.setText("");
		
		} else if (e.type == UserEvent.SHOUTS_RECEIVED) {
			int newShouts = user.getShoutsJustReceived();
			if (newShouts > 0) {
				String pluralShout = "shout" + (newShouts > 1 ? "s" : "");
				String notice = "just heard " + newShouts + " new " + pluralShout;
				giveNotice(notice);
				_inboxListViewAdapter.updateDisplay(user.getInbox().getShoutsForUI());
			}
		
		} else if (e.type == UserEvent.VOTE_COMPLETE) {
			_inboxListViewAdapter.updateDisplay(user.getInbox().getShoutsForUI());
		
		} else if (e.type == UserEvent.ACCOUNT_CREATED) {
			// Do anything?
		
		} else if (e.type == UserEvent.DENSITY_CHANGE) {
			// This goes directly to UserLocationOverlay.  Handled there.
		
		} else if (e.type == UserEvent.LEVEL_CHANGE) {
			giveNotice("shoutreach grew +" + C.CONFIG_PEOPLE_PER_LEVEL + " people");
	
		} else if (e.type == UserEvent.SCORES_CHANGE) {
			_inboxListViewAdapter.updateDisplay(user.getInbox().getShoutsForUI());
			
		}
	};
	
	private void enableInputs() {
		_cBtnShout.setEnabled(true);
		_cShoutText.setEnabled(true);
		_cShoutText.setText("");
		_inboxListViewAdapter.setInputAllowed(true);
	}
	
	private void disableInputs() {
		_cBtnShout.setEnabled(false);
		_cShoutText.setEnabled(false);
		_cShoutText.setText("   Turn on power to shout...");
		_inboxListViewAdapter.setInputAllowed(false);	
	}
	
	private void toggleGPSUsage(boolean turnOn) {
		if (_serviceBridge != null) {
			_serviceBridge.toggleLocationTracker(turnOn);
		}
		if (turnOn) {
			if (_cMapView.getOverlays().size() == 0) {
				_cMapView.getOverlays().add(_userLocationOverlay);
				_userLocationOverlay.enableMyLocation();
				_cMapView.postInvalidate();
				
				_mapController.animateTo(_userLocationOverlay.getMyLocation());
			}
		} else {
			if (_cMapView.getOverlays().size() > 0) {
				_userLocationOverlay.disableMyLocation();
				_cMapView.getOverlays().clear();
				_cMapView.postInvalidate();
			}
		}
	}
	
	public IServiceBridge getServiceBridge() {
		return _serviceBridge;
	}
	
	protected void initMap() {
		_cMapView = (CustomMapView) findViewById(R.id.cmvMap);
		_userLocationOverlay = new UserLocationOverlay(this, _cMapView);
		if (_cMapView.getOverlays().size() == 0) {
			_cMapView.getOverlays().add(_userLocationOverlay);
		}
		_mapController = _cMapView.getController();
		_mapController.setZoom(C.DEFAULT_ZOOM_LEVEL);
		_cMapView.setClickable(true);
		_cMapView.setEnabled(true);
		// _cMapView.setUI(this);
		_cMapView.setUserLocationOverlay(_userLocationOverlay);
		_cMapView.postInvalidate();
		_userLocationOverlay.runOnFirstFix(new Runnable() {
			public void run() {
				//GeoPoint loc = _userLocationOverlay.getMyLocation();
				//_mapController.animateTo(loc);
			}
		});
		_userLocationOverlay.enableMyLocation();
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
				if (((Shout) _inboxListViewAdapter.getItem(position)).state_flag == C.SHOUT_STATE_NEW) {
					_serviceBridge.markShoutAsRead(shoutID);
					_inboxListViewAdapter.notifyDataSetChanged();
				}

				_inboxListViewAdapter.getCacheExpandState().put(shoutID, true);
			}
		});
		
	}

	protected void handleExtras(Bundle extras) {
		if (extras != null) {
			if (extras.containsKey(C.EXTRA_REFERRED_FROM_NOTIFICATION)
					&& extras.getBoolean(C.EXTRA_REFERRED_FROM_NOTIFICATION)) {
				goToInbox();
			}
		}

	}

	public void hideKeyboard() {
		_inputMM.hideSoftInputFromWindow(_cShoutText.getWindowToken(), 0);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
			WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
	}

	public void setTitleBarText(String s) {
		_cTvTitleBar.setText(s);
	}

	protected void goToInbox() {
		_cBtnInboxTab.setImageResource(R.drawable.tab_inbox_on);
		_cBtnShoutingTab.setImageResource(R.drawable.tab_shouting);
		_cBtnUserTab.setImageResource(R.drawable.tab_user);
		_cRlMap.setVisibility(View.GONE);
		_cRlShoutInput.setVisibility(View.GONE);
		_cRlInbox.setVisibility(View.VISIBLE);
		_cRlUser.setVisibility(View.GONE);
	}

	public void giveNotice(String noticeText) {
		// shouting notice
		_cNoticeTextShouting.setText(noticeText);
		_cNoticeBoxShouting.startAnimation(_animNoticeExpand);
		_cNoticeTextShouting.setTextColor(Color.WHITE);
		_cNoticeTextShouting.startAnimation(_animNoticeShowText);
		// inbox notice
		_cNoticeTextInbox.setText(noticeText);
		_cNoticeBoxInbox.startAnimation(_animNoticeExpand);
		_cNoticeTextInbox.setTextColor(Color.WHITE);
		_cNoticeTextInbox.startAnimation(_animNoticeShowText);
		// user notice
		_cNoticeTextUser.setText(noticeText);
		_cNoticeBoxUser.startAnimation(_animNoticeExpand);
		_cNoticeTextUser.setTextColor(Color.WHITE);
		_cNoticeTextUser.startAnimation(_animNoticeShowText);
	}

	
	public void updateUserInfo() {
		//_cTvUserLevel.setText(
		//		"Points: " + _userInfo.getPoints() 
		//		+ "\nLevel: " + _userInfo.getLevel()
		//		+ "\nShoutreach: " + _userInfo.getLevel() * C.CONFIG_PEOPLE_PER_LEVEL + " people"
		//		+ "\nYou need " +  _userInfo.getNextLevelAt() + " more points to reach " + (level + 1) * C.CONFIG_PEOPLE_PER_LEVEL + " people."
		//		+ "\n\nCell: [" + _userInfo.getCellDensity().cellX + ", " + _userInfo.getCellDensity().cellY + "] = " + _userInfo.getCellDensity().density);
	}
	
	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}
	
	// SERVICE CONNECTION /////////////////////////////////////////////////////

	private void figureOutService() {
		if (User.getBooleanPreference(this, C.PREF_APP_ON_OFF_STATUS, true)) {
			serviceOnUIOn();
		} else {
			serviceOffUIOn();
		}
	}

	private void serviceOnUIOn() {
		if (_serviceBridge == null) {
			startService(_serviceIntent);
			_serviceConn = new ShoutbreakServiceConnection();
			bindService(_serviceIntent, _serviceConn, Context.BIND_AUTO_CREATE);
		}
	}

	private void serviceOffUIOn() {
		handleServiceChangeWithUIOn(false);
	}

	private void serviceOnUIOff() {
		//disableInputs();
		if (_serviceBridge != null) {
			toggleGPSUsage(false);
			//_serviceBridge.unRegisterUIBridge();
			unbindService(_serviceConn);
			//_serviceBridge = null;
		}
	}

	// TODO put this in asynctask maybe
	private void handleServiceChangeWithUIOn(boolean isOn) {
		if (isOn) {
			if (_serviceBridge != null) {
				_isPowerOn = true;
				_cBtnPower.setImageResource(R.drawable.power_button_on);
				User.setBooleanPreference(this, C.PREF_APP_ON_OFF_STATUS, true);
				_serviceBridge.registerUserListener(_ui);
				_serviceBridge.registerUserListener(_userLocationOverlay);
				_serviceBridge.pullUserInfo();
				// Service will be triggered when LOCATION_SERVICES_CHANGE event is fired.
				//_serviceBridge.runServiceFromUI();
			}
		} else {
			disableInputs();
			toggleGPSUsage(false);
			_isPowerOn = false;
			_cBtnPower.setImageResource(R.drawable.power_button_off);
			User.setBooleanPreference(this, C.PREF_APP_ON_OFF_STATUS, false);
			if (_serviceBridge != null) {
				_serviceBridge.stopServiceFromUI();
				unbindService(_serviceConn);
			}
			stopService(_serviceIntent);
			_serviceBridge = null;
		}
		Log.e("HANDLE SERVICE", "done");
	}
	
	class ShoutbreakServiceConnection implements ServiceConnection {
		public void onServiceConnected(ComponentName className, IBinder service) {
			_serviceBridge = (IServiceBridge) service;
			handleServiceChangeWithUIOn(true);
		}
		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			handleServiceChangeWithUIOn(false);
			Toast.makeText(Shoutbreak.this, "you dropped off the grid\nignoring all shouts", Toast.LENGTH_SHORT).show();
		}
	}

}