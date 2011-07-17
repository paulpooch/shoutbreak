package co.shoutbreak.shared;

public class UserEvent {
	
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
	
	public UserEvent() {
		
	}
	
	public String toLogString() {
		StringBuffer sb = new StringBuffer();
		sb.append("\n/////////////////////////////////////////////////////////////\n");
		sb.append("////////////////////// STATE EVENT //////////////////////////\n");
		sb.append("/////////////////////////////////////////////////////////////\n");
		sb.append("densityChanged = " + densityChanged);
		sb.append("locationServicesChanged = " + locationServicesChanged);
		sb.append("inboxChanged = " + inboxChanged);
		sb.append("levelChanged = " + levelChanged);
		sb.append("pointsChanged = " + pointsChanged);
		sb.append("shoutSent = " + shoutSent);
		sb.append("shoutsReceived = " + shoutsReceived);
		sb.append("voteCompleted = " + voteCompleted);
		sb.append("accountCreated = " + accountCreated);
		sb.append("scoresChanged = " + scoresChanged);
		sb.append("locationChanged = " + locationChanged);
		return sb.toString();
	}
	
}
