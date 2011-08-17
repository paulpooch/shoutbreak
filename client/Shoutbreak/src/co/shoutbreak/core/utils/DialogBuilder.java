package co.shoutbreak.core.utils;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import co.shoutbreak.core.C;
import co.shoutbreak.ui.Shoutbreak;

public class DialogBuilder {

	public static final int DIALOG_SERVER_DOWN = 0;
	private boolean _isServerDownDialogAlreadyShowing;
	private Shoutbreak _ui;

	public DialogBuilder(Shoutbreak ui) {
		_ui = ui;
		_isServerDownDialogAlreadyShowing = false;
	}

	public void showDialog(int whichDialog) {

		switch (whichDialog) {
			case DIALOG_SERVER_DOWN: {
				if (!_isServerDownDialogAlreadyShowing) {
					_isServerDownDialogAlreadyShowing = true;
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
									_isServerDownDialogAlreadyShowing = false;
									Uri uri = Uri.parse(C.CONFIG_SUPPORT_ADDRESS);
									Intent intent = new Intent(Intent.ACTION_VIEW, uri);
									_ui.startActivity(intent);
								}
							}).setNegativeButton("No", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									_isServerDownDialogAlreadyShowing = false;
									dialog.cancel();
								}
							});
					AlertDialog alert = builder.create();
					alert.show();
				}

				break;
			}
			default: {
				break;
			}
		}
	}
}
