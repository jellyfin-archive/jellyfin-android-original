package com.mesmotronic.plugins;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.graphics.Point;
import android.os.Build;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ActionMode.Callback;
import android.view.WindowManager.LayoutParams;
import android.view.accessibility.AccessibilityEvent;

public class FullScreenPlugin extends CordovaPlugin
{
	public static final String ACTION_IS_SUPPORTED = "isSupported";
	public static final String ACTION_IS_IMMERSIVE_MODE_SUPPORTED = "isImmersiveModeSupported";
	public static final String ACTION_IMMERSIVE_WIDTH = "immersiveWidth";
	public static final String ACTION_IMMERSIVE_HEIGHT = "immersiveHeight";
	public static final String ACTION_LEAN_MODE = "leanMode";
	public static final String ACTION_SHOW_SYSTEM_UI = "showSystemUI";
	public static final String ACTION_SHOW_UNDER_SYSTEM_UI = "showUnderSystemUI";
	public static final String ACTION_IMMERSIVE_MODE = "immersiveMode";
	
	private static Window.Callback _windowCallback;
	
	private CallbackContext context;
	private Activity activity;
	private Window window;
	private View decorView;
	
	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException
	{
		context = callbackContext;
		activity = cordova.getActivity();
		window = activity.getWindow();
		decorView = window.getDecorView();
		
		if (ACTION_IS_SUPPORTED.equals(action))
			return isSupported();
		else if (ACTION_IS_IMMERSIVE_MODE_SUPPORTED.equals(action))
			return isImmersiveModeSupported();
		else if (ACTION_IMMERSIVE_WIDTH.equals(action))
			return immersiveWidth();
		else if (ACTION_IMMERSIVE_HEIGHT.equals(action))
			return immersiveHeight();
		else if (ACTION_LEAN_MODE.equals(action))
			return leanMode();
		else if (ACTION_SHOW_SYSTEM_UI.equals(action))
			return showSystemUI();
		else if (ACTION_SHOW_UNDER_SYSTEM_UI.equals(action))
			return showUnderSystemUI();
		else if (ACTION_IMMERSIVE_MODE.equals(action))
			return immersiveMode();
		
		return false;
	}
	
	protected Window.Callback getWindowCallback()
	{
		if (_windowCallback == null)
		{
			_windowCallback = window.getCallback();
		}
		
		return _windowCallback;
	}
	
	protected void resetWindow()
	{
		decorView.setOnFocusChangeListener(null); 
		decorView.setOnSystemUiVisibilityChangeListener(null);
		
		window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		window.setCallback(getWindowCallback());
	}
	
	/**
	 * Are any of the features of this plugin supported?
	 */
	protected boolean isSupported()
	{
		boolean supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
		
        PluginResult res = new PluginResult(PluginResult.Status.OK, supported);
        context.sendPluginResult(res);
		return true;
	}
	
	/**
	 * Is immersive mode supported?
	 */
	protected boolean isImmersiveModeSupported()
	{
		boolean supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
		
        PluginResult res = new PluginResult(PluginResult.Status.OK, supported);
        context.sendPluginResult(res);
		return true;
	}
	
	/**
	 * The width of the screen in immersive mode
	 */
	protected boolean immersiveWidth()
	{
		activity.runOnUiThread(new Runnable()
		{
			@Override
			public void run() 
			{
				try
				{
					Point outSize = new Point();
					
					decorView.getDisplay().getRealSize(outSize);
					
			        PluginResult res = new PluginResult(PluginResult.Status.OK, outSize.x);
			        context.sendPluginResult(res);
				}
				catch (Exception e)
				{
					context.error(e.getMessage());
				}
			}
		});
		
		return true;
	}
	
	/**
	 * The height of the screen in immersive mode
	 */	
	protected boolean immersiveHeight()
	{
		activity.runOnUiThread(new Runnable()
		{
			@Override
			public void run() 
			{
				try
				{
					Point outSize = new Point();
					
					decorView.getDisplay().getRealSize(outSize);
					
			        PluginResult res = new PluginResult(PluginResult.Status.OK, outSize.y);
			        context.sendPluginResult(res);
				}
				catch (Exception e)
				{
					context.error(e.getMessage());
				}
			}
		});
        
		return true;
	}
	
	/**
	 * Hide system UI until user interacts
	 */
	protected boolean leanMode()
	{
		if (!isSupported())
		{
			context.error("Not supported");
			return false;
		}
		
		activity.runOnUiThread(new Runnable()
		{
			@Override
			public void run() 
			{
				try
				{
					resetWindow();
					
					int uiOptions = 
						View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_FULLSCREEN;
					
					decorView.setOnSystemUiVisibilityChangeListener(null);
					decorView.setSystemUiVisibility(uiOptions);
					
					context.success();
				}
				catch (Exception e)
				{
					context.error(e.getMessage());
				}
			}
		});
		
		return true;
	}
	
	/**
	 * Show system UI
	 */
	protected boolean showSystemUI()
	{
		if (!isSupported())
		{
			context.error("Not supported");
			return false;
		}
		
		activity.runOnUiThread(new Runnable()
		{
			@Override
			public void run() 
			{
				try
				{
					resetWindow();
			        
					// Remove translucent theme from bars
					
					window.clearFlags
					(
						WindowManager.LayoutParams.FLAG_FULLSCREEN 
						| WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION 
						| WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
					);
					
			        // Update system UI
					
					decorView.setOnSystemUiVisibilityChangeListener(null);
					decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
					
					PluginResult res = new PluginResult(PluginResult.Status.OK, true);
			        context.sendPluginResult(res);
					
					context.success();
				}
				catch (Exception e)
				{
					context.error(e.getMessage());
				}
			}
		});			
		
		return true;
	}
	
