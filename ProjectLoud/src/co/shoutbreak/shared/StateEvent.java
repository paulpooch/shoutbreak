package co.shoutbreak.shared;

public class StateEvent {

	// ui events
	public boolean pollingTurnedOn = false;
	public boolean pollingTurnedOff = false;
	public boolean locationTurnedOn = false;
	public boolean locationTurnedOff = false;
	
	// user events
	public boolean densityChanged = false;
	public boolean locationServicesChanged = false;
	public boolean inboxChanged = false;
	public boolean levelChanged = false;
	public boolean pointsChanged = false;
	public boolean shoutSent = false;
	public boolean shoutsReceived = false;
	public boolean voteCompleted = false;
	public boolean accountCreated = false;
	public boolean scoresChanged = false;
	public boolean locationChanged = false;
	
	public boolean uiJustSentShout = false;
	public String shoutText;
	public int shoutPower;
		
	public boolean uiJustVoted = false;
	public String voteShoutId;
	public int voteValue;
	
	public StateEvent() {
		
	}
	
	public String toLogString() {
		StringBuffer sb = new StringBuffer();
		sb.append("\n/////////////////////////////////////////////////////////////\n");
		sb.append("////////////////////// STATE EVENT //////////////////////////\n");
		sb.append("/////////////////////////////////////////////////////////////\n");
		sb.append("pollingTurnedOn = " + pollingTurnedOn);
		sb.append("pollingTurnedOff = " + pollingTurnedOff);
		sb.append("locationTurnedOn = " + locationTurnedOn);
		sb.append("locationTurnedOff = " + locationTurnedOff);
		sb.append("uiJustSentShout = " + uiJustSentShout);
		sb.append("shoutText = " + shoutText);
		sb.append("shoutPower = " + shoutPower);
		sb.append("uiJustVoted = " + uiJustVoted);
		sb.append("voteShoutId = " + voteShoutId);
		sb.append("voteValue = " + voteValue);
		return sb.toString();
	}
	
}
