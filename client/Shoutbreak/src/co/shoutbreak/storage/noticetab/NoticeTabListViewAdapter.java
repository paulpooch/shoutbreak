package co.shoutbreak.storage.noticetab;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Typeface;
import android.graphics.PorterDuff.Mode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import co.shoutbreak.R;
import co.shoutbreak.core.C;
import co.shoutbreak.core.Colleague;
import co.shoutbreak.core.Mediator;

public class NoticeTabListViewAdapter extends BaseAdapter implements Colleague {

	private Mediator _m;
	private List<Notice> _displayedNotices;
	private LayoutInflater _inflater;
	private OnClickListener _rowClickListener;
	private OnClickListener _linkButtonClickListener;
	
	public NoticeTabListViewAdapter(Mediator mediator, LayoutInflater inflater) {
		
		_m = mediator;
		_displayedNotices = new ArrayList<Notice>();
		_inflater = inflater;

		_rowClickListener = new OnClickListener() {
			public void onClick(View view) {
				// NoticeViewHolder holder = (NoticeViewHolder) view.getTag();
				_m.markAllNoticesRead();
			}
		};
		
		_linkButtonClickListener = new OnClickListener() {
			public void onClick(View view) {
				NoticeViewHolder holder = (NoticeViewHolder) view.getTag();
				switch (holder.type) {
					case C.NOTICE_SHOUTS_RECEIVED: {
						_m.getUiGateway().jumpToShoutInInbox(holder.ref);
						_m.getUiGateway().hideNoticeTab();
						break;
					}
					case C.NOTICE_ACCOUNT_CREATED: {
						_m.getUiGateway().jumpToProfile();
						_m.getUiGateway().hideNoticeTab();
						break;
					}
				}
			}
		};
		
	}

	@Override
	public void unsetMediator() {
		_m = null;
	}

	public View getView(int position, View convertView, ViewGroup parent) {

		NoticeViewHolder holder;
		Notice entry = _displayedNotices.get(position);

		if (convertView == null) {

			convertView = _inflater.inflate(R.layout.notice_item, parent, false);
			convertView.setOnClickListener(_rowClickListener);
			
			holder = new NoticeViewHolder();
			holder.text = (TextView) convertView.findViewById(R.id.textTv);
			holder.icon = (ImageView) convertView.findViewById(R.id.iconIv);
			holder.linkBtn = (ImageButton) convertView.findViewById(R.id.linkBtn);
			holder.linkBtn.setOnClickListener(_linkButtonClickListener);
        	holder.linkBtn.getBackground().setColorFilter(0xAA9900FF, Mode.SRC_ATOP);
			
			holder.linkBtn.setTag(holder);
			convertView.setTag(holder);
		} else {
			holder = (NoticeViewHolder) convertView.getTag();
		}

		holder.isLinkable = false;
		holder.text.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
		holder.linkBtn.setVisibility(View.GONE);
		
		if (entry.state_flag == C.NOTICE_STATE_NEW) {
			holder.text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
		}
		
		switch (entry.type) {
			case C.NOTICE_POINTS: {
				if (entry.value > 0) {
					holder.icon.setBackgroundResource(R.drawable.notice_icon_points);
				} else {
					holder.icon.setBackgroundResource(R.drawable.notice_icon_points_down);
				}
				break;
			}
			case C.NOTICE_SHOUTS_RECEIVED: {
				holder.icon.setBackgroundResource(R.drawable.notice_icon_shout);
				holder.isLinkable = true;
				break;
			}
			case C.NOTICE_ACCOUNT_CREATED: {
				holder.icon.setBackgroundResource(R.drawable.notice_icon_user);
				holder.isLinkable = true;
				break;
			}
			case C.NOTICE_LEVEL_UP: {
				holder.icon.setBackgroundResource(R.drawable.notice_icon_user);
				break;
			}
			case C.NOTICE_CREATE_ACCOUNT_FAILED:
			case C.NOTICE_SHOUT_FAILED: {
				holder.icon.setBackgroundResource(R.drawable.notice_icon_warning);
				break;
			}
			case C.NOTICE_SHOUT_SENT: {
				holder.icon.setBackgroundResource(R.drawable.notice_icon_shout_sent);
				break;
			}
			case C.NOTICE_NO_ACCOUNT: {
				holder.icon.setBackgroundResource(R.drawable.notice_icon_info);
				break;
			}
			default: {
				holder.icon.setBackgroundResource(R.drawable.notice_icon_blank);
				break;
			}
		}

		holder.text.setText(entry.text);
		holder.type = entry.type;
		holder.ref = entry.ref;

		if (holder.isLinkable) {
			holder.linkBtn.setVisibility(View.VISIBLE);
		}
		
		return convertView;
	}

	public void refresh(List<Notice> list) {
		_displayedNotices = list;
		this.notifyDataSetChanged();
	}

	public int getCount() {
		return _displayedNotices.size();
	}

	public Object getItem(int position) {
		return _displayedNotices.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

}
