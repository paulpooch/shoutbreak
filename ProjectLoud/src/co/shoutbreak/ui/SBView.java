package co.shoutbreak.ui;

import android.view.View;

public abstract class SBView {
	
	public static final String NOTIFICATION_REFERRAL_ID = "NOTIFICATION_REFERRAL_ID";
	
	private SBContext _Context;
	private String _name;
	private final int _notificationId;
	private final int _resourceId;
	private boolean _isVisible;
	
	abstract void onShow();
	abstract void onHide();
	
	public SBView(SBContext context, String name, int resourceId, int notificationId) {
		_Context = context;
		_name = name;
		_resourceId = resourceId;
		_notificationId = notificationId;
		_isVisible = false;
	}
	
	public void show() {
		View view = _Context.findViewById(_resourceId);
		view.setVisibility(View.VISIBLE);
		_isVisible = true;
		onShow();
	}
	
	public void hide() {
		View view = _Context.findViewById(_resourceId);
		view.setVisibility(View.INVISIBLE);
		_isVisible = false;
		onHide();
	}
	
	public SBContext getContext() {
		return _Context;
	}
	
	public String getName() {
		return _name;
	}
	
	public boolean isVisible() {
		return _isVisible;
	}

	public final int getResourceId() {
		return _resourceId;
	}
	
	public final int getNotificationId() {
		return _notificationId;
	}
}
