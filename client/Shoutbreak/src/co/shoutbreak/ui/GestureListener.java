package co.shoutbreak.ui;

import android.view.GestureDetector;
import android.view.MotionEvent;

// http://mobile.tutsplus.com/tutorials/android/android-gesture/
// http://code.google.com/p/android-gestures-tutorials/source/browse/trunk/src/com/mamlambo/gesturefun/GestureFunActivity.java
// http://www.codeshogun.com/blog/2009/04/16/how-to-implement-swipe-action-in-android/
// http://savagelook.com/blog/android/swipes-or-flings-for-navigation-in-android
public class GestureListener implements GestureDetector.OnGestureListener {
	
	IGestureCapable view;
	String TAG = "gesture";
	
	 public GestureListener(IGestureCapable view) {
	     this.view = view;
	 }
	
	 @Override
	 public boolean onFling(MotionEvent e1, MotionEvent e2, final float velocityX, final float velocityY) {
	     return view.onFling(e1, e2, velocityX, velocityY);
	 }
	 
	 @Override
	 public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		 return view.onScroll(e1, e2, distanceX, distanceY);
	 }

	@Override
	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

}