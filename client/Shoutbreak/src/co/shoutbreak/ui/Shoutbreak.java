package co.shoutbreak.ui;

import java.util.List;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;

import co.shoutbreak.R;
import co.shoutbreak.core.C;
import co.shoutbreak.core.Colleague;
import co.shoutbreak.core.Mediator;
import co.shoutbreak.core.ServiceBridgeInterface;
import co.shoutbreak.core.Shout;
import co.shoutbreak.core.ShoutbreakService;
import co.shoutbreak.core.utils.Flag;
import co.shoutbreak.core.utils.SBLog;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Shoutbreak extends MapActivity implements Colleague {
	
	private static String TAG = "Shoutbreak";

	private Flag _isPowerOn = new Flag("_isPowerOn");
	private Flag _isComposeShowing = new Flag("_isComposeShowing");
	private Flag _isInboxShowing = new Flag("_isInboxShowing");
	private Flag _isProfileShowing = new Flag("_isProfileShowing");

	private Mediator _m;
	private Intent _serviceIntent;
	private ServiceBridgeInterface _serviceBridge;
	private ImageButton _powerBtn;
	private ImageButton _composeTabBtn;
	private ImageButton _inboxTabBtn;
	private ImageButton _profileTabBtn;
	private ImageButton _shoutBtn;
	private ImageButton _enableLocationBtn;
	private EditText _shoutInputEt;
	private LinearLayout _splashLl;
	private LinearLayout _composeViewLl;
	private LinearLayout _inboxViewLl;
	private LinearLayout _profileViewLl;
	private RelativeLayout _mapContainer;
	private RelativeLayout _inputContainer;
	private TextView _titleBarTv;
	private NoticeTab _noticeTab;

	private CustomMapView _map;
	private UserLocationOverlay _overlay;
	private InboxListViewAdapter _inboxListViewAdapter;
	private NoticeListViewAdapter _noticeListViewAdapter;
	private ListView _inboxListView;
	private ListView _noticeListView;
	
	@Override
	public void onCreate(Bundle extras) {
		SBLog.i(TAG, "onCreate()");
		super.onCreate(extras);
		setContentView(R.layout.main);

		// register button listeners
		_composeTabBtn = (ImageButton) findViewById(R.id.composeTabBtn);
		_composeTabBtn.setOnClickListener(_composeTabListener);
		_inboxTabBtn = (ImageButton) findViewById(R.id.inboxTabBtn);
		_inboxTabBtn.setOnClickListener(_inboxTabListener);
		_profileTabBtn = (ImageButton) findViewById(R.id.profileTabBtn);
		_profileTabBtn.setOnClickListener(_profileTabListener);
		_powerBtn = (ImageButton) findViewById(R.id.powerBtn);
		_powerBtn.setOnClickListener(_powerButtonListener);
		_shoutBtn = (ImageButton) findViewById(R.id.shoutBtn);
		_shoutBtn.setOnClickListener(_shoutButtonListener);
		_shoutInputEt = (EditText) findViewById(R.id.shoutInputEt);
		_inboxListView = (ListView) findViewById(R.id.inboxLv);
		_noticeListView = (ListView) findViewById(R.id.noticeLv);
		_noticeTab = (NoticeTab) findViewById(R.id.noticeTab);
		_splashLl = (LinearLayout) findViewById(R.id.splashLl);
		_composeViewLl = (LinearLayout) findViewById(R.id.composeViewLl);
		_inboxViewLl = (LinearLayout) findViewById(R.id.inboxViewLl);
		_profileViewLl = (LinearLayout) findViewById(R.id.profileViewLl);
		_titleBarTv = (TextView) findViewById(R.id.titleBarTv);
		_mapContainer = (RelativeLayout) findViewById(R.id.mapRl);
		_inputContainer = (RelativeLayout) findViewById(R.id.inputRl);
		_enableLocationBtn = (ImageButton) findViewById(R.id.enableLocationBtn);
		_enableLocationBtn.setOnClickListener(_enableLocationListener);
				
		// bind to service, initializes mediator
		_serviceIntent = new Intent(Shoutbreak.this, ShoutbreakService.class);
		bindService(_serviceIntent, _serviceConnection,
				Context.BIND_AUTO_CREATE);
	}

	public void setMediator(Mediator mediator) {
		SBLog.i(TAG, "setMediator()");
		_m = mediator;
	}

	@Override
	public void unsetMediator() {
		SBLog.i(TAG, "unsetMediator()");
		_m = null;
	}

	// all mediator interaction must occur after onServiceConnected()
	private ServiceConnection _serviceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			SBLog.i(TAG, "onServiceConnected()");
			_serviceBridge = (ServiceBridgeInterface) service;
			_serviceBridge.registerUIWithMediator(Shoutbreak.this);
			_m.onServiceConnected();

			startupSequence();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			SBLog.i(TAG, "onServiceDisconnected()");
			_m.onServiceDisconnected();
		}
	};

	@Override
	public void onResume() {
		SBLog.i(TAG, "onResume()");
		super.onResume();
		if (_m != null) {
			// call this whenever the ui starts / resumes
			checkLocationProviderStatus();
		}
	}

	@Override
	public void onNewIntent(Intent intent) {
		SBLog.i(TAG, "onNewIntent()");
		super.onNewIntent(intent);
		Bundle extras = intent.getExtras();
		if (extras != null
				&& extras.getBoolean(C.APP_LAUNCHED_FROM_NOTIFICATION)) {
			// show inbox view
		} else {
			SBLog.e(TAG, "ui relaunched from something other than notification");
		}
	}

	@Override
	public void onDestroy() {
		SBLog.i(TAG, "onDestroy()");
		_m.unregisterUI(false);
		_m = null;
		super.onDestroy();
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	public void setPowerState(boolean isOn) {
		SBLog.i(TAG, "setPowerState()");
		_isPowerOn.set(isOn);
		if (isOn) {
			_powerBtn.setImageResource(R.drawable.power_button_on);
			_m.onPowerEnabled();
		} else {
			_powerBtn.setImageResource(R.drawable.power_button_off);
			_m.onPowerDisabled();
			if (_isComposeShowing.get()) {
				disableComposeView();
			}
		}
	}

	/* Button Listeners */

	private OnClickListener _composeTabListener = new OnClickListener() {
		public void onClick(View v) {
			showCompose();
		}
	};

	private OnClickListener _inboxTabListener = new OnClickListener() {
		public void onClick(View v) {
			showInbox();
		}
	};

	private OnClickListener _profileTabListener = new OnClickListener() {
		public void onClick(View v) {
			showProfile();
		}
	};

	private OnClickListener _powerButtonListener = new OnClickListener() {
		public void onClick(View v) {
			if (_isPowerOn.get()) {
				setPowerState(false);
			} else {
				setPowerState(true);
			}
		}
	};

	private OnClickListener _shoutButtonListener = new OnClickListener() {
		public void onClick(View v) {
			CharSequence text = _shoutInputEt.getText();

			if (text.length() == 0) {
				Toast.makeText(Shoutbreak.this, "cannot shout blanks",
						Toast.LENGTH_SHORT).show();
			} else {
				// TODO: filter all text going to server

				_shoutBtn.setImageResource(R.anim.shout_button_down);
				AnimationDrawable shoutButtonAnimation = (AnimationDrawable) _shoutBtn
						.getDrawable();
				shoutButtonAnimation.start();

				_m.shoutStart(text.toString(), _overlay.getCurrentPower());
				hideKeyboard();
			}
		}
	};
	
	private OnClickListener _enableLocationListener = new OnClickListener() {
		public void onClick(View v) {
			Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);  
            startActivityForResult(intent, 1);
		}
	};

	/* View Methods */

	public void showCompose() {
		SBLog.i(TAG, "showCompose()");
		_isComposeShowing.set(true);
		_isInboxShowing.set(false);
		_isProfileShowing.set(false);
		_composeTabBtn.setImageResource(R.drawable.tab_shouting_on);
		_inboxTabBtn.setImageResource(R.drawable.tab_inbox);
		_profileTabBtn.setImageResource(R.drawable.tab_user);
		_composeViewLl.setVisibility(View.VISIBLE);
		_inboxViewLl.setVisibility(View.GONE);
		_profileViewLl.setVisibility(View.GONE);
		findViewById(R.id.mapRl).setVisibility(View.VISIBLE);
		findViewById(R.id.inputRl).setVisibility(View.VISIBLE);
		findViewById(R.id.enableLocationBtn).setVisibility(View.GONE);
	}

	public void showInbox() {
		SBLog.i(TAG, "showInbox()");
		_isComposeShowing.set(false);
		_isInboxShowing.set(true);
		_isProfileShowing.set(false);
		_composeTabBtn.setImageResource(R.drawable.tab_shouting);
		_inboxTabBtn.setImageResource(R.drawable.tab_inbox_on);
		_profileTabBtn.setImageResource(R.drawable.tab_user);
		_composeViewLl.setVisibility(View.GONE);
		_inboxViewLl.setVisibility(View.VISIBLE);
		_profileViewLl.setVisibility(View.GONE);
	}

	public void showProfile() {
		SBLog.i(TAG, "showProfile()");
		_isComposeShowing.set(false);
		_isInboxShowing.set(false);
		_isProfileShowing.set(true);
		_composeTabBtn.setImageResource(R.drawable.tab_shouting);
		_inboxTabBtn.setImageResource(R.drawable.tab_inbox);
		_profileTabBtn.setImageResource(R.drawable.tab_user_on);
		_composeViewLl.setVisibility(View.GONE);
		_inboxViewLl.setVisibility(View.GONE);
		_profileViewLl.setVisibility(View.VISIBLE);
	}

	public void disableComposeView() {
		SBLog.i(TAG, "disableComposeView()");
		findViewById(R.id.mapRl).setVisibility(View.GONE);
		findViewById(R.id.inputRl).setVisibility(View.GONE);
		findViewById(R.id.enableLocationBtn).setVisibility(View.VISIBLE);
	}

	/* Location and Data */

	public void onLocationDisabled() {
		SBLog.i(TAG, "onLocationDisabled()");
		setPowerState(false);
	}

	public void onDataDisabled() {
		SBLog.i(TAG, "onDataDisable()");
		setPowerState(false);
	}

	public void checkLocationProviderStatus() {
		SBLog.i(TAG, "checkLocationProviderStatus()");
		_m.checkLocationProviderStatus();
	}

	public void unableToTurnOnApp(boolean isLocationAvailable,
			boolean isDataAvailable) {
		SBLog.i(TAG, "unableToTurnOnApp()");
		String text = "unable to turn on app, ";
		if (!isLocationAvailable && !isDataAvailable) {
			text += "location and data connection unavailable";
		} else if (!isLocationAvailable) {
			text += "location unavailable";
		} else if (!isDataAvailable) {
			text += "data unavailable";
		}
		Toast.makeText(Shoutbreak.this, text, Toast.LENGTH_SHORT).show();
	}

	public void enableMapAndOverlay() {
		// TODO: just threw this together, not sure if it works
		if (_map == null) {
			_map = (CustomMapView) findViewById(R.id.mapCmv);
		}
		if (_overlay == null) {
			_overlay = new UserLocationOverlay(Shoutbreak.this, _map);
		}
		if (_map.getOverlays().size() == 0) {
			_map.getOverlays().add(_overlay);
		}
		MapController mapController = _map.getController();
		mapController.setZoom(C.DEFAULT_ZOOM_LEVEL);
		_map.setClickable(true);
		_map.setEnabled(true);
		_map.setUserLocationOverlay(_overlay);
		_map.postInvalidate();

		// TODO: remove this. called when 'on/off' switch is clicked
		/*_overlay.runOnFirstFix(new Runnable() {
			public void run() {
				GeoPoint loc = _overlay.getMyLocation();
				MapController mapController = _map.getController();
				mapController.animateTo(loc);
			}
		});*/
		_overlay.enableMyLocation();
	}

	public void startupSequence() {
		// begin the service
		_serviceIntent.putExtra(C.APP_LAUNCHED_FROM_UI, true);
		startService(_serviceIntent);

		// call this whenever the ui starts / resumes
		checkLocationProviderStatus();

		_inboxListViewAdapter = new InboxListViewAdapter(Shoutbreak.this, _m);
		_inboxListView.setAdapter(_inboxListViewAdapter);
		_inboxListView.setItemsCanFocus(false);
		_inboxListView.setOnItemClickListener(new ListView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> adapter, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				InboxViewHolder holder = (InboxViewHolder) view
						.getTag();
				String shoutId = holder.shoutId;
				holder.collapsed.setVisibility(View.GONE);
				holder.expanded.setVisibility(View.VISIBLE);
				Shout shout = (Shout) _inboxListViewAdapter
						.getItem(position);
				if (shout.state_flag == C.SHOUT_STATE_NEW) {
					_m.inboxNewShoutSelected(shout);
					_inboxListViewAdapter.notifyDataSetChanged();
				}
				_inboxListViewAdapter.getCacheExpandState().put(
						shoutId, true);
			}
		});
		
		_noticeListViewAdapter = new NoticeListViewAdapter(Shoutbreak.this, _m);
		_noticeListView.setAdapter(_noticeListViewAdapter);
			
		// hide splash
		_splashLl.setVisibility(View.GONE);
		
		// switch views
		Bundle extras = getIntent().getExtras();
		if (extras != null && extras.getBoolean(C.APP_LAUNCHED_FROM_NOTIFICATION)) {
			showInbox();
		} else {
			showCompose();
		}
		
	}
		
	public void disableMapAndOverlay() {
		_map.setEnabled(false);
	}

	public int getMapHeight() {
		return _map.getMeasuredHeight();
	}
	
	public void setPeopleCountText(String text) {
		SBLog.i(TAG, "setTitleBarText()");
		// TODO: semi transparent text box on map
		//_titleBarTv.setText(text);
	}

	public void hideKeyboard() {
		SBLog.i(TAG, "hideKeyboard()");
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(_shoutInputEt.getWindowToken(), 0);
	}

	public void handleShoutsReceived(List<Shout> inboxContent, int newShouts) {
		_inboxListViewAdapter.refresh(inboxContent);

		if (newShouts > 0) {
			String pluralShout = "shout" + (newShouts > 1 ? "s" : "");
			String notice = "just heard " + newShouts + " new " + pluralShout;
			giveNotice(notice);
		}
	}

	public void handleDensityChange(double newDensity, int level) {
		_overlay.handleDensityChange(newDensity, level);
	}

	public void handleLevelUp(double cellDensity, int newLevel) {
		_overlay.handleLevelUp(cellDensity, newLevel);
		giveNotice("you leveled up to level " + newLevel
				+ " !\nshoutreach grew +" + C.CONFIG_PEOPLE_PER_LEVEL
				+ " people");
	}

	public void handlePointsChange(int newPoints) {
		// TODO: something probably should be updated... stats page?
	}

	public void handleShoutSent() {
		AnimationDrawable shoutButtonAnimation = (AnimationDrawable) _shoutBtn
				.getDrawable();
		shoutButtonAnimation.stop();
		_shoutBtn.setImageResource(R.drawable.shout_button_up);

		giveNotice("shout sent");
		_shoutInputEt.setText("");
	}

	public void refreshInbox(List<Shout> inboxContent) {
		_inboxListViewAdapter.refresh(inboxContent);
	}

	public void giveNotice(String text) {
		//_noticeTv.setText(text);
		//_noticeRl.startAnimation(_noticeExpandAnim);
		//_noticeTv.setTextColor(Color.WHITE);
		//_noticeTv.startAnimation(_noticeShowTextAnim);
	}
	
}