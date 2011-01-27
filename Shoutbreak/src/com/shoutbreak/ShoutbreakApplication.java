package com.shoutbreak;

import java.lang.ref.WeakReference;

import android.app.Application;
import com.shoutbreak.service.User;

public class ShoutbreakApplication extends Application {

	private User _user = null;
	private WeakReference<ShoutbreakUI> _uiRef; 
	
	public void setUIReference(ShoutbreakUI ui) {
		_uiRef = new WeakReference<ShoutbreakUI>(ui);
	}
	
	public WeakReference<ShoutbreakUI> getUIReference() {
		return _uiRef;
	}
	
	public ShoutbreakApplication getAppContext() {
		return this;
	}
	
	public User getUser() {
		if (_user == null) {
			_user = new User(this);
		}
		return _user;
	}
	
}
