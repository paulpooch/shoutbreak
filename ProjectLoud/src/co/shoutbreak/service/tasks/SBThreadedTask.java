package co.shoutbreak.service.tasks;

import android.os.Handler;

public abstract class SBThreadedTask extends Thread {

	private Handler _handler;
	
	abstract void respond();
	
	public abstract void run();
	
	public SBThreadedTask(Handler handler) {
		_handler = handler;
	}
}
