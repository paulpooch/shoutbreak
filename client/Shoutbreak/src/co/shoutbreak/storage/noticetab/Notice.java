package co.shoutbreak.storage.noticetab;

import co.shoutbreak.core.utils.SBLog;

public class Notice {
	
	private static final String TAG = "Notice";
	
	public long id;
	public int type;
	public int value;
	public String text;
	public String ref;
	public long timestamp;
	public int state_flag;
	
	public Notice() {
		SBLog.constructor(TAG);
	}

	public Notice(int id, int type, int value, String text, String ref, long timestamp, int stateFlag) {
		SBLog.constructor(TAG);
		this.id = id;
		this.type = type;
		this.value = value;
		this.text = text;
		this.ref = ref;
		this.timestamp = timestamp;
		this.state_flag = stateFlag;
	}
	
}
