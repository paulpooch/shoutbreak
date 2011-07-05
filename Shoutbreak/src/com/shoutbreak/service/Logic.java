package com.shoutbreak.service;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.shoutbreak.C;
import com.shoutbreak.CrossThreadPacket;
import com.shoutbreak.ErrorManager;
import com.shoutbreak.service.http.HttpConnection;
import com.shoutbreak.service.http.PostData;

import android.os.Handler;
import android.os.Message;

public class Logic {
	
    private Handler _uiThreadHandler;
    private User _user;
    
    public Logic(Handler uiThreadHandler, User user) {
    	_uiThreadHandler = uiThreadHandler;
    	_user = user;
    }
    
	public void go(Message message) {
		switch (message.what) {
			case C.STATE_IDLE: {
				idle(message);
				break;
			}
			case C.STATE_RECEIVE_SHOUTS: {
				receiveShouts(message);
				break;
			}
			case C.STATE_VOTE: {
				vote(message);
				break;
			}
			case C.STATE_SHOUT: {
				shout(message);
				break;
			}
			case C.STATE_EXPIRED_AUTH: {
				expiredAuth(message);
				break;
			}
			case C.STATE_CREATE_ACCOUNT_2: {
				createAccountStep2(message);
				break;
			}
		}
	}
    
	public void idle(Message message) {

		if (!_user.hasAccount()) {
			createAccount(message);
			return;
		}
		ping(message);
		
	}
	
	public void ping(Message message) {
		Handler httpHandler = new Handler() {
			public void handleMessage(Message message) {
				switch (message.what) {
					case C.HTTP_DID_SUCCEED: {
						CrossThreadPacket xPacket = (CrossThreadPacket)message.obj;
						try {
							String code = xPacket.json.getString(C.JSON_CODE);
												
							if (code.equals(C.JSON_CODE_PING_OK)) {
								// if normal ping, introduce delay
								if (xPacket.purpose == C.PURPOSE_LOOP_FROM_UI) {
									xPacket.purpose = C.PURPOSE_LOOP_FROM_UI_DELAYED;
								}
								_uiThreadHandler.sendMessage(Message.obtain(_uiThreadHandler, C.STATE_IDLE, xPacket));
							} else if (code.equals(C.JSON_CODE_SHOUTS)) {
								_uiThreadHandler.sendMessage(Message.obtain(_uiThreadHandler, C.STATE_RECEIVE_SHOUTS, xPacket));
							} else if (code.equals(C.JSON_CODE_EXPIRED_AUTH)) {
								_uiThreadHandler.sendMessage(Message.obtain(_uiThreadHandler, C.STATE_EXPIRED_AUTH, xPacket));
							} else if (code.equals(C.JSON_CODE_INVALID_UID)) {
								
							} else {
								// some invalid response from server, do anything?
							}
						} catch (JSONException ex) {
							ErrorManager.manage(ex);
						}
						break;
					}
				}
			}
		};
		CrossThreadPacket xPacket = (CrossThreadPacket)message.obj;
		ArrayList<String> scoresToRequest = _user.getInbox().getOpenShoutIDs();
		PostData postData = new PostData();
		postData.add(C.JSON_ACTION, C.JSON_ACTION_USER_PING);
		postData.add(C.JSON_UID, _user.getUID());
		postData.add(C.JSON_AUTH, _user.getAuth());
		postData.add(C.JSON_LAT, Double.toString(_user.getLatitude()));
		postData.add(C.JSON_LONG, Double.toString(_user.getLongitude()));
		
		// do we need to pull a density?
		if (! _user.getCellDensity().isSet) {	
			postData.add(C.JSON_DENSITY, "1");
			//Toast.makeText(_context, "Requesting Density: " + tempCellDensity.cellX + " , " + tempCellDensity.cellY, Toast.LENGTH_SHORT).show();
		}
		
		// acknowledge any level up packets
		if (_user.getLevelUpOccured()) {
			postData.add(C.JSON_LEVEL, Integer.toString(_user.getLevel()));
		}
		
		if (scoresToRequest.size() > 0) {
			StringBuilder scoreReq = new StringBuilder("[");
			int i = 0;
			for (String reqID : scoresToRequest) {
				scoreReq.append("\"" + reqID + "\"");
				if (++i != scoresToRequest.size()) {
					scoreReq.append(", ");
				}
			}
			scoreReq.append("]");
			postData.add(C.JSON_SCORES, scoreReq.toString());
		}
			
		new HttpConnection(httpHandler).post(postData, xPacket);	
	}
	
