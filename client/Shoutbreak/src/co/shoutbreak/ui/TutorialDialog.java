package co.shoutbreak.ui;

import co.shoutbreak.R;
import co.shoutbreak.core.utils.SBLog;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;

public class TutorialDialog extends Dialog {

	private static final String TAG = "TutorialDialog";
	
	private final int NUMBER_OF_SLIDES = 3;
	
	private Shoutbreak _ui;
	private TextView _text;
	private Button _back;
    private Button _next;
    private Button _finish;
    
    private int _slide;

    public TutorialDialog(Shoutbreak ui) {
        super(ui, R.style.popupStyle);
		SBLog.constructor(TAG);
        setContentView(R.layout.tutorial_dialog);
        _ui = ui;
        _slide = 0;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tutorial_dialog);
        _text = (TextView) findViewById(R.id.tutorialTextTv);
        _back = (Button) findViewById(R.id.tutorialBackBtn);
        _next = (Button) findViewById(R.id.tutorialNextBtn);
        _finish = (Button) findViewById(R.id.tutorialFinishBtn);
        _back.setOnClickListener(_backListener);
        _next.setOnClickListener(_nextListener);
        _finish.setOnClickListener(_finishListener);
        changeSlide(_slide);
    }
    
    private android.view.View.OnClickListener _backListener = new android.view.View.OnClickListener() {
			@Override
			public void onClick(View v) {
				changeSlide(--_slide);				// back
			}
    };
    
    private android.view.View.OnClickListener _nextListener = new android.view.View.OnClickListener() {
			@Override
			public void onClick(View v) {
				changeSlide(++_slide);				// next
			}
    };
    
    private android.view.View.OnClickListener _finishListener = new android.view.View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();		// finish
			}
    };
    
    private void changeSlide(int i) {
    	if (i >= NUMBER_OF_SLIDES) {
    		i = NUMBER_OF_SLIDES;
    		_next.setVisibility(View.GONE);
    		_finish.setVisibility(View.VISIBLE);
    	} else if (i <= 0) {
    		i = 0;
    		_back.setVisibility(View.GONE);
    		_finish.setVisibility(View.GONE);
    	} else {
    		_back.setVisibility(View.VISIBLE);
    		_next.setVisibility(View.VISIBLE);
    		_finish.setVisibility(View.GONE);
    	}
    	
    	switch(i) {
    		case 0: {
    			_text.setText("Tutorial");
    			_ui.showCompose();
    			break;
    		}
    		
    		case 1: {
    			_text.setText("show inbox");
    			_ui.showInbox();
    			break;
    		}
    		
    		case 2: {
    			_text.setText("show profile");
    			_ui.showProfile();
    			break;
    		}
    		
    		case 3: {
    			_text.setText("show shout");
    			_ui.showCompose();
    			break;
    		}
    	}
    	
    }
}
