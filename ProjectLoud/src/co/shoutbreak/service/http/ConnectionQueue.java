package co.shoutbreak.service.http;

import java.util.ArrayList;

// http://masl.cis.gvsu.edu/2010/04/05/android-code-sample-asynchronous-http-connections/
public class ConnectionQueue {

	public static final int MAX_SIMULTANEOUS_HTTP_CONNECTIONS = 5;
	
	private ArrayList<Runnable> active = new ArrayList<Runnable>();
	private ArrayList<Runnable> queue = new ArrayList<Runnable>();

	private static ConnectionQueue instance;

	public static ConnectionQueue getInstance() {
		if (instance == null)
			instance = new ConnectionQueue();
		return instance;
	}

	public void push(Runnable runnable) {
		queue.add(runnable);
		if (active.size() < MAX_SIMULTANEOUS_HTTP_CONNECTIONS)
			startNext();
	}

	private void startNext() {
		if (!queue.isEmpty()) {
			Runnable next = queue.get(0);
			queue.remove(0);
			active.add(next);

			Thread thread = new Thread(next);
			thread.start();
		}
	}

	public void didComplete(Runnable runnable) {
		active.remove(runnable);
		startNext();
	}

}