	public void receiveShouts(Message message) {
		CrossThreadPacket xPacket = (CrossThreadPacket)message.obj;
		try {
			if (xPacket.json.has(C.JSON_DENSITY)) {
				double density = (double) xPacket.json.optDouble(C.JSON_DENSITY);
				_user.saveDensity(density);
				_user.fireUserEvent(UserEvent.DENSITY_CHANGE);
			}
			if (xPacket.json.has(C.JSON_SHOUTS)) {
				JSONArray shouts = xPacket.json.getJSONArray(C.JSON_SHOUTS);
				_user.setShoutsJustReceived(shouts.length());
				for (int i = 0; i < shouts.length(); i++) {
					JSONObject jsonShout = shouts.getJSONObject(i);
					_user.getInbox().addShout(jsonShout);
				}
				xPacket.uiCode = C.UI_RECEIVE_SHOUTS;
				_user.fireUserEvent(UserEvent.SHOUTS_RECEIVED);
			}
			if (xPacket.json.has(C.JSON_SCORES)) {
				_user.setScoresJustReceived(true);
				JSONArray scores = xPacket.json.getJSONArray(C.JSON_SCORES);
				for (int i = 0; i < scores.length(); i++) {
					JSONObject jsonScore = scores.getJSONObject(i);
					_user.getInbox().updateScore(jsonScore);
				}
				_user.fireUserEvent(UserEvent.SCORES_CHANGE);
			}
			if (xPacket.json.has(C.JSON_LEVEL_CHANGE)) {
				JSONObject levelInfo = xPacket.json.getJSONObject(C.JSON_LEVEL_CHANGE);				
				int newLevel = (int) levelInfo.getLong(C.JSON_LEVEL);
				int newPoints = (int) levelInfo.getLong(C.JSON_POINTS);
				int nextLevelAt = (int) levelInfo.getLong(C.JSON_NEXT_LEVEL_AT);
				_user.levelUp(newLevel, newPoints, nextLevelAt);
				_user.fireUserEvent(UserEvent.LEVEL_CHANGE);
				_user.fireUserEvent(UserEvent.POINTS_CHANGE);
			}
			if (xPacket.purpose == C.PURPOSE_LOOP_FROM_UI) {
				xPacket.purpose = C.PURPOSE_LOOP_FROM_UI_DELAYED;
			}			
			_uiThreadHandler.sendMessage(Message.obtain(_uiThreadHandler, C.STATE_IDLE, xPacket));	
			
		} catch (JSONException ex) {
			ErrorManager.manage(ex);
		}				
	}
	
	public void shout(Message message) {
		Handler httpHandler = new Handler() {
			public void handleMessage(Message message) {
				switch (message.what) {
					case C.HTTP_DID_SUCCEED: {
						_user.fireUserEvent(UserEvent.SHOUT_SENT);				
						// Unless shout occurs in idle thread, we don't need to sendMessage.
						//CrossThreadPacket xPacket = (CrossThreadPacket)message.obj;
						//_uiThreadHandler.sendMessage(Message.obtain(_uiThreadHandler, C.STATE_IDLE, xPacket)); // STATE doesn't matter - going to die
						break;
					}
				}
			}
		};
		CrossThreadPacket xPacket = (CrossThreadPacket)message.obj;
		String shoutText = xPacket.sArgs[0];
		int shoutPower = xPacket.iArgs[0];
		PostData postData = new PostData();
		postData.add(C.JSON_ACTION, C.JSON_ACTION_SHOUT);
		postData.add(C.JSON_UID, _user.getUID());
		postData.add(C.JSON_AUTH, _user.getAuth());
		postData.add(C.JSON_LAT, Double.toString(_user.getLatitude()));
		postData.add(C.JSON_LONG, Double.toString(_user.getLongitude()));
		postData.add(C.JSON_SHOUT_TEXT, shoutText);
		postData.add(C.JSON_SHOUT_POWER, Integer.toString(shoutPower));
		new HttpConnection(httpHandler).post(postData, xPacket);	
	}
	
