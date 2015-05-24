package com.hutchind.cordova.plugins.launcher;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.Build;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class Launcher extends CordovaPlugin {
	public static final String TAG = "Launcher Plugin";
	public static final String ACTION_CAN_LAUNCH = "canLaunch";
	public static final String ACTION_LAUNCH = "launch";
	public static final int LAUNCH_REQUEST = 0;

	private CallbackContext callback;

	private abstract class LauncherRunnable implements Runnable {
		public CallbackContext callbackContext;
		LauncherRunnable(CallbackContext cb) {
			this.callbackContext = cb;
		}
	}

	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		callback = callbackContext;
		if (ACTION_CAN_LAUNCH.equals(action)) {
			return canLaunch(args);
		} else if (ACTION_LAUNCH.equals(action)) {
			return launch(args);
		}
		return false;
	}

	private boolean canLaunch(JSONArray args) throws JSONException {
		final JSONObject options = args.getJSONObject(0);
		final CordovaInterface mycordova = cordova;
		final CordovaPlugin plugin = this;

		if (options.has("packageName")) {
			final String appPackageName = options.getString("packageName");
			cordova.getThreadPool().execute(new LauncherRunnable(this.callback) {
				public void run() {
					final Intent intent = new Intent(Intent.ACTION_VIEW);
					String packageName = appPackageName;
					String passedActivityName = null;
					if (packageName.contains("/")) {
						String[] items = appPackageName.split("/");
						packageName = items[0];
						passedActivityName = items[1];
					}
					final ActivityInfo appInfo = getAppInfo(intent, packageName);

					if (appInfo != null) {
						Log.d(TAG, "App Info found for " + packageName);
						callbackContext.success();
					} else {
						final PackageManager pm = plugin.webView.getContext().getPackageManager();
						final Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
						if (launchIntent != null) {
							Log.d(TAG, "Launch Intent for " + packageName + " found.");
							callbackContext.success();
						} else {
							Log.d(TAG, "Could not find launch intent for package: " + packageName);
							callbackContext.error("Application is not installed.");
						}
					}

				}
			});
		} else if (options.has("uri")) {
			final String uri = options.getString("uri");
			final String dataType = options.has("dataType") ? options.getString("dataType") : null;

			cordova.getThreadPool().execute(new LauncherRunnable(this.callback) {
				public void run() {
					final PackageManager pm = plugin.webView.getContext().getPackageManager();
					final Intent intent = new Intent(Intent.ACTION_VIEW);
					if (dataType != null) {
						intent.setDataAndType(Uri.parse(uri), dataType);
					} else {
						intent.setData(Uri.parse(uri));
					}

					List<ResolveInfo> resInfos = pm.queryIntentActivities(intent, 0);
					if (resInfos.size() > 0) {
						Log.d(TAG, "Found Activities that handle uri: " + uri);

						boolean shouldGetAppList = false;
						try {
							shouldGetAppList = options.has("getAppList") && options.getBoolean("getAppList") == true;
						} catch (JSONException e) {}

						if (shouldGetAppList) {
							JSONObject obj = new JSONObject();
							JSONArray appList = new JSONArray();

							for(ResolveInfo resolveInfo : resInfos) {
								try {
									appList.put(resolveInfo.activityInfo.packageName);
								} catch (Exception e) {
									//Do Nothing
								}
							}

							try {
								obj.put("appList", wrap(appList));
							} catch(Exception e) {

							}
							callbackContext.success(obj);
						} else {
							callbackContext.success();
						}
					} else {
						Log.d(TAG, "No Activities found that handle uri: " + uri);
						callbackContext.error("No application found.");
					}
				}
			});
		}

		return true;
	}

	private ActivityInfo getAppInfo(final Intent intent, final String appPackageName) {
		final PackageManager pm = webView.getContext().getPackageManager();
		try {
			Log.d(TAG, pm.getApplicationInfo(appPackageName, 0) + "");
		}catch (NameNotFoundException e) {
			Log.i(TAG, "No info found for package: " + appPackageName);
		}
		return null;
	}

	private boolean launch(JSONArray args) throws JSONException {
		final JSONObject options = args.getJSONObject(0);
		Bundle extras = null;
		if (options.has("extras")) {
			extras = createExtras(options.getJSONArray("extras"));
		} else {
			extras = new Bundle();
		}
		if (options.has("uri") && (options.has("packageName") || options.has("dataType"))) {
			String dataType = null;
			String packageName = null;
			if (options.has("packageName")) {
				packageName = options.getString("packageName");
			}
			if (options.has("dataType")) {
				dataType = options.getString("dataType");
			}
			launchAppWithData(packageName, options.getString("uri"), dataType, extras);
			return true;
		} else if (options.has("packageName")) {
			launchApp(options.getString("packageName"), extras);
			return true;
		} else if (options.has("uri")) {
			launchIntent(options.getString("uri"), extras);
			return true;
		}
		return false;
	}

	private Bundle createExtras(JSONArray extrasObj) throws JSONException {
		Bundle extras = new Bundle();
		for (int i = 0, size = extrasObj.length(); i < size; i++) {
			JSONObject extra = extrasObj.getJSONObject(i);
			if (extra.has("name") && extra.has("value") && extra.has("dataType")) {
				String extraName = extra.getString("name");
				String dataType = extra.getString("dataType");
				try {
					if (dataType.equalsIgnoreCase("Byte")) {
						try {
							extras.putByte(extraName, ((byte) extra.getInt("value")));
						} catch (Exception e) {
							Log.e(TAG, "Error converting to byte for extra: " + extraName);
							e.printStackTrace();
							throw e;
						}
					} else if (dataType.equalsIgnoreCase("ByteArray")) {
						try {
							extras.putByteArray(extraName, ParseTypes.toByteArray(extra.getJSONArray("value")));
						} catch (Exception e) {
							Log.e(TAG, "Error converting to byte for extra: " + extraName);
							e.printStackTrace();
							throw e;
						}
					} else if (dataType.equalsIgnoreCase("Short")) {
						try {
							extras.putShort(extraName, ((short) extra.getInt("value")));
						} catch (Exception e) {
							Log.e(TAG, "Error converting to short for extra: " + extraName);
							e.printStackTrace();
							throw e;
						}
					} else if (dataType.equalsIgnoreCase("ShortArray")) {
						extras.putShortArray(extraName, ParseTypes.toShortArray(extra.getJSONArray("value")));
					} else if (dataType.equalsIgnoreCase("Int")) {
						extras.putInt(extraName, extra.getInt("value"));
					} else if (dataType.equalsIgnoreCase("IntArray")) {
						extras.putIntArray(extraName, ParseTypes.toIntArray(extra.getJSONArray("value")));
					} else if (dataType.equalsIgnoreCase("IntArrayList")) {
						extras.putIntegerArrayList(extraName, ParseTypes.toIntegerArrayList(extra.getJSONArray("value")));
					} else if (dataType.equalsIgnoreCase("Long")) {
						extras.putLong(extraName, extra.getLong("value"));
					} else if (dataType.equalsIgnoreCase("LongArray")) {
						extras.putLongArray(extraName, ParseTypes.toLongArray(extra.getJSONArray("value")));
					} else if (dataType.equalsIgnoreCase("Float")) {
						try {
							extras.putFloat(extraName, Float.parseFloat(extra.getString("value")));
						} catch (Exception e) {
							Log.e(TAG, "Error parsing float for extra: " + extraName);
							e.printStackTrace();
							throw e;
						}
					} else if (dataType.equalsIgnoreCase("FloatArray")) {
						try {
							extras.putFloatArray(extraName, ParseTypes.toFloatArray(extra.getJSONArray("value")));
						} catch (Exception e) {
							Log.e(TAG, "Error parsing float for extra: " + extraName);
							e.printStackTrace();
							throw e;
						}
					} else if (dataType.equalsIgnoreCase("Double")) {
						extras.putDouble(extraName, extra.getDouble("value"));
					} else if (dataType.equalsIgnoreCase("DoubleArray")) {
						extras.putDoubleArray(extraName, ParseTypes.toDoubleArray(extra.getJSONArray("value")));
					} else if (dataType.equalsIgnoreCase("Boolean")) {
						extras.putBoolean(extraName, extra.getBoolean("value"));
					} else if (dataType.equalsIgnoreCase("BooleanArray")) {
						extras.putBooleanArray(extraName, ParseTypes.toBooleanArray(extra.getJSONArray("value")));
					} else if (dataType.equalsIgnoreCase("String")) {
						extras.putString(extraName, extra.getString("value"));
					} else if (dataType.equalsIgnoreCase("StringArray")) {
						extras.putStringArray(extraName, ParseTypes.toStringArray(extra.getJSONArray("value")));
					} else if (dataType.equalsIgnoreCase("StringArrayList")) {
						extras.putStringArrayList(extraName, ParseTypes.toStringArrayList(extra.getJSONArray("value")));
					} else if (dataType.equalsIgnoreCase("Char")) {
						extras.putChar(extraName, ParseTypes.toChar(extra.getString("value")));
					} else if (dataType.equalsIgnoreCase("CharArray")) {
						extras.putCharArray(extraName, ParseTypes.toCharArray(extra.getString("value")));
					} else if (dataType.equalsIgnoreCase("CharSequence")) {
						extras.putCharSequence(extraName, extra.getString("value"));
					} else if (dataType.equalsIgnoreCase("CharSequenceArray")) {
						extras.putCharSequenceArray(extraName, ParseTypes.toCharSequenceArray(extra.getJSONArray("value")));
					} else if (dataType.equalsIgnoreCase("CharSequenceArrayList")) {
						extras.putCharSequenceArrayList(extraName, ParseTypes.toCharSequenceArrayList(extra.getJSONArray("value")));
					/*
					} else if (dataType.equalsIgnoreCase("Size") && Build.VERSION.SDK_INT >= 21) {
						extras.putSize(extraName, extra.getJSONObject("value"));
					} else if (dataType.equalsIgnoreCase("SizeF") && Build.VERSION.SDK_INT >= 21) {
						extras.putSizeF(extraName, extra.getJSONObject("value"));
					*/
					} else if (dataType.toLowerCase().contains("parcelable")) {
						if (!extra.has("paType")) {
							Log.e(TAG, "Property 'paType' must be provided if dataType is " + dataType + ".");
							throw new Exception("Missing property paType.");
						} else {
							String paType = extra.getString("paType").toUpperCase();
							if (ParseTypes.SUPPORTED_PA_TYPES.contains(paType)) {
								if (dataType.equalsIgnoreCase("Parcelable")) {
									extras.putParcelable(extraName, ParseTypes.toParcelable(extra.getString("value"), paType));
								} else if (dataType.equalsIgnoreCase("ParcelableArray")) {
									extras.putParcelableArray(extraName, ParseTypes.toParcelableArray(extra.getJSONArray("value"), paType));
								} else if (dataType.equalsIgnoreCase("ParcelableArrayList")) {
									extras.putParcelableArrayList(extraName, ParseTypes.toParcelableArrayList(extra.getJSONArray("value"), paType));
								} else if (dataType.equalsIgnoreCase("SparseParcelableArray")) {
									extras.putSparseParcelableArray(extraName, ParseTypes.toSparseParcelableArray(extra.getJSONObject("value"), paType));
								}
							} else {
								Log.e(TAG, "ParcelableArray type '" + paType + "' is not currently supported.");
								throw new Exception("Provided parcelable array type not supported.");
							}
						}
					}
				} catch (Exception e) {
					Log.e(TAG, "Error processing extra. Skipping: " + extraName);
				}
			} else {
				Log.e(TAG, "Extras must have a name, value, and datatype.");
			}
		}

		Log.d(TAG, "EXTRAS");
		Log.d(TAG, "" + extras);

		return extras;
	}

	private void launchAppWithData(final String packageName, final String uri, final String dataType, final Bundle extras) throws JSONException {
		final CordovaInterface mycordova = cordova;
		final CordovaPlugin plugin = this;
		final CallbackContext callbackContext = this.callback;
		cordova.getThreadPool().execute(new LauncherRunnable(this.callback) {
			public void run() {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				if (dataType != null) {
					intent.setDataAndType(Uri.parse(uri), dataType);
				} else {
					intent.setData(Uri.parse(uri));
				}

				if (packageName != null && !packageName.equals("")) {
					intent.setPackage(packageName);
				}

				intent.putExtras(extras);

				try {
					mycordova.startActivityForResult(plugin, intent, LAUNCH_REQUEST);
					((Launcher) plugin).callbackLaunched();
				} catch(ActivityNotFoundException e) {
					Log.e(TAG, "Error: No applications installed that can handle uri " + uri);
					e.printStackTrace();
					callbackContext.error("Application not found for uri.");
				}

			}
		});
	}

	private void launchApp(final String packageName, final Bundle extras) {
		final CordovaInterface mycordova = cordova;
		final CordovaPlugin plugin = this;
		Log.i(TAG, "Trying to launch app: " + packageName);
		cordova.getThreadPool().execute(new LauncherRunnable(this.callback) {
			public void run() {
				final PackageManager pm = plugin.webView.getContext().getPackageManager();
				final Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
				boolean appNotFound = launchIntent == null;

				if (!appNotFound) {
					try {
						launchIntent.putExtras(extras);
						mycordova.startActivityForResult(plugin, launchIntent, LAUNCH_REQUEST);
						((Launcher) plugin).callbackLaunched();
					} catch (ActivityNotFoundException e) {
						Log.e(TAG, "Error: Activity for package" + packageName + " was not found.");
						e.printStackTrace();
						callbackContext.error("Activity not found for package name.");
					}
				} else {
					callbackContext.error("Activity not found for package name.");
				}
			}
		});
	}

	private void launchIntent(final String uri, final Bundle extras) {
		final CordovaInterface mycordova = cordova;
		final CordovaPlugin plugin = this;
		cordova.getThreadPool().execute(new LauncherRunnable(this.callback) {
			public void run() {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(uri));
				try {
					intent.putExtras(extras);
					mycordova.startActivityForResult(plugin, intent, LAUNCH_REQUEST);
					((Launcher) plugin).callbackLaunched();
				} catch (ActivityNotFoundException e) {
					Log.e(TAG, "Error: Activity for " + uri + " was not found.");
					e.printStackTrace();
					callbackContext.error("Activity not found for uri.");
				}
			}
		});
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		if (requestCode == LAUNCH_REQUEST) {
			if (resultCode == Activity.RESULT_OK || resultCode == Activity.RESULT_CANCELED) {
				JSONObject json = new JSONObject();
				try {
					json.put("isActivityDone", true);
				} catch(JSONException ignored) {}
				if (intent != null) {
					Bundle extras = intent.getExtras();
					if (extras != null) {
						JSONObject jsonExtras = new JSONObject();
						Set<String> keys = extras.keySet();
						for (String key : keys) {
							try {
								jsonExtras.put(key, wrap(extras.get(key)));
							} catch(JSONException ignored) {}
						}
						try {
							json.put("extras", jsonExtras);
						} catch(JSONException ignored) {}
					}

					try {
						json.put("data", intent.getDataString());
					} catch(JSONException ignored) {}
				}
				callback.success(json);
			} else {
				callback.error("Activity failed (" + resultCode + ").");
			}
		}
	}

	public void callbackLaunched() {
		try {
			JSONObject json = new JSONObject();
			json.put("isLaunched", true);
			PluginResult result = new PluginResult(PluginResult.Status.OK, json);
			result.setKeepCallback(true);
			callback.sendPluginResult(result);
		} catch (JSONException e) {
			PluginResult result = new PluginResult(PluginResult.Status.OK, "{'isLaunched':true}");
			result.setKeepCallback(true);
			callback.sendPluginResult(result);
		}
	}

	private Object wrap(Object o) {
		if (o == null) {
			return JSONObject.NULL;
		}
		if (o instanceof JSONArray || o instanceof JSONObject) {
			return o;
		}
		if (o.equals(JSONObject.NULL)) {
			return o;
		}
		try {
			if (o instanceof Collection) {
				return new JSONArray((Collection) o);
			} else if (o.getClass().isArray()) {
				JSONArray jsa = new JSONArray();
				int length = Array.getLength(o);
				for (int i = 0; i < length; i += 1) {
					jsa.put(wrap(Array.get(o, i)));
				}
				return jsa;
			}
			if (o instanceof Map) {
				return new JSONObject((Map) o);
			}
			if (o instanceof Boolean ||
					o instanceof Byte ||
					o instanceof Character ||
					o instanceof Double ||
					o instanceof Float ||
					o instanceof Integer ||
					o instanceof Long ||
					o instanceof Short ||
					o instanceof String) {
				return o;
			}
			if (o.getClass().getPackage().getName().startsWith("java.")) {
				return o.toString();
			}
		} catch (Exception ignored) {
		}
		return null;
	}
}