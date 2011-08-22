package co.shoutbreak.storage.noticetab;

import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class NoticeViewHolder {
	public String id;
	TextView text;
	ImageView icon;
	ImageButton linkBtn;
	boolean isLinkable;
	int type;
	String ref;
}