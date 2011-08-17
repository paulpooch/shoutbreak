package co.shoutbreak.ui;

import co.shoutbreak.R;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ProfileViewAdapter {
	
	private Shoutbreak _ui;
	private TextView _levelTv;
	private TextView _pointsTv;
	private TextView _nextLevelAtTv;
	private ProgressBar _progressPb;
	
	public ProfileViewAdapter(Shoutbreak ui) {
		_ui = ui;
		_levelTv = (TextView) _ui.findViewById(R.id.userLevelTv);
		_pointsTv = (TextView) _ui.findViewById(R.id.userPointsTv);
		_nextLevelAtTv = (TextView) _ui.findViewById(R.id.userNextLevelAtTv);
		_progressPb = (ProgressBar) _ui.findViewById(R.id.userProgressPb);
	}
	
	public void refresh(int level, int points, int nextLevelAt) {
		_levelTv.setText("Your level: " + level);
		_pointsTv.setText("Points earned: " + points);
		_nextLevelAtTv.setText("Next level at: " + nextLevelAt);
		//_progressPb.setMax(100);
		_progressPb.setProgress(50);
		if (nextLevelAt > 0) {
			_progressPb.setProgress(points / nextLevelAt);
		}
	}
}
