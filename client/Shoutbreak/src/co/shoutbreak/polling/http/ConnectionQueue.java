package co.shoutbreak.polling.http;

import java.util.ArrayList;

import co.shoutbreak.core.C;

// http://masl.cis.gvsu.edu/2010/04/05/android-code-sample-asynchronous-http-connections/
public class ConnectionQueue {

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
		if (active.size() < C.CONFIG_MAX_SIMULTANEOUS_HTTP_CONNECTIONS)
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
