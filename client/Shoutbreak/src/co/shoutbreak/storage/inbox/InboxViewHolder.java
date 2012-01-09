package co.shoutbreak.storage.inbox;

import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class InboxViewHolder {
	public String shoutId;
	TextView textC;
	TextView textE;
	TextView timeAgoC;
	TextView timeAgoE;
	TextView scoreC;
	TextView scoreE;
	TextView hitCount;
	LinearLayout hitCountLl;
	public RelativeLayout collapsed;
	public RelativeLayout expanded;
	ImageButton btnVoteUp;
	ImageButton btnVoteDown;
	ImageButton btnDelete;
	//ImageButton btnReply;
}