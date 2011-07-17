package co.shoutbreak.ui.views;

import java.util.Observable;
import java.util.Observer;

import com.google.android.maps.MapController;

import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import co.shoutbreak.R;
import co.shoutbreak.shared.C;
import co.shoutbreak.shared.StateEvent;
import co.shoutbreak.shared.StateManager;
import co.shoutbreak.shared.User;
import co.shoutbreak.shared.utils.SBLog;
import co.shoutbreak.ui.SBContext;
import co.shoutbreak.ui.map.CustomMapView;
import co.shoutbreak.ui.map.UserLocationOverlay;

public class ComposeView extends SBView implements Observer {
	
	private final String TAG = "ComposeView.java";
	
	private CustomMapView _cMapView;
	private MapController _mapController;
	private UserLocationOverlay _userLocationOverlay;
	private EditText _cShoutText;
	// keyboard
	protected InputMethodManager _inputMM;
	
	/* Do NOT store any SBContext parameters, will cause service leak */
	// Pretty sure we have to John.  Why can't we?
	// Map initialization takes it by default
	
	public ComposeView(SBContext context, String name, int resourceId, int notificationId) {
		super(context, name, resourceId, notificationId);
		_context.getStateManager().addObserver(this);
		
		_inputMM = (InputMethodManager)context.getSystemService(context.INPUT_METHOD_SERVICE);
		
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
		if (observable instanceof StateManager) {
			// STATE MANAGER //////////////////////////////////////////////////
			StateManager stateManager = (StateManager)observable;
			StateEvent e = (StateEvent)data;
			if (e.locationTurnedOn) {
				if (_cMapView.getOverlays().size() == 0) {
					_cMapView.getOverlays().add(_userLocationOverlay);
					_cMapView.postInvalidate();
					stateManager.setIsUserOverlayVisible(true);
		
					//animateMap(_userLocationOverlay.getLocation(), false);
				}
			}
			if (e.locationTurnedOff) {
				if (_cMapView.getOverlays().size() > 0) {
					_cMapView.getOverlays().clear();
					_cMapView.postInvalidate();
				}
				stateManager.setIsUserOverlayVisible(false);
			}
		} else if (observable instanceof User) {
			// USER ///////////////////////////////////////////////////////////
				
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
		//_userLocationOverlay.runOnFirstFix(new Runnable() {
		//	public void run() {
		//		GeoPoint loc = _userLocationOverlay.getMyLocation();
		//		_mapController.animateTo(loc);
		//	}
		//});
		_userLocationOverlay.enableMyLocation();
	}	
	
	public void hideKeyboard() {
		_inputMM.hideSoftInputFromWindow(_cShoutText.getWindowToken(), 0);
		_context.getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
			WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
	}	
	
}