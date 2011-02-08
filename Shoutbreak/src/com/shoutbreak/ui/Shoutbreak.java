package com.shoutbreak.ui;

import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
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
import com.shoutbreak.R;
import com.shoutbreak.Shout;
import com.shoutbreak.service.IServiceBridge;
import com.shoutbreak.service.ShoutbreakService;
import com.shoutbreak.service.User;

import com.shoutbreak.service.CellDensity;
import com.shoutbreak.ui.map.CustomMapView;

public class Shoutbreak extends MapActivity {

	private Intent _serviceIntent;
	private ShoutbreakServiceConnection _serviceConn;
	private IServiceBridge _serviceBridge; // This is how we access the service.
	private InboxListViewAdapter _inboxListViewAdapter;
	private UserLocationOverlay _userLocationOverlay;
	protected MapController _mapController;
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
	
		_serviceIntent = new Intent(Shoutbreak.this, ShoutbreakService.class);
		_inboxListViewAdapter = new InboxListViewAdapter(this);
		_inputMM = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		
		_cRlMap = (RelativeLayout)findViewById(R.id.rlMap);
		_cRlShoutInput = (RelativeLayout)findViewById(R.id.rlShoutInput);
		_cRlInbox = (RelativeLayout)findViewById(R.id.rlInbox);
		_cRlUser = (RelativeLayout)findViewById(R.id.rlUser);
		_cBtnPower = (ImageButton)findViewById(R.id.btnPower);
		_cBtnShout = (ImageButton)findViewById(R.id.btnShout);
		_cBtnShoutingTab = (ImageButton)findViewById(R.id.btnShoutingTab);
		_cBtnInboxTab = (ImageButton)findViewById(R.id.btnInboxTab);
		_cBtnUserTab = (ImageButton)findViewById(R.id.btnUserTab);
		_cTvTitleBar = (TextView)findViewById(R.id.tvTitleBar);
		_cShoutText = (EditText) findViewById(R.id.etShoutText);
		_cInboxListView = (ListView)findViewById(R.id.lvInbox);
		
		_cNoticeBoxShouting = (RelativeLayout) findViewById(R.id.rlNoticeShouting);
		_cNoticeTextShouting = (TextView) findViewById(R.id.tvNoticeShouting);
		_cNoticeBoxInbox = (RelativeLayout) findViewById(R.id.rlNoticeInbox);
		_cNoticeTextInbox = (TextView) findViewById(R.id.tvNoticeInbox);
		_cNoticeBoxUser = (RelativeLayout) findViewById(R.id.rlNoticeUser);
		_cNoticeTextUser = (TextView) findViewById(R.id.tvNoticeUser);
		
