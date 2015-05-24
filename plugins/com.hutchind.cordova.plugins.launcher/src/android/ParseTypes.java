package com.hutchind.cordova.plugins.launcher;

import android.net.Uri;
import android.os.Parcelable;
import android.util.SparseArray;
//import android.annotation.TargetApi;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import java.util.Iterator;

public class ParseTypes {
	public static final List<String> SUPPORTED_PA_TYPES = Collections.unmodifiableList(Arrays.asList("URI"));

	public static byte[] toByteArray(JSONArray arr) throws JSONException {
		int jsize = arr.length();
		byte[] exVal = new byte[jsize];
		for(int j=0; j < jsize; j++) {
			exVal[j] = (byte) arr.getInt(j);
		}
		return exVal;
	}
	public static short[] toShortArray(JSONArray arr) throws JSONException {
		int jsize = arr.length();
		short[] exVal = new short[jsize];
		for(int j=0; j < jsize; j++) {
			exVal[j] = (short) arr.getInt(j);
		}
		return exVal;
	}
	public static int[] toIntArray(JSONArray arr) throws JSONException {
		int jsize = arr.length();
		int[] exVal = new int[jsize];
		for(int j=0; j < jsize; j++) {
			exVal[j] = arr.getInt(j);
		}
		return exVal;
	}
	public static ArrayList<Integer> toIntegerArrayList(JSONArray arr) throws JSONException {
		int[] ints = ParseTypes.toIntArray(arr);
		ArrayList<Integer> exVal = new ArrayList<Integer>();
		for (int i = 0; i < ints.length; i++) {
			exVal.add(ints[i]);
		}
		return exVal;
	}
	public static long[] toLongArray(JSONArray arr) throws JSONException {
		int jsize = arr.length();
		long[] exVal = new long[jsize];
		for(int j=0; j < jsize; j++) {
			exVal[j] = arr.getLong(j);
		}
		return exVal;
	}
	public static float[] toFloatArray(JSONArray arr) throws JSONException, NumberFormatException {
		int jsize = arr.length();
		float[] exVal = new float[jsize];
		for(int j=0; j < jsize; j++) {
			exVal[j] = Float.parseFloat(arr.getString(j));
		}
		return exVal;
	}
	public static double[] toDoubleArray(JSONArray arr) throws JSONException {
		int jsize = arr.length();
		double[] exVal = new double[jsize];
		for(int j=0; j < jsize; j++) {
			exVal[j] = arr.getDouble(j);
		}
		return exVal;
	}
	public static boolean[] toBooleanArray(JSONArray arr) throws JSONException {
		int jsize = arr.length();
		boolean[] exVal = new boolean[jsize];
		for(int j=0; j < jsize; j++) {
			exVal[j] = arr.getBoolean(j);
		}
		return exVal;
	}
	public static String[] toStringArray(JSONArray arr) throws JSONException {
		int jsize = arr.length();
		String[] exVal = new String[jsize];
		for(int j=0; j < jsize; j++) {
			exVal[j] = arr.getString(j);
		}
		return exVal;
	}
	public static ArrayList<String> toStringArrayList(JSONArray arr) throws JSONException {
		String[] strs = ParseTypes.toStringArray(arr);
		ArrayList<String> exVal = new ArrayList<String>();
		for (int i = 0; i < strs.length; i++) {
			exVal.add(strs[i]);
		}
		return exVal;
	}
	public static char toChar(String str) {
		if (str == null) {
			return (char) 0;
		} else {
			return str.charAt(0);
		}
	}
	public static char[] toCharArray(String str) {
		if (str == null) {
			return new char[0];
		} else {
			return str.toCharArray();
		}
	}
	public static CharSequence[] toCharSequenceArray(JSONArray arr) throws JSONException {
		return ParseTypes.toStringArray(arr);
	}
	public static ArrayList<CharSequence> toCharSequenceArrayList(JSONArray arr) throws JSONException {
		CharSequence[] strs = ParseTypes.toCharSequenceArray(arr);
		ArrayList<CharSequence> exVal = new ArrayList<CharSequence>();
		for (int i = 0; i < strs.length; i++) {
			exVal.add(strs[i]);
		}
		return exVal;
	}
	/*@TargetApi(21)
	public static android.util.Size toSize(JSONObject o) throws JSONException {
		int width = 0, height = 0;
		try {
			width = o.getInt("width");
			height = o.getInt("height");
		} catch (JSONException e) {
			width = o.getInt("w");
			height = o.getInt("h");
		}
		return new android.util.Size(width, height);
	}
	@TargetApi(21)
	public static android.util.SizeF toSizeF(JSONObject o) throws JSONException {
		float width = 0, height = 0;
		try {
			width = Float.parseFloat(o.getString("width"));
			height = Float.parseFloat(o.getString("height"));
		} catch (JSONException e) {
			width = Float.parseFloat(o.getString("w"));
			height = Float.parseFloat(o.getString("h"));
		}
		return new android.util.SizeF(width, height);
	}*/
	public static SparseArray<? extends Parcelable> toSparseParcelableArray(JSONObject o, String pType) throws Exception, NullPointerException, JSONException {
		SparseArray<Parcelable> sa = new SparseArray<Parcelable>();
		for(Iterator<String> iter = o.keys(); iter.hasNext();) {
			String jsonKey = iter.next();
			Integer saKey = Integer.parseInt(jsonKey);
			sa.put(saKey, ParseTypes.toParcelable(o.getString(jsonKey), pType));
		}
		return sa;
	}
	public static Parcelable toParcelable(String str, String pType) throws Exception, NullPointerException {
		if (pType.equalsIgnoreCase("URI")) {
			return Uri.parse(str);
		}
		throw new Exception("Parcelable type " + pType + " is not supported.");
	}
	public static Parcelable[] toParcelableArray(JSONArray arr, String pType) throws Exception, NullPointerException, JSONException {
		ArrayList<Parcelable> items = (ArrayList<Parcelable>)ParseTypes.toParcelableArrayList(arr, pType);
		return items.toArray(new Parcelable[items.size()]);
	}
	public static ArrayList<? extends Parcelable> toParcelableArrayList(JSONArray arr, String pType) throws Exception, NullPointerException, JSONException {
		int jsize = arr.length();
		ArrayList<Parcelable> items = new ArrayList<Parcelable>();
		for(int j=0; j < jsize; j++) {
			items.add(ParseTypes.toParcelable(arr.getString(j), pType));
		}

		return items;
	}
}
