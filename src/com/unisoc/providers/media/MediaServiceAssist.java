package com.unisoc.providers.media;

import static android.media.RingtoneManager.TYPE_ALARM;
import static android.media.RingtoneManager.TYPE_NOTIFICATION;
import static android.media.RingtoneManager.TYPE_RINGTONE;
import static android.media.RingtoneManager.TYPE_RINGTONE1;


import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.SystemProperties;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.provider.Settings;

import com.android.providers.media.scan.MediaScanner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;

public class MediaServiceAssist {
    private static final String RINGTONES_DIR = "/product/media/audio/ringtones";
    private static final String NOTIFICATIONS_DIR = "/product/media/audio/notifications";
    private static final String ALARMS_DIR = "/product/media/audio/alarms";

    private static void scanRingtoneDir(Context context, int type) {
        String filePath = null;
        if (type == TYPE_RINGTONE || type == TYPE_RINGTONE1) {
            filePath = RINGTONES_DIR;
        } else if (type == TYPE_NOTIFICATION) {
            filePath = NOTIFICATIONS_DIR;
        } else if (type == TYPE_ALARM) {
            filePath = ALARMS_DIR;
        }
        if (filePath != null) {
            MediaScanner.instance(context).scanDirectory(new File(filePath));
        }
    }

    /**
     * Ensure that we've set ringtones at least once after initial scan.
     */
    public static void ensureDefaultRingtones(Context context) {
        for (int type : new int[] {
                TYPE_RINGTONE,
                TYPE_RINGTONE1,
                TYPE_NOTIFICATION,
                TYPE_ALARM,
        }) {
            // Skip if we've already defined it at least once, so we don't
            // overwrite the user changing to null
            final String setting = getDefaultRingtoneSetting(type);
            if (Settings.System.getInt(context.getContentResolver(), setting, 0) != 0) {
                continue;
            }
            //scan ringtones dir
            scanRingtoneDir(context,type);
            // Try finding the scanned ringtone
            final String filename = getDefaultRingtoneFilename(type);
            final Uri baseUri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI;
            try (Cursor cursor = context.getContentResolver().query(baseUri,
                    new String[] { MediaColumns._ID },
                    MediaColumns.DISPLAY_NAME + "=?",
                    new String[] { filename }, null)) {
                if (cursor.moveToFirst()) {
                    final Uri ringtoneUri = context.getContentResolver().canonicalizeOrElse(
                            ContentUris.withAppendedId(baseUri, cursor.getLong(0)));
                    RingtoneManager.setActualDefaultRingtoneUri(context, type, ringtoneUri);
                    Settings.System.putInt(context.getContentResolver(), setting, 1);
                }
            }
        }
    }

    private static String getDefaultRingtoneSetting(int type) {
        switch (type) {
            case TYPE_RINGTONE: return "ringtone_set";
            case TYPE_RINGTONE1: return "ringtone1_set";
            case TYPE_NOTIFICATION: return "notification_sound_set";
            case TYPE_ALARM: return "alarm_alert_set";
            default: throw new IllegalArgumentException();
        }
    }

    private static String getDefaultRingtoneFilename(int type) {
        switch (type) {
            case TYPE_RINGTONE: return SystemProperties.get("ro.config.ringtone");
            case TYPE_RINGTONE1: return SystemProperties.get("ro.config.ringtone1");
            case TYPE_NOTIFICATION: return SystemProperties.get("ro.config.notification_sound");
            case TYPE_ALARM: return SystemProperties.get("ro.config.alarm_alert");
            default: throw new IllegalArgumentException();
        }
    }
}
