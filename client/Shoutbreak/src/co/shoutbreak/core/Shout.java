package co.shoutbreak.core;

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
	public int state_flag;
	public int score;
	
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
		this.state_flag = C.SHOUT_STATE_NEW;
		this.score = -1;
	}

	public void calculateScore() {
		// begin here:
		// http://www.derivante.com/2009/09/01/php-content-rating-confidence/
		score = ratingAverage(ups, ups + downs);
	}

	public static int ratingAverage(int positive, int total) {
		double power = C.CONFIG_SHOUT_SCORING_DEFAULT_POWER;
		return ratingAverage(positive, total, power);
	}

	public static int ratingAverage(int positive, int total, double power) {
		if (total == 0) {
			return 0;
		}
		double z = pNormalDist(1 - power / 2);
		double p = 1.0 * positive / total;
		double s = (p + z * z / (2 * total) - z * Math.sqrt((p * (1 - p) + z * z / (4 * total)) / total))
				/ (1 + z * z / total);
		return (int) Math.round(s * 100);
	}

	public static double pNormalDist(double quantile) {

		if (quantile < 0.0 || 1.0 < quantile) {
			return 0.0;
		} else if (quantile == 0.5) {
			return 0.0;
		}

		double w1 = quantile;

		if (quantile > 0.5) {
			w1 = 1.0 - w1;
		}

		double w3 = -Math.log(4.0 * w1 * (1.0 - w1));
		w1 = C.NORMAL_DIST_B[0];

		for (int i = 1; i <= 10; i++) {
			w1 += C.NORMAL_DIST_B[i] * Math.pow(w3, i);
		}

		if (quantile > 0.5) {
			return Math.sqrt(w1 * w3);
		}

		return -Math.sqrt(w1 * w3);
	}

}
