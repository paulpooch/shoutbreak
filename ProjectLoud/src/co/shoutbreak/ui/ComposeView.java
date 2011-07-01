package co.shoutbreak.ui;

import java.util.Observable;
import java.util.Observer;

import co.shoutbreak.components.SBStateManager;
import co.shoutbreak.misc.SBLog;

public class ComposeView extends SBView implements Observer {
	
	private final String TAG = "ComposeView.java";
	
	/* Do NOT store any SBContext parameters, will cause service leak */
	
	public ComposeView(SBContext context, String name, int resourceId, int notificationId) {
		super(context, name, resourceId, notificationId);
		super.getContext().getStateManager().addObserver(this);
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
		super.getContext().getStateManager().deleteObserver(this);		
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