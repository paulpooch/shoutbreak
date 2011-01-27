package com.shoutbreak.ui;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.ocpsoft.pretty.time.PrettyTime;
import com.shoutbreak.R;
import com.shoutbreak.Shout;
import com.shoutbreak.ShoutbreakUI;
import com.shoutbreak.Vars;
import com.shoutbreak.service.ErrorManager;

import android.content.Context;
import android.graphics.Typeface;
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
   
    private ShoutbreakUI _ui;
    private List<Shout> _displayedShouts;
    private LayoutInflater _inflater;
    private PrettyTime _prettyTime;
    public OnClickListener onCollapseClickListener;
    public OnClickListener onVoteUpClickListener;
    public OnClickListener onVoteDownClickListener;
    public OnClickListener onDeleteClickListener;
    private HashMap<String, Boolean> _cacheExpandState;
    private HashMap<String, String> _cachePrettyTimeAgo;
    private HashMap<String, Integer> _cacheVoteTemporary;
    
    public HashMap<String, Boolean> getCacheExpandState() {
    	return _cacheExpandState;
    }
        
    public InboxListViewAdapter(ShoutbreakUI ui) {
    	
        _ui = ui;
        _displayedShouts = new ArrayList<Shout>();
        _prettyTime = new PrettyTime();
        _inflater = (LayoutInflater) _ui.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        _cacheExpandState = new HashMap<String, Boolean>();
        _cachePrettyTimeAgo = new HashMap<String, String>();
        _cacheVoteTemporary = new HashMap<String, Integer>();
        
        onCollapseClickListener = new OnClickListener() {
        	public void onClick(View view) {
        		InboxViewHolder holder = (InboxViewHolder) view.getTag();
        		holder.collapsed.setVisibility(View.VISIBLE);
		        holder.expanded.setVisibility(View.GONE);
		        _cacheExpandState.put(holder.shoutID, false);
            }        	
        };
        
        onVoteUpClickListener = new OnClickListener() {
        	public void onClick(View view) {
        		InboxViewHolder holder = (InboxViewHolder) view.getTag();
        		_cacheVoteTemporary.put(holder.shoutID, Vars.SHOUT_VOTE_UP);
            	try {
					_ui.getService().vote(holder.shoutID, Vars.SHOUT_VOTE_UP);
				} catch (RemoteException ex) {
					ErrorManager.manage(ex);
				}
        	}
        };
        
        onVoteDownClickListener = new OnClickListener() {
        	public void onClick(View view) {
        		InboxViewHolder holder = (InboxViewHolder) view.getTag();
        		_cacheVoteTemporary.put(holder.shoutID, Vars.SHOUT_VOTE_DOWN);
            	try {
					_ui.getService().vote(holder.shoutID, Vars.SHOUT_VOTE_DOWN);
				} catch (RemoteException ex) {
					ErrorManager.manage(ex);
				}
        	}
        };
        
        onDeleteClickListener = new OnClickListener() {
        	public void onClick(View view) {
        		InboxViewHolder holder = (InboxViewHolder) view.getTag();        		
        		_ui.getUser().getInbox().deleteShout(holder.shoutID);
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
            // TODO: can we reduce the number of items?
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
        	holder.voteLabelE= (TextView) convertView.findViewById(R.id.tvVoteLabel);
        	holder.btnVoteUp = (ImageButton) convertView.findViewById(R.id.btnVoteUp);
        	holder.btnVoteUp.setOnClickListener(onVoteUpClickListener);
        	holder.btnVoteDown = (ImageButton) convertView.findViewById(R.id.btnVoteDown);
        	holder.btnVoteDown.setOnClickListener(onVoteDownClickListener);
        	holder.btnDelete = (ImageButton) convertView.findViewById(R.id.btnDelete);
        	holder.btnDelete.setOnClickListener(onDeleteClickListener);
        	holder.expanded.setOnClickListener(onCollapseClickListener);
        	holder.expanded.setTag(holder);
        	convertView.setTag(holder);
        } else {
        	holder = (InboxViewHolder) convertView.getTag();
        }
        
        // Is shout expanded?
        boolean isExpanded = false;
        if (_cacheExpandState.containsKey(entry.id)) {
        	isExpanded = _cacheExpandState.get(entry.id);
        }
        if (isExpanded) {
        	holder.collapsed.setVisibility(View.GONE);
			holder.expanded.setVisibility(View.VISIBLE);
        } else {
        	holder.collapsed.setVisibility(View.VISIBLE);
			holder.expanded.setVisibility(View.GONE);
        }
        
        // How long ago was shout sent?
        // TODO: this caching can become inaccurate
        String timeAgo = "";
        if (_cachePrettyTimeAgo.containsKey(entry.id)) {
        	timeAgo = _cachePrettyTimeAgo.get(entry.id);
        } else {
        	try {
       			timeAgo = _prettyTime.format(ISO8601DateParser.parse(entry.timestamp));
       			_cachePrettyTimeAgo.put(entry.id, timeAgo);
       		} catch (ParseException ex) {
       			ErrorManager.manage(ex);
       		}
        }

        // Can shout be voted on?
        boolean isVotingAllowed = true;
        if (entry.open) {
        	int vote = entry.vote;
        	if (_cacheVoteTemporary.containsKey(entry.id)) {
        		vote |= _cacheVoteTemporary.get(entry.id);
        	}
        	if (vote != Vars.NULL_VOTE) {
    	      	holder.voteLabelE.setText("ALREADY VOTED" + entry.open +" | "+vote);
    	      	isVotingAllowed = false;
        	} else {
        		holder.voteLabelE.setText("VOTE" + entry.open +" | "+vote);
    	      	isVotingAllowed = true;
        	}
        } else {
        	holder.voteLabelE.setText("VOTING CLOSED");
        	isVotingAllowed = false;
        }
        if (isVotingAllowed) {
        	holder.btnVoteUp.setEnabled(true);
        	holder.btnVoteUp.setTag(holder);
        	holder.btnVoteDown.setEnabled(true);
        	holder.btnVoteDown.setTag(holder);
        } else {
        	holder.btnVoteUp.setEnabled(false);
        	holder.btnVoteDown.setEnabled(false);
        }
        
        // Is there a score?
        String score = Integer.toString(entry.score);
        if (entry.score == Vars.NULL_APPROVAL || entry.score == Vars.NULL_SCORE) {
        	score = "?";
        }
        
        // Mark shout as read/unread
        if (entry.state_flag == Vars.SHOUT_STATE_NEW) {
    		holder.textC.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    	} else {
    		holder.textC.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
    	}
        
        holder.textC.setText(entry.text);
        holder.textE.setText(entry.text);
        holder.scoreC.setText(score);
        holder.scoreE.setText(score);
        holder.shoutID = entry.id;
        holder.timeAgoC.setText(timeAgo);
		holder.timeAgoE.setText(timeAgo);
		holder.btnDelete.setTag(holder);
		
		return convertView;
    }
    
}