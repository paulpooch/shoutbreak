package com.shoutbreak.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.provider.Settings;

public class DialogBuilder {
	
	public static final int DIALOG_LOCATION_DISABLED = 0;
	private Shoutbreak _ui;
	
	public DialogBuilder(Shoutbreak ui) {
		_ui = ui;
	}

	public void showDialog(int whichDialog) {
		
		switch (whichDialog) {
			case DIALOG_LOCATION_DISABLED: {
				AlertDialog.Builder builder = new AlertDialog.Builder(_ui);				
				builder.setMessage("You have location services disabled.\n\nEnable them now?")
				       .setCancelable(false)
				       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				        	  // Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
				              // _ui.startActivity(intent);
				        	   Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				               _ui.startActivity(myIntent);
				           }
				       })
				       .setNegativeButton("No", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				                dialog.cancel();
				           }
				       });
				AlertDialog alert = builder.create();
				alert.show();
				break;
			}
			default: {
				break;
			}
		}
	}
	
}