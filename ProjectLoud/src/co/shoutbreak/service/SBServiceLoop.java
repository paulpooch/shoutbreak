package co.shoutbreak.service;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

// looper pipelines message tasks
// http://mindtherobot.com/blog/159/android-guts-intro-to-loopers-and-handlers/
public class SBServiceLoop extends Thread {
	
	private SBService _Service;
	private Handler _LoopHandler;
	
	private boolean _isLoopOn = false;
	
	public SBServiceLoop(SBService service) {
		_Service = service;
	}
	
	// pipelined loop thread, spawns new tasks
	public void run() {
		Looper.prepare();
		
		_isLoopOn = true;

		_LoopHandler = new Handler() {
			public void handleMessage(Message msg) {
				// process incoming messages here
				
			}
		};

		Looper.loop();
	}
	
	// response handler, handles responses of spawned threads
	private Handler _ResponseHandler = new Handler() {
		public void handleMessage(Message msg) {
			// handle responses
			switch (msg.what) {
			
			}
		}
	};
	
	public void quit() {
		if (_isLoopOn & _LoopHandler != null) {
			Toast.makeText(_Service, "Loop Quit" , Toast.LENGTH_SHORT).show();
			_LoopHandler.removeCallbacks(this);
			_LoopHandler.getLooper().quit();
			_LoopHandler = null;
			_ResponseHandler.removeCallbacks(this);
			_ResponseHandler = null;
		}
	}
}
