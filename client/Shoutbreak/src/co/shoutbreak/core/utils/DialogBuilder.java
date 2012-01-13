package co.shoutbreak.core.utils;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import co.shoutbreak.R;
import co.shoutbreak.core.C;
import co.shoutbreak.ui.Shoutbreak;

public class DialogBuilder {

	private static final String TAG = "DialogBuilder";
	
	public static final int DIALOG_SERVER_ANNOUNCEMENT = 0;
	public static final int DIALOG_SERVER_DOWNTIME = 1; // server error is the server giving us {code: error}
	public static final int DIALOG_SERVER_INVALID_RESPONSE = 2; // server giving garbage data back
	public static final int DIALOG_SERVER_HTTP_ERROR = 3; // server is giving http status errors
	
	public static final int DIALOG_WAIT_FOR_MAP_TO_HAVE_LOCATION = 4;
	public static final int DISMISS_DIALOG_WAIT_FOR_MAP_TO_HAVE_LOCATION = 5;
	
	private static ProgressDialog _waitForMapToHaveLocationDialog;
	private boolean _isDialogAlreadyShowing;
	private Shoutbreak _ui;

	public DialogBuilder(Shoutbreak ui) {
    	SBLog.constructor(TAG);
		_ui = ui;
		_isDialogAlreadyShowing = false;
	}

	public void showScoreDetailsDialog(int ups, int downs, int score) {
		if (!_isDialogAlreadyShowing) {
			_isDialogAlreadyShowing = true;
			LayoutInflater inflater = _ui.getLayoutInflater();
			View dialogLayout = inflater.inflate(R.layout.score_dialog, null);
			TextView upsTv = (TextView)dialogLayout.findViewById(R.id.scoreDialogUpsTv);
			TextView downsTv = (TextView)dialogLayout.findViewById(R.id.scoreDialogDownsTv);
			TextView scoreTv = (TextView)dialogLayout.findViewById(R.id.scoreDialogScoreTv);
			upsTv.setText(Integer.toString(ups));
			downsTv.setText(Integer.toString(downs));
			scoreTv.setText(Integer.toString(score));
			AlertDialog.Builder builder = new AlertDialog.Builder(_ui);
			builder
				.setView(dialogLayout)
				.setTitle("Score Details")
				.setCancelable(false)
				.setPositiveButton("Close", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							_isDialogAlreadyShowing = false;
							dialog.cancel();
						}
				});							
			AlertDialog alert = builder.create();
			alert.show();
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
					builder.setMessage(msg)
							.setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
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
					builder.setMessage(text)
							.setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
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
					            // Use either finish() or return() to either close the activity or just the dialog
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
				if (_waitForMapToHaveLocationDialog != null) {
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
