package com.mb.android.webviews;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;

import com.mb.android.logging.AppLogger;

import org.apache.cordova.LOG;
import org.apache.cordova.engine.SystemWebChromeClient;
import org.apache.cordova.engine.SystemWebViewEngine;

import mediabrowser.model.logging.ILogger;

/**
 * Created by Luke on 12/20/2015.
 */
public class MySystemWebChromeClient extends SystemWebChromeClient {

    private ILogger logger;
    private static final String LOG_TAG = "SystemWebChromeClient";

    private final Activity activity;

    public MySystemWebChromeClient(SystemWebViewEngine parentEngine, ILogger logger, Activity activity) {
        super(parentEngine);
        this.logger = logger;
        this.activity = activity;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onConsoleMessage(String message, int lineNumber, String sourceID)
    {
        //This is only for Android 2.1
        if(android.os.Build.VERSION.SDK_INT == android.os.Build.VERSION_CODES.ECLAIR_MR1)
        {
            LOG.d(LOG_TAG, "%s: Line %d : %s", sourceID, lineNumber, message);
            logger.Info("%s: Line %d : %s", sourceID, lineNumber, message);
            super.onConsoleMessage(message, lineNumber, sourceID);
        }
    }

    @TargetApi(8)
    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage)
    {
        if (consoleMessage.message() != null) {

            logger.Info("%s: Line %s : %s", consoleMessage.sourceId(), consoleMessage.lineNumber(), consoleMessage.message());
        }

        return super.onConsoleMessage(consoleMessage);
    }

    @Override
    public void onShowCustomView(View view, CustomViewCallback callback) {

        //immersiveMode();
    }

    @Override
    public void onHideCustomView() {

        /*if (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT))
        {
            return;
        }

        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    resetImmersiveWindow();

                    // Remove translucent theme from bars

                    final Window window = activity.getWindow();
                    final View decorView = window.getDecorView();

                    window.clearFlags
                            (
                                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                                            | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
                                            | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                            );

                    // Update system UI

                    decorView.setOnSystemUiVisibilityChangeListener(null);
                    decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                }
                catch (Exception e)
                {

                }
            }
        });*/
    }

    /**
     * Hide system UI and switch to immersive mode (Android 4.4+ only)
     */
    public boolean immersiveMode()
    {
        if (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT))
        {
            return false;
        }

        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    resetImmersiveWindow();

                    final int uiOptions =
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

                    Window window = activity.getWindow();
                    final View decorView = window.getDecorView();

                    //window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
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
                }
                catch (Exception e)
                {

                }
            }
        });

        return true;
    }

    protected void resetImmersiveWindow()
    {
        Window window = activity.getWindow();
        View decorView = window.getDecorView();

        decorView.setOnFocusChangeListener(null);
        decorView.setOnSystemUiVisibilityChangeListener(null);

        window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
    }
}
