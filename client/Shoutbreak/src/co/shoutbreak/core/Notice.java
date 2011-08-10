package co.shoutbreak.core;

public class Notice {

	public long id;
	public int type;
	public String text;
	public String ref;
	public long timestamp;
	public int stateFlag;
	
	public int state_flag;
	
	public Notice() {
		
	}

	public Notice(int id, int type, String text, String ref, long timestamp, int stateFlag) {
		this.id = id;
		this.type = type;
		this.text = text;
		this.ref = ref;
		this.timestamp = timestamp;
		this.stateFlag = stateFlag;
	}
	
}
