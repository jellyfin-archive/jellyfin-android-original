package org.jellyfin.mobile;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.jellyfin.mobile.R;

public class AppPreferences {
    private SharedPreferences sharedPreferences;
    private Context context;

    public AppPreferences(Context context) {
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.context = context;
    }

    public static AppPreferences get(Context context) {
        return new AppPreferences(context);
    }

    public Boolean getIgnoreBatteryOptimizations() {
        return sharedPreferences.getBoolean(context.getString(R.string.pref_ignore_battery_optimizations), false);
    }

    public void setIgnoreBatteryOptimizations(boolean value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(context.getString(R.string.pref_ignore_battery_optimizations), value);
        editor.apply();
    }
}
