package co.shoutbreak.ui.views;

import java.util.Observer;

import co.shoutbreak.ui.SBContext;

import android.view.View;

public abstract class SBView implements Observer {
	
	protected SBContext _context;
	protected String _name;
	protected boolean _isVisible;
	private final int _notificationId;
	private final int _resourceId;
	
	abstract void onShow();
	abstract void onHide();
	abstract void onDestroy();
	
	public SBView(SBContext context, String name, int resourceId, int notificationId) {
		_context = context;
		_name = name;
		_resourceId = resourceId;
		_notificationId = notificationId;
		_isVisible = false;
	}
	
	public void show() {
		View view = _context.findViewById(_resourceId);
		view.setVisibility(View.VISIBLE);
		_isVisible = true;
		onShow();
	}
	
	public void hide() {
		View view = _context.findViewById(_resourceId);
		view.setVisibility(View.INVISIBLE);
		_isVisible = false;
		onHide();
	}
	
	public void destroy() {
		onDestroy();
	}
	
	public String getName() {
		return _name;
	}

	public final int getResourceId() {
		return _resourceId;
	}
	
	public final int getNotificationId() {
		return _notificationId;
	}

}
