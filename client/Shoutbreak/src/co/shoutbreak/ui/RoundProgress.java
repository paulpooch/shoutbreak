package co.shoutbreak.ui;

import co.shoutbreak.R;
import co.shoutbreak.core.utils.SBLog;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.ClipDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class RoundProgress extends RelativeLayout {
	
	private static final String TAG = "RoundProgress";
	
	private ImageView progressDrawableImageView;
	private ImageView trackDrawableImageView;
	private double max = 100;

	public RoundProgress(Context context, AttributeSet attrs) {
		super(context, attrs);
		SBLog.constructor(TAG);
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.round_progress, RoundProgress.this);
		setup(context, attrs);
	}
	
	public int getMax() {
		Double d = new Double(max);
		return d.intValue();
	}

	public double getMaxDouble() {
		return max;
	}

	public void setMax(int max) {
		Integer maxInt = new Integer(max);
		maxInt.doubleValue();
		this.max = max;
	}

	public void setMax(double max) {
		this.max = max;
	}

	protected void setup(Context context, AttributeSet attrs) {
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RoundProgress);

		final String xmlns = "http://schemas.android.com/apk/res/co.shoutbreak";
		int bgResource = attrs.getAttributeResourceValue(xmlns, "progressDrawable", 0);
		progressDrawableImageView = (ImageView) findViewById(R.id.progress_drawable_image_view);
		progressDrawableImageView.setBackgroundResource(bgResource);

		int trackResource = attrs.getAttributeResourceValue(xmlns, "track", 0);
		trackDrawableImageView = (ImageView) findViewById(R.id.track_image_view);
		trackDrawableImageView.setBackgroundResource(trackResource);

		int progress = attrs.getAttributeIntValue(xmlns, "progress", 0);
		setProgress(progress);
		int max = attrs.getAttributeIntValue(xmlns, "max", 100);
		setMax(max);

		//int numTicks = attrs.getAttributeIntValue(xmlns, "numTicks", 0);

		a.recycle();
	}

	public void setProgress(Integer value) {
		setProgress((double) value);
	}

	public void setProgress(double value) {
		ClipDrawable drawable = (ClipDrawable) progressDrawableImageView.getBackground();
		double percent = (double) value / (double) max;
		int level = (int) Math.floor(percent * 10000);

		drawable.setLevel(level);
	}
}
