package co.shoutbreak.core.utils;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import co.shoutbreak.R;
import co.shoutbreak.core.C;
import co.shoutbreak.core.Colleague;
import co.shoutbreak.core.Mediator;
import co.shoutbreak.core.Shout;
import co.shoutbreak.ui.Shoutbreak;

public class DialogBuilder implements Colleague {

	private static final String TAG = "DialogBuilder";

	public static final int DIALOG_SERVER_ANNOUNCEMENT = 0;
	public static final int DIALOG_SERVER_DOWNTIME = 1; // server error is the
																											// server giving us {code:
																											// error}
	public static final int DIALOG_SERVER_INVALID_RESPONSE = 2; // server giving
																															// garbage data
																															// back
	public static final int DIALOG_SERVER_HTTP_ERROR = 3; // server is giving http
																												// status errors

	public static final int DIALOG_WAIT_FOR_MAP_TO_HAVE_LOCATION = 4;
	public static final int DISMISS_DIALOG_WAIT_FOR_MAP_TO_HAVE_LOCATION = 5;

	private Mediator _m;
	private static ProgressDialog _waitForMapToHaveLocationDialog;
	private boolean _isDialogAlreadyShowing;
	private Shoutbreak _ui;

	private View _replyLayout;
	private EditText _replyEt;
	private ImageButton _replyBtn;
	private TextView _replyText;
	private AlertDialog _replyDialog;

	public DialogBuilder(Shoutbreak ui, Mediator m) {
		SBLog.constructor(TAG);
		_ui = ui;
		_m = m;
		_isDialogAlreadyShowing = false;

		LayoutInflater inflater = _ui.getLayoutInflater();
		_replyLayout = inflater.inflate(R.layout.reply_dialog, null);
		_replyEt = (EditText) _replyLayout.findViewById(R.id.replyInputEt);
		_replyBtn = (ImageButton) _replyLayout.findViewById(R.id.replyInputBtn);
		_replyText = (TextView) _replyLayout.findViewById(R.id.shoutTextTv);
	}

	@Override
	public void unsetMediator() {
		_m = null;
	}

