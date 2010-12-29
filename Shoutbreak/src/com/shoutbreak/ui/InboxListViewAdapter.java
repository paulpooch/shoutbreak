package com.shoutbreak.ui;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;

import com.ocpsoft.pretty.time.PrettyTime;
import com.shoutbreak.R;
import com.shoutbreak.Shout;
import com.shoutbreak.ShoutbreakUI;
import com.shoutbreak.Vars;
import com.shoutbreak.service.ErrorManager;

import android.content.Context;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class InboxListViewAdapter extends BaseAdapter {
   
	private Context _context;
    private List<Shout> _displayedShouts;
    private LayoutInflater _inflater;
    private PrettyTime _prettyTime;
    public OnClickListener onCollapseClickListener;
    public OnClickListener onVoteUpClickListener;
    public OnClickListener onVoteDownClickListener;
    private ShoutbreakUI _ui;
    private HashMap<String, Boolean> _expandStateTracker;
    
    static class InboxViewHolder {
    	int indexInList;
    	TextView textC;
    	TextView textE;
    	TextView timeAgoC;
    	TextView timeAgoE;
    	TextView scoreC;
    	TextView scoreE;
    	RelativeLayout collapsed;
    	RelativeLayout expanded;
    	ImageButton btnVoteUp;
    	ImageButton btnVoteDown;
    }
    
    public InboxListViewAdapter(Context context, ShoutbreakUI ui, List<Shout> displayedShouts) {
        _context = context;
        _ui = ui;
        _displayedShouts = displayedShouts;
        _prettyTime = new PrettyTime();
        _inflater = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        _expandStateTracker = new HashMap<String, Boolean>();
        
        onCollapseClickListener = new OnClickListener() {
        	public void onClick(View view) {
            	InboxViewHolder holder = (InboxViewHolder) view.getTag();
            	Shout entry = _displayedShouts.get(holder.indexInList);
				if (entry.isExpandedInInbox) {
					holder.collapsed.setVisibility(View.VISIBLE);
					holder.expanded.setVisibility(View.GONE);
    				entry.isExpandedInInbox = false;
				}
            }        	
        };
        
        onVoteUpClickListener = new OnClickListener() {
        	public void onClick(View view) {
        		Shout entry = (Shout) view.getTag();
//        		InboxViewHolder holder = (InboxViewHolder) view.getTag();
//            	Shout entry = _displayedShouts.get(holder.indexInList);
            	try {
					_ui.getService().vote(entry.id, Vars.SHOUT_VOTE_UP);
				} catch (RemoteException ex) {
					ErrorManager.manage(ex);
				}
        	}
        };
        
        onVoteDownClickListener = new OnClickListener() {
        	public void onClick(View view) {
        		Shout entry = (Shout) view.getTag();
//        		InboxViewHolder holder = (InboxViewHolder) view.getTag();
//            	Shout entry = _displayedShouts.get(holder.indexInList);
            	try {
					_ui.getService().vote(entry.id, Vars.SHOUT_VOTE_DOWN);
				} catch (RemoteException ex) {
					ErrorManager.manage(ex);
				}
        	}
        };
        
    }
    
    public void updateDisplay(List<Shout> list) {
    	_displayedShouts = list;
    	this.notifyDataSetChanged();
    }
    
    public int getCount() {
        return _displayedShouts.size();
    }

    public Object getItem(int position) {
        return _displayedShouts.get(position);
    }

    public long getItemId(int position) {
        return position;
    }
    
    public View getView(int position, View convertView, ViewGroup parent) {
        
    	InboxViewHolder holder;
    	
        if (convertView == null) {
            convertView = _inflater.inflate(R.layout.inbox_item, parent, false);
        	holder = new InboxViewHolder();
        	holder.textC = (TextView) convertView.findViewById(R.id.tvTextC);
        	holder.timeAgoC = (TextView) convertView.findViewById(R.id.tvTimeAgoC);
        	holder.scoreC = (TextView) convertView.findViewById(R.id.tvScoreC);
        	holder.textE = (TextView) convertView.findViewById(R.id.tvTextE);
        	holder.timeAgoE = (TextView) convertView.findViewById(R.id.tvTimeAgoE);
        	holder.scoreE = (TextView) convertView.findViewById(R.id.tvScoreE);
        	holder.collapsed = (RelativeLayout) convertView.findViewById(R.id.rlCollapsed);
        	holder.expanded = (RelativeLayout) convertView.findViewById(R.id.rlExpanded);
        	holder.btnVoteUp = (ImageButton) convertView.findViewById(R.id.btnVoteUp);
        	holder.btnVoteUp.setOnClickListener(onVoteUpClickListener);
        	//holder.btnVoteUp.setTag(holder);
        	holder.btnVoteDown = (ImageButton) convertView.findViewById(R.id.btnVoteDown);
        	holder.btnVoteDown.setOnClickListener(onVoteDownClickListener);
        	//holder.btnVoteDown.setTag(holder);
        	holder.expanded.setOnClickListener(onCollapseClickListener);
        	//holder.collapsed.setTag(holder);
        	holder.expanded.setTag(holder);
        	convertView.setTag(holder);
        } else {
        	holder = (InboxViewHolder) convertView.getTag();
        }
        
        Shout entry = _displayedShouts.get(position);
        
        holder.indexInList = position; // this changes everytime a view is redrawn
        holder.textC.setText(entry.text);
        holder.textE.setText(entry.text);
        holder.scoreC.setText(entry.score + "");
        holder.scoreE.setText(entry.score + "");
        
        if (entry.open) {
        	holder.btnVoteUp.setEnabled(true);
        	holder.btnVoteUp.setTag(entry);
        	holder.btnVoteDown.setEnabled(true);
        	holder.btnVoteDown.setTag(entry);
        } else {
        	holder.btnVoteUp.setEnabled(false);
        	holder.btnVoteDown.setEnabled(false);
        }
        
//        final OnClickListener onExpandedClickListener = new OnClickListener() {
//            public void onClick(View view) {
//            	InboxViewHolder holder = (InboxViewHolder) view.getTag();
//            	Shout entry = _displayedShouts.get(holder.indexInList); // is it bad performance to iterate list here?
//				if (!entry.isExpandedInInbox) {
//					holder.collapsed.setVisibility(View.GONE);
//					holder.expanded.setVisibility(View.VISIBLE);
//					entry.isExpandedInInbox = true;
//				}
//            }        	
//        };
        
       //holder.collapsed.setOnClickListener(onExpandedClickListener);
        
        boolean isExpanded = false;
        if (_expandStateTracker.containsKey(entry.id)) {
        	isExpanded = _expandStateTracker.get(entry.id)
        }
        
        if (entry.isExpandedInInbox) {
        	holder.collapsed.setVisibility(View.GONE);
			holder.expanded.setVisibility(View.VISIBLE);
        } else {
        	holder.collapsed.setVisibility(View.VISIBLE);
			holder.expanded.setVisibility(View.GONE);
        }
        
        try {
			// TODO: cache this prettytime overhead
			String timeAgo = _prettyTime.format(ISO8601DateParser.parse(entry.timestamp));
			holder.timeAgoC.setText(timeAgo);
			holder.timeAgoE.setText(timeAgo);
		} catch (ParseException ex) {
			ErrorManager.manage(ex);
		}
		return convertView;
        // Set the onClick Listener on this button
//        Button btnRemove = (Button) convertView.findViewById(R.id.btnRemove);
//        btnRemove.setOnClickListener(this);
//        // Set the entry, so that you can capture which item was clicked and
//        // then remove it
//        // As an alternative, you can use the id/position of the item to capture
//        // the item
//        // that was clicked.
//        btnRemove.setTag(entry);

        // btnRemove.setId(position);
    }
    
//	public void onClick(View v) {
//		// TODO Auto-generated method stub
//		InboxViewHolder holder = (InboxViewHolder) v.getTag();
//		if (holder.isCollapsed) {
//			holder.collapsed.setVisibility(View.GONE);
//			holder.expanded.setVisibility(View.VISIBLE);
//		} else {
//			holder.collapsed.setVisibility(View.VISIBLE);
//			holder.expanded.setVisibility(View.GONE);	
//		}
//		holder.isCollapsed = !holder.isCollapsed;
//	}

}