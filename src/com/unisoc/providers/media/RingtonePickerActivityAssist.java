package com.unisoc.providers.media;
import android.text.TextUtils;
import android.os.Environment;
import android.net.Uri;
import android.media.RingtoneManager;
import android.system.Os;
import android.content.Context;
import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.util.Log;


/**
 * Assist Class of RingtonePickerActivity {@link com.android.providers.media.RingtonePickerActivity}
 * Used to carry the functions developed by unisoc company.
 */
public class RingtonePickerActivityAssist {
    private static final String TAG = "RingtonePickerActivityAssist";

    private Context mContext;
    private boolean mPresence;

    public boolean isPresent(){
        return mPresence;
    }

    public RingtonePickerActivityAssist(Context context){
        mContext = context;
    }

    /**
     * Whether the audio is irregular or not.
     * @param fileUri
     * @param type
     * @return
     */
    public boolean isAudioIrregular(final Uri fileUri, final int type) {
        String sourceFileName = getFileDisplayNameFromUri(mContext, fileUri);
        Log.d(TAG, "sourceFileName=" + sourceFileName);

        if (!TextUtils.isEmpty(sourceFileName)) {
            if ("dcf".equals(getExtensionName(sourceFileName).toLowerCase())) {
                return true;
            }

            final String subdirectory = getExternalDirectoryForType(type);
            String sourceFilePath = Environment.getExternalStoragePublicDirectory(subdirectory).getAbsolutePath()
                    + "/" + sourceFileName;
            Log.d(TAG, "sourceFilePath=" + sourceFilePath);

            mPresence = false;
            try {
                mPresence = Os.access(sourceFilePath, android.system.OsConstants.F_OK);
            } catch (Exception e) {
                Log.d(TAG, "checkAudio -> exception", e);
            } finally {
                if (mPresence) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Return public folder name by type
     * @param type
     * @return
     */
    private static final String getExternalDirectoryForType(final int type) {
        switch (type) {
            case RingtoneManager.TYPE_RINGTONE:
                return Environment.DIRECTORY_RINGTONES;
            case RingtoneManager.TYPE_NOTIFICATION:
                return Environment.DIRECTORY_NOTIFICATIONS;
            case RingtoneManager.TYPE_ALARM:
                return Environment.DIRECTORY_ALARMS;
            default:
                throw new IllegalArgumentException("Unsupported ringtone type: " + type);
        }
    }

    /**
     * Returns a file's display name from its {@link android.content.ContentResolver.SCHEME_FILE}
     * or {@link android.content.ContentResolver.SCHEME_CONTENT} Uri. The display name of a file
     * includes its extension.
     *
     * @param context Context trying to resolve the file's display name.
     * @param uri Uri of the file.
     * @return the file's display name, or the uri's string if something fails or the uri isn't in
     *            the schemes specified above.
     */
    private static String getFileDisplayNameFromUri(Context context, Uri uri) {
        String scheme = uri.getScheme();

        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            return uri.getLastPathSegment();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            // We need to query the ContentResolver to get the actual file name as the Uri masks it.
            // This means we want the name used for display purposes only.
            String[] proj = {
                    OpenableColumns.DISPLAY_NAME
            };
            try (Cursor cursor = context.getContentResolver().query(uri, proj, null, null, null)) {
                if (cursor != null && cursor.getCount() != 0) {
                    cursor.moveToFirst();
                    return cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        // This will only happen if the Uri isn't either SCHEME_CONTENT or SCHEME_FILE, so we assume
        // it already represents the file's name.
        return uri.toString();
    }

    /**
     * Get the extension name of a file.
     * @param fileName
     * @return
     */
    private String getExtensionName(String fileName) {
        if ((fileName != null) && (fileName.length() > 0)) {
            int dot = fileName.lastIndexOf('.');
            if ((dot > -1) && (dot < (fileName.length() -1))) {
                return fileName.substring(dot + 1);
            }
        }

        return fileName;
    }
}