package com.unisoc.providers.media.ringtone;

import android.app.ActivityManager;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.Settings;
import android.test.InstrumentationTestCase;
import android.util.Log;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

/**
 * Double card ringtone test(card 1).
 * The test logic for this part of the ringtone is borrowed from the CTS test case.
 * We can say that the test standard is consistent.
 */
@RunWith(AndroidJUnit4.class)
public class Ringtone0Test extends InstrumentationTestCase {
    private static final String TAG = "MediaProvider.RingtoneTest";

    private Context mContext;
    private Ringtone mRingtone;
    private AudioManager mAudioManager;
    private int mOriginalVolume;
    private int mOriginalRingerMode;
    private int mOriginalStreamType;
    private Uri mDefaultRingUri;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        mContext = InstrumentationRegistry.getTargetContext();
        Utils.enableAppOps(mContext.getPackageName(), "android:write_settings", InstrumentationRegistry.getInstrumentation());
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mRingtone = RingtoneManager.getRingtone(mContext, Settings.System.DEFAULT_RINGTONE_URI);
        Log.d(TAG, "setUp mRingtone:" + mRingtone);
        // backup ringer settings
        mOriginalRingerMode = mAudioManager.getRingerMode();
        mOriginalVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_RING);
        mOriginalStreamType = mRingtone.getStreamType();

        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_RING);

        if (mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) {
            mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            mAudioManager.setStreamVolume(AudioManager.STREAM_RING, maxVolume / 2,
                    AudioManager.FLAG_ALLOW_RINGER_MODES);
        } else if (mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
            mAudioManager.setStreamVolume(AudioManager.STREAM_RING, maxVolume / 2,
                    AudioManager.FLAG_ALLOW_RINGER_MODES);
        } else if (!ActivityManager.isLowRamDeviceStatic()) {
            try {
                Utils.toggleNotificationPolicyAccess(
                        mContext.getPackageName(), InstrumentationRegistry.getInstrumentation(), true);
                // set ringer to a reasonable volume
                mAudioManager.setStreamVolume(AudioManager.STREAM_RING, maxVolume / 2,
                        AudioManager.FLAG_ALLOW_RINGER_MODES);
                // make sure that we are not in silent mode
                mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            } finally {
                Utils.toggleNotificationPolicyAccess(
                        mContext.getPackageName(), InstrumentationRegistry.getInstrumentation(), false);
            }
        }
        mDefaultRingUri = RingtoneManager.getActualDefaultRingtoneUri(mContext,
                RingtoneManager.TYPE_RINGTONE);
    }

    @After
    public void tearDown() throws Exception {
        // restore original settings
        if (mRingtone != null) {
            if (mRingtone.isPlaying()) mRingtone.stop();
            mRingtone.setStreamType(mOriginalStreamType);
        }
        if (mAudioManager != null && !ActivityManager.isLowRamDeviceStatic()) {
            try {
                Utils.toggleNotificationPolicyAccess(
                        mContext.getPackageName(), InstrumentationRegistry.getInstrumentation(), true);
                mAudioManager.setRingerMode(mOriginalRingerMode);
                mAudioManager.setStreamVolume(AudioManager.STREAM_RING, mOriginalVolume,
                        AudioManager.FLAG_ALLOW_RINGER_MODES);
            } finally {
                Utils.toggleNotificationPolicyAccess(
                        mContext.getPackageName(), InstrumentationRegistry.getInstrumentation(), false);
            }
        }
        RingtoneManager.setActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_RINGTONE,
                mDefaultRingUri);
        Utils.disableAppOps(mContext.getPackageName(), "android:write_settings", InstrumentationRegistry.getInstrumentation());
        super.tearDown();
    }

    private boolean hasAudioOutput() {
        return mContext.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT);
    }

    /**
     * Test ringtone :
     * 1.title
     * 2.stream type
     * 3.playing state
     *
     * @throws Exception
     */
    @Test
    public void testRingtone() throws Exception {
        if (!hasAudioOutput()) {
            Log.i(TAG, "Skipping testRingtone(): device doesn't have audio output.");
            return;
        }

        assertNotNull(mRingtone.getTitle(mContext));

        assertTrue(mOriginalStreamType >= 0);
        mRingtone.setStreamType(AudioManager.STREAM_MUSIC);
        assertEquals(AudioManager.STREAM_MUSIC, mRingtone.getStreamType());
        mRingtone.setStreamType(AudioManager.STREAM_ALARM);
        assertEquals(AudioManager.STREAM_ALARM, mRingtone.getStreamType());
        // make sure we play on STREAM_RING because we the volume on this stream is not 0
        mRingtone.setStreamType(AudioManager.STREAM_RING);
        assertEquals(AudioManager.STREAM_RING, mRingtone.getStreamType());

        // test both the "None" ringtone and an actual ringtone
        RingtoneManager.setActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_RINGTONE, null);
        mRingtone = RingtoneManager.getRingtone(mContext, Settings.System.DEFAULT_RINGTONE_URI);
        assertTrue(mRingtone.getStreamType() == AudioManager.STREAM_RING);
        mRingtone.play();
//        assertFalse(mRingtone.isPlaying());

        Uri uri = RingtoneManager.getValidRingtoneUri(mContext);
        assertNotNull("ringtone was unexpectedly null", uri);
        RingtoneManager.setActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_RINGTONE, uri);
        mRingtone = RingtoneManager.getRingtone(mContext, Settings.System.DEFAULT_RINGTONE_URI);
        assertTrue(mRingtone.getStreamType() == AudioManager.STREAM_RING);
        mRingtone.play();
        assertTrue("couldn't play ringtone " + uri, mRingtone.isPlaying());
        mRingtone.stop();
        assertFalse(mRingtone.isPlaying());
    }

    /**
     * Test ringtone volume
     *
     * @throws Exception
     */
    @Test
    public void testLoopingVolume() throws Exception {
        if (!hasAudioOutput()) {
            Log.i(TAG, "Skipping testRingtone(): device doesn't have audio output.");
            return;
        }

        Uri uri = RingtoneManager.getValidRingtoneUri(mContext);
        assertNotNull("ringtone was unexpectedly null", uri);
        RingtoneManager.setActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_RINGTONE, uri);
        assertNotNull(mRingtone.getTitle(mContext));
        final AudioAttributes ringtoneAa = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).
                        build();
        mRingtone.setAudioAttributes(ringtoneAa);
        assertEquals(ringtoneAa, mRingtone.getAudioAttributes());
        mRingtone.setLooping(true);
        mRingtone.setVolume(0.5f);
        mRingtone.play();
        assertTrue("couldn't play ringtone " + uri, mRingtone.isPlaying());
        assertTrue(mRingtone.isLooping());
        assertEquals("invalid ringtone player volume", 0.5f, mRingtone.getVolume());
        mRingtone.stop();
        assertFalse(mRingtone.isPlaying());
    }
}