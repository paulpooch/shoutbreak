package co.shoutbreak.core.utils;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import co.shoutbreak.core.C;
import co.shoutbreak.ui.Shoutbreak;

public class DialogBuilder {

	public static final int DIALOG_SERVER_DOWN = 0; // server down is unknown problem
	public static final int DIALOG_SERVER_ANNOUNCEMENT = 1;
	public static final int DIALOG_SERVER_ERROR = 2; // server error is the server giving us {code: error}
	
	public static final int DIALOG_WAIT_FOR_MAP_TO_HAVE_LOCATION = 3;
	public static final int DISMISS_DIALOG_WAIT_FOR_MAP_TO_HAVE_LOCATION = 4;
	
	private static ProgressDialog _waitForMapToHaveLocationDialog;
	private boolean _isDialogAlreadyShowing;
	private Shoutbreak _ui;

	public DialogBuilder(Shoutbreak ui) {
		_ui = ui;
		_isDialogAlreadyShowing = false;
	}

	public void showDialog(int whichDialog, String text) {

		switch (whichDialog) {
			case DIALOG_SERVER_DOWN: {
				if (!_isDialogAlreadyShowing) {
					_isDialogAlreadyShowing = true;
					AlertDialog.Builder builder = new AlertDialog.Builder(_ui);
					builder.setMessage(C.STRING_SERVER_DOWN)
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
			case DIALOG_SERVER_ERROR: {
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
					            DialogBuilder.this.showDialog(DISMISS_DIALOG_WAIT_FOR_MAP_TO_HAVE_LOCATION, "");
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