	public void vote(Message message) {
		CrossThreadPacket xPacket = (CrossThreadPacket)message.obj;
		final String shoutID = xPacket.sArgs[0];
		final int vote = xPacket.iArgs[0];
		Handler httpHandler = new Handler() {
			public void handleMessage(Message message) {
				switch (message.what) {
					case C.HTTP_DID_SUCCEED: {
						_user.getInbox().reflectVote(shoutID, vote);
						_user.fireUserEvent(UserEvent.VOTE_COMPLETE);
						// Unless vote occurs in idle thread, we don't need to sendMessage.
						//CrossThreadPacket xPacket = (CrossThreadPacket)message.obj;
						//_uiThreadHandler.sendMessage(Message.obtain(_uiThreadHandler, C.STATE_IDLE, xPacket)); // STATE doesn't matter - going to die
						break;
					}
				}
			}
		};
		PostData postData = new PostData();
		postData.add(C.JSON_ACTION, C.JSON_ACTION_VOTE);
		postData.add(C.JSON_UID, _user.getUID());
		postData.add(C.JSON_AUTH, _user.getAuth());
		postData.add(C.JSON_SHOUT_ID, shoutID);
		postData.add(C.JSON_VOTE, Integer.toString(vote));
		new HttpConnection(httpHandler).post(postData, xPacket);	
	}
	
	public void createAccount(Message message) {
		Handler httpHandler = new Handler() {
			public void handleMessage(Message message) {
				switch (message.what) {
					case C.HTTP_DID_SUCCEED: {
						_uiThreadHandler.sendMessage(Message.obtain(_uiThreadHandler, C.STATE_CREATE_ACCOUNT_2, message.obj));
						break;
					}
				}
			}
		};
		CrossThreadPacket xPacket = (CrossThreadPacket)message.obj;
		PostData postData = new PostData();
		postData.add(C.JSON_ACTION, C.JSON_ACTION_CREATE_ACCOUNT);
		new HttpConnection(httpHandler).post(postData, xPacket);	
	}
	
	public void createAccountStep2(Message message) {
		Handler httpHandler = new Handler() {
			public void handleMessage(Message message) {
				switch (message.what) {
					case C.HTTP_DID_SUCCEED: {			
						try {
							CrossThreadPacket xPacket = (CrossThreadPacket)message.obj;
							String password = xPacket.json.getString(C.JSON_PW);
							String uid = xPacket.sArgs[0];
							_user.setUID(uid);
							_user.setPassword(password);
							_user.fireUserEvent(UserEvent.ACCOUNT_CREATED);
							_uiThreadHandler.sendMessage(Message.obtain(_uiThreadHandler, C.STATE_IDLE, xPacket));
						} catch (JSONException ex) {
							ErrorManager.manage(ex);
						}
						break;
					}
				}
			}
		};
		try {
			CrossThreadPacket xPacket = (CrossThreadPacket)message.obj;
			String tempUID = xPacket.json.getString(C.JSON_UID);
			PostData postData = new PostData();
			postData.add(C.JSON_ACTION, C.JSON_ACTION_CREATE_ACCOUNT);
			postData.add(C.JSON_UID, tempUID);
			postData.add(C.JSON_ANDROID_ID, _user.getAndroidId());
			postData.add(C.JSON_DEVICE_ID, _user.getDeviceId());
			postData.add(C.JSON_PHONE_NUM, _user.getPhoneNumber());
			postData.add(C.JSON_CARRIER_NAME, _user.getNetworkOperator());
			xPacket.sArgs = new String[] { tempUID };
			new HttpConnection(httpHandler).post(postData, xPacket);
		} catch (JSONException ex) {
			ErrorManager.manage(ex);
		}
	}
	
	public void expiredAuth(Message message) {
		try {
			CrossThreadPacket xPacket = (CrossThreadPacket)message.obj;
			String nonce = xPacket.json.getString(C.JSON_NONCE);
			_user.updateAuth(nonce);
			ping(message);
		} catch (JSONException ex) {
			ErrorManager.manage(ex);
		}
	}
		
}