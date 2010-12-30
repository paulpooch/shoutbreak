package com.shoutbreak;

import com.shoutbreak.service.ErrorManager;
import com.shoutbreak.service.MessageObject;
import com.shoutbreak.service.ServiceThread;
import com.shoutbreak.service.StateEngine;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

// TODO: this should use AlarmService to sleep then wake itself up
// C:\Program Files (x86)\android-sdk-windows\samples\android-8\ApiDemos\src\com\example\android\apis\app

// note that we're still in the UI thread here
// ServiceThread is the new thread that lives beyond the UI
public class ShoutbreakService extends Service {

	final RemoteCallbackList<IShoutbreakServiceCallback> _uiCallbacks = new RemoteCallbackList<IShoutbreakServiceCallback>();
	
	private Handler _uiThreadHandler;
	private StateEngine _stateEngine;
	private boolean _uiHasConnectedOnce;
	
	///////////////////////////////////////////////////////////////////////////
	// LIFECYCLE METHODS
	///////////////////////////////////////////////////////////////////////////
	
	@Override
	public void onCreate() {
		super.onCreate();
		_uiHasConnectedOnce = false;
		
		_uiThreadHandler = new Handler() {
			public void handleMessage(Message msg) {
				MessageObject obj = (MessageObject)msg.obj;	
				switch (msg.what) {
					
					case Vars.CALLBACK_SERVICE_EVENT_COMPLETE: {
						final int N = _uiCallbacks.beginBroadcast();
						for (int i = 0; i < N; i++) {
							try {
								_uiCallbacks.getBroadcastItem(i).serviceEventComplete(obj.serviceEventCode);
							} catch (RemoteException ex) {
								// The RemoteCallbackList will take care of removing the dead object for us.
								ErrorManager.manage(ex);
							}
						}
						_uiCallbacks.finishBroadcast();
						
						// back to idle
						Message message = new Message();
						message.what = Vars.MESSAGE_STATE_IDLE;
						runOnServiceThreadDelayed(message, Vars.IDLE_THREAD_LOOP_INTERVAL);
						
						break;						
					}
					
					case Vars.MESSAGE_REPOST_IDLE_DELAYED: {
						runOnServiceThreadDelayed(msg, Vars.IDLE_THREAD_LOOP_INTERVAL);
						break;
					}
					
					default: {
						runOnServiceThread(msg);
						break;
					}
					
				}
			}
		};
		
		// User should have been instantiated by UI already.
		ShoutbreakApplication app = (ShoutbreakApplication)this.getApplication();
		_stateEngine = new StateEngine(_uiThreadHandler, app.getUser());
		Log.d(getClass().getSimpleName(), "onCreate()");
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		Message message = new Message();
		if (_uiHasConnectedOnce) {
			emptyCallbacks();		
			message.what = Vars.MESSAGE_STATE_UI_RECONNECT;
			runOnServiceThread(message);
		} else {
			_uiHasConnectedOnce = true;
			message.what = Vars.MESSAGE_STATE_INIT;
			runOnServiceThread(message);
		}
		Log.d(getClass().getSimpleName(), "onStart()");
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		_uiThreadHandler = null;
		_uiCallbacks.kill();
		Log.d(getClass().getSimpleName(), "onDestroy()");
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.d(getClass().getSimpleName(), "onBind()");
		return _shoutbreakServiceStub;
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}
	
	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
	}
	
	///////////////////////////////////////////////////////////////////////////
	// END LIFECYCLE METHODS
	///////////////////////////////////////////////////////////////////////////

	private IShoutbreakService.Stub _shoutbreakServiceStub = new IShoutbreakService.Stub() {

		public void shout(String shoutText) {
			MessageObject messageObject = new MessageObject();
			messageObject.args = new String[] { shoutText };
			Message message = new Message();
			message.what = Vars.MESSAGE_STATE_SHOUT;
			message.obj = messageObject;
			runOnServiceThread(message);
		}
		
		public void vote(String shoutID, int vote) {
			MessageObject messageObject = new MessageObject();
			messageObject.args = new String[] { shoutID, Integer.toString(vote) };
			Message message = new Message();
			message.what = Vars.MESSAGE_STATE_VOTE;
			message.obj = messageObject;
			runOnServiceThread(message);
		}
		
		public void deleteShout(String shoutID) {
			MessageObject messageObject = new MessageObject();
			messageObject.args = new String[] { shoutID };
			Message message = new Message();
			message.what = Vars.MESSAGE_STATE_DELETE_SHOUT;
			message.obj = messageObject;
			runOnServiceThread(message);
		}
		
		public void registerCallback(IShoutbreakServiceCallback cb) {
			if (cb != null)
				_uiCallbacks.register(cb);
		}

		public void unregisterCallback(IShoutbreakServiceCallback cb) {
			if (cb != null)
				_uiCallbacks.unregister(cb);
		}

	};

	private void emptyCallbacks() {
		final int N = _uiCallbacks.beginBroadcast();
		for (int i = 0; i < N; i++) {
			_uiCallbacks.unregister(_uiCallbacks.getBroadcastItem(i));
		}
		_uiCallbacks.finishBroadcast();
	}
	
	
	public void runOnServiceThread(Message message) {
		ServiceThread tempServiceThread = new ServiceThread(_stateEngine, message);
		// launch serviceThread from uiThread
		_uiThreadHandler.removeCallbacks(tempServiceThread); // ensure no rogue callbacks
		_uiThreadHandler.post(tempServiceThread);
	}
	
	public void runOnServiceThreadDelayed(Message message, long delay) {
		ServiceThread tempServiceThread = new ServiceThread(_stateEngine, message);
		// launch serviceThread from uiThread
		_uiThreadHandler.removeCallbacks(tempServiceThread); // ensure no rogue callbacks
		_uiThreadHandler.postDelayed(tempServiceThread, delay);
	}

}
