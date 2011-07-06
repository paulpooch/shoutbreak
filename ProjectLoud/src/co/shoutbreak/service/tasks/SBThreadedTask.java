package co.shoutbreak.service.tasks;

import android.os.Handler;

public abstract class SBThreadedTask extends Thread {

	private Handler _Handler;
	
	abstract void respond();
	
	public abstract void run();
	
	public SBThreadedTask(Handler handler) {
		_Handler = handler;
	}
}
