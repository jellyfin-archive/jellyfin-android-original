package com.mb.android.media.legacy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.KeyEvent;

import mediabrowser.apiinteraction.android.mediabrowser.Constants;

/**
 * Created by Luke on 6/23/2015.
 */
public class RemoteControlClientReceiver extends BroadcastReceiver {

    /* It should be safe to use static variables here once registered via the AudioManager */
    private static long mHeadsetDownTime = 0;
    private static long mHeadsetUpTime = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if(action.equalsIgnoreCase(Intent.ACTION_MEDIA_BUTTON)) {

            KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
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
                    long time = SystemClock.uptimeMillis();
                    switch (event.getAction())
                    {
                        case KeyEvent.ACTION_DOWN:
                            if (event.getRepeatCount() > 0)
                                break;
                            mHeadsetDownTime = time;
                            break;
                        case KeyEvent.ACTION_UP:
                            // long click
                            if (time - mHeadsetDownTime >= 1000) {
                                i = new Intent(Constants.ACTION_PREVIOUS);
                                time = 0;
                                // double click
                            } else if (time - mHeadsetUpTime <= 500) {
                                i = new Intent(Constants.ACTION_NEXT);
                            }
                            // one click
                            else {
                                i = new Intent(Constants.ACTION_PLAYPAUSE);
                            }
                            mHeadsetUpTime = time;
                            break;
                    }
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    i = new Intent(Constants.ACTION_PLAYPAUSE);
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    i = new Intent(Constants.ACTION_PLAY);
                    break;
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
        }
    }
}