package org.jellyfin.mobile.exoPlayer;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import org.jellyfin.mobile.R;

public class ExoPlayerActivity extends Activity {

    private SimpleExoPlayer player = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //make fullscreen player
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // set player view layout
        setContentView(R.layout.exo_player);

        PlayerView playerView = (PlayerView) findViewById(R.id.exoPlayer);

        player = ExoPlayerFactory.newSimpleInstance(getApplicationContext());

        playerView.setPlayer(player);

        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "Jellyfin android"));

        Intent intent = getIntent();
        String uri = intent.getStringExtra("uri");

        MediaSource mediasource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(uri));

        player.prepare(mediasource);

        player.setPlayWhenReady(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.release();
    }
}
