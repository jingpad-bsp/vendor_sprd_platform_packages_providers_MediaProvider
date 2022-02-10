package com.unisoc.providers.media.scan;

import com.android.internal.annotations.VisibleForTesting;
import com.android.providers.media.scan.ModernMediaScanner;
import com.unisoc.providers.media.MediaFileAssist;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.drm.DecryptHandle;
import android.drm.DrmManagerClientEx;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactoryEx;
import android.media.MediaFile;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.Optional;
import android.util.SparseArray;
import android.annotation.NonNull;

/**
 * Assist Class of ModernMediaScanner {@link com.android.providers.media.scan.ModernMediaScanner}
 * Used to carry the functions developed by unisoc company.
 */
public class ModernMediaScannerAssist {
    private static final String TAG = "ModernMediaScanner";
    public static final boolean LOCAL_LOG_ENABLE = true; //Log.isLoggable(TAG, Log.DEBUG)
    public static final String DCF_UNKNOWN_CONTENT_TYPE = "application/unknown";
    private boolean mDrmEnabled;

    private static boolean isDrmEnabled() {
        String prop = SystemProperties.get("drm.service.enabled");
        return prop != null && prop.equals("true");
    }

    @VisibleForTesting
    DrmManagerClientEx mDrmManagerClient = null;


    public static final int KEY_DRMINFO_MIMETYPE = 0;
    public static final int KEY_DRMINFO_WIDTH = 1;
    public static final int KEY_DRMINFO_HEIGTH = 2;

    public ModernMediaScannerAssist(Context context){
        mDrmEnabled = isDrmEnabled();
    }

    /**
     * Support special images taken by AI Camera
     * @param op
     * @param value
     */
    public static void withOptionalValueForSpecialPictures(ContentProviderOperation.Builder op,
                                                           Optional<?> value) {
        if (value.isPresent()) {
            String cameraType = (String) value.get();
            int cameraTypeValue = Integer.parseInt(cameraType);
            switch (cameraTypeValue) {
                case MediaFileAssist.IMG_TYPE_MODE_NORMAL:
                case MediaFileAssist.IMG_TYPE_MODE_BLUR_HAS_BOKEH:
                case MediaFileAssist.IMG_TYPE_MODE_REAL_BOKEH_HAS_BOKEH:
                case MediaFileAssist.IMG_TYPE_MODE_HDR_BOKEH_HAS_BOKEH:
                case MediaFileAssist.IMG_TYPE_MODE_BLUR_NO_BOKEH:
                case MediaFileAssist.IMG_TYPE_MODE_REAL_BOKEH_NO_BOKEH:
                case MediaFileAssist.IMG_TYPE_MODE_HDR_BOKEH_NO_BOKEH:
                case MediaFileAssist.IMG_TYPE_MODE_HDR:
                case MediaFileAssist.IMG_TYPE_MODE_THUMBNAIL:
                case MediaFileAssist.IMG_TYPE_MODE_AUDIO_CAPTURE:
                case MediaFileAssist.IMG_TYPE_MODE_HDR_AUDIO_CAPTURE:
                case MediaFileAssist.IMG_TYPE_MODE_AI_SCENE:
                //Bug 1422617: add the file_flag for some special pictures, such as AI+HDR, and so on.
                case MediaFileAssist.IMG_TYPE_MODE_AI_SCENE_HDR:
                case MediaFileAssist.IMG_TYPE_MODE_AI_SCENE_FDR:
                case MediaFileAssist.IMG_TYPE_MODE_FDR:
                case MediaFileAssist.IMG_TYPE_MODE_MOTION_HDR_PHOTO:
                case MediaFileAssist.IMG_TYPE_MODE_MOTION_FDR_PHOTO:
                case MediaFileAssist.IMG_TYPE_MODE_MOTION_AI_PHOTO:
                case MediaFileAssist.IMG_TYPE_MODE_MOTION_HDR_AI_PHOTO:
                case MediaFileAssist.IMG_TYPE_MODE_MOTION_FDR_AI_PHOTO:
                    if (LOCAL_LOG_ENABLE)
                        Log.d(TAG, "withOptionalValueForSpecialPictures -> FILE_FLAG : " + cameraType);
                    op.withValue(MediaFileAssist.Images.FILE_FLAG, cameraTypeValue);
                    break;
                case MediaFileAssist.IMG_TYPE_MODE_BURST:
                case MediaFileAssist.IMG_TYPE_MODE_BURST_COVER:
                    if (LOCAL_LOG_ENABLE)
                        Log.d(TAG, "withOptionalValueForSpecialPictures -> FILE_FLAG : " + cameraType);
                    op.withValue(MediaFileAssist.Images.FILE_FLAG, MediaFileAssist.IMG_TYPE_MODE_NORMAL);
                    break;
            }
        }
    }

