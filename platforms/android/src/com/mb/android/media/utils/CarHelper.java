package com.mb.android.media.utils;

import android.os.Bundle;

public class CarHelper {

    private static final String AUTO_APP_PACKAGE_NAME = "com.google.android.projection.gearhead";

    // Use these extras to reserve space for the corresponding actions, even when they are disabled
    // in the playbackstate, so the custom actions don't reflow.
    private static final String SLOT_RESERVATION_SKIP_TO_NEXT =
            "com.google.android.gms.car.media.ALWAYS_RESERVE_SPACE_FOR.ACTION_SKIP_TO_NEXT";
    private static final String SLOT_RESERVATION_SKIP_TO_PREV =
            "com.google.android.gms.car.media.ALWAYS_RESERVE_SPACE_FOR.ACTION_SKIP_TO_PREVIOUS";
    private static final String SLOT_RESERVATION_QUEUE =
            "com.google.android.gms.car.media.ALWAYS_RESERVE_SPACE_FOR.ACTION_QUEUE";


    public static boolean isValidCarPackage(String packageName) {
        return AUTO_APP_PACKAGE_NAME.equals(packageName);
    }

    public static void setSlotReservationFlags(Bundle extras, boolean reservePlayingQueueSlot,
                                               boolean reserveSkipToNextSlot, boolean reserveSkipToPrevSlot) {
        if (reservePlayingQueueSlot) {
            extras.putBoolean(SLOT_RESERVATION_QUEUE, true);
        } else {
            extras.remove(SLOT_RESERVATION_QUEUE);
        }
        if (reserveSkipToPrevSlot) {
            extras.putBoolean(SLOT_RESERVATION_SKIP_TO_PREV, true);
        } else {
            extras.remove(SLOT_RESERVATION_SKIP_TO_PREV);
        }
        if (reserveSkipToNextSlot) {
            extras.putBoolean(SLOT_RESERVATION_SKIP_TO_NEXT, true);
        } else {
            extras.remove(SLOT_RESERVATION_SKIP_TO_NEXT);
        }
    }
}