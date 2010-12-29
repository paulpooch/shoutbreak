package com.shoutbreak;

import java.util.Date;


public class Shout {

	public String id;
	public String timestamp;
	public String text;
	public String re;
	public Long time_received;
	public boolean open;
	public boolean is_outbox;
	public int vote;
	public int hit;
	public int ups;
	public int downs;
	public int pts;
	public int approval;
	public int state_flag;
	public int score;
	
	public boolean isExpandedInInbox;
	
	public Shout() {
		this("", "", "");
	}
	
	public Shout(String id, String timestamp, String text) {
		this.id = id;
		this.timestamp = timestamp;
		this.text = text;
		this.re = "";
		Date d = new Date();
		this.time_received = d.getTime();
		this.open = true;
		this.is_outbox = false;
		this.vote = 0;
		this.hit = 0;
		this.ups = 0;
		this.downs = 0;
		this.pts = 0;
		this.approval = 0;
		this.state_flag = Vars.SHOUT_STATE_NEW;		
		this.score = -1;
		this.isExpandedInInbox = false;
	}
	
	public void calculateScore() {
		if (hit > Vars.MIN_TARGETS_FOR_HIT_COUNT) {
			double z = Vars.SHOUT_SCORING_Z_SCORE;
			int n = ups + downs;
			float positive = ups / n;
			float phat = positive / n;
			score = (int) ( (phat + z * z / (2 * n) - z * Math.sqrt( (phat * (1 - phat) + z * z / (4 * n)) / n) ) / (1 + z * z / n) );
		} else {
			score = approval;
		}
	}
	
}
