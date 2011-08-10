package co.shoutbreak.ui;

import co.shoutbreak.core.utils.SBLog;
import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class NoticeTab extends LinearLayout implements IGestureCapable {

	private Shoutbreak _ui;
	private GestureDetector _gestures;
    private OvershootInterpolator _overshootInterpolator;
    private long _startTime;
    private long _endTime;
    private float _totalAnimDy;
    private int _minHeight = 47;
    private int _maxHeight = -1;
    private int _fullsizeMaxHeight = -1;
    private int _oneLineHeight = _minHeight + 80;
    private boolean _isDirectionDown = true;
    
	public NoticeTab(Context context) {
		super(context);
		_ui = (Shoutbreak) context;
		_gestures = new GestureDetector(context, new GestureListener(this));
	}

	public NoticeTab(Context context, AttributeSet attrs) {
		super(context, attrs);
		_ui = (Shoutbreak) context;
		_gestures = new GestureDetector(context, new GestureListener(this));
	}
	
	public boolean onTouchEvent(MotionEvent event) {
		if (_fullsizeMaxHeight == -1) {
			_fullsizeMaxHeight = _ui.getMapHeight() + _minHeight - 5;
		}
		return _gestures.onTouchEvent(event);
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		int newHeight = (int) (e2.getY());
		this.adjustTabHeight(newHeight);
		// translate.postTranslate(dx, dy);
		invalidate();
		return true;
	}
	
	public void adjustTabHeight(int newHeight) {
		this.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, newHeight));
	}
	
	public void showOneLine() {
		if (this.getHeight() > _minHeight + _oneLineHeight) {
			// already open
			
		} else {
			_maxHeight = _oneLineHeight;
			long duration = 400;
		    _isDirectionDown = true;
		    _overshootInterpolator = new OvershootInterpolator();
	        _startTime = System.currentTimeMillis();
	        _endTime = _startTime + duration;
	        _totalAnimDy = 30;
	        post(new Runnable() {
	            @Override
	            public void run() {
	                onAnimateStep();
	            }
	        });
		}
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		_maxHeight = _fullsizeMaxHeight;
		final float distanceTimeFactor = 0.4f;
	    final float totalDy = (distanceTimeFactor * velocityY / 2);
	    long duration = (long) (1000 * distanceTimeFactor);
	    _isDirectionDown = (e2.getY() > e1.getY()) ? true : false;
        _overshootInterpolator = new OvershootInterpolator();
        _startTime = System.currentTimeMillis();
        _endTime = _startTime + duration;
        _totalAnimDy = totalDy;
        SBLog.e("D", "total anim dy = " + totalDy);
        post(new Runnable() {
            @Override
            public void run() {
                onAnimateStep();
            }
        });
        return true;
    }

     private void onAnimateStep() {
        long curTime = System.currentTimeMillis();
        float percentTime = (float) (curTime - _startTime) / (float) (_endTime - _startTime);
        float percentDistance = _overshootInterpolator.getInterpolation(percentTime);
        float curDy = percentDistance * _totalAnimDy;
 		int newHeight = (int)(this.getHeight() + curDy);
 		if (_isDirectionDown && newHeight > _maxHeight) {
 			this.adjustTabHeight(_maxHeight);
 			return;
 		} else if (!_isDirectionDown && newHeight < _minHeight) {
 			this.adjustTabHeight(_minHeight);
 			return;
 		} else {
	 		this.adjustTabHeight(newHeight);
	        //Log.v(DEBUG_TAG, "We're " + percentDistance + " of the way there!");
	 		if (percentTime < 1.0f) {
	 			post(new Runnable() {
	 				@Override
	                public void run() {
	 					onAnimateStep();
	                }
	 			});
	 		}
 		}
     }

}
