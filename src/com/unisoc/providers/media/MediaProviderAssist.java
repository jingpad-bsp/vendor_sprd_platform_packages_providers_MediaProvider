package com.unisoc.providers.media;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Environment;
import libcore.io.IoUtils;
import android.text.TextUtils;
import android.util.Log;
import com.android.providers.media.scan.MediaScanner;
import java.io.File;
import android.os.Binder;
import static android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY;

/**
 * Assist Class of MediaProvider {@link com.android.providers.media.MediaProvider}
 * Used to carry the functions developed by unisoc company.
 */
public class MediaProviderAssist {
    private static final String TAG = "MediaProvider";
    public static final String INTERNAL_VOLUME = "internal";
    public static final String EXTERNAL_VOLUME = "external";
    private ProviderPlugin mPlugin;

    public static final String IMAGE_COLUMS_NAME = "android.provider.MediaStore$Images$Media";
    public static final String FILE_COLUMS_NAME = "android.provider.MediaStore$Files$FileColumns";

    // Anything older than Q is recreated from scratch
    public static final int CREATE_LATEST_SCHEMA_THRESHOLD = 1000;

    /**
     * Since AndroidQ, MediaProvider has optimized the permission check, which aims to to avoid extra binder calls intoThe OS .
     * Dispatch all change notifications asynchronously, and delay them by some time while the camera is being actively used,
     * to give More important foreground work a fighting chance.
     */
    public static final int BACKGROUND_NOTIFY_DELAY = 500;// TODO: 7/4/19  Need to investigate whether the delay strategy is correct.

    public static final boolean LOCAL_LOG_ENABLE = Log.isLoggable(TAG, Log.DEBUG); //Log.isLoggable(TAG, Log.DEBUG)

    public static final boolean IPC_LOG_ENABLE = Log.isLoggable(TAG, Log.VERBOSE);

    public static final String[] MEDIA_TYPE_DATA_ID_PROJECTION = new String[]{
            MediaFileAssist.Files.MEDIA_TYPE,
            MediaFileAssist.Files.DATA,
            MediaFileAssist.Files._ID,
            MediaFileAssist.Files.IS_DOWNLOAD,
            MediaFileAssist.Files.MIME_TYPE,
            MediaFileAssist.Files.FILE_FLAG,
            MediaFileAssist.Images.DATE_TAKEN
    };

    private static final String[] sFileFlagProjection = new String[]{
            MediaFileAssist.Files.FILE_FLAG
    };

    private static final String[] sBurstImagesProjection = new String[]{
            MediaFileAssist.Files._ID,
            MediaFileAssist.Files.TITLE
    };

    public static final String[] DEFAULT_FOLDER_NAME = {
            Environment.DIRECTORY_MUSIC,
            Environment.DIRECTORY_PODCASTS,
            Environment.DIRECTORY_RINGTONES,
            Environment.DIRECTORY_ALARMS,
            Environment.DIRECTORY_NOTIFICATIONS,
            Environment.DIRECTORY_PICTURES,
            Environment.DIRECTORY_MOVIES,
            Environment.DIRECTORY_DOWNLOADS,
            Environment.DIRECTORY_DCIM,
            Environment.DIRECTORY_DCIM + "/.thumbnails",
            Environment.DIRECTORY_DCIM + "/Camera",
            Environment.DIRECTORY_DOCUMENTS,
    };

    public MediaProviderAssist(ProviderPlugin plugin) {
        mPlugin = plugin;
    }

    /**
     * The interface that the MediaProvider {@link com.android.providers.media.MediaProvider}needs to implement.
     * Used to plugin unisoc features and wrap AOSP MediaProvider logic
     */
    public interface ProviderPlugin {
        //Wrapped AOSP MediaProvider logic
        public Context providerGetContext();

        public Cursor providerQuery(Uri uri, String[] projection, Bundle queryArgs, CancellationSignal signal);

    }

    private Context getContext() {
        return mPlugin.providerGetContext();
    }

