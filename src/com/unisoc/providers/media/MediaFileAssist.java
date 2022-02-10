package com.unisoc.providers.media;

import android.database.Cursor;
import android.provider.Column;
import android.provider.MediaStore;
import android.annotation.NonNull;


/**
 * Expanded the media file framework layer,
 * And used for decoupling the framework layer from the app layer
 */
public class MediaFileAssist {

    static final String TAG = "MediaFileAssist";
    public static final int IMG_TYPE_MODE_NORMAL = 0;
    public static final int IMG_TYPE_MODE_3D_CAPTURE = 7;
    public static final int IMG_TYPE_MODE_SOFY_OPTICAL_ZOOM = 11;
    public static final int IMG_TYPE_MODE_BLUR_HAS_BOKEH = 12;//0X000C blur has bokeh
    public static final int IMG_TYPE_MODE_REAL_BOKEH_HAS_BOKEH = 16;//0X0010 real-bokeh has bokeh
    public static final int IMG_TYPE_MODE_HDR_BOKEH_HAS_BOKEH = 17;//0X0011 real-bokeh with hdr has bokeh
    public static final int IMG_TYPE_MODE_AI_SCENE = 36;
    public static final int IMG_TYPE_MODE_BURST = 51;
    public static final int IMG_TYPE_MODE_HDR = 52;
    public static final int IMG_TYPE_MODE_AUDIO_CAPTURE = 53;
    public static final int IMG_TYPE_MODE_HDR_AUDIO_CAPTURE = 54;
    public static final int IMG_TYPE_MODE_BURST_COVER = 55;
    public static final int IMG_TYPE_MODE_THUMBNAIL = 56;
    public static final int IMG_TYPE_MODE_FDR = 57;
    public static final int IMG_TYPE_MODE_BLUR_NO_BOKEH = 268;//0X010C blur not bokeh
    public static final int IMG_TYPE_MODE_REAL_BOKEH_NO_BOKEH = 272;//0X0110 real-bokeh not bokeh
    public static final int IMG_TYPE_MODE_HDR_BOKEH_NO_BOKEH = 273;//0X0111 real-bokeh with hdr not bokeh
    public static final int IMG_TYPE_MODE_AI_SCENE_HDR = 37;
    public static final int IMG_TYPE_MODE_AI_SCENE_FDR = 38;
    public static final int IMG_TYPE_MODE_MOTION_HDR_PHOTO = 1025;
    public static final int IMG_TYPE_MODE_MOTION_AI_PHOTO = 1026;
    public static final int IMG_TYPE_MODE_MOTION_HDR_AI_PHOTO = 1027;
    public static final int IMG_TYPE_MODE_MOTION_FDR_PHOTO = 1028;
    public static final int IMG_TYPE_MODE_MOTION_FDR_AI_PHOTO = 1029;

    public static final class Images implements MediaStore.Images.ImageColumns{
        @Column(Cursor.FIELD_TYPE_INTEGER)
        public static final String FILE_FLAG = "file_flag";
    }

    public static final class Files implements MediaStore.Files.FileColumns {
        @Column(Cursor.FIELD_TYPE_INTEGER)
        public static final String FILE_FLAG = "file_flag";
    }

    /**
     * Special processing for audio/3gpp, only used in the file scanning process
     * @param mimeType
     * @return
     */
    public static boolean isAudioMimeType(@NonNull String mimeType) {
        return mimeType.equals("audio/3gpp");
    }

//    public static boolean isSpecialPicture(@Nullable String mimeType){
//        return android.text.TextUtils.equals(mimeType,"image/jpeg");
//    }

//    public static void updateContentValuesForSpecialPictures(ExifInterface exif, ContentValues values) {
//        int cameraType = exif.getAttributeInt(
//                ExifInterface.TAG_CAMERA_TYPE, -1);
//        switch (cameraType) {
//            case IMG_TYPE_MODE_NORMAL:
//            case IMG_TYPE_MODE_BLUR_HAS_BOKEH:
//            case IMG_TYPE_MODE_REAL_BOKEH_HAS_BOKEH:
//            case IMG_TYPE_MODE_HDR_BOKEH_HAS_BOKEH:
//            case IMG_TYPE_MODE_BLUR_NO_BOKEH:
//            case IMG_TYPE_MODE_REAL_BOKEH_NO_BOKEH:
//            case IMG_TYPE_MODE_HDR_BOKEH_NO_BOKEH:
//            case IMG_TYPE_MODE_HDR:
//            case IMG_TYPE_MODE_THUMBNAIL:
//            case IMG_TYPE_MODE_AUDIO_CAPTURE:
//            case IMG_TYPE_MODE_HDR_AUDIO_CAPTURE:
//            case IMG_TYPE_MODE_AI_SCENE:
//                Log.d(TAG,"updateContentValuesForSpecialPictures -> FILE_FLAG : " + cameraType);
//                values.put(Images.FILE_FLAG, cameraType);
//                break;
//            case IMG_TYPE_MODE_BURST:
//            case IMG_TYPE_MODE_BURST_COVER:
//                Log.d(TAG,"updateContentValuesForSpecialPictures -> FILE_FLAG : " + cameraType);
//                values.put(Images.FILE_FLAG, IMG_TYPE_MODE_NORMAL);
//                break;
//        }
//    }


}
