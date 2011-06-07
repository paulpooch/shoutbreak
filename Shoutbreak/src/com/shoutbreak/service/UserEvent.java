package com.shoutbreak.service;

public class UserEvent {
	
	public static final int DENSITY_CHANGE = 0;
	public static final int LOCATION_SERVICES_CHANGE = 1;
	public static final int INBOX_CHANGE = 2;
	public static final int LEVEL_CHANGE = 3;
	public static final int POINTS_CHANGE = 4;
	public static final int SHOUT_SENT = 5;
	public static final int SHOUTS_RECEIVED = 6;
	public static final int VOTE_COMPLETE = 7;
	public static final int ACCOUNT_CREATED = 9;
	public static final int SCORES_CHANGE = 10;
	
	private User _source;
	public int type;	
	
	public UserEvent(User source, int type) {
		_source = source;
		this.type = type;
	}

	public User getUser() {
		return _source;
	}
	
}