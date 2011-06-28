package co.shoutbreak.ui;

import java.util.Observable;
import java.util.Observer;

import co.shoutbreak.misc.SBLog;

public class ProfileView extends SBView implements Observer {
	
	private final String TAG = "InboxView.java";
	
	public ProfileView(SBContext context, String name, int resourceId, int notificationId) {
		super(context, name, resourceId, notificationId);
	}
	
	@Override
	void onHide() {
		SBLog.i(TAG, "onHide()");
		// TODO Auto-generated method stub
	}

	@Override
	void onShow() {
		SBLog.i(TAG, "onHide()");
		// TODO Auto-generated method stub
	}

	/* OBSERVER METHODS */
	
	public void update(Observable observable, Object data) {
		// TODO Auto-generated method stub
	}	
}
