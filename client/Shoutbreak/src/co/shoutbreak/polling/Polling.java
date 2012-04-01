package co.shoutbreak.polling;

import java.util.ArrayList;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.c2dm.C2DMessaging;

import co.shoutbreak.core.C;
import co.shoutbreak.core.Mediator.ThreadSafeMediator;
import co.shoutbreak.core.utils.ErrorManager;
import co.shoutbreak.core.utils.SBLog;
import co.shoutbreak.polling.http.HttpConnection;
import co.shoutbreak.polling.http.PostData;

import android.os.Handler;
import android.os.Message;

public class Polling {
	
	private static final String TAG = "Polling";
	
	private ThreadSafeMediator _safeM;
  private Handler _uiThreadHandler;
  private UUID _keyForLife;
  private int _threadPurpose; 
  
  public Polling(ThreadSafeMediator threadSafeMediator, Handler uiThreadHandler, int threadPurpose, UUID keyForLife) {
  	SBLog.constructor(TAG);
    _safeM = threadSafeMediator;
    _uiThreadHandler = uiThreadHandler;
    _threadPurpose = threadPurpose;
    _keyForLife = keyForLife;
   }
	
	public void go(Message message) {
		SBLog.logic("Polling - go");
		switch (message.what) {
			case C.STATE_IDLE: {
				SBLog.logic("Polling - idle");
				idle(message);
				break;
			}
			case C.STATE_VOTE: {
				SBLog.logic("Polling - vote");
				_safeM.resetPollingDelay();				
				vote(message);
				break;
			}
			case C.STATE_SHOUT: {
				SBLog.logic("Polling - shout");
				_safeM.resetPollingDelay();				
				shout(message);
				break;
			}
		}
	}
    
	public void idle(Message message) {
		SBLog.logic("Polling - idle");
		if (!_safeM.userHasAccount()) {
			createAccount(message);
			return;
		}
		ping(message);
	}
	
	public void ping(Message message) {
		SBLog.method(TAG, "ping()");
		Handler httpHandler = new Handler() {
			public void handleMessage(Message message) {
				switch (message.what) {
					case C.HTTP_DID_SUCCEED: {
						if (_safeM.isResponseClean(message)) {						
							_safeM.handlePingSuccess();
							CrossThreadPacket xPacket = (CrossThreadPacket)message.obj;
							message.obj = xPacket;
							try {
								String code = xPacket.json.getString(C.JSON_CODE);
								if (code.equals(C.JSON_CODE_PING_OK)) {
									receivePing(message);
								} else if (code.equals(C.JSON_CODE_EXPIRED_AUTH)) {
									expiredAuth(message);
								} else if (code.equals(C.JSON_CODE_INVALID_UID)) {
									
								} else {
									// some invalid response from server, do anything?
								}
							} catch (JSONException ex) {
								// TODO: Manage exception
								SBLog.error(TAG, ex.getMessage());
								_safeM.handlePingFailed(message);
							}
						} else {
							_safeM.handlePingFailed(message);
						}
						break;
					}
					case C.HTTP_DID_EXCEPTION: {
						_safeM.handlePingFailed(message);
						break;
					}
					case C.HTTP_DID_STATUS_CODE_ERROR: {
						_safeM.handlePingFailed(message);
						_safeM.handleServerHttpError();
						break;
					}
				}
			}
		};
		PostData postData = new PostData();
		postData.add(C.JSON_ACTION, C.JSON_ACTION_USER_PING);
		postData.add(C.JSON_UID, _safeM.getUserId());
		postData.add(C.JSON_AUTH, _safeM.getAuth());
		postData.add(C.JSON_LAT, Double.toString(_safeM.getLatitude()));
		postData.add(C.JSON_LONG, Double.toString(_safeM.getLongitude()));

		// acknowledge any level up packets
		if (_safeM.getUserLevelUpOccurred()) {
			postData.add(C.JSON_LEVEL, Integer.toString(_safeM.getUserLevel()));
			_safeM.setUserLevelUpOccured(false);
		}
		
		String c2dmIdClient = C2DMessaging.getRegistrationIdClient(_safeM.getContext());
		String c2dmIdServer = C2DMessaging.getRegistrationIdServer(_safeM.getContext());
		// If we have an id and it doesn't match what the server has.
		if (!c2dmIdClient.equals(C.NULL_C2DM_ID) && !c2dmIdClient.equals(c2dmIdServer)) {
			// Include new C2dm registration id here.
			postData.add(C.JSON_C2DM_ID, c2dmIdClient);
		}
		
		// Only do this stuff if UI is open.
		SBLog.logic("In Polling - checking if we need radius.");
		if (_safeM.isUiInForeground()) {
			// do we need to pull a density?
			SBLog.logic("In Polling - UI is in foreground.");
			if (! _safeM.getRadiusAtCell(true).isSet) {	
				SBLog.logic("In Polling - radius is NOT set.");
				postData.add(C.JSON_RADIUS, "1");
				//Toast.makeText(_context, "Requesting Density: " + tempCellDensity.cellX + " , " + tempCellDensity.cellY, Toast.LENGTH_SHORT).show();
			} else {
				SBLog.logic("In Polling - radius is set.");
			}
			
			ArrayList<String> scoresToRequest = _safeM.getScoreRequestIds();
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
		}
			
		new HttpConnection(httpHandler).post(postData);	
	}
	
