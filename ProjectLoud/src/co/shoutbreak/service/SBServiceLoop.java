package co.shoutbreak.service;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

// looper pipelines message tasks
// http://mindtherobot.com/blog/159/android-guts-intro-to-loopers-and-handlers/
public class SBServiceLoop extends Thread {
	
	private SBService _Service;
	private Handler _LoopHandler;
	
	public SBServiceLoop(SBService service) {
		_Service = service;
	}
	
	// pipelined loop thread, spawns new tasks
	public void run() {
		Looper.prepare();

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
	
	public Handler getLoopHandler() {
		return _LoopHandler;
	}
}
