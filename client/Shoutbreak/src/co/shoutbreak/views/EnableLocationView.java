package co.shoutbreak.views;

import java.util.Observable;
import java.util.Observer;

import co.shoutbreak.shared.StateEvent;
import co.shoutbreak.shared.StateManager;

import co.shoutbreak.shared.utils.SBLog;
import co.shoutbreak.ui.SBContext;


public class EnableLocationView extends SBView implements Observer {
	
	private final String TAG = "EnableLocationView";
	
	public EnableLocationView(SBContext context, String name, int resourceId, int notificationId) {
		super(context, name, resourceId, notificationId);
		_context.getStateManager().addObserver(this);
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
	
	/* OBSERVER METHODS */
	public void update(Observable observable, Object data) {
		StateManager stateManager = (StateManager)observable;
		StateEvent e = (StateEvent)data;
	}
}