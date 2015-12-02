package com.mb.android.media.legacy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.KeyEvent;

import mediabrowser.apiinteraction.android.mediabrowser.Constants;

/**
 * Small class to receive events passed out by the remote controls (wired, bluetooth, lock screen, ...)
 */
public class RemoteControlClientReceiver extends BroadcastReceiver {
    @SuppressWarnings("unused")
    private static final String TAG = "VLC/RemoteControlClientReceiver";

    /* It should be safe to use static variables here once registered via the AudioManager */
    private static long mHeadsetDownTime = 0;
    private static long mHeadsetUpTime = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if(action.equalsIgnoreCase(Intent.ACTION_MEDIA_BUTTON)) {

            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event == null)
                return;

            if (event.getKeyCode() != KeyEvent.KEYCODE_HEADSETHOOK &&
                    event.getKeyCode() != KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE &&
                    event.getAction() != KeyEvent.ACTION_DOWN)
                return;

            Intent i = null;
            switch (event.getKeyCode())
            {
            /*
             * one click => play/pause
             * long click => previous
             * double click => next
             */
                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    long time = SystemClock.uptimeMillis();
                    switch (event.getAction()) {
                        case KeyEvent.ACTION_DOWN:
                            if (event.getRepeatCount() <= 0)
                                mHeadsetDownTime = time;
                            break;
                        case KeyEvent.ACTION_UP:
                            if (time - mHeadsetDownTime >= 1000) { // long click
                                i = new Intent(Constants.ACTION_PREVIOUS);
                                time = 0;
                                break;
                            } else if (time - mHeadsetUpTime <= 500) { // double click
                                i = new Intent(Constants.ACTION_NEXT);
                                break;
                            }
                            else {
                                // one click
                                i = new Intent(Constants.ACTION_PLAYPAUSE);
                            }
                            mHeadsetUpTime = time;
                            break;
                    }
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    //i = new Intent(context, PlaybackService.class);
                    //i.setAction(Constants.ACTION_PLAY);
                    //context.startService(i);
                    return;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    i = new Intent(Constants.ACTION_PAUSE);
                    break;
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    i = new Intent(Constants.ACTION_STOP);
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    i = new Intent(Constants.ACTION_NEXT);
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    i = new Intent(Constants.ACTION_PREVIOUS);
                    break;
            }

            if (isOrderedBroadcast())
                abortBroadcast();
            if(i != null)
                context.sendBroadcast(i);
        } else if (action.equals(Constants.ACTION_PLAYPAUSE)){
            //intent = new Intent(context, PlaybackService.class);
            //intent.setAction(PlaybackService.ACTION_REMOTE_PLAYPAUSE);
            //context.startService(intent);
        }
    }
}