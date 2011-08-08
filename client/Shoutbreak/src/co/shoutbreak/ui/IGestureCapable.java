package co.shoutbreak.ui;

import android.view.MotionEvent;

public interface IGestureCapable {

	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY);
	public boolean onFling(MotionEvent e1, MotionEvent e2, final float velocityX, final float velocityY);
	 
}
