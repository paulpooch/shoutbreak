package com.shoutbreak.ui;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.google.android.maps.MapView;
import com.shoutbreak.ShoutbreakUI;
import com.shoutbreak.Vars;

public class CustomMapView extends MapView {

	private ShoutbreakUI _ui;
	private UserLocationOverlay _userLocationOverlay;
	private boolean _isBeingResized;
	private Point _touchBegin;
	private int _lastLength;
	private int _lastZoomLevel;
	private float _lastX, _lastY;
	
	public CustomMapView(Context context, AttributeSet attrs) {
		super(context, attrs);
		_isBeingResized = false;
		_lastZoomLevel = -1;
	}
	
	public void setUI(ShoutbreakUI ui) {
		_ui = ui;
	}
	
	public void setUserLocationOverlay(UserLocationOverlay userLocationOverlay) {
		_userLocationOverlay = userLocationOverlay;
		_userLocationOverlay.setMapView(this);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		
		int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
        	_ui.hideKeyboard();
        	_touchBegin = new Point((int)event.getX(), (int)event.getY());
			Point iconLocation = _userLocationOverlay.getResizeIconLocation();
			if (_touchBegin.x < iconLocation.x + Vars.RESIZE_ICON_TOUCH_TOLERANCE &&
			_touchBegin.x > iconLocation.x - Vars.RESIZE_ICON_TOUCH_TOLERANCE &&
			_touchBegin.y < iconLocation.y + Vars.RESIZE_ICON_TOUCH_TOLERANCE &&
			_touchBegin.y > iconLocation.y - Vars.RESIZE_ICON_TOUCH_TOLERANCE) {
				_isBeingResized = true;
				_lastLength = 0;
			}
        } else if (action == MotionEvent.ACTION_MOVE) {
        	if (_isBeingResized) {
        		
        		float x = event.getX();
        		float y = event.getY();
        		float dx = Math.abs(y - _lastX);
        		float dy = Math.abs(x - _lastY);
                if (dx >= Vars.TOUCH_TOLERANCE || dy >= Vars.TOUCH_TOLERANCE) {
                
                	float xChange = _touchBegin.x - x;
                	float yChange = _touchBegin.y - y; 
            		// in canvas down is positive y
            		int direction = 1;
            		if (yChange > xChange) {
            			direction = (dy <= 0) ? 1 : -1;
            		} else {
            			direction = (dx <= 0) ? -1 : 1;
            		}
//            		if (yChange > xChange) {
//            			direction = (yChange <= 0) ? 1 : -1;
//            		} else {
//            			direction = (xChange <= 0) ? -1 : 1;
//            		}
            		int length = (int) Math.sqrt( Math.pow(xChange, 2) + Math.pow(yChange, 2)  );
            		int diff = length - _lastLength;
            		_lastLength = length;
            		diff *= direction;
            		_userLocationOverlay.resize(diff);
            		// we're forced to bubble the event up to UserLocationOverlay which will trigger drawMyLocation()
            		// but since we don't want the map to move around we lie and say the user hasn't moved
            		event.offsetLocation(xChange, yChange);
                	
                	_lastX = x;
                    _lastY = y;
                }

        	}
        } else if (action == MotionEvent.ACTION_UP) {
        	if (_isBeingResized) {
        		_isBeingResized = false;
        	}
        }
        if (this.getZoomLevel() != _lastZoomLevel) {
        	if ( _lastZoomLevel == -1) {
        		// ignore the first zoom (done automatically - not by user)
        		_lastZoomLevel = this.getZoomLevel();
        	} else {
				_userLocationOverlay.handleZoomLevelChange();
				_lastZoomLevel = this.getZoomLevel();
        	}
		}
        return super.onTouchEvent(event);
	
	}
	
}
