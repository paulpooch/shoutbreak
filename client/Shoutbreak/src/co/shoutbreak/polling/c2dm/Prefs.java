package co.shoutbreak.polling.c2dm;

import co.shoutbreak.core.C;
import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {
    public static SharedPreferences get(Context context) {
        return context.getSharedPreferences(C.PREFERENCE_FILE, Context.MODE_PRIVATE);
    }
}