package co.shoutbreak.polling.c2dm;

import co.shoutbreak.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class C2DMReceiver extends C2DMBaseReceiver {
    private static final String TAG = "DataMessageReceiver";

    public C2DMReceiver() {
        super(DeviceRegistrar.SENDER_ID);
    }

    @Override
    public void onRegistrered(Context context, String registration) {
        DeviceRegistrar.registerWithServer(context, registration);
    }

    @Override
    public void onUnregistered(Context context) {
        SharedPreferences prefs = Prefs.get(context);
        String deviceRegistrationID = prefs.getString("deviceRegistrationID", null);
        DeviceRegistrar.unregisterWithServer(context, deviceRegistrationID);
    }

    @Override
    public void onError(Context context, String errorId) {
        context.sendBroadcast(new Intent("com.google.ctp.UPDATE_UI"));
    }

    @Override
    public void onMessage(Context context, Intent intent) {
       Bundle extras = intent.getExtras();
       if (extras != null) {
           String url = (String) extras.get("url");
           String title = (String) extras.get("title");
           if (url != null && title != null) {
               if (url.startsWith("http")) {
                   SharedPreferences settings = Prefs.get(context);
                   if (settings.getBoolean("launchBrowserOrMaps", false)) {
                       launchBrowserOrMaps(context, url, title);
                   } else {
                       generateNotification(context, url, title);
                   }
               } else {
                   Log.w(TAG, "Invalid URL: " + url);
               }
           }
       }
   }

   private void launchBrowserOrMaps(Context context, String url, String title) {
       final String GMM_PACKAGE_NAME = "com.google.android.apps.maps";
       final String GMM_CLASS_NAME = "com.google.android.maps.MapsActivity";
       boolean isMapsURL = url.startsWith("http://maps.google.");

       RingtoneManager.getRingtone(context,
               RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)).play();

       try {
           Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
           intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
           if (isMapsURL) {
               intent.setClassName(GMM_PACKAGE_NAME, GMM_CLASS_NAME);
           }
           context.startActivity(intent);
       } catch (ActivityNotFoundException e) {
           if (isMapsURL) {  // try again without GMM
               Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
               intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
               context.startActivity(intent);
           }
       }
   }

   private void generateNotification(Context context, String url, String title) {
       int icon = R.drawable.notification_icon;
       long when = System.currentTimeMillis();

       Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
       PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, 0);

       Notification notification = new Notification(icon, title, when);
       notification.setLatestEventInfo(context, title, url, contentIntent);
       notification.defaults = Notification.DEFAULT_SOUND;
       notification.flags |= Notification.FLAG_AUTO_CANCEL;

       SharedPreferences settings = Prefs.get(context);
       int notificatonID = settings.getInt("notificationID", 0); // allow multiple notifications

       NotificationManager nm =
               (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
       nm.notify(notificatonID, notification);

       SharedPreferences.Editor editor = settings.edit();
       editor.putInt("notificationID", ++notificatonID % 32);
       editor.commit();
   }
}
