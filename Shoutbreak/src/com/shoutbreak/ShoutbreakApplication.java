package com.shoutbreak;

import android.app.Application;
import android.content.Context;
import com.shoutbreak.service.User;

public class ShoutbreakApplication extends Application {

	private User _user = null;
	private Context _appContext;
	
	public ShoutbreakApplication() {
		_appContext = this;
	}
	
	public Context getContext() {
		return _appContext;
	}
	
	public User getUser() {
		if (_user == null) {
			_user = new User(_appContext);
		}
		return _user;
	}
	
}