    /**
     * Support special images taken by AI Camera
     *
     * @param uri
     * @param values
     */
    public void updateContentValuesForSpecialPictures(Uri uri, ContentValues values) {
        int fileFlag = 0;
        Cursor c = null;
        try {
            c = mPlugin.providerQuery(uri, sFileFlagProjection, null, null);
            if (c != null && c.getCount() == 1) {
                c.moveToFirst();
                fileFlag = c.getInt(0);
                c.deactivate();
            }
        } finally {
            IoUtils.closeQuietly(c);
        }
        if (LOCAL_LOG_ENABLE) {
            Log.d(TAG, "updateContentValuesForSpecialPictures -> uri :" + uri);
            Log.d(TAG, "updateContentValuesForSpecialPictures -> fileFlag :" + fileFlag);
        }
        switch (fileFlag) {
            case MediaFileAssist.IMG_TYPE_MODE_BLUR_HAS_BOKEH:
            case MediaFileAssist.IMG_TYPE_MODE_REAL_BOKEH_HAS_BOKEH:
            case MediaFileAssist.IMG_TYPE_MODE_HDR_BOKEH_HAS_BOKEH:
            case MediaFileAssist.IMG_TYPE_MODE_BLUR_NO_BOKEH:
            case MediaFileAssist.IMG_TYPE_MODE_REAL_BOKEH_NO_BOKEH:
            case MediaFileAssist.IMG_TYPE_MODE_HDR_BOKEH_NO_BOKEH:
                values.remove(MediaFileAssist.Images.DATE_TAKEN);
                values.remove(MediaFileAssist.Images.DATE_MODIFIED);
        }
    }

    /**
     * Used to delete pictures in burst shooting mode
     *
     * @param fileFlag
     * @param db
     * @param id
     * @param datetaken
     * @param userWhere
     * @param args
     */
    public void updateBurstImage(int fileFlag, SQLiteDatabase db,
                                 final long id, final long datetaken, String userWhere, String[] args) {

        //if the deleted image a burst one(no matter is cover or not), we should go to FUNTION updateBurstImage,
        //because when there's only one picture left, we have to set it to normal picture.
        if (fileFlag != MediaFileAssist.IMG_TYPE_MODE_BURST_COVER && fileFlag != MediaFileAssist.IMG_TYPE_MODE_BURST) {
            return;
        }

        String selection = "datetaken = ? AND (file_flag = ? OR file_flag = ?)";
        String[] whereArgs = new String[]{
                String.valueOf(datetaken),
                String.valueOf(MediaFileAssist.IMG_TYPE_MODE_BURST),
                String.valueOf(MediaFileAssist.IMG_TYPE_MODE_BURST_COVER)};

        String[] whereArgsFinal = null;
        if (args != null) {
            whereArgsFinal = new String[whereArgs.length + args.length];
            System.arraycopy(args, 0, whereArgsFinal, 0, args.length);
            System.arraycopy(whereArgs, 0, whereArgsFinal, args.length, whereArgs.length);
        }
        String selectionFinal = null;
        if (userWhere != null && !userWhere.isEmpty()) {
            selectionFinal = "_id NOT IN (SELECT _id from files WHERE " + userWhere + ") AND " + selection;
        }

        Cursor c = db.query("files", sBurstImagesProjection,
                (selectionFinal == null) ? selection : selectionFinal,
                (whereArgsFinal == null) ? whereArgs : whereArgsFinal, null, null, null);
        try {
            //There is no burst picture with the same datetaken,
            //so we don't have to do anything.
            if (c == null || c.getCount() < 1) return;

            //There is only one burst picture with the same datetaken,
            //so we have to update it to normal picture:
            //file_flag set to normal.
            if (c.getCount() == 1) {
                long pickedId = 0l;
                while (c.moveToNext()) {
                    pickedId = c.getLong(0);
                }
                Log.d(TAG, "pickedId = " + pickedId);
                ContentValues updateToNormal = new ContentValues();
                updateToNormal.put(MediaFileAssist.Images.FILE_FLAG, MediaFileAssist.IMG_TYPE_MODE_NORMAL);
                db.update("files", updateToNormal,
                        "_id = ?",
                        new String[]{String.valueOf(pickedId)});
            } else if (fileFlag == MediaFileAssist.IMG_TYPE_MODE_BURST_COVER) {
                //There are more than one burst pictures with the same datetaken,
                //and the deleted one is burst cover,
                //so we have to find a new burst cover.
                if (c.moveToNext()) {
                    String sPickedId = String.valueOf(c.getLong(0));
                    Log.d(TAG, "sPickedId = " + sPickedId);
                    if (sPickedId != null) {
                        ContentValues burstCover = new ContentValues();
                        burstCover.put(MediaFileAssist.Images.FILE_FLAG, MediaFileAssist.IMG_TYPE_MODE_BURST_COVER);
                        db.update("files", burstCover,
                                "_id = ?",
                                new String[]{sPickedId});
                    }
                }
            }
        } finally {
            IoUtils.closeQuietly(c);
        }
    }

