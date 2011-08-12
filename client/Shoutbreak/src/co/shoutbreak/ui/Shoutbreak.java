package co.shoutbreak.ui;

import java.util.List;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;

import co.shoutbreak.R;
import co.shoutbreak.core.C;
import co.shoutbreak.core.Colleague;
import co.shoutbreak.core.Mediator;
import co.shoutbreak.core.Notice;
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
import android.widget.Toast;

public class Shoutbreak extends MapActivity implements Colleague {
	
	private static String TAG = "Shoutbreak";

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
	private NoticeTab _noticeTab;

	private CustomMapView _map;
	private UserLocationOverlay _overlay;
	private InboxListViewAdapter _inboxListViewAdapter;
	private NoticeListViewAdapter _noticeListViewAdapter;
	private ListView _inboxListView;
	private ListView _noticeListView;
		
	private Flag _isComposeShowing = new Flag("ui:_isComposeShowing");
	private Flag _isInboxShowing = new Flag("ui:_isInboxShowing");
	private Flag _isProfileShowing = new Flag("ui:_isProfileShowing");
	private Flag _isTurnedOn = new Flag("ui:_isTurnedOn");
	private Flag _isLocationEnabled = new Flag("ui:_isLocationEnabled");
	private Flag _isDataEnabled = new Flag("ui:_isDataEnabled");
	private Flag _isPowerPreferenceEnabled = new Flag("ui:_isPowerPreferenceEnabled");		// is power preference set to on
	
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
		_enableLocationBtn = (ImageButton) findViewById(R.id.enableLocationBtn);
		_enableLocationBtn.setOnClickListener(_enableLocationListener);
				