	public void receivePing(Message message) {
		SBLog.logic("Polling - receiveShouts");
		CrossThreadPacket xPacket = (CrossThreadPacket)message.obj;
		boolean delay = true;
		try {
			if (xPacket.json.has(C.JSON_SHOUTS)) {
				_safeM.resetPollingDelay();
				JSONArray shouts = xPacket.json.getJSONArray(C.JSON_SHOUTS);
				_safeM.handleShoutsReceived(shouts);
			}
			if (xPacket.json.has(C.JSON_SCORES)) {
				JSONArray scores = xPacket.json.getJSONArray(C.JSON_SCORES);
				_safeM.handleScoresReceived(scores);
			}
			if (xPacket.json.has(C.JSON_POINTS)) {
				int newPoints = (int) xPacket.json.optLong(C.JSON_POINTS);
				_safeM.handlePointsSync(newPoints);
			}
			if (xPacket.json.has(C.JSON_LEVEL_CHANGE)) {
				JSONObject levelInfo = xPacket.json.getJSONObject(C.JSON_LEVEL_CHANGE);
				_safeM.handleLevelUp(levelInfo);
				delay = false;
			}
			// Radius stuff must come after level change... so new radius is set based on new level.
			if (xPacket.json.has(C.JSON_RADIUS)) {
				long radius = (long) xPacket.json.optLong(C.JSON_RADIUS);
				_safeM.handleRadiusChange(radius);
			}
			if (xPacket.json.has(C.JSON_C2DM_ID)) {
				String c2dmIdAtServer = xPacket.json.optString(C.JSON_C2DM_ID);
				C2DMessaging.setRegistrationIdServer(_safeM.getContext(), c2dmIdAtServer);
			}
			// If normal ping, introduce delay
			if (_threadPurpose == C.PURPOSE_LOOP_FROM_UI) {
				if (delay) {
					_threadPurpose = C.PURPOSE_LOOP_FROM_UI_DELAYED;
				} else {
					_threadPurpose = C.PURPOSE_LOOP_FROM_UI;
				}
			}
			xPacket.purpose = _threadPurpose;
			xPacket.keyForLife = _keyForLife;
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
						if (_safeM.isResponseClean(message)) {
							CrossThreadPacket xPacket = (CrossThreadPacket)message.obj;
							String code = xPacket.json.optString(C.JSON_CODE);
							if (code.equals(C.JSON_CODE_EXPIRED_AUTH)) {
								_safeM.handleShoutFailed(message);
							} else {
								_safeM.handleShoutSent();
							}
						} else {
							_safeM.handleShoutFailed(message);
						}
						break;
					}
					case C.HTTP_DID_EXCEPTION: {
						_safeM.handleShoutFailed(message);
						break;
					}
					case C.HTTP_DID_STATUS_CODE_ERROR: {
						_safeM.handleShoutFailed(message);
						_safeM.handleServerHttpError();
						break;
					}
				}
			}
		};
		CrossThreadPacket xPacket = (CrossThreadPacket)message.obj;
		String shoutText = xPacket.sArgs[0];
		int shoutPower = xPacket.iArgs[0];
		long radiusHint = xPacket.lArgs[0];
		PostData postData = new PostData();
		postData.add(C.JSON_ACTION, C.JSON_ACTION_SHOUT);
		postData.add(C.JSON_UID, _safeM.getUserId());
		postData.add(C.JSON_AUTH, _safeM.getAuth());
		postData.add(C.JSON_LAT, Double.toString(_safeM.getLatitude()));
		postData.add(C.JSON_LONG, Double.toString(_safeM.getLongitude()));
		postData.add(C.JSON_SHOUT_TEXT, shoutText);
		postData.add(C.JSON_SHOUT_POWER, Integer.toString(shoutPower));
		if (xPacket.sArgs.length > 1 && !xPacket.sArgs[1].equals(C.NULL_REPLY)) {
			postData.add(C.JSON_SHOUT_RE, xPacket.sArgs[1]);
		}
		if (radiusHint > 0) {
			postData.add(C.JSON_RADIUS_HINT, Long.toString(radiusHint));
		}
		new HttpConnection(httpHandler).post(postData);	
	}
	
	public void vote(Message message) {
		CrossThreadPacket xPacket = (CrossThreadPacket)message.obj;
		final String shoutId = xPacket.sArgs[0];
		final int vote = xPacket.iArgs[0];
		Handler httpHandler = new Handler() {
			public void handleMessage(Message message) {
				switch (message.what) {
					case C.HTTP_DID_SUCCEED: {
						if (_safeM.isResponseClean(message)) {
							CrossThreadPacket xPacket = (CrossThreadPacket)message.obj;
							try {
								String code = xPacket.json.getString(C.JSON_CODE);
								if (code.equals(C.JSON_CODE_VOTE_OK)) {
									_safeM.handleVoteFinish(shoutId, vote);
								} else if (code.equals(C.JSON_CODE_VOTE_FAIL)) {
									// server knows it failed
									_safeM.handleVoteFailed(message, shoutId, vote);
								} else {
									// something horrible happened
									_safeM.handleVoteFailed(message, shoutId, vote);
								}
							} catch (JSONException ex) {
								SBLog.error(TAG, ex.getMessage());
								_safeM.handleVoteFailed(message, shoutId, vote);
							}
						} else {
							_safeM.handleVoteFailed(message, shoutId, vote);
						}
						break;
					}
					case C.HTTP_DID_EXCEPTION: {
						_safeM.handleVoteFailed(message, shoutId, vote);
						break;
					}
					case C.HTTP_DID_STATUS_CODE_ERROR: {
						_safeM.handleVoteFailed(message, shoutId, vote);
						_safeM.handleServerHttpError();
						break;
					}
				}				
			}
		};
		PostData postData = new PostData();
		postData.add(C.JSON_ACTION, C.JSON_ACTION_VOTE);
		postData.add(C.JSON_UID, _safeM.getUserId());
		postData.add(C.JSON_AUTH, _safeM.getAuth());
		postData.add(C.JSON_SHOUT_ID, shoutId);
		postData.add(C.JSON_VOTE, Integer.toString(vote));
		new HttpConnection(httpHandler).post(postData);	
	}
	
	public void createAccount(Message message) {
		_safeM.handleCreateAccountStarted();
		Handler httpHandler = new Handler() {
			public void handleMessage(Message message) {
				switch (message.what) {
					case C.HTTP_DID_SUCCEED: {
						if (_safeM.isResponseClean(message)) {
							createAccountStep2(message);
						} else {
							_safeM.handleCreateAccountFailed(message);
						}
						break;
					}
					case C.HTTP_DID_EXCEPTION: {
						_safeM.handleCreateAccountFailed(message);
						break;
					}
					case C.HTTP_DID_STATUS_CODE_ERROR: {
						_safeM.handleCreateAccountFailed(message);
						_safeM.handleServerHttpError();
						break;
					}
				}
			}
		};
		PostData postData = new PostData();
		postData.add(C.JSON_ACTION, C.JSON_ACTION_CREATE_ACCOUNT);
		new HttpConnection(httpHandler).post(postData);	
	}
	
	public void createAccountStep2(Message message) {
		SBLog.logic("Polling - createAccountStep2");
		try {
			
			CrossThreadPacket xPacket2 = (CrossThreadPacket)message.obj;
			final String tempUid = xPacket2.json.getString(C.JSON_UID);
		
			Handler httpHandler = new Handler() {
				public void handleMessage(Message message) {
					switch (message.what) {
						case C.HTTP_DID_SUCCEED: {
							if (_safeM.isResponseClean(message)) {
								try {
									CrossThreadPacket xPacket = (CrossThreadPacket)message.obj;
									String password = xPacket.json.getString(C.JSON_PW);
									_safeM.handleAccountCreated(tempUid, password);
									xPacket.purpose = _threadPurpose;
									xPacket.keyForLife = _keyForLife;
									_uiThreadHandler.sendMessage(Message.obtain(_uiThreadHandler, C.STATE_IDLE, xPacket));
								} catch (JSONException ex) {
									ErrorManager.manage(ex);
									_safeM.handleCreateAccountFailed(message);
								}
							} else {
								_safeM.handleCreateAccountFailed(message);
							}
							break;
						}
						case C.HTTP_DID_EXCEPTION: {
							_safeM.handleCreateAccountFailed(message);
							break;
						}
						case C.HTTP_DID_STATUS_CODE_ERROR: {
							_safeM.handleCreateAccountFailed(message);
							_safeM.handleServerHttpError();
							break;
						}
					}
				}
			};
		
			PostData postData = new PostData();
			postData.add(C.JSON_ACTION, C.JSON_ACTION_CREATE_ACCOUNT);
			postData.add(C.JSON_UID, tempUid);
			postData.add(C.JSON_ANDROID_ID, _safeM.getAndroidId());
			postData.add(C.JSON_DEVICE_ID, _safeM.getDeviceId());
			postData.add(C.JSON_PHONE_NUM, _safeM.getPhoneNumber());
			postData.add(C.JSON_CARRIER_NAME, _safeM.getNetworkOperator());
			new HttpConnection(httpHandler).post(postData);
		} catch (JSONException ex) {
			ErrorManager.manage(ex);
		}
	}
	
	public void expiredAuth(Message message) {
		SBLog.logic("Polling - expiredAuth");
		try {
			CrossThreadPacket xPacket = (CrossThreadPacket)message.obj;
			String nonce = xPacket.json.getString(C.JSON_NONCE);
			_safeM.updateAuth(nonce);
			ping(message);
		} catch (JSONException ex) {
			ErrorManager.manage(ex);
		}
	}
	
}