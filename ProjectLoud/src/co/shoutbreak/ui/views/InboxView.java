package co.shoutbreak.ui.views;

import java.util.Observable;
import java.util.Observer;

import co.shoutbreak.shared.StateManager;
import co.shoutbreak.shared.User;
import co.shoutbreak.shared.utils.SBLog;
import co.shoutbreak.ui.SBContext;

public class InboxView extends SBView implements Observer {
	
	private final String TAG = "InboxView.java";
	
	/* Do NOT store any SBContext parameters, will cause service leak */
	
	public InboxView(SBContext context, String name, int resourceId, int notificationId) {
		super(context, name, resourceId, notificationId);
		_context.getStateManager().addObserver(this);
	}

	@Override
	void onShow() {
		SBLog.i(TAG, "onShow()");
		// TODO Auto-generated method stub
	}
	
	@Override
	void onHide() {
		SBLog.i(TAG, "onHide()");
		// TODO Auto-generated method stub
	}
	
	@Override
	void onDestroy() {
		SBLog.i(TAG, "onDestroy()");
		_context.getStateManager().deleteObserver(this);		
	}
	
	/* OBSERVER METHODS */
	public void update(Observable observable, Object data) {
	}

}