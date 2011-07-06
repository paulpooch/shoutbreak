package co.shoutbreak.ui.views;

import java.util.Observable;
import java.util.Observer;

import co.shoutbreak.components.SBStateManager;
import co.shoutbreak.misc.SBLog;
import co.shoutbreak.ui.SBContext;

public class ProfileView extends SBView implements Observer {
	
	private final String TAG = "InboxView.java";
	
	/* Do NOT store any SBContext parameters, will cause service leak */
	
	public ProfileView(SBContext context, String name, int resourceId, int notificationId) {
		super(context, name, resourceId, notificationId);
		_Context.getStateManager().addObserver(this);
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
	
	@Override
	void onDestroy() {
		_Context.getStateManager().deleteObserver(this);		
	}

	/* OBSERVER METHODS */

	public void update(Observable observable, Object data) {
		SBStateManager stateManager = (SBStateManager) observable;
		switch (stateManager.getState()) {
			case 1: 
			break;
		}
	}
}
