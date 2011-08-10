package co.shoutbreak.ui;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.TextView;
import co.shoutbreak.R;
import co.shoutbreak.core.Notice;

public class NoticeListViewAdapter extends BaseAdapter {
	
    private List<Notice> _displayedNotices;
    private LayoutInflater _inflater;
    public OnClickListener onCollapseClickListener;
    public OnClickListener onVoteUpClickListener;
    public OnClickListener onVoteDownClickListener;
    public OnClickListener onDeleteClickListener;

    public NoticeListViewAdapter(Shoutbreak ui) {
    	_displayedNotices = new ArrayList<Notice>();
        _inflater = (LayoutInflater) ui.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
    
    /*
    public HashMap<String, Boolean> getCacheExpandState() {
    	return _cacheExpandState;
    }
        
    private class VoteTask extends AsyncTask<Object, Void, Void> {    	
    	// TODO: why is this an async task? all it does is fire off
    	// a message that gets handled by a separate thread
    	@Override
		protected Void doInBackground(Object... params) {
			InboxViewHolder holder = (InboxViewHolder)params[0];
			Integer voteDirection = (Integer)params[1];
    		_cacheVoteTemporary.put(holder.shoutId, voteDirection);
        	_m.voteStart(holder.shoutId, voteDirection);
			return null;
		}
        protected void onPostExecute(Void unused) {
        }
    }
    
    public NoticeListViewAdapter(Shoutbreak ui, Mediator mediator) {
    	_m = mediator;
        _displayedShouts = new ArrayList<Shout>();
        _prettyTime = new PrettyTime();
        _inflater = (LayoutInflater) ui.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        _cacheExpandState = new HashMap<String, Boolean>();
        _cachePrettyTimeAgo = new HashMap<String, String>();
        _cacheVoteTemporary = new HashMap<String, Integer>();
        _isInputAllowed = false;
        
        onCollapseClickListener = new OnClickListener() {
        	public void onClick(View view) {
        		InboxViewHolder holder = (InboxViewHolder) view.getTag();
        		holder.collapsed.setVisibility(View.VISIBLE);
		        holder.expanded.setVisibility(View.GONE);
		        _cacheExpandState.put(holder.shoutId, false);
            }        	
        };
        
        onVoteUpClickListener = new OnClickListener() {
        	public void onClick(View view) {
        		InboxViewHolder holder = (InboxViewHolder) view.getTag();
        		holder.btnVoteUp.setEnabled(false);
        		holder.btnVoteDown.setEnabled(false);
                holder.btnVoteUp.setImageResource(R.drawable.vote_up_button);
        		view.invalidate();
        		
        		VoteTask task = new VoteTask();
        		task.execute(view.getTag(), C.SHOUT_VOTE_UP);
        	}
        };        
         
        onVoteDownClickListener = new OnClickListener() {
        	public void onClick(View view) {
        		InboxViewHolder holder = (InboxViewHolder) view.getTag();
        		holder.btnVoteUp.setEnabled(false);
        		holder.btnVoteDown.setEnabled(false);
                holder.btnVoteDown.setImageResource(R.drawable.vote_down_button);
        		view.invalidate();
        		
        		VoteTask task = new VoteTask();
        		task.execute(view.getTag(), C.SHOUT_VOTE_DOWN);
        	}
        };
        
        onDeleteClickListener = new OnClickListener() {
        	public void onClick(View view) {
        		InboxViewHolder holder = (InboxViewHolder) view.getTag();
            	_m.deleteShout(holder.shoutId);
			}
        };
        
    }

	@Override
	public void unsetMediator() {
		SBLog.i(TAG, "unsetMediator()");
		_m = null;	
	}	
    
	public void refresh(List<Shout> shouts) {
		updateDisplay(shouts);
	}
    
    public void setInputAllowed(boolean b) {
    	_isInputAllowed = b;
    	notifyDataSetChanged();
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
        	holder.btnVoteUp = (ImageButton) convertView.findViewById(R.id.btnVoteUp);
        	holder.btnVoteUp.setOnClickListener(onVoteUpClickListener);
        	holder.btnVoteDown = (ImageButton) convertView.findViewById(R.id.btnVoteDown);
        	holder.btnVoteDown.setOnClickListener(onVoteDownClickListener);
        	holder.btnDelete = (ImageButton) convertView.findViewById(R.id.btnDelete);
        	holder.btnDelete.setOnClickListener(onDeleteClickListener);
        	holder.btnReply = (ImageButton) convertView.findViewById(R.id.btnReply);
        	holder.btnVoteUp.getBackground().setColorFilter(0xAA9900FF, Mode.SRC_ATOP);
        	holder.btnVoteDown.getBackground().setColorFilter(0xAA9900FF, Mode.SRC_ATOP);
        	holder.btnDelete.getBackground().setColorFilter(0xAA9900FF, Mode.SRC_ATOP);
        	holder.btnReply.getBackground().setColorFilter(0xAA9900FF, Mode.SRC_ATOP);
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

        holder.btnVoteUp.setImageResource(R.drawable.vote_up_button);
        holder.btnVoteDown.setImageResource(R.drawable.vote_down_button);
        
        // Can shout be voted on?
        boolean isVotingAllowed = true;
        if (!entry.open) {
        	isVotingAllowed = false;
        }
        
        int vote = entry.vote;
        if (_cacheVoteTemporary.containsKey(entry.id)) {
        	vote |= _cacheVoteTemporary.get(entry.id);
        }
        if (vote != C.NULL_VOTE) {
        	isVotingAllowed = false;
        	//holder.voteLabelE.setText("ALREADY VOTED" + entry.open +" | "+vote);
        	if (vote == C.SHOUT_VOTE_DOWN) {
        		holder.btnVoteDown.setImageResource(R.drawable.inbox_down_lit);
        	} else {
        		holder.btnVoteUp.setImageResource(R.drawable.inbox_up_lit);
        	}
        }
        
        if (isVotingAllowed) {
        	holder.btnVoteUp.setEnabled(_isInputAllowed);
        	holder.btnVoteUp.setTag(holder);
        	holder.btnVoteDown.setEnabled(_isInputAllowed);
        	holder.btnVoteDown.setTag(holder);
        } else {
        	holder.btnVoteUp.setEnabled(false);
        	holder.btnVoteDown.setEnabled(false);
        }
        
        // Is there a score?
        String score = Integer.toString(entry.score);
        if (entry.score == C.NULL_APPROVAL || entry.score == C.NULL_SCORE) {
        	score = "?";
        }
        
        // Mark shout as read/unread
        if (entry.state_flag == C.SHOUT_STATE_NEW) {
    		holder.textC.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    	} else {
    		holder.textC.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
    	}
        
        holder.textC.setText(entry.text);
        holder.textE.setText(entry.text);
        holder.scoreC.setText(score);
        holder.scoreE.setText(score);
        holder.shoutId = entry.id;
        holder.timeAgoC.setText(timeAgo);
		holder.timeAgoE.setText(timeAgo);
		holder.btnDelete.setTag(holder);
		
		// TODO this should be setEnabled(_isPowerOn) once implemented
		holder.btnReply.setEnabled(false); 
		
		return convertView;
    }
    */
    
}
