package co.shoutbreak.core;

public class Notice {

	public String id;
	public String timestamp;
	public String text;
	public int state_flag;

	public Notice() {
		this("", "", "");
	}

	public Notice(String id, String timestamp, String text) {
		this.id = id;
		this.timestamp = timestamp;
		this.text = text;
		this.state_flag = C.SHOUT_STATE_NEW;
	}
	
}