	/**
	 * Extend your app underneath the system UI (Android 4.4+ only)
	 */
	protected boolean showUnderSystemUI()
	{
		if (!isSupported())
		{
			context.error("Not supported");
			return false;
		}
		
		activity.runOnUiThread(new Runnable()
		{
			@Override
			public void run() 
			{
				try
				{
					resetWindow();
					
					// Make the status and nav bars translucent
					
			        window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
			        window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			        
			        // Extend view underneath status and nav bars
					
					int uiOptions = 
						View.SYSTEM_UI_FLAG_LAYOUT_STABLE
						| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
						//| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
					
					decorView.setSystemUiVisibility(uiOptions);
					
					context.success();
				}
				catch (Exception e)
				{
					context.error(e.getMessage());
				}
			}
		});
		
		return true;
	}
	
	/**
	 * Hide system UI and switch to immersive mode (Android 4.4+ only)
	 */
	protected boolean immersiveMode()
	{
		if (!isImmersiveModeSupported())
		{
			context.error("Not supported");
			return false;
		}
		
		activity.runOnUiThread(new Runnable()
		{
			@Override
			public void run() 
			{
				try
				{
					resetWindow();
					
					final int uiOptions = 
						View.SYSTEM_UI_FLAG_LAYOUT_STABLE
						| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
						| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_FULLSCREEN
						| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
					
					window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
					decorView.setSystemUiVisibility(uiOptions);
					
					decorView.setOnFocusChangeListener(new View.OnFocusChangeListener() 
					{
						@Override
						public void onFocusChange(View v, boolean hasFocus) 
						{
							if (hasFocus)
							{
								decorView.setSystemUiVisibility(uiOptions);
							}
						}
					});
					
					decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener()
					{
						@Override
						public void onSystemUiVisibilityChange(int visibility) 
						{
							decorView.setSystemUiVisibility(uiOptions);
						}
					});					
					
					final Window.Callback windowCallback = getWindowCallback();
					
					window.setCallback(new Window.Callback()
					{
						@Override
						public ActionMode onWindowStartingActionMode(Callback callback) 
						{
							return windowCallback.onWindowStartingActionMode(callback);
						}
						
						@Override
						public void onWindowFocusChanged(boolean hasFocus) 
						{
							if (hasFocus)
							{
								decorView.setSystemUiVisibility(uiOptions);
							}
							
							windowCallback.onWindowFocusChanged(hasFocus);
						}
						
						@Override
						public void onWindowAttributesChanged(LayoutParams attrs) 
						{
							windowCallback.onWindowAttributesChanged(attrs);
						}
						
						@Override
						public boolean onSearchRequested() 
						{
							return windowCallback.onSearchRequested();
						}
						
						@Override
						public boolean onPreparePanel(int featureId, View view, Menu menu) 
						{
							return windowCallback.onPreparePanel(featureId, view, menu);
						}
						
						@Override
						public void onPanelClosed(int featureId, Menu menu)
						{
							windowCallback.onPanelClosed(featureId, menu);
						}
						
						@Override
						public boolean onMenuOpened(int featureId, Menu menu) 
						{
							return windowCallback.onMenuOpened(featureId, menu);
						}
						
						@Override
						public boolean onMenuItemSelected(int featureId, MenuItem item) 
						{
							return windowCallback.onMenuItemSelected(featureId, item);
						}
						
						@Override
						public void onDetachedFromWindow() 
						{
							windowCallback.onDetachedFromWindow();
						}
						
						@Override
						public View onCreatePanelView(int featureId) 
						{
							return windowCallback.onCreatePanelView(featureId);
						}
						
						@Override
						public boolean onCreatePanelMenu(int featureId, Menu menu) 
						{
							return windowCallback.onCreatePanelMenu(featureId, menu);
						}
						
						@Override
						public void onContentChanged()
						{
							windowCallback.onContentChanged();
						}
						
						@Override
						public void onAttachedToWindow() 
						{
							windowCallback.onAttachedToWindow();
						}
						
						@Override
						public void onActionModeStarted(ActionMode mode)
						{
							windowCallback.onActionModeStarted(mode);
						}
						
						@Override
						public void onActionModeFinished(ActionMode mode) 
						{
							windowCallback.onActionModeFinished(mode);
						}
						
						@Override
						public boolean dispatchTrackballEvent(MotionEvent event) 
						{
							return windowCallback.dispatchTrackballEvent(event);
						}
						
						@Override
						public boolean dispatchTouchEvent(MotionEvent event) 
						{
							return windowCallback.dispatchTouchEvent(event);
						}
						
						@Override
						public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) 
						{
							return windowCallback.dispatchPopulateAccessibilityEvent(event);
						}
						
						@Override
						public boolean dispatchKeyShortcutEvent(KeyEvent event) 
						{
							return windowCallback.dispatchKeyShortcutEvent(event);
						}
						
						@Override
						public boolean dispatchKeyEvent(KeyEvent event) 
						{
							return windowCallback.dispatchKeyEvent(event);
						}
						
						@Override
						public boolean dispatchGenericMotionEvent(MotionEvent event) 
						{
							return windowCallback.dispatchGenericMotionEvent(event);
						}
					});
					
					context.success();
				}
				catch (Exception e)
				{
					context.error(e.getMessage());
				}
			}
		});
			
		return true;
	}
	
}