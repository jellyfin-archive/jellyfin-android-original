package com.mb.android.media;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;
import com.mb.android.R;

/**
 * Created by Luke on 6/23/2015.
 */
public class MediaWidgetProvider extends AppWidgetProvider {

    public static final String START_FROM_NOTIFICATION = "from_notification";

    public static final String ACTION_WIDGET_INIT = "com.mb.android.widget.INIT";
    public static final String ACTION_WIDGET_UPDATE = "com.mb.android.widget.UPDATE";
    public static final String ACTION_WIDGET_UPDATE_COVER = "com.mb.android.widget.UPDATE_COVER";
    public static final String ACTION_WIDGET_UPDATE_POSITION = "com.mb.android.widget.UPDATE_POSITION";

    public static final String ACTION_WIDGET_PREFIX = "com.mb.android.widget.";

    public static final String APP_PACKAGE = "com.mb.android";
    public static final String APP_MAIN = "com.mb.android.MainActivity";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        /* init widget */
        Intent i = new Intent(ACTION_WIDGET_INIT);
        onReceive(context, i);

        /* ask a refresh from the service if there is one */
        i = new Intent(ACTION_WIDGET_INIT);
        i.setPackage(APP_PACKAGE);
        context.sendBroadcast(i);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!action.startsWith(ACTION_WIDGET_PREFIX)) {
            super.onReceive(context, intent);
            return;
        }

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.embyminiplayerwidget);
        boolean partial = true;

        if (ACTION_WIDGET_INIT.equals(action) || !partial) {
            /* commands */
            Intent iBackward = new Intent(Constants.ACTION_PREVIOUS);
            Intent iPlay = new Intent(Constants.ACTION_PLAYPAUSE);
            Intent iStop = new Intent(Constants.ACTION_STOP);
            Intent iForward = new Intent(Constants.ACTION_NEXT);
            Intent iappIntent = new Intent();
            iappIntent.setClassName(APP_PACKAGE, APP_MAIN);
            iappIntent.putExtra(START_FROM_NOTIFICATION, true);

            PendingIntent piBackward = PendingIntent.getBroadcast(context, 0, iBackward, PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent piPlay = PendingIntent.getBroadcast(context, 0, iPlay, PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent piStop = PendingIntent.getBroadcast(context, 0, iStop, PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent piForward = PendingIntent.getBroadcast(context, 0, iForward, PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent piAppIntent = PendingIntent.getActivity(context, 0, iappIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            views.setOnClickPendingIntent(R.id.backward, piBackward);
            views.setOnClickPendingIntent(R.id.play_pause, piPlay);
            views.setOnClickPendingIntent(R.id.stop, piStop);
            views.setOnClickPendingIntent(R.id.forward, piForward);
            views.setOnClickPendingIntent(R.id.cover, piAppIntent);
            partial = false;
        }

        if (ACTION_WIDGET_UPDATE.equals(action)) {
            String title = intent.getStringExtra("title");
            String artist = intent.getStringExtra("artist");
            boolean isplaying = intent.getBooleanExtra("isplaying", false);

            views.setTextViewText(R.id.songName, title);
            views.setTextViewText(R.id.artist, artist);
            views.setImageViewResource(R.id.play_pause, isplaying ? R.drawable.ic_pause_w : R.drawable.ic_play_w);
            views.setViewVisibility(R.id.timeline_parent, artist != null && artist.length() > 0 ? View.VISIBLE : View.INVISIBLE);
        }
        else if (ACTION_WIDGET_UPDATE_COVER.equals(action)) {
            Bitmap cover = intent.getParcelableExtra("cover");
            if (cover != null)
                views.setImageViewBitmap(R.id.cover, cover);
            else
                views.setImageViewResource(R.id.cover, R.drawable.icon);
            views.setProgressBar(R.id.timeline, 100, 0, false);
        }
        else if (ACTION_WIDGET_UPDATE_POSITION.equals(action)) {
            float pos = intent.getFloatExtra("position", 0f);
            views.setProgressBar(R.id.timeline, 100, (int) (100 * pos), false);
        }

        ComponentName widget = new ComponentName(context, MediaWidgetProvider.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        if (partial)
            manager.partiallyUpdateAppWidget(manager.getAppWidgetIds(widget), views);
        else
            manager.updateAppWidget(widget, views);
    }
}