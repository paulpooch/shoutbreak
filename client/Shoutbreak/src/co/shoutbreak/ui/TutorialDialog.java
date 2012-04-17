package co.shoutbreak.ui;

import android.app.Dialog;
import android.graphics.PorterDuff.Mode;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import co.shoutbreak.R;
import co.shoutbreak.core.utils.SBLog;

public class TutorialDialog extends Dialog {

	private static final String TAG = "TutorialDialog";

	private final int NUMBER_OF_SLIDES = 4;

	private TextView _text;
	private ImageView _img;
	private ScrollView _scroll;
	private Button _back;
	private Button _next;
	private Button _finish;

	private String _t1 = "Welcome to Shoutbreak.\n\nAn anonymous way to message nearby people.";
	private String _t2 = "Your REACH is the number of people your shouts will hit.\n\nIn this case 5.";
	private String _t3 = "Those 5 people are somewhere in this circle.";
	private String _t4 = "Earn points to increase your reach.\nEither by voting or sending good shouts.";
	private String _t5 = "By using this app you agree to the Terms of Use.";
	
	private int _slide;

	public TutorialDialog(Shoutbreak ui) {
		super(ui, R.style.popupStyle);
		SBLog.constructor(TAG);
		_slide = 0;
    setContentView(R.layout.tutorial_dialog);
//		LayoutInflater inflater = _ui.getLayoutInflater();
//		_rootView = inflater.inflate(R.layout.tutorial_dialog, null);
//		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT);
//		lp.setMargins(0, 0, 0, 0);
//		setContentView(_rootView, lp);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tutorial_dialog);
		_text = (TextView) findViewById(R.id.tutorialTextTv);
		_back = (Button) findViewById(R.id.tutorialBackBtn);
		_next = (Button) findViewById(R.id.tutorialNextBtn);
		_finish = (Button) findViewById(R.id.tutorialFinishBtn);
		_img = (ImageView) findViewById(R.id.tutorialImageIv);
		_scroll = (ScrollView) findViewById(R.id.tutorialScrollSv);
		_back.getBackground().setColorFilter(0xAA9900FF, Mode.SRC_ATOP);
		_next.getBackground().setColorFilter(0xAA9900FF, Mode.SRC_ATOP);
		_finish.getBackground().setColorFilter(0xAA9900FF, Mode.SRC_ATOP);
		_back.setOnClickListener(_backListener);
		_next.setOnClickListener(_nextListener);
		_finish.setOnClickListener(_finishListener);
		changeSlide(_slide);
	}

	private android.view.View.OnClickListener _backListener = new android.view.View.OnClickListener() {
		@Override
		public void onClick(View v) {
			changeSlide(--_slide); // back
		}
	};

	private android.view.View.OnClickListener _nextListener = new android.view.View.OnClickListener() {
		@Override
		public void onClick(View v) {
			changeSlide(++_slide); // next
		}
	};

	private android.view.View.OnClickListener _finishListener = new android.view.View.OnClickListener() {
		@Override
		public void onClick(View v) {
			dismiss(); // finish
		}
	};

	private void changeSlide(int i) {
		if (i >= NUMBER_OF_SLIDES) {
			i = NUMBER_OF_SLIDES;
			_next.setVisibility(View.GONE);
			_back.setVisibility(View.VISIBLE);
			_finish.setVisibility(View.VISIBLE);
			_img.setVisibility(View.GONE);
			_scroll.setVisibility(View.VISIBLE);
		} else if (i <= 0) {
			i = 0;
			_back.setVisibility(View.GONE);
			_finish.setVisibility(View.GONE);
			_next.setVisibility(View.VISIBLE);
			_img.setVisibility(View.VISIBLE);
			_scroll.setVisibility(View.GONE);
		} else {
			_finish.setVisibility(View.GONE);
			_back.setVisibility(View.VISIBLE);
			_next.setVisibility(View.VISIBLE);
			_img.setVisibility(View.VISIBLE);
			_scroll.setVisibility(View.GONE);
		}

		switch (i) {
			case 0: {
				_text.setText(_t1);
				_img.setImageResource(R.drawable.tutorial_1);
				//_ui.showCompose();
				break;
			}
	
			case 1: {
				_text.setText(_t2);
				_img.setImageResource(R.drawable.tutorial_2);
				//_ui.showInbox();
				break;
			}
	
			case 2: {
				_text.setText(_t3);
				_img.setImageResource(R.drawable.tutorial_3);
				//_ui.showProfile();
				break;
			}
	
			case 3: {
				_text.setText(_t4);
				_img.setImageResource(R.drawable.tutorial_4);
				//_ui.showCompose();
				break;
			}
			
			case 4: {
				_text.setText(_t5);
				//_ui.showCompose();
				break;
			}
		}

	}
}
