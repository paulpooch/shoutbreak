package co.shoutbreak.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import co.shoutbreak.R;
import co.shoutbreak.core.C;
import co.shoutbreak.core.Colleague;
import co.shoutbreak.core.Mediator;
import co.shoutbreak.core.Mediator.ThreadSafeMediator;
import co.shoutbreak.core.ServiceBridgeInterface;
import co.shoutbreak.core.ShoutbreakService;
import co.shoutbreak.core.utils.DialogBuilder;
import co.shoutbreak.core.utils.Flag;
import co.shoutbreak.core.utils.SBLog;
import co.shoutbreak.storage.noticetab.MultiDirectionSlidingDrawer;

import com.crittercism.app.Crittercism;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;

public class Shoutbreak extends MapActivity implements Colleague {

	private static String TAG = "Shoutbreak";

	private Mediator _m;
	private Intent _serviceIntent;
	private ServiceBridgeInterface _serviceBridge;

	public UserLocationOverlay userLocationOverlay;
	public ImageButton shoutBtn;
	public EditText shoutInputEt;
	public DialogBuilder dialogBuilder;
	public MultiDirectionSlidingDrawer noticeTabSd;
	public TextView noticeTabShoutsTv;
	public TextView noticeTabPointsTv;
	public ListView noticeTabListView;
	public ListView inboxListView;
	public TextView mapPeopleCountTv;
	public TextView userStatsParagraphTv;
	public TextView userCurrentShoutreachTv;
	public TextView userPointsTv;
	public TextView userNextLevelAtTv;
	public TextView userNextShoutreachTv;
	public RoundProgress userLevelUpProgessRp;
	public CheckBox sigCheckboxCb;
	public EditText sigInputEt;
	
	private RelativeLayout _inputLayoutRl;
	private LinearLayout _composeBlanketLl;
	private RelativeLayout _blanketDataRl;
	private RelativeLayout _blanketRadiusRl;
	private RelativeLayout _blanketLocationRl;
	private RelativeLayout _blanketPowerRl;
	private ImageButton _powerBtn;
	private ImageButton _powerExtensionBtn;
	private ImageButton _composeTabBtn;
	private ImageButton _inboxTabBtn;
	private ImageButton _profileTabBtn;
	private Button _enableLocationBtn;
	private Button _turnOnBtn;
	private ImageButton _mapCenterBtn;
	private LinearLayout _splashLl;
	private LinearLayout _composeViewLl;
	private LinearLayout _inboxViewLl;
	private LinearLayout _profileViewLl;
	private CustomMapView _map;
	private LinearLayout _mapOptionsLl;
	private ImageButton _sigClearBtn;
	private ImageButton _sigSaveBtn;

	private Flag _isComposeShowing = new Flag("ui:_isComposeShowing");
	private Flag _isInboxShowing = new Flag("ui:_isInboxShowing");
	private Flag _isProfileShowing = new Flag("ui:_isProfileShowing");
	private Flag _isTurnedOn = new Flag("ui:_isTurnedOn");
	// This flag isn't critical but may be nice to have one day.
	private Flag _doesMapKnowLocation = new Flag("ui:_doesMapKnowLocation");

