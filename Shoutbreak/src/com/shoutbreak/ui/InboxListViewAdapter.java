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
    private HashMap<String, String> _prettyTimeAgoCache;
    
    static class InboxViewHolder {
    	String shoutID;
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
    
    public HashMap<String, Boolean> getExpandStateTracker() {
    	return _expandStateTracker;
    }
    
    public InboxListViewAdapter(Context context, ShoutbreakUI ui, List<Shout> displayedShouts) {
        _context = context;
        _ui = ui;
        _displayedShouts = displayedShouts;
        _prettyTime = new PrettyTime();
        _inflater = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        _expandStateTracker = new HashMap<String, Boolean>();
        _prettyTimeAgoCache = new HashMap<String, String>();
        
        onCollapseClickListener = new OnClickListener() {
        	public void onClick(View view) {
        		InboxViewHolder holder = (InboxViewHolder) view.getTag();
        		holder.collapsed.setVisibility(View.VISIBLE);
		        holder.expanded.setVisibility(View.GONE);
		        _expandStateTracker.put(holder.shoutID, false);
            }        	
        };
        
        onVoteUpClickListener = new OnClickListener() {
        	public void onClick(View view) {
        		String shoutID = (String) view.getTag();
            	try {
					_ui.getService().vote(shoutID, Vars.SHOUT_VOTE_UP);
				} catch (RemoteException ex) {
					ErrorManager.manage(ex);
				}
        	}
        };
        
        onVoteDownClickListener = new OnClickListener() {
        	public void onClick(View view) {
        		String shoutID = (String) view.getTag();
            	try {
					_ui.getService().vote(shoutID, Vars.SHOUT_VOTE_DOWN);
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
        Shout entry = _displayedShouts.get(position);
        
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
        	holder.btnVoteDown = (ImageButton) convertView.findViewById(R.id.btnVoteDown);
        	holder.btnVoteDown.setOnClickListener(onVoteDownClickListener);
        	holder.expanded.setOnClickListener(onCollapseClickListener);
        	holder.expanded.setTag(holder);
        	convertView.setTag(holder);
        } else {
        	holder = (InboxViewHolder) convertView.getTag();
        }
                
        holder.textC.setText(entry.text);
        holder.textE.setText(entry.text);
        holder.scoreC.setText(entry.score + "");
        holder.scoreE.setText(entry.score + "");
        holder.shoutID = entry.id;
        
        // Can shout be voted on?
        if (entry.open) {
        	holder.btnVoteUp.setEnabled(true);
        	holder.btnVoteUp.setTag(entry.id);
        	holder.btnVoteDown.setEnabled(true);
        	holder.btnVoteDown.setTag(entry.id);
        } else {
        	holder.btnVoteUp.setEnabled(false);
        	holder.btnVoteDown.setEnabled(false);
        }
        
        // Is shout expanded?
        boolean isExpanded = false;
        if (_expandStateTracker.containsKey(entry.id)) {
        	isExpanded = _expandStateTracker.get(entry.id);
        }
        if (isExpanded) {
        	holder.collapsed.setVisibility(View.GONE);
			holder.expanded.setVisibility(View.VISIBLE);
        } else {
        	holder.collapsed.setVisibility(View.VISIBLE);
			holder.expanded.setVisibility(View.GONE);
        }
        
        // How long ago was shout sent?
        String timeAgo = "";
        if (_prettyTimeAgoCache.containsKey(entry.id)) {
        	timeAgo = _prettyTimeAgoCache.get(entry.id);
        } else {
        	try {
       			timeAgo = _prettyTime.format(ISO8601DateParser.parse(entry.timestamp));
       			_prettyTimeAgoCache.put(entry.id, timeAgo);
       		} catch (ParseException ex) {
       			ErrorManager.manage(ex);
       		}
        }
		holder.timeAgoC.setText(timeAgo);
		holder.timeAgoE.setText(timeAgo);
		  
		return convertView;
    }
    
}