    /**
     * In order to connect the download module to the processing of the drm file, we do a scan task for the dcf file.
     *
     * @param initialValues
     */
    public void maybeTriggerScanDrmFile(ContentValues initialValues) {
        if (initialValues == null || initialValues.size() == 0) {
            if (LOCAL_LOG_ENABLE) Log.d(TAG, "maybeTriggerScanDrmFile -> ContentValues is null");
            return;
        }

        String filepath = initialValues.getAsString(MediaFileAssist.Files.DATA);
        if (TextUtils.isEmpty(filepath)) {
            if (LOCAL_LOG_ENABLE) Log.d(TAG, "maybeTriggerScanDrmFile -> filepath is null");
            return;
        }
        //file is ready.
        Integer isPending = initialValues.getAsInteger(MediaFileAssist.Files.IS_PENDING);
        //file is Drm file.
        Integer isDrm = initialValues.getAsInteger(MediaFileAssist.Files.IS_DRM);
        //file come from download.
        Boolean isDownload = initialValues.getAsBoolean(MediaFileAssist.Files.IS_DOWNLOAD);
        if ((isPending != null && isPending.intValue() == 0) &&
                (isDrm != null && isDrm.intValue() == 1) &&
                (isDownload != null && isDownload.booleanValue())) {
            if (LOCAL_LOG_ENABLE)
                Log.d(TAG, "maybeTriggerScanDrmFile -> start scan drm file in new thread");
            new Thread(() -> MediaScanner.instance(getContext()).scanFile(new File(filepath))).start();
        }
    }

    /**
     * When the external_primary is not available, try to determine if there are other externa lvolumes currently.
     * @param volumeName
     * @param attachedVolumeSize
     * @return
     */
    public boolean maybeCheckOtherExternalVolumes(String volumeName, int attachedVolumeSize) {
        if (VOLUME_EXTERNAL_PRIMARY.equals(volumeName) && (attachedVolumeSize > 1)) {
            return false;
        }
        return true;
    }

    /**
     * sqlite handling track log
     *
     * @param funTag
     * @param uri
     * @param initialValues
     */
    public void sqliteTrackLog(String funTag, Uri uri, ContentValues initialValues, String userWhere,
                               String[] userWhereArgs) {
        StringBuilder tmp = new StringBuilder(funTag);
        tmp.append(" -> ").append("\n");
        if (uri != null) {
            tmp.append(" uri: " + uri).append("\n");
        }
        if (initialValues != null && initialValues.size() != 0) {
            tmp.append(" initialValues :" + initialValues).append("\n");
        }
        if (!TextUtils.isEmpty(userWhere)) {
            tmp.append(" userWhere :" + userWhere).append("\n");
        }
        if (userWhereArgs != null && userWhereArgs.length != 0) {
            for (String arg : userWhereArgs) {
                tmp.append(" -userWhereArg :" + arg).append("\n");
            }
        }
        tmp.append(" CallingPid: " + Binder.getCallingPid());
        Log.d(TAG, tmp.toString());
    }
}