	@Override
	public void onCreate(Bundle extras) {
		SBLog.lifecycle(TAG, "onCreate()");
		SBLog.constructor(TAG);

		super.onCreate(extras);
		// Crittercism crash reporting.
		// We run it without the secondary service. https://www.crittercism.com/developers/docs-optional-android
		boolean crittercismServiceDisabled = true;
		Crittercism.init(getApplicationContext(), "4efcb1a6b093157faa0000bf", "4efcb1a6b093157faa0000bfrh8c36ab", "fpylqvhcbkz9e3dvaouixtwfgnjrpocu", crittercismServiceDisabled);
		setContentView(R.layout.main);

		shoutBtn = (ImageButton) findViewById(R.id.shoutBtn);
		shoutInputEt = (EditText) findViewById(R.id.shoutInputEt);
		inboxListView = (ListView) findViewById(R.id.inboxLv);
		noticeTabSd = (MultiDirectionSlidingDrawer) findViewById(R.id.noticeDrawerSd);
		noticeTabListView = (ListView) findViewById(R.id.noticeLv);
		noticeTabShoutsTv = (TextView) findViewById(R.id.noticeTabShoutsTv);
		noticeTabPointsTv = (TextView) findViewById(R.id.noticeTabPointsTv);
		mapPeopleCountTv = (TextView) findViewById(R.id.mapPeopleCountTv);
		userStatsParagraphTv = (TextView) findViewById(R.id.userStatsParagraphTv);
		userCurrentShoutreachTv = (TextView) findViewById(R.id.userCurrentShoutreachTv);
		userPointsTv = (TextView) findViewById(R.id.userPointsTv);
		userNextLevelAtTv = (TextView) findViewById(R.id.userNextLevelAtTv);
		userNextShoutreachTv = (TextView) findViewById(R.id.userNextShoutreachTv);
		userLevelUpProgessRp = (RoundProgress) findViewById(R.id.userLevelUpProgressRp);
		sigInputEt = (EditText) findViewById(R.id.sigInputEt);
		sigCheckboxCb = (CheckBox) findViewById(R.id.sigCheckboxCb);
		sigCheckboxCb.setOnCheckedChangeListener(_sigCheckboxListener);
		
		_inputLayoutRl = (RelativeLayout) findViewById(R.id.inputRl);
		_composeBlanketLl = (LinearLayout) findViewById(R.id.composeBlanketLl);
		_blanketDataRl = (RelativeLayout) findViewById(R.id.blanketDataRl);
		_blanketRadiusRl = (RelativeLayout) findViewById(R.id.blanketDensityRl);
		_blanketLocationRl = (RelativeLayout) findViewById(R.id.blanketLocationRl);
		_blanketPowerRl = (RelativeLayout) findViewById(R.id.blanketPowerRl);
		_composeTabBtn = (ImageButton) findViewById(R.id.composeTabBtn);
		_inboxTabBtn = (ImageButton) findViewById(R.id.inboxTabBtn);
		_profileTabBtn = (ImageButton) findViewById(R.id.profileTabBtn);
		_powerBtn = (ImageButton) findViewById(R.id.powerBtn);
		_powerExtensionBtn = (ImageButton) findViewById(R.id.powerExtensionBtn);
		_mapCenterBtn = (ImageButton) findViewById(R.id.mapCenterBtn);
		_splashLl = (LinearLayout) findViewById(R.id.splashLl);
		_composeViewLl = (LinearLayout) findViewById(R.id.composeViewLl);
		_inboxViewLl = (LinearLayout) findViewById(R.id.inboxViewLl);
		_profileViewLl = (LinearLayout) findViewById(R.id.profileViewLl);
		_map = (CustomMapView) findViewById(R.id.mapCmv);
		_enableLocationBtn = (Button) findViewById(R.id.enableLocationBtn);
		_turnOnBtn = (Button) findViewById(R.id.turnOnBtn);
		_mapOptionsLl = (LinearLayout) findViewById(R.id.mapOptionsLl);
		_sigClearBtn = (ImageButton) findViewById(R.id.sigClearBtn);
		_sigSaveBtn = (ImageButton) findViewById(R.id.sigSaveBtn);
		
		shoutBtn.setOnClickListener(_shoutButtonListener);
		_composeTabBtn.setOnClickListener(_composeTabListener);
		_inboxTabBtn.setOnClickListener(_inboxTabListener);
		_profileTabBtn.setOnClickListener(_profileTabListener);
		_powerExtensionBtn.setOnClickListener(_powerButtonListener);
		_enableLocationBtn.setOnClickListener(_enableLocationListener);
		_enableLocationBtn.getBackground().setColorFilter(0xAA9900FF, Mode.SRC_ATOP);
		_turnOnBtn.setOnClickListener(_turnOnListener);
		_turnOnBtn.getBackground().setColorFilter(0xAA9900FF, Mode.SRC_ATOP);
		_sigClearBtn.getBackground().setColorFilter(0xAA9900FF, Mode.SRC_ATOP);
		_sigClearBtn.setOnClickListener(_sigClearListener);
		_sigSaveBtn.getBackground().setColorFilter(0xAA9900FF, Mode.SRC_ATOP);
		_sigSaveBtn.setOnClickListener(_sigSaveListener);

		//noticeTabShoutsIv.setVisibility(View.INVISIBLE);
		//noticeTabPointsIv.setVisibility(View.INVISIBLE);
		noticeTabShoutsTv.setVisibility(View.INVISIBLE);
		noticeTabPointsTv.setVisibility(View.INVISIBLE);
		
		_mapCenterBtn.getBackground().setColorFilter(0xAA9900FF, Mode.SRC_ATOP);
		_mapCenterBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				SBLog.userAction("_mapCenterBtn.onClick");
				// CenterMapTask task = new CenterMapTask();
				// // true = do show toast
				// task.execute(true);
				centerMapOnUser(true);
			}
		});

		if (android.os.Build.MODEL.equals(C.PHONE_DROID_X)) {
			shoutInputEt.setOnFocusChangeListener(new OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					SBLog.userAction("shoutInputEt.onFocusChange");
					String text = shoutInputEt.getText().toString();
					text = "\n\n" + text.trim();
					shoutInputEt.setText(text);
					shoutInputEt.setSelection(text.length());
				}
			});
		}
		
		// bind to service, initializes mediator
		_serviceIntent = new Intent(Shoutbreak.this, ShoutbreakService.class);
		bindService(_serviceIntent, _serviceConnection, Context.BIND_AUTO_CREATE);
		
	}

	@Override
	public void onResume() {
		SBLog.lifecycle(TAG, "onResume()");
		if (_m != null) {
			// This is not a cold start.
			_m.setIsUiInForeground(true);
			_m.clearNotifications();
			wasLaunchFromReferral();
			reflectPowerState();

			ThreadSafeMediator threadSafeMediator = _m.getAThreadSafeMediator();
			threadSafeMediator.resetPollingDelay();
		}
		enableMapAndOverlay();
		super.onResume();
		// refreshFlags();
	}

	@Override
	public void onPause() {
		SBLog.lifecycle(TAG, "onPause()");
		if (_m != null) {
			_m.setIsUiInForeground(false);
		}
		disableMapAndOverlay();
		super.onPause();
	}

	public void setMediator(Mediator mediator) {
		SBLog.lifecycle(TAG, "setMediator()");
		_m = mediator;
	}

	@Override
	public void unsetMediator() {
		SBLog.lifecycle(TAG, "unsetMediator()");
		_m = null;
	}

	/*
	 * // Map centering occurs on the UIThread. Therefore pointless to be Async.
	 * private class CenterMapTask extends AsyncTask<Boolean, Void, Boolean> {
	 * 
	 * @Override protected Boolean doInBackground(Boolean... showToast) { if
	 * (overlay != null && _map != null) { GeoPoint loc = overlay.getMyLocation();
	 * if (loc != null) { MapController mapController = _map.getController(); if
	 * (mapController != null) { mapController.animateTo(loc); return
	 * showToast[0]; } } } return false; }
	 * 
	 * @Override protected void onPostExecute(Boolean showToast) { if (showToast)
	 * { // this from user pressing centerMap button
	 * _m.getUiGateway().toast("You are here.", Toast.LENGTH_SHORT); } } }
	 */

	public void centerMapOnUser(boolean showToast) {
		if (_isTurnedOn.get() && _map != null && userLocationOverlay != null && _map.getOverlays().size() > 0 && userLocationOverlay.isMyLocationEnabled()) {

			// CenterMapTask task = new CenterMapTask();
			// task.execute(false);

			// MyLocationOverlay is ghetto. The below line does not work.
			// GeoPoint loc = overlay.getMyLocation();
			// We do this elaborate crap instead.
			GeoPoint loc = null;
			if (_m != null) {
				ThreadSafeMediator threadSafeMediator = _m.getAThreadSafeMediator();
				loc = threadSafeMediator.getLocationAsGeoPoint();

				// Best place to show 'User has screen open' even if they're not
				// touching anything.
				// threadSafeMediator.resetPollingDelay();
			}

			if (loc != null) {
				MapController mapController = _map.getController();
				if (mapController != null) {
					mapController.animateTo(loc);
					_doesMapKnowLocation.set(true);
					dialogBuilder.showDialog(DialogBuilder.DISMISS_DIALOG_WAIT_FOR_MAP_TO_HAVE_LOCATION, "");
					if (showToast) {
						Toast.makeText(Shoutbreak.this, "You are here.", Toast.LENGTH_SHORT).show();
					}
				}
			} else {
				_doesMapKnowLocation.set(false);
				dialogBuilder.showDialog(DialogBuilder.DIALOG_WAIT_FOR_MAP_TO_HAVE_LOCATION, this.getString(R.string.holdWhileFinding));
			}

		}
	}

	private ServiceConnection _serviceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			SBLog.lifecycle(TAG, "onServiceConnected()");

			_serviceBridge = (ServiceBridgeInterface) service;
			_serviceBridge.registerUIWithMediator(Shoutbreak.this);
			_m.onServiceConnected();

			_serviceIntent.putExtra(C.NOTIFICATION_LAUNCHED_FROM_UI, true);
			startService(_serviceIntent);

			userLocationOverlay = new UserLocationOverlay(Shoutbreak.this, _map);
			dialogBuilder = new DialogBuilder(Shoutbreak.this, _m);

			refreshFlags();
			_m.refreshUiComponents(noticeTabSd);

			if (wasLaunchFromReferral()) {
				hideSplash(false);
			} else {
				hideSplash(true);
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			SBLog.lifecycle(TAG, "onServiceDisconnected()");
			_m.onServiceDisconnected();
		}
	};

	@Override
	public void onNewIntent(Intent intent) {
		// Triggered when a notification tries to launch a new intent and app is set
		// to SINGLE_TOP.
		// In our case this happens when user doesn't kill app, and then returns.
		// Like hitting home button, then clicking a notification.
		SBLog.lifecycle(TAG, "onNewIntent()");
		super.onNewIntent(intent);

		// checkForReferral() will check the ORIGINAL intent which has no extras,
		// even though we may now be coming from a notification, so let's forward
		// those extras before checkForReferral()
		Bundle extras = intent.getExtras();
		if (extras != null) {
			getIntent().putExtras(extras);
		}
		wasLaunchFromReferral();
	}

	private void refreshFlags() {
		SBLog.method(TAG, "refreshFlags()");


		_isTurnedOn.set(false);

		_m.refreshFlags();
		





	}

	private void hideSplash(boolean showCompose) {
		// This uses a timer to hide the splash, then show compose screen.
		// Because of timer, even if we showInbox after this is called, compose will
		// end up displayed when timer triggers.
		// So we gotta pass in showCompose in case launch was from a referral and
		// should end up in inbox after we hide splash.
		SBLog.lifecycle(TAG, "hideSplash()");
		final boolean showComposeScreen = showCompose;
		Handler splashHandler = new Handler() {
			@Override
			public void handleMessage(Message message) {
				_splashLl.startAnimation(AnimationUtils.loadAnimation(Shoutbreak.this, android.R.anim.fade_out));
				_splashLl.setVisibility(View.GONE);
				if (showComposeScreen) {
					showCompose();
				} else {
					showInbox();
				}
				handleFirstRun();
				super.handleMessage(message);
			}
		};
		splashHandler.sendMessageDelayed(splashHandler.obtainMessage(), 1000);
	}

	private boolean wasLaunchFromReferral() {
		SBLog.lifecycle(TAG, "wasLaunchFromReferral()");
		boolean wasFromReferral = false;
		Bundle extras = getIntent().getExtras();
		if (extras != null && extras.getBoolean(C.NOTIFICATION_LAUNCHED_FROM_NOTIFICATION)) {
			showInbox(); // app launched from notification
			wasFromReferral = true;
		} else {
			// Do we want to force them into compose view if their phone idles out?
			// showCompose();
		}
		// Let's clear the extras now so they don't haunt us.
		// Remember if app is OnResume()'d without being destroyed we could have
		// stale extras.
		if (extras != null) {
			extras.clear();
		}
		return wasFromReferral;
	}

	private void handleFirstRun() {
		SBLog.method(TAG, "handleFirstRun()");
		if (_m != null && _m.isFirstRun()) {
			// TODO: tutorial goes here
			// TutorialDialog tut = new TutorialDialog(this);
			// tut.show();
		}
	}

	private void showComposeBlanket() {
		SBLog.method(TAG, "showComposeBlanket()");
		_composeBlanketLl.setVisibility(View.VISIBLE);
		_map.setVisibility(View.GONE);
		_mapOptionsLl.setVisibility(View.GONE);
		_inputLayoutRl.setVisibility(View.GONE);
		disableMapAndOverlay();
	}

	private void hideComposeBlanket() {
		SBLog.method(TAG, "hideComposeBlanket()");
		_composeBlanketLl.setVisibility(View.GONE);
		_map.setVisibility(View.VISIBLE);
		_mapOptionsLl.setVisibility(View.VISIBLE);
		_inputLayoutRl.setVisibility(View.VISIBLE);
		enableMapAndOverlay();
	}	

	public void showCompose() {
		SBLog.method(TAG, "showCompose()");
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

	public void showInbox() {
		SBLog.method(TAG, "showInbox()");
		_isComposeShowing.set(false);
		_isInboxShowing.set(true);
		_isProfileShowing.set(false);
		_composeTabBtn.setImageResource(R.drawable.tab_shouting);
		_inboxTabBtn.setImageResource(R.drawable.tab_inbox_on);
		_profileTabBtn.setImageResource(R.drawable.tab_user);
		_composeViewLl.setVisibility(View.GONE);
		_inboxViewLl.setVisibility(View.VISIBLE);
		_profileViewLl.setVisibility(View.GONE);
		clearShoutsNotice();
	}

	public void showProfile() {
		SBLog.method(TAG, "showProfile()");
		_isComposeShowing.set(false);
		_isInboxShowing.set(false);
		_isProfileShowing.set(true);
		_composeTabBtn.setImageResource(R.drawable.tab_shouting);
		_inboxTabBtn.setImageResource(R.drawable.tab_inbox);
		_profileTabBtn.setImageResource(R.drawable.tab_user_on);
		_composeViewLl.setVisibility(View.GONE);
		_inboxViewLl.setVisibility(View.GONE);
		_profileViewLl.setVisibility(View.VISIBLE);
		clearPointsNotice();
	}

	private void enableMapAndOverlay() {
		SBLog.method(TAG, "enableMapAndOverlay()");
	
		// Make sure this isn't initial bootup.
		if (userLocationOverlay != null) {
			
			MapController mapController = _map.getController();
			if (_map.getOverlays().size() == 0) {
				_map.getOverlays().add(userLocationOverlay);
				mapController.setZoom(C.DEFAULT_ZOOM_LEVEL);
			}
			_map.setClickable(true);
			_map.setEnabled(true);
			_map.setUserLocationOverlay(userLocationOverlay);
			_map.postInvalidate();
			userLocationOverlay.enableMyLocation();
			
			// This is sort of irrelevant, but this is just a convenient place for this
			// code chunk.
			if (!_doesMapKnowLocation.get()) {
				dialogBuilder.showDialog(DialogBuilder.DIALOG_WAIT_FOR_MAP_TO_HAVE_LOCATION, this.getString(R.string.holdWhileFinding));
			}
		}
		
	}

	private void disableMapAndOverlay() {
		SBLog.method(TAG, "disableMapAndOverlay()");
		// Make sure this isn't initial bootup.
		if (_map != null && userLocationOverlay != null) {
			_map.setEnabled(false);
			userLocationOverlay.disableMyLocation();
			// TODO: disable overlay
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		SBLog.method(TAG, "onPrepareOptionsMenu()");
		menu.clear();
		MenuInflater inflater = getMenuInflater();
		if (_isComposeShowing.get()) {
			inflater.inflate(R.menu.compose_menu, menu);
		} else if (_isInboxShowing.get()) {
			inflater.inflate(R.menu.inbox_menu, menu);
		} else if (_isProfileShowing.get()) {
			inflater.inflate(R.menu.profile_menu, menu);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		SBLog.method(TAG, "onOptionsItemSelected()");
		switch (item.getItemId()) {
		case R.id.clear_notices:
			// TODO: clear notices
			return true;
		case R.id.empty_inbox:
			// TODO: empty inbox
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onDestroy() {
		SBLog.lifecycle(TAG, "onDestroy()");
		if (_m != null) {
			_m.unregisterUI(false);
			;
			_m = null;
		}
		super.onDestroy();
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	public boolean canAppTurnOn(boolean onUiThread, boolean causedByPowerButton) {
		SBLog.method(TAG, "canAppTurnOn()");

		boolean canTurnOn = true;
		boolean showBlanket = false;
		boolean suppressPowerButtonError = false;

		if (!_m.isDataEnabled()) {
			canTurnOn = false;
			showBlanket = true;
			suppressPowerButtonError = true;
			if (onUiThread) {
				_blanketDataRl.setVisibility(View.VISIBLE);
				SBLog.logic("Data Blanket - isDataEnabled = false");
			}
		} else {
			if (onUiThread) {
				_blanketDataRl.setVisibility(View.GONE);
			}
		}

		if (!_m.isLocationEnabled()) {
			canTurnOn = false;
			showBlanket = true;
			suppressPowerButtonError = true;
			if (onUiThread) {
				_blanketLocationRl.setVisibility(View.VISIBLE);
				SBLog.logic("Location Blanket - isLocationEnabled = false");
			}
		} else {
			if (onUiThread) {
				_blanketLocationRl.setVisibility(View.GONE);
			}
		}

		if (!causedByPowerButton && !_m.isPowerPreferenceEnabled()) {
			canTurnOn = false;
			showBlanket = true;
			if (!suppressPowerButtonError) {
				if (onUiThread) {
					_blanketPowerRl.setVisibility(View.VISIBLE);
					SBLog.logic("Power Blanket - isPowerPreferenceEnabled = false");
				}
			} else {
				if (onUiThread) {
					_blanketPowerRl.setVisibility(View.GONE);
				}
			}
		} else {
			if (onUiThread) {
				_blanketPowerRl.setVisibility(View.GONE);
			}
		}

		if (canTurnOn) {
			if (!userLocationOverlay.isShoutreachRadiusSet()) {
				showBlanket = true;
				if (onUiThread) {
					_blanketRadiusRl.setVisibility(View.VISIBLE);
				}
			} else {
				if (onUiThread) {
					_blanketRadiusRl.setVisibility(View.GONE);
				}
			}
		} else {
			if (onUiThread) {
				_blanketRadiusRl.setVisibility(View.GONE);
			}
		}

		if (onUiThread) {
			if (showBlanket) {
				showComposeBlanket();
				if (_m != null) {
					_m.getUiGateway().disableInputs();
				}
			} else {
				hideComposeBlanket();
				if (_m != null) {
					_m.getUiGateway().enableInputs();
				}
			}
		}
		
		// Let's double check all blankets manually
		int blanketFlagSum = _blanketRadiusRl.getVisibility() +
				_blanketPowerRl.getVisibility() +
				_blanketLocationRl.getVisibility() +
				_blanketDataRl.getVisibility();
		if (blanketFlagSum != 4 * View.GONE) {
			int issue = 0;
		}

		return canTurnOn;
	}

	public int getMapHeight() {
		return _map.getMeasuredHeight();
	}

	// http://stackoverflow.com/questions/2150078/android-is-software-keyboard-shown
	public void hideKeyboard() {
		SBLog.method(TAG, "hideKeyboard()");
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(shoutInputEt.getWindowToken(), 0);
	}

	private OnClickListener _composeTabListener = new OnClickListener() {
		public void onClick(View v) {
			SBLog.userAction("_composeTabListener.onClick()");
			showCompose();
		}
	};

	private OnClickListener _inboxTabListener = new OnClickListener() {
		public void onClick(View v) {
			SBLog.userAction("_inboxTabListener.onClick()");
			showInbox();
		}
	};

	private OnClickListener _profileTabListener = new OnClickListener() {
		public void onClick(View v) {
			SBLog.userAction("_profileTabListener.onClick()");
			showProfile();
		}
	};

	private OnClickListener _shoutButtonListener = new OnClickListener() {
		public void onClick(View v) {
			SBLog.userAction("_shoutButtonListener.onClick()");
			String text = shoutInputEt.getText().toString().trim();

			if (text.length() == 0) {
				Toast.makeText(Shoutbreak.this, "Cannot shout blanks.", Toast.LENGTH_LONG);
			} else {
				// TODO: filter all text going to server
				
				String signature = sigInputEt.getText().toString();
				if (sigCheckboxCb.isChecked() && signature.length() > 0) {
					text += "     [" + sigInputEt.getText().toString() + "]";
					text.trim();
				}
				
				if (text.length() <= getResources().getInteger(R.integer.shoutMaxLength)) {
					shoutBtn.setImageResource(R.anim.shout_button_down);
					AnimationDrawable shoutButtonAnimation = (AnimationDrawable) shoutBtn.getDrawable();
					shoutButtonAnimation.start();
					_m.handleShoutStart(text.toString(), userLocationOverlay.getCurrentPower(), C.NULL_REPLY);
					hideKeyboard();
				} else {
					Toast.makeText(Shoutbreak.this, "Shout is too long (256 char limit).", Toast.LENGTH_LONG);
				}
			}
		}
	};

	private OnClickListener _enableLocationListener = new OnClickListener() {
		public void onClick(View v) {
			SBLog.userAction("_enableLocationListener.onClick()");
			Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			startActivityForResult(intent, C.ACTIVITY_RESULT_LOCATION);
		}
	};

	private OnCheckedChangeListener _sigCheckboxListener = new OnCheckedChangeListener() {
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			if (_m != null) {
				_m.saveUserSignature(sigInputEt.getText().toString(), isChecked);				
			}
		}
	};
	
	private OnClickListener _sigClearListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (_m != null) {
				sigInputEt.setText("");
				sigCheckboxCb.setChecked(false);
				_m.saveUserSignature(sigInputEt.getText().toString(), sigCheckboxCb.isChecked());
			}
			hideKeyboard();			
		}
	};

	private OnClickListener _sigSaveListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (_m != null) {
				sigCheckboxCb.setChecked(true);
				_m.saveUserSignature(sigInputEt.getText().toString(), sigCheckboxCb.isChecked());
			}
			hideKeyboard();
		}
	};

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		SBLog.method(TAG, "onActivityResult()");
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case C.ACTIVITY_RESULT_LOCATION: { // returning from location provider
																				// activity
			if (_m.isLocationEnabled()) { // update location status
				_m.onLocationEnabled();
			} else {
				_m.onLocationDisabled();
			}
			break;
		}
		}
	}

	/////////////////////////////////////////////////////////////////////////////
	// POWER SYSTEM
	/////////////////////////////////////////////////////////////////////////////
	
	// Power button listener.
	private OnClickListener _powerButtonListener = new OnClickListener() {
		public void onClick(View v) {
			SBLog.userAction("_powerButtonListener.onClick()");
			PowerButtonTask task = new PowerButtonTask();
			task.execute();
		}
	};
	
	// From power blanket.
	private OnClickListener _turnOnListener = new OnClickListener() {
		public void onClick(View v) {
			SBLog.userAction("_turnOnListener.onClick()");
			_m.setPowerPreferenceToOn(true);
		}
	};

	// This is the call stack of how this works:
	//
	// _mediator.setPowerPreferenceToOn(onUiThread) ->
	// _preferences.setPowerPreferenceToOn(onUiThread) ->
	// _m.onPowerPreferenceEnabled(onUiThread) ->
	// _uiGateway.onPowerPreferenceEnabled(onUiThread) ->
	// _ui.onPowerPreferenceEnabled(onUiThread) ->
	// onPowerPreferenceEnabled(onUiThread) [right under this function]
	private class PowerButtonTask extends AsyncTask<Void, Void, Void> {
		
		private boolean failedToTurnOn;
		
		public PowerButtonTask() {
			failedToTurnOn = false;
		}
		
		@Override
		protected Void doInBackground(Void... unused) {
			SBLog.method(TAG, "PowerButtonTask.doInBackground()");
			// only change the power preference when they press the on/off switch
			if (!_isTurnedOn.get()) {
				// Turn On.
				if (canAppTurnOn(false, true)) {
					_m.setPowerPreferenceToOn(false);
				} else {
					failedToTurnOn = true;
				}
			} else {
				// Turn Off.
				_m.setPowerPreferenceToOff(false);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void unused) {
			// Now we're on the Ui Thread, just see what the outcome of callback city
			// was and set button to reflect that outcome.
			reflectPowerState();
			if (failedToTurnOn) {
				String error = "Unable to turn on.\n\n";
				if (!_m.isDataEnabled()) {
					error += "3G (cell network) and Wifi are both not providing data.\n\n";
				}
				if (!_m.isLocationEnabled()) {
					error += "Cell towers and GPS are both not providing location.\n\n";
				}
				if (!_m.isPowerPreferenceEnabled()) {
					error += "Your power button is set to Off.\n\n";
				}
				if (!userLocationOverlay.isShoutreachRadiusSet()) {
					error += "Cannot get nearby users from server.\n\n";
				}
				Toast.makeText(Shoutbreak.this, error, Toast.LENGTH_LONG).show();
			}
		}
	}
	
	public void reflectPowerState() {
		SBLog.method(TAG, "reflectPowerState()");
		canAppTurnOn(true, false);
		refreshPowerButtonImage();
	}

	public void onPowerPreferenceEnabled(boolean onUiThread) {
		SBLog.method(TAG, "onPowerPreferenceEnabled()");
		tryToTurnOn(onUiThread);
	}

	public void onPowerPreferenceDisabled(boolean onUiThread) {
		SBLog.method(TAG, "onPowerPreferenceDisabled()");
		turnOff(onUiThread);
	}

	public boolean tryToTurnOn(boolean onUiThread) {
		SBLog.method(TAG, "turnOn()");
		if (canAppTurnOn(onUiThread, false)) {
			_isTurnedOn.set(true);
			if (onUiThread) {
				refreshPowerButtonImage();
			}
		  _m.startPolling(onUiThread);
			return true;
		} else {
			turnOff(onUiThread);
	 		return false;
		}
	}

	public void turnOff(boolean onUiThread) {
		SBLog.method(TAG, "turnOff()");
		_isTurnedOn.set(false);
		if (onUiThread) {
			refreshPowerButtonImage();
		}
		canAppTurnOn(onUiThread, false);
	}
	
	private void refreshPowerButtonImage() {
		if (_isTurnedOn.get()) {
			_powerBtn.setImageResource(R.drawable.power_button_on);
		} else {
			_powerBtn.setImageResource(R.drawable.power_button_off);
		}
	}
	
	public void clearShoutsNotice() {
		noticeTabShoutsTv.setVisibility(View.INVISIBLE);
	}
	
	public void clearPointsNotice() {
		noticeTabPointsTv.setVisibility(View.INVISIBLE);
	}
	
}
