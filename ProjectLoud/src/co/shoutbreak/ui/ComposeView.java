package co.shoutbreak.ui;

import java.util.Observable;
import java.util.Observer;

import co.shoutbreak.components.SBStateManager;
import co.shoutbreak.misc.SBLog;

public class ComposeView extends SBView implements Observer {
	
	private final String TAG = "ComposeView.java";
	
	public ComposeView(SBContext context, String name, int resourceId, int notificationId) {
		super(context, name, resourceId, notificationId);
	}
	
	/* LIFECYCLE METHODS */
	
	@Override
	void onHide() {
		SBLog.i(TAG, "onHide()");
		// TODO Auto-generated method stub
	}

	@Override
	void onShow() {
		SBLog.i(TAG, "onShow()");
		// TODO Auto-generated method stub	
	}

	/* OBSERVER METHODS */

	public void update(Observable observable, Object data) {
		SBStateManager smgr = (SBStateManager) observable;
	}
}