	public void showScoreDetailsDialog(int ups, int downs, int score) {
		if (!_isDialogAlreadyShowing) {
			_isDialogAlreadyShowing = true;
			LayoutInflater inflater = _ui.getLayoutInflater();
			View dialogLayout = inflater.inflate(R.layout.score_dialog, null);
			TextView upsTv = (TextView) dialogLayout.findViewById(R.id.scoreDialogUpsTv);
			TextView downsTv = (TextView) dialogLayout.findViewById(R.id.scoreDialogDownsTv);
			TextView scoreTv = (TextView) dialogLayout.findViewById(R.id.scoreDialogScoreTv);
			upsTv.setText(Integer.toString(ups));
			downsTv.setText(Integer.toString(downs));
			scoreTv.setText(Integer.toString(score));
			AlertDialog.Builder builder = new AlertDialog.Builder(_ui);
			builder.setView(dialogLayout).setTitle("Score Details").setCancelable(false).setPositiveButton("Close", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					_isDialogAlreadyShowing = false;
					dialog.cancel();
				}
			});
			AlertDialog alert = builder.create();
			alert.show();
		}
	}

	public void handleReplySent() {
		Drawable d =  _replyBtn.getDrawable();
		if (d.getClass().equals(AnimationDrawable.class)) {			
			AnimationDrawable shoutButtonAnimation = (AnimationDrawable) _replyBtn.getDrawable();
			shoutButtonAnimation.stop();
			_replyBtn.setImageResource(R.drawable.shout_button_up);
			_replyEt.setText("");
			if (_replyDialog != null && _replyDialog.isShowing()) {
				_isDialogAlreadyShowing = false;
				_replyDialog.cancel();
			}
		} else {
			SBLog.error(TAG, "This should never happen.  Reply Button is not an animation.");
		}
	}

	// http://stackoverflow.com/questions/2150078/android-is-software-keyboard-shown
	public void hideReplyKeyboard(EditText et) {
		SBLog.method(TAG, "hideKeyboard()");
		InputMethodManager imm = (InputMethodManager) _m.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
	}
	
	public void showReplyKeyboard(EditText et) {
		SBLog.method(TAG, "hideKeyboard()");
		InputMethodManager imm = (InputMethodManager) _m.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
	}

	public void sendReply(Shout shout) {
		String text = _replyEt.getText().toString().trim();
		if (text.length() == 0) {
			_m.getUiGateway().toast("Cannot shout blanks.", Toast.LENGTH_LONG);
		} else {
			String signature = _m.getSignature();
			if (_m.getIsSignatureEnabled() && signature.length() > 0) {
				text += "     [" + signature + "]";
				text.trim();
			}
			if (text.length() <= C.CONFIG_SHOUT_MAXLENGTH) {
				_replyBtn.setImageResource(R.anim.shout_button_down);
				AnimationDrawable shoutButtonAnimation = (AnimationDrawable) _replyBtn.getDrawable();
				shoutButtonAnimation.start();
				_m.handleShoutStart(text.toString(), 0, shout.id);
				hideReplyKeyboard(_replyEt);
			} else {
				_m.getUiGateway().toast("Shout is too long (256 char limit).", Toast.LENGTH_LONG);
			}
		}
	}

	public void showReplyDialog(final Shout shout) {
		if (!_isDialogAlreadyShowing) {
			_isDialogAlreadyShowing = true;
			_replyText.setText(shout.text);
			OnClickListener onReplyInputClickListener = new OnClickListener() {
				public void onClick(View view) {
					sendReply(shout);
				}
			};
			_replyBtn.setOnClickListener(onReplyInputClickListener);
			AlertDialog.Builder builder = new AlertDialog.Builder(_ui);

			View parent = (View) _replyLayout.getParent();
			if (parent != null) {
				FrameLayout parentLayout = (FrameLayout) parent;
				parentLayout.removeAllViews();
			}
			
			builder.setView(_replyLayout).setTitle("Reply to All").setCancelable(false).setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					_isDialogAlreadyShowing = false;
					dialog.cancel();
				}
			});
			_replyDialog = builder.create();
			_replyDialog.show();
			_replyEt.requestFocus();
			showReplyKeyboard(_replyEt);
		}
	}

	public void showDialog(int whichDialog, String text) {

		String msg = "";
		switch (whichDialog) {

		case DIALOG_SERVER_INVALID_RESPONSE:
			msg = _ui.getString(R.string.serverInvalidResponse);
		case DIALOG_SERVER_HTTP_ERROR: {
			msg = _ui.getString(R.string.serverHttpError);
			if (!_isDialogAlreadyShowing) {
				_isDialogAlreadyShowing = true;
				AlertDialog.Builder builder = new AlertDialog.Builder(_ui);
				builder.setMessage(msg).setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// Intent intent = new
						// Intent(Settings.ACTION_SECURITY_SETTINGS);
						// _ui.startActivity(intent);
						// Intent myIntent = new
						// Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
						// _ui.startActivity(myIntent);
						_isDialogAlreadyShowing = false;
						Uri uri = Uri.parse(C.CONFIG_SUPPORT_ADDRESS);
						Intent intent = new Intent(Intent.ACTION_VIEW, uri);
						_ui.startActivity(intent);
						dialog.cancel();
					}
				}).setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						_isDialogAlreadyShowing = false;
						dialog.cancel();
					}
				});
				AlertDialog alert = builder.create();
				alert.show();
			}
			break;
		}

		case DIALOG_SERVER_ANNOUNCEMENT:
		case DIALOG_SERVER_DOWNTIME: {
			if (!_isDialogAlreadyShowing) {
				_isDialogAlreadyShowing = true;
				AlertDialog.Builder builder = new AlertDialog.Builder(_ui);
				builder.setMessage(text).setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						_isDialogAlreadyShowing = false;
						dialog.cancel();
					}
				});
				AlertDialog alert = builder.create();

				alert.show();
			}
			break;
		}

		case DIALOG_WAIT_FOR_MAP_TO_HAVE_LOCATION: {
			if (_waitForMapToHaveLocationDialog == null) {
				// Version without cancel button below:
				// _waitForMapToHaveLocationDialog = ProgressDialog.show(_ui, "", text);
				_waitForMapToHaveLocationDialog = new ProgressDialog(_ui);
				_waitForMapToHaveLocationDialog.setMessage(text);
				_waitForMapToHaveLocationDialog.setButton("I Don't Care", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// Use either finish() or return() to either close the activity or
						// just the dialog
						showDialog(DISMISS_DIALOG_WAIT_FOR_MAP_TO_HAVE_LOCATION, "");
						return;
					}
				});
				// We need dumb isFinishing check for Android bug.
				// http://vinnysoft.blogspot.com/2010/11/androidviewwindowmanagerbadtokenexcepti.html
				if (!_ui.isFinishing()) {
					_waitForMapToHaveLocationDialog.show();
				}
			}
			break;
		}

		case DISMISS_DIALOG_WAIT_FOR_MAP_TO_HAVE_LOCATION: {
			if (_waitForMapToHaveLocationDialog != null && !_ui.isFinishing()) {
				_waitForMapToHaveLocationDialog.dismiss();
				_waitForMapToHaveLocationDialog = null;
			}
			break;
		}

		default: {
			break;
		}

		}
	}
}