		_cBtnPower.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (_isPowerOn) {
					turnServiceOff();
				} else {
					turnServiceOn();
				}
			}
		});
		
		_cBtnShout.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String text = _cShoutText.getText().toString().trim();
				int power = _userLocationOverlay.getCurrentPower();
				_serviceBridge.shout(text, power);
				hideKeyboard();
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
		
		_cBtnShoutingTab.setImageResource(R.drawable.tab_shouting_on); // start in shouts tab
		
		initMap();
		initInboxListView();
		
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (User.getBooleanPreference(this, C.PREF_APP_ON_OFF_STATUS, true)) {
			turnEverythingOn();
		} else {
			turnEverythingOff();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		// we disable/enable to be nice to user battery
		_userLocationOverlay.disableMyLocation();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		releaseService();
	}
	
	// SERVICE CONNECTION /////////////////////////////////////////////////////

	// This is how the service accesses us.
	private IUIBridge _uiBridge = new IUIBridge() {

		public void test(String s) {
			setTitleBarText(s);
		}

		public void updateInboxView(List<Shout> shoutsForDisplay) {
			_inboxListViewAdapter.updateDisplay(shoutsForDisplay);			
		}

		public void shoutSent() {
			// TODO Auto-generated method stub
			
		}

	};

	class ShoutbreakServiceConnection implements ServiceConnection {

		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service. Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			_serviceBridge = (IServiceBridge) service;
			_serviceBridge.registerUIBridge(_uiBridge);
			
			// we disable/enable to be nice to user battery
			_userLocationOverlay.enableMyLocation();
			_serviceBridge.activateLocationTracker();
			
			CellDensity cellDensity = _serviceBridge.getCurrentCellDensity();
			if (cellDensity.isSet) {	
				_userLocationOverlay.setPopulationDensity(cellDensity.density);
			}	
			
			_serviceBridge.runServiceFromUI();
			// Tell the user about this for our demo.
			Toast.makeText(Shoutbreak.this, R.string.local_service_connected, Toast.LENGTH_SHORT).show();
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			_serviceBridge.unRegisterUIBridge();
			_serviceBridge = null;
			
			Toast.makeText(Shoutbreak.this, R.string.local_service_disconnected, Toast.LENGTH_SHORT).show();
		}
	};
	
	private void turnServiceOn() {
		if (_serviceBridge == null) {
			startService(_serviceIntent);
			// Establish a connection with the service. We use an explicit
			// class name because we want a specific service implementation that
			// we know will be running in our own process (and thus won't be
			// supporting component replacement by other applications).
			_serviceConn = new ShoutbreakServiceConnection();
			bindService(_serviceIntent, _serviceConn, Context.BIND_AUTO_CREATE);
		}
		User.setBooleanPreference(this, C.PREF_APP_ON_OFF_STATUS, true);
		_cBtnPower.setImageResource(R.drawable.power_button_on);
		_inboxListViewAdapter.setServiceIsOn(true);
		_isPowerOn = true;
	}

	private void turnServiceOff() {
		releaseService();
		stopService(_serviceIntent);
		User.setBooleanPreference(this, C.PREF_APP_ON_OFF_STATUS, false);
		_cBtnPower.setImageResource(R.drawable.power_button_off);
		_inboxListViewAdapter.setServiceIsOn(false);
		_isPowerOn = false;
	}

	protected void releaseService() {
		if (_serviceBridge != null) {
			// Detach our existing connection.
			unbindService(_serviceConn);
			_serviceConn = null;
		}
	}
	
	// THE REST ///////////////////////////////////////////////////////////////

	protected void initMap() {
		_cMapView = (CustomMapView)findViewById(R.id.cmvMap);
		_userLocationOverlay = new UserLocationOverlay(this, _cMapView);
		_mapController = _cMapView.getController();
		_mapController.setZoom(C.DEFAULT_ZOOM_LEVEL);
		_cMapView.setClickable(true);
		_cMapView.setEnabled(true);
		//_cMapView.setUI(this);
		_cMapView.setUserLocationOverlay(_userLocationOverlay);
		_cMapView.getOverlays().add(_userLocationOverlay);
		_cMapView.postInvalidate();
		_userLocationOverlay.runOnFirstFix(new Runnable() {
			public void run() {
				_mapController.animateTo(_userLocationOverlay.getMyLocation());
			}
		});
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
		        if (((Shout)_inboxListViewAdapter.getItem(position)).state_flag == C.SHOUT_STATE_NEW) {
		        	_serviceBridge.markShoutAsRead(shoutID);
		        	_inboxListViewAdapter.notifyDataSetChanged();
		    	}
		        
		        _inboxListViewAdapter.getCacheExpandState().put(shoutID, true);
			}

		
        });
	}
	
	public void hideKeyboard() {
		_inputMM.hideSoftInputFromWindow(_cShoutText.getWindowToken(), 0);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
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
	
	protected void turnEverythingOn() {
		turnServiceOn();
	}
	
	protected void turnEverythingOff() {
		// we disable/enable to be nice to user battery
		_userLocationOverlay.disableMyLocation();
		if (_serviceBridge != null) {
			_serviceBridge.disableLocationTracker();
		}
		turnServiceOff(); // this is probably redundant but we'll be totally sure service is dead with this
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
	
	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

}
