package co.shoutbreak.ui.views;

import java.util.Observable;
import java.util.Observer;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;

import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import co.shoutbreak.CustomMapView;
import co.shoutbreak.R;
import co.shoutbreak.shared.C;
import co.shoutbreak.shared.StateEvent;
import co.shoutbreak.shared.StateManager;
import co.shoutbreak.shared.User;
import co.shoutbreak.shared.utils.ErrorManager;
import co.shoutbreak.shared.utils.SBLog;
import co.shoutbreak.ui.SBContext;
import co.shoutbreak.ui.map.UserLocationOverlay;

public class ComposeView extends SBView implements Observer {
	
	private final String TAG = "ComposeView.java";
	
	private CustomMapView _cMapView;
	private MapController _mapController;
	private UserLocationOverlay _userLocationOverlay;
	
	// keyboard
	protected InputMethodManager _inputMM;
	
	private EditText _cShoutText;
	private ImageButton _cBtnShout;

	
	/* Do NOT store any SBContext parameters, will cause service leak */
	// Pretty sure we have to John.  Why can't we?
	// Map initialization takes it by default
	
	public ComposeView(SBContext context, String name, int resourceId, int notificationId) {
		super(context, name, resourceId, notificationId);
		_context.getStateManager().addObserver(this);
		
		_inputMM = (InputMethodManager)_context.getSystemService(SBContext.INPUT_METHOD_SERVICE);
		_cShoutText = (EditText)_context.findViewById(R.id.etShoutText);
		_cBtnShout = (ImageButton)_context.findViewById(R.id.btnShout);
		
		_cBtnShout.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String text = _cShoutText.getText().toString().trim();
				int power = _userLocationOverlay.getCurrentPower();
				boolean valid = true;
				if (text.equals("")) {
					ErrorManager.warnUser(_context, "cannot shout blanks");
					valid = false;
				}
				if (valid && power < 1) {
					ErrorManager.warnUser(_context, "a shout with zero power won't reach won't reach anybody");
					valid = false;
				}
				if (valid) {
					if (_context.getStateManager().isAppFullyFunctional()) {
						_context.getStateManager().shout(text, power);
					} else {
						ErrorManager.warnUser(_context, "cannot shout with service offline");
					}
				}
				hideKeyboard();
			}
		});
		
		initMap();
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
		_context.getStateManager().deleteObserver(this);		
	}
	
	/* LISTENERS: use AsyncTasks for expensive shit */
	
	/* OBSERVER METHODS */
	public void update(Observable observable, Object data) {
		StateManager stateManager = (StateManager)observable;
		StateEvent e = (StateEvent)data;
		if (e.locationTurnedOn) {
			if (_cMapView.getOverlays().size() == 0) {
				_cMapView.getOverlays().add(_userLocationOverlay);
				_cMapView.postInvalidate();
				stateManager.setIsUserOverlayVisible(true);
				animateMap(_userLocationOverlay.getMyLocation());
			}
		}
		if (e.locationTurnedOff) {
			if (_cMapView.getOverlays().size() > 0) {
				_cMapView.getOverlays().clear();
				_cMapView.postInvalidate();
			}
			stateManager.setIsUserOverlayVisible(false);
		}
	}
	
	private void initMap() {
		_cMapView = (CustomMapView) _context.findViewById(R.id.cmvMap);
		_userLocationOverlay = new UserLocationOverlay(_context, _cMapView);
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
		
		// TODO: remove this. called when 'on/off' switch is clicked
		_userLocationOverlay.runOnFirstFix(new Runnable() {
			public void run() {
				GeoPoint loc = _userLocationOverlay.getMyLocation();
				_mapController.animateTo(loc);
			}
		});
		_userLocationOverlay.enableMyLocation();
	}	
	
	public void animateMap(GeoPoint location) {
		if (location != null) {
			_mapController.animateTo(location);
		} else {
			Toast.makeText(_context, "animateMap called with null location", Toast.LENGTH_SHORT).show();
		}
	}
	
	public void hideKeyboard() {
		_inputMM.hideSoftInputFromWindow(_cShoutText.getWindowToken(), 0);
		_context.getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
			WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
	}	
	
}