		// bind to service, initializes mediator
		_serviceIntent = new Intent(Shoutbreak.this, ShoutbreakService.class);
		bindService(_serviceIntent, _serviceConnection, Context.BIND_AUTO_CREATE);
	}
	
	@Override
	public void onResume() {
		SBLog.i(TAG, "onResume()");
		super.onResume();
		refreshFlags();
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
	
	private ServiceConnection _serviceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			SBLog.i(TAG, "onServiceConnected()");

			_serviceBridge = (ServiceBridgeInterface) service;
			_serviceBridge.registerUIWithMediator(Shoutbreak.this);
			_m.onServiceConnected();
			
			_serviceIntent.putExtra(C.APP_LAUNCHED_FROM_UI, true);
			startService(_serviceIntent);
			
			_noticeListViewAdapter = new NoticeListViewAdapter(Shoutbreak.this);
			_noticeListView.setAdapter(_noticeListViewAdapter);

			refreshFlags();
			
			_inboxListViewAdapter = new InboxListViewAdapter(Shoutbreak.this, _m);
			_inboxListView.setAdapter(_inboxListViewAdapter);
			_inboxListView.setItemsCanFocus(false);
			_inboxListView.setOnItemClickListener(new ListView.OnItemClickListener() {
				public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
					InboxViewHolder holder = (InboxViewHolder) view.getTag();
					String shoutId = holder.shoutId;
					holder.collapsed.setVisibility(View.GONE);
					holder.expanded.setVisibility(View.VISIBLE);
					Shout shout = (Shout) _inboxListViewAdapter.getItem(position);
					if (shout.state_flag == C.SHOUT_STATE_NEW) {
						_m.inboxNewShoutSelected(shout);
						_inboxListViewAdapter.notifyDataSetChanged();
					}
					_inboxListViewAdapter.getCacheExpandState().put(shoutId, true);
				}
			});
			
			hideSplash();
			
			checkForReferral();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			SBLog.i(TAG, "onServiceDisconnected()");
			_m.onServiceDisconnected();
		}
	};
	
	@Override
	public void onNewIntent(Intent intent) {
		SBLog.i(TAG, "onNewIntent()");
		super.onNewIntent(intent);
		// triggered when a notification tries to launch a new intent
		// and app is set to SINGLE_TOP
		checkForReferral();
	}
	
	
	private void refreshFlags() {
		SBLog.i(TAG, "refreshFlags()");
		_isDataEnabled.set(_m.isDataEnabled());
		_isLocationEnabled.set(_m.isLocationEnabled());
		_isPowerPreferenceEnabled.set(_m.isPowerPreferenceEnabled());
		
		if (_isDataEnabled.get()) {
			_m.onDataEnabled();
		} else {
			_m.onDataDisabled();
		}
		
		if (_isLocationEnabled.get()) {
			_m.onLocationEnabled();
		} else {
			_m.onLocationDisabled();
		}
		
		if (_isPowerPreferenceEnabled.get()) {
			_m.onPowerPreferenceEnabled();
		} else {
			_m.onPowerPreferenceDisabled();
		}
	}
	
	
	private void hideSplash() {
		SBLog.i(TAG, "hideSplash()");
		_splashLl.setVisibility(View.GONE);
	}
	
	private void checkForReferral() {
		SBLog.i(TAG, "checkForReferral()");
		Bundle extras = getIntent().getExtras();
		if (extras != null && extras.getBoolean(C.APP_LAUNCHED_FROM_NOTIFICATION)) {
			showInbox();	// app launched from notification
		} else {
			showCompose();
		}
	}
	
	public void onLocationEnabled() {
		SBLog.i(TAG, "onLocationEnabled()");
		_isLocationEnabled.set(true);
		turnOn();
	}
	
	public void onLocationDisabled() {
		SBLog.i(TAG, "onLocationDisabled()");
		_isLocationEnabled.set(false);
		turnOff();
	}
	
	public void onDataEnabled() {
		SBLog.i(TAG, "onDataEnabled()");
		_isDataEnabled.set(true);
		turnOn();
	}
	
	public void onDataDisabled() {
		SBLog.i(TAG, "onDataDisabled()");
		_isDataEnabled.set(false);
		turnOff();
	}
	
	public void onPowerPreferenceEnabled() {
		SBLog.i(TAG, "onPowerPreferenceEnabled()");
		_isPowerPreferenceEnabled.set(true);
		turnOn();
	}
	
	public void onPowerPreferenceDisabled() {
		SBLog.i(TAG, "onPowerPreferenceDisabled()");
		_isPowerPreferenceEnabled.set(false);
		turnOff();
	}
	
	private void enableComposeView() {
		SBLog.i(TAG, "enableComposeView()");
		boolean removeBlanket = true;
		if (!_isLocationEnabled.get()) {
			// TODO: hide blanket location button
			removeBlanket = false;
		}
		if (!_isDataEnabled.get()) {
			// TODO: hide blanket data button
			removeBlanket = false;
		}
		if (!_isPowerPreferenceEnabled.get()) {
			// TODO: hide blanket power button
			removeBlanket = false;
		}
		
		if (removeBlanket) {
			SBLog.i(TAG, "compose view successfully re-enabled");
			hideComposeBlanket();
			enableMapAndOverlay();
		} else {
			SBLog.i(TAG, "unable to enable compose view");
		}
	}
	
	private void disableComposeView() {
		SBLog.i(TAG, "disableComposeView()");
		showComposeBlanket();
		disableMapAndOverlay();
		
		if (!_isLocationEnabled.get()) {
			// TODO: show location blanket button
		}
		if (!_isDataEnabled.get()) {
			// TODO: show data blanket button
		}
		if (!_isPowerPreferenceEnabled.get()) {
			// TODO: show power blanket button
		}
		
		
	}
	
	private boolean turnOn() {
		SBLog.i(TAG, "turnOn()");
		if (_isPowerPreferenceEnabled.get() && _isLocationEnabled.get() && _isDataEnabled.get()) {
			if (!_isTurnedOn.get()) {
				setPowerSwitchButtonToOn();
				enableComposeView();
				//_m.startPolling();
				_isTurnedOn.set(true);
			}
			return true;
		} else {
			turnOff();
			return false;
		}
	}
	
	private void turnOff() {
		SBLog.i(TAG, "turnOff()");
		setPowerSwitchButtonToOff();
		disableComposeView();
		//_m.stopPolling();
		_isTurnedOn.set(false);
	}
	
	private void setPowerSwitchButtonToOn() {
		SBLog.i(TAG, "setPowerSwitchButtonToOn()");
		_powerBtn.setImageResource(R.drawable.power_button_on);
	}
	
	private void setPowerSwitchButtonToOff() {
		SBLog.i(TAG, "setPowerSwitchButtonToOff()");
		_powerBtn.setImageResource(R.drawable.power_button_off);
	}
	
	private void showCompose() {
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
	}
	
	private void showComposeBlanket() {
		SBLog.i(TAG, "showComposeBlanket()");
		findViewById(R.id.mapRl).setVisibility(View.GONE);
		findViewById(R.id.inputRl).setVisibility(View.GONE);
		findViewById(R.id.enableLocationBtn).setVisibility(View.VISIBLE);
	}
	
	private void hideComposeBlanket() {
		SBLog.i(TAG, "hideComposeBlanket()");
		findViewById(R.id.mapRl).setVisibility(View.VISIBLE);
		findViewById(R.id.inputRl).setVisibility(View.VISIBLE);
		findViewById(R.id.enableLocationBtn).setVisibility(View.GONE);
	}

	private void showInbox() {
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

	private void showProfile() {
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

	private void enableMapAndOverlay() {
		SBLog.i(TAG, "enableMapAndOverlay()");
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

		// Don't fucking remove this.
		_overlay.runOnFirstFix(new Runnable() {
			public void run() {
				GeoPoint loc = _overlay.getMyLocation();
				MapController mapController = _map.getController();
				mapController.animateTo(loc);
			}
		});
		_overlay.enableMyLocation();
	}
	
	private void disableMapAndOverlay() {
		SBLog.i(TAG, "disableMapAndOverlay()");
		if (_map != null) {
			_map.setEnabled(false);
			// TODO: disable overlay
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
		SBLog.i(TAG, "isRouteDisplayed()");
		return false;
	}
	
	public int getMapHeight() {
		SBLog.i(TAG, "getMapHeight()");
		return _map.getMeasuredHeight();
	}
	
	public void setPeopleCountText(String text) {
		SBLog.i(TAG, "setPeopleCountText()");
		// TODO: semi transparent text box on map
		//_titleBarTv.setText(text);
	}

	public void hideKeyboard() {
		SBLog.i(TAG, "hideKeyboard()");
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(_shoutInputEt.getWindowToken(), 0);
	}

	public void giveNotice(List<Notice> noticeContent) {
		SBLog.i(TAG, "giveNotice()");
		_noticeListViewAdapter.refresh(noticeContent);
		_noticeTab.showOneLine();
	}
	
	public void handleShoutsReceived(List<Shout> inboxContent, int newShouts) {
		SBLog.i(TAG, "handleShoutsReceived()");
		_inboxListViewAdapter.refresh(inboxContent);
	}

	public void handleDensityChange(double newDensity, int level) {
		SBLog.i(TAG, "handleDensityChange()");
		_overlay.handleDensityChange(newDensity, level);
	}

	public void handleLevelUp(double cellDensity, int newLevel) {
		SBLog.i(TAG, "handleLevelUp()");
		_overlay.handleLevelUp(cellDensity, newLevel);
	}

	public void handlePointsChange(int newPoints) {
		SBLog.i(TAG, "handlePointsChange()");
		// TODO: something probably should be updated... stats page?
	}

	public void handleShoutSent() {
		SBLog.i(TAG, "handleShoutSent()");
		AnimationDrawable shoutButtonAnimation = (AnimationDrawable) _shoutBtn.getDrawable();
		shoutButtonAnimation.stop();
		_shoutBtn.setImageResource(R.drawable.shout_button_up);
		_shoutInputEt.setText("");
	}

	public void refreshInbox(List<Shout> inboxContent) {
		SBLog.i(TAG, "refreshInbox()");
		_inboxListViewAdapter.refresh(inboxContent);
	}
	
	private OnClickListener _composeTabListener = new OnClickListener() {
		public void onClick(View v) {
			SBLog.i(TAG, "_composeTabListener.onClick()");
			showCompose();
		}
	};

	private OnClickListener _inboxTabListener = new OnClickListener() {
		public void onClick(View v) {
			SBLog.i(TAG, "_inboxTabListener.onClick()");
			showInbox();
		}
	};

	private OnClickListener _profileTabListener = new OnClickListener() {
		public void onClick(View v) {
			SBLog.i(TAG, "_profileTabListener.onClick()");
			showProfile();
		}
	};

	private OnClickListener _powerButtonListener = new OnClickListener() {
		public void onClick(View v) {
			SBLog.i(TAG, "_powerButtonListener.onClick()");
			// only change the power preference when they press the on/off switch
			if (!_isTurnedOn.get()) {
				_m.setPowerPreferenceToOn();
				if (!turnOn()) {
					String text = "unable to turn on app, ";
					if (!_isLocationEnabled.get() && !_isDataEnabled.get()) {
						text += "location and data connection unavailable";
					} else if (!_isLocationEnabled.get()) {
						text += "location unavailable";
					} else if (!_isDataEnabled.get()) {
						text += "data unavailable";
					}
					Toast.makeText(Shoutbreak.this, text, Toast.LENGTH_SHORT).show();
					SBLog.i(TAG, text);
				}
			} else {
				_m.setPowerPreferenceToOff();
				turnOff();
			}
		}
	};

	private OnClickListener _shoutButtonListener = new OnClickListener() {
		public void onClick(View v) {
			SBLog.i(TAG, "_shoutButtonListener.onClick()");
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
			SBLog.i(TAG, "_enableLocationListener.onClick()");
			Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);  
            startActivityForResult(intent, 1);
		}
	};
	
}