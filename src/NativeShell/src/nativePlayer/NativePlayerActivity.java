package org.jellyfin.mobile.nativePlayer;

import android.app.Activity;
import android.app.Application;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class NativePlayerActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Application app = getApplication();
        String packageName = app.getPackageName();
        Resources resources = app.getResources();

        //make fullscreen player
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // set player view layout using constraintLayout
        setContentView(resources.getIdentifier("native_player", "layout", packageName));
    }
}
