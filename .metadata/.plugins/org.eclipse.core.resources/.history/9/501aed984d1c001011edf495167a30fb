package com.shoutbreak;

import android.app.Application;
import android.content.Context;

import com.shoutbreak.service.User;

public class ShoutbreakApplication extends Application {

	private User _user = null;
	
	public User getUser(Context context) {
		if (_user == null) {
			_user = new User(context);
		}
		return _user;
	}
	
	
	public User getUser() {
		return _user;
	}
	
}
