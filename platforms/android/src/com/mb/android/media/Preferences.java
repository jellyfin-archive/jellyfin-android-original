package com.mb.android.media;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import org.json.JSONArray;
import org.json.JSONException;

public class Preferences {

    public static float[] getFloatArray(SharedPreferences pref, String key) {
        float[] array = null;
        String s = pref.getString(key, null);
        if (s != null) {
            try {
                JSONArray json = new JSONArray(s);
                array = new float[json.length()];
                for (int i = 0; i < array.length; i++)
                    array[i] = (float) json.getDouble(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return array;
    }

    public static void putFloatArray(Editor editor, String key, float[] array) {
        try {
            JSONArray json = new JSONArray();
            for (float f : array)
                json.put(f);
            editor.putString("equalizer_values", json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}