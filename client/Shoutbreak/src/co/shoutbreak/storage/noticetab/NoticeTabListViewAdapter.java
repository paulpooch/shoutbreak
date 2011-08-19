package co.shoutbreak.storage.noticetab;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.TextView;
import co.shoutbreak.R;
import co.shoutbreak.core.C;

public class NoticeTabListViewAdapter extends BaseAdapter {
	
    private List<Notice> _displayedNotices;
    private LayoutInflater _inflater;
    public OnClickListener onCollapseClickListener;
    public OnClickListener onVoteUpClickListener;
    public OnClickListener onVoteDownClickListener;
    public OnClickListener onDeleteClickListener;

    public NoticeTabListViewAdapter(LayoutInflater inflater) {
    	_displayedNotices = new ArrayList<Notice>();
        _inflater = inflater;
    }
    
   public View getView(int position, View convertView, ViewGroup parent) {
        
    	NoticeViewHolder holder;
        Notice entry = _displayedNotices.get(position);
        
        if (convertView == null) {
            // TODO: can we reduce the number of items?
        	convertView = _inflater.inflate(R.layout.notice_item, parent, false);
        	holder = new NoticeViewHolder();
        	holder.text = (TextView) convertView.findViewById(R.id.textTv);
            convertView.setTag(holder);
        } else {
        	holder = (NoticeViewHolder) convertView.getTag();
        }
        
        if (entry.state_flag == C.NOTICE_STATE_NEW) {
    		holder.text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    	} else {
    		holder.text.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
    	}
        
        holder.text.setText(entry.text);
        
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