    /**
     * Support DRM file processing
     * @param context
     * @param file
     * @return
     */
    public SparseArray processDrmFile(Context context, File file) {
        //1.check drm trigger
        if (!mDrmEnabled) {
            if (LOCAL_LOG_ENABLE) Log.d(TAG, "processDrmFile -> drm not enable");
            return null;
        }

        //2.check file path
        String path = file.getPath();
        if (TextUtils.isEmpty(path)) {
            if (LOCAL_LOG_ENABLE) Log.d(TAG, "processDrmFile -> file path is not valid");
            return null;
        }

        //3.check file mimetype
        String extension = MediaFile.getFileExtension(path);
        if (TextUtils.isEmpty(extension) || !extension.toLowerCase().equals("dcf")) {
//        String mimetype = MediaFile.getMimeTypeForFile(path);
//        if (!MediaFile.isDrmMimeType(mimetype)) {
            if (LOCAL_LOG_ENABLE) Log.d(TAG, "processDrmFile -> file is not drm type");
            return null;
        }

        //4.ensure drm manager client has created.
        SparseArray drminfo = new SparseArray();
        if (mDrmManagerClient == null) {
            mDrmManagerClient = new DrmManagerClientEx(context);
            if (LOCAL_LOG_ENABLE)
                Log.d(TAG, "processDrmFile -> create drm mgr client: " + mDrmManagerClient);
        }

        //5.fetch original mimetype from drm file.
        String originalMimeType = null;
        if (mDrmManagerClient.canHandle(path, null)) {
            originalMimeType = mDrmManagerClient.getOriginalMimeType(path);
            if (LOCAL_LOG_ENABLE)
                Log.d(TAG, "processDrmFile -> getOriginalMimeType:" + originalMimeType + " from:" + path);
        }

        //6.collect original mimetype
        if (originalMimeType != null) {
            drminfo.append(KEY_DRMINFO_MIMETYPE, originalMimeType);
        } else {
            return null;
        }

        //7.fetch width/height from drm image file and collect them.
        if (MediaFile.isImageMimeType(originalMimeType)) {
            try {
                DecryptHandle handle = mDrmManagerClient.openDecryptSession(path);
                if (handle != null) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    BitmapFactoryEx.decodeDrmStream(mDrmManagerClient, handle, options);
                    int width = options.outWidth;
                    int heigth = options.outHeight;
                    if (LOCAL_LOG_ENABLE)
                        Log.d(TAG, "processDrmFile -> get Original width & height: " + width + " x " + heigth);
                    if (width > 0 && heigth > 0) {
                        drminfo.append(KEY_DRMINFO_WIDTH, String.valueOf(width));
                        drminfo.append(KEY_DRMINFO_HEIGTH, String.valueOf(heigth));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "processDrmFile", e);
            }
        }

        return drminfo;
    }

    /**
     *  Release drm manager client resource.
     *  We need to consider the reuse of DrmManagerClient objects, so only at the end of the big task to do this:
     *  {@link com.android.providers.media.scan.ModernMediaScanner#scanDirectory(java.io.File)}
     *  {@link ModernMediaScanner#scanFile(java.io.File)}
     */
    public void releaseDrmMgrClient(){
        if(mDrmManagerClient != null){
            if (LOCAL_LOG_ENABLE)
                Log.d(TAG, "processDrmFile -> releaseDrmMgrClient: " + mDrmManagerClient);
            mDrmManagerClient.close();
            mDrmManagerClient = null;
        }
    }

    /**
     * If a 3gpp file is placed in a public audio folder such as Ringtones, we recognize it as audio by default.
     *
     * @param mimeType
     * @param file
     * @return
     */
    public static @NonNull
    String maybeOverrideMimeType(@NonNull String mimeType, @NonNull File file) {
        //Non-3gpp files are not processed
        if (!mimeType.equals("video/3gpp")) {
            return mimeType;
        }
        String filePath = file.getParent();
        Log.d(TAG, "maybeOverrideMimeType -> file.getParent :" + filePath);
        if (filePath.endsWith("Alarms") || filePath.endsWith("Notifications") || filePath.endsWith("Ringtones")) {
            // TODO: 7/9/19  How do we judge that this scan task is from a specific scene, such as triggered after a ringtone copy?
            return "audio/3gpp";
        }
        return mimeType;
    }
}