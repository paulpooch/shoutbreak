package com.shoutbreak.ui;

import com.shoutbreak.UserInfo;

public interface IUIBridge {

	public void shoutSent();
	public void giveNoticeUI(String s);
	public void pushUserInfo(UserInfo userInfo);
	
}
