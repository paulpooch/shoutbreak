<?php
///////////////////////////////////////////////////////////////////////////////
// Shout
///////////////////////////////////////////////////////////////////////////////

class Shout {
	private $shout_id;
	public $uid;
	public $time;
	private $txt;
	private $lat;
	private $long;
	private $power;
	private $hit;
	public $open;
	public $ups;
	public $downs;
	public $outbox;

	function __construct($shout_id, $uid, $time, $txt, $lat, $long, $power, $hit, $open = null, $ups = null, $downs = null) {
		$this->shout_id = $shout_id;
		$this->uid = $uid;
		$this->time = $time;
		$this->txt = $txt;
		$this->lat = $lat;
		$this->long = $long;
		$this->power = $power;
		$this->hit = $hit;
		$this->open = ($open) ? $open : 0;
		$this->ups = ($ups) ? $ups : 1;
		$this->downs = ($downs) ? $downs : 0;
		$this->outbox = null;
		// is it better to store redundant info or build array on each delivery?
	}
	
	public function calculatePoints() {
		// TODO: calculate points... duh
		return $this->ups - $this->downs;
	}
	
	public function calculateApproval() {
		// if we use for more than small number of ppl, then
		// http://www.evanmiller.org/how-not-to-sort-by-average-rating.html
		$approval = 0;
		if ($this->ups > 0 || $this->downs > 0) {
			$approval = floor( ($this->ups / ($this->ups + $this->downs)) * 20) * 5; // * 100 / 5 = * 20
		}
		e("APPROVAL = $approval");
		return $approval;
	}
	
	public function toShoutsArray() {
		if ($this->outbox != null) {
			return array(
				'shout_id' => $this->shout_id,
				'ts' => $this->time,
				'txt' => $this->txt,
				'hit' => $this->hit,
				'outbox' => 1
			);
		} else {
			return array(
				'shout_id' => $this->shout_id,
				'ts' => $this->time,
				'txt' => $this->txt,
				'hit' => $this->hit
			);
		}	
	}
	
	public function toScoresArray() {
		$arr = null;
		if ($this->hit > Config::$MIN_TARGETS_FOR_HIT_COUNT) {
			$arr = array(
				'shout_id' => $this->shout_id,
				'ups' => $this->ups,
				'downs' => $this->downs,
				'hit' => $this->hit,
				'pts' => $this->calculatePoints(),
				'open' => $this->open);
		} else {
			$arr = array(
				'shout_id' => $this->shout_id,
				'approval' => $this->calculateApproval(),
				'pts' => $this->calculatePoints(),
				'open' => $this->open);
		}
		return $arr;
	}
	
	public function toSimpleDBAttrs() {
		return array(
			'shout_id' => array('value' => $this->shout_id),
			'user_id' => array('value' => $this->uid),
			'time' => array('value' => $this->time),
			'txt' => array('value' => $this->txt),
			'lat' => array('value' => $this->lat),
			'long' => array('value' => $this->long),
			'power' => array('value' => $this->power),
			'hit' => array('value' => $this->hit),
			'open' => array('value' => $this->open),
			'ups' => array('value' => $this->ups),
			'downs' => array('value' => $this->downs)
		);
	}

}

?>