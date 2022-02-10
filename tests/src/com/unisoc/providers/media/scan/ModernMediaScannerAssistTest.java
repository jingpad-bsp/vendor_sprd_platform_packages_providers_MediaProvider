package com.unisoc.providers.media.scan;


import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;
import com.android.providers.media.tests.R;

import static com.android.providers.media.scan.MediaScannerTest.stage;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.ContentResolver;

import android.content.Context;

import android.database.Cursor;
import android.drm.DrmConvertedStatus;
import android.drm.DrmEvent;
import android.drm.DrmInfo;
import android.drm.DrmInfoRequest;
import android.drm.DrmInfoStatus;
import android.drm.DrmManagerClient;
import android.drm.DrmManagerClientEx;
import android.drm.ProcessedData;
import android.net.Uri;
import android.os.Environment;
import android.os.FileUtils;
import android.provider.MediaStore;

import com.android.providers.media.MediaProvider;
import com.android.providers.media.scan.MediaScannerTest.IsolatedContext;
import com.android.providers.media.scan.ModernMediaScanner;
import com.unisoc.providers.media.MediaFileAssist;


import org.junit.After;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Test for Class {@link com.unisoc.providers.media.scan.ModernMediaScannerAssist}
 */
@RunWith(AndroidJUnit4.class)
public class ModernMediaScannerAssistTest {
    // TODO: scan directory-vs-files and confirm identical results

    private File mDir;
    private Context mIsolatedContext;
    private ContentResolver mIsolatedResolver;
    private ModernMediaScanner mModern;

    @Before
    public void setUp() {
        mDir = new File(Environment.getExternalStorageDirectory(), "unisoc_test_" + System.nanoTime());
        mDir.mkdirs();
        FileUtils.deleteContents(mDir);

        final Context context = InstrumentationRegistry.getTargetContext();
        mIsolatedContext = new IsolatedContext(context, "modern");
        mIsolatedResolver = mIsolatedContext.getContentResolver();

        mModern = new ModernMediaScanner(mIsolatedContext);
    }

    @After
    public void tearDown() {
        FileUtils.deleteContentsAndDir(mDir);
    }

    /**
     * Verify that the default folder has been created on emulated storage.
     * {@link com.unisoc.providers.media.MediaProviderAssist#DEFAULT_FOLDER_NAME}
     * @throws Exception
     */

    @Test
    public void testDefaultDirectoryCreated_EmulatedStorage() throws Exception{
        String emulatedStorageRoot = "/storage/emulated/0/";
        for (String dir : new String[]{
                emulatedStorageRoot + Environment.DIRECTORY_MUSIC,
                emulatedStorageRoot + Environment.DIRECTORY_PODCASTS,
                emulatedStorageRoot + Environment.DIRECTORY_RINGTONES,
                emulatedStorageRoot + Environment.DIRECTORY_ALARMS,
                emulatedStorageRoot + Environment.DIRECTORY_NOTIFICATIONS,
                emulatedStorageRoot + Environment.DIRECTORY_PICTURES,
                emulatedStorageRoot + Environment.DIRECTORY_MOVIES,
                emulatedStorageRoot + Environment.DIRECTORY_DOWNLOADS,
                emulatedStorageRoot + Environment.DIRECTORY_DCIM,
                emulatedStorageRoot + Environment.DIRECTORY_DCIM + "/.thumbnails",
                emulatedStorageRoot + Environment.DIRECTORY_DCIM + "/Camera"
        }) {
            assertDirectoryExist(new File(dir));
        }
    }

    private static void assertDirectoryExist(File file){
        assertTrue(file.getAbsolutePath(),file.exists());
    }


    /**
     * ====================================================================
     *                          CAMERA PICTURE TESTS
     * ====================================================================
     */

    private static final String sImageTestFileName = "test_file.jpg";

    private Cursor getImageFileFlag(String displayName) throws Exception{
        String[] projection = new String[]{MediaFileAssist.Files.FILE_FLAG};
        String selection = MediaFileAssist.Images.DISPLAY_NAME + "=?";
        String[] selectionArgs = new String[]{displayName};
        Cursor cursor = mIsolatedResolver
                .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null);
        return cursor;
    }

    private void assertQueryCountAfterClean(File file, String displayName) {
        file.delete();
        mModern.scanDirectory(mDir);
        String selection = MediaFileAssist.Files.DISPLAY_NAME + "=?";
        String[] selectionArgs = new String[]{displayName};
        Cursor cursor = mIsolatedResolver
                .query(MediaStore.Files.getContentUri("external"), null, selection, selectionArgs, null);
        assertEquals(0, cursor.getCount());
    }

    /**
     * Test image type: {@link com.unisoc.providers.media.MediaFileAssist#IMG_TYPE_MODE_BURST}
     * @throws Exception
     */
    @Test
    public void testScanImage_Burst() throws Exception {
        Assume.assumeTrue(MediaProvider.ENABLE_MODERN_SCANNER);
        // copy raw file into test folder
        final File file = new File(mDir, sImageTestFileName);
        stage(R.raw.test_image_burst, file);
        mModern.scanDirectory(mDir);
        // Confirm that we found new image and scanned it
        try (Cursor cursor = getImageFileFlag(sImageTestFileName)) {
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            final int fileFlag = cursor.getInt(0);
            assertEquals(MediaFileAssist.IMG_TYPE_MODE_NORMAL, fileFlag);
        }
        // Delete raw file and confirm it's cleaned up
        assertQueryCountAfterClean(file, sImageTestFileName);
    }

    /**
     * Test image type: {@link com.unisoc.providers.media.MediaFileAssist#IMG_TYPE_MODE_BURST_COVER}
     * @throws Exception
     */
    @Test
    public void testScanImage_BurstCover() throws Exception{
        Assume.assumeTrue(MediaProvider.ENABLE_MODERN_SCANNER);
        // copy raw file into test folder
        final File file = new File(mDir,sImageTestFileName);
        stage(R.raw.test_image_burst_cover,file);
        mModern.scanDirectory(mDir);
        // Confirm that we found new image and scanned it
        try (Cursor cursor = getImageFileFlag(sImageTestFileName)) {
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            final int fileFlag = cursor.getInt(0);
            assertEquals(MediaFileAssist.IMG_TYPE_MODE_NORMAL, fileFlag);
        }
        // Delete raw file and confirm it's cleaned up
        assertQueryCountAfterClean(file, sImageTestFileName);
    }

    /**
     * Test image type: {@link com.unisoc.providers.media.MediaFileAssist#IMG_TYPE_MODE_REAL_BOKEH_HAS_BOKEH}
     * @throws Exception
     */
    @Test
    public void testScanImage_realBokehHasBokeh() throws Exception{
        Assume.assumeTrue(MediaProvider.ENABLE_MODERN_SCANNER);
        // copy raw file into test folder
        final File file = new File(mDir,sImageTestFileName);
        stage(R.raw.test_image_real_bokeh_has_bokeh,file);
        mModern.scanDirectory(mDir);
        // Confirm that we found new image and scanned it
        try (Cursor cursor = getImageFileFlag(sImageTestFileName)) {
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            final int fileFlag = cursor.getInt(0);
            assertEquals(MediaFileAssist.IMG_TYPE_MODE_REAL_BOKEH_HAS_BOKEH, fileFlag);
        }
        // Delete raw file and confirm it's cleaned up
        assertQueryCountAfterClean(file, sImageTestFileName);
    }

    /**
     * Test image type: {@link com.unisoc.providers.media.MediaFileAssist#IMG_TYPE_MODE_HDR}
     * @throws Exception
     */
    @Test
    public void testScanImage_hdr() throws Exception{
        Assume.assumeTrue(MediaProvider.ENABLE_MODERN_SCANNER);
        // copy raw file into test folder
        final File file = new File(mDir,sImageTestFileName);
        stage(R.raw.test_image_hdr,file);
        mModern.scanDirectory(mDir);
        // Confirm that we found new image and scanned it
        try (Cursor cursor = getImageFileFlag(sImageTestFileName)) {
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            final int fileFlag = cursor.getInt(0);
            assertEquals(MediaFileAssist.IMG_TYPE_MODE_HDR, fileFlag);
        }
        // Delete raw file and confirm it's cleaned up
        assertQueryCountAfterClean(file, sImageTestFileName);
    }

    /**
     * Test image type: {@link com.unisoc.providers.media.MediaFileAssist#IMG_TYPE_MODE_AUDIO_CAPTURE}
     * @throws Exception
     */
    @Test
    public void testScanImage_audioCapture() throws Exception{
        Assume.assumeTrue(MediaProvider.ENABLE_MODERN_SCANNER);
        // copy raw file into test folder
        final File file = new File(mDir,sImageTestFileName);
        stage(R.raw.test_image_audio_capture,file);
        mModern.scanDirectory(mDir);
        // Confirm that we found new image and scanned it
        try (Cursor cursor = getImageFileFlag(sImageTestFileName)) {
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            final int fileFlag = cursor.getInt(0);
            assertEquals(MediaFileAssist.IMG_TYPE_MODE_AUDIO_CAPTURE, fileFlag);
        }
        // Delete raw file and confirm it's cleaned up
        assertQueryCountAfterClean(file, sImageTestFileName);
    }

    /**
     * Test image type: {@link com.unisoc.providers.media.MediaFileAssist#IMG_TYPE_MODE_HDR_AUDIO_CAPTURE}
     * @throws Exception
     */
    @Test
    public void testScanImage_hdrAudioCapture() throws Exception{
        Assume.assumeTrue(MediaProvider.ENABLE_MODERN_SCANNER);
        // copy raw file into test folder
        final File file = new File(mDir,sImageTestFileName);
        stage(R.raw.test_image_hdr_audio_capture,file);
        mModern.scanDirectory(mDir);
        // Confirm that we found new image and scanned it
        try (Cursor cursor = getImageFileFlag(sImageTestFileName)) {
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            final int fileFlag = cursor.getInt(0);
            assertEquals(MediaFileAssist.IMG_TYPE_MODE_HDR_AUDIO_CAPTURE, fileFlag);
        }
        // Delete raw file and confirm it's cleaned up
        assertQueryCountAfterClean(file, sImageTestFileName);
    }

    /**
     * ====================================================================
     *                           DRM FILE TESTS
     * ====================================================================
     */
    private static final String sDrmTestDcfFileName = "test_file.dcf";
    private static final String sDrmTestDmFileName = "test_file.dm";
    private static final String sDrmTestFileMimeType = "application/vnd.oma.drm.content";
    private static final int DRM_PROCESS_OK = 0;
    private DrmManagerClientEx mDrmManagerClient;

    /**
     * processDrmFile parameter incoming check
     */
    @Test
    public void testDrmCommon(){
        String emulatedStorageRoot = "/storage/emulated/0/";
        for (String path : new String[]{
                emulatedStorageRoot + Environment.DIRECTORY_MUSIC,
                emulatedStorageRoot + "file_is_not_exist",
        }) {
            assertNull(mModern.mAssist.processDrmFile(null ,new File(path)));
        }
    }

    private Cursor getFileMimeType(String displayName, Uri uri) throws Exception {
        String[] projection = new String[]{MediaFileAssist.Files.MIME_TYPE};
        String selection = MediaFileAssist.Images.DISPLAY_NAME + "=?";
        String[] selectionArgs = new String[]{displayName};
        Cursor cursor = mIsolatedResolver
                .query(uri, projection, selection, selectionArgs, null);
        return cursor;
    }

    private DrmManagerClientEx getDrmManagerClient(){
        mDrmManagerClient = mModern.mAssist.mDrmManagerClient;
        if(mDrmManagerClient == null){
            mDrmManagerClient = new DrmManagerClientEx(mIsolatedContext);
        }
        return mDrmManagerClient;
    }

    private int getConvertSessionId(String mimeType){
        int id = getDrmManagerClient().openConvertSession(mimeType);
        return id;
    }

    /**
     * We skipped the download process directly,then call the DRM tool class for processing.
     * This ensures that the DUT has drm file copyright after the dm file is converted to a dcf file.
     * The above process should be integrated in the download module, we are only used for subsequent tests can be carried out smoothly.
     *
     * @param fileIn
     * @param fileOut
     * @param sessionId
     * @return
     */
    public int processConvertDrmInfo(File fileIn, File fileOut, int sessionId) {
        final DrmManagerClientEx clientEx = getDrmManagerClient();
        clientEx.setOnEventListener(new DrmManagerClient.OnEventListener() {
            @Override
            public void onEvent(DrmManagerClient drmManagerClient, DrmEvent drmEvent) {
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(fileOut.getPath());
                    DrmInfoStatus status = (DrmInfoStatus) drmEvent.getAttribute(DrmEvent.DRM_INFO_STATUS_OBJECT);
                    ProcessedData data = status.data;
                    byte[] header = data.getData();
                    fos.write(header);
                    while (true) {
                        DrmConvertedStatus convertSatus = clientEx.convertData(sessionId, new byte[1]);
                        if (convertSatus.statusCode == DrmConvertedStatus.STATUS_OK) {
                            final byte[] body = convertSatus.convertedData;
                            if (body != null) {
                                if (body.length > 0) {
                                    fos.write(body);
                                } else {
                                    DrmConvertedStatus close = clientEx.closeConvertSession(sessionId);
                                    final byte[] padding = close.convertedData;
                                    fos.write(padding);
                                    break;
                                }
                            } else {
                                DrmConvertedStatus close = clientEx.closeConvertSession(sessionId);
                                final byte[] padding = close.convertedData;
                                fos.write(padding);
                                break;
                            }
                        } else if (convertSatus.statusCode == DrmConvertedStatus.STATUS_ERROR) {
                            DrmConvertedStatus close = clientEx.closeConvertSession(sessionId);
                            final byte[] padding = close.convertedData;
                            fos.write(padding);
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (fos != null) {
                        DrmConvertedStatus close = clientEx.closeConvertSession(sessionId);
                        try {
                            fos.close();
                            fos = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }
        });
        DrmInfoRequest reqest = new DrmInfoRequest(DrmInfoRequest.TYPE_REGISTRATION_INFO, sDrmTestFileMimeType);
        reqest.put("file_in", fileIn.getPath());
        reqest.put("file_out", fileOut.getPath());
        reqest.put("convert_id", String.valueOf(sessionId));
        DrmInfo info = clientEx.acquireDrmInfo(reqest);
        int result = clientEx.processDrmInfo(info);
        return result;
    }

    /**
     * Test file : FL_audio_midi.dcf
     * @throws Exception
     */
    @Test
    public void testScanFile_FL_audio_midi() throws Exception {
        Assume.assumeTrue(MediaProvider.ENABLE_MODERN_SCANNER);
        // copy raw file into test folder
        final File dm = new File(mDir, sDrmTestDmFileName);
        final File dcf = new File(mDir, sDrmTestDcfFileName);
        stage(R.raw.FL_audio_midi, dm);
        // create drm process session
        int sessionId = getConvertSessionId(sDrmTestFileMimeType);
        if(processConvertDrmInfo(dm, dcf, sessionId) == DRM_PROCESS_OK) {
            mModern.scanDirectory(mDir);
            // Confirm that we found new file and scanned it
            try (Cursor cursor = getFileMimeType(sDrmTestDcfFileName, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)) {
                assertEquals(1, cursor.getCount());
                cursor.moveToFirst();
                final String mimeType = cursor.getString(0);
                assertEquals("audio/sp-midi", mimeType);
                // Delete raw file and confirm it's cleaned up
                assertQueryCountAfterClean(dcf, sDrmTestDcfFileName);
            }
        }else{
            fail("Drm file processing failed .");
        }
    }

    /**
     * Test file : FL_audio_mpeg2.dcf
     * @throws Exception
     */
    @Test
    public void testScanFile_FL_audio_mpeg2() throws Exception {
        Assume.assumeTrue(MediaProvider.ENABLE_MODERN_SCANNER);
        // copy raw file into test folder
        final File dm = new File(mDir, sDrmTestDmFileName);
        final File dcf = new File(mDir, sDrmTestDcfFileName);
        stage(R.raw.FL_audio_mpeg2, dm);
        // create drm process session
        int sessionId = getConvertSessionId(sDrmTestFileMimeType);
        if(processConvertDrmInfo(dm, dcf, sessionId) == DRM_PROCESS_OK) {
            mModern.scanDirectory(mDir);
            // Confirm that we found new file and scanned it
            try (Cursor cursor = getFileMimeType(sDrmTestDcfFileName, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)) {
                assertEquals(1, cursor.getCount());
                cursor.moveToFirst();
                final String mimeType = cursor.getString(0);
                assertEquals("audio/mpeg", mimeType);
                // Delete raw file and confirm it's cleaned up
                assertQueryCountAfterClean(dcf, sDrmTestDcfFileName);
            }
        }else{
            fail("Drm file processing failed .");
        }
    }

    /**
     * Test file : FL_audio_acc.dcf
     * @throws Exception
     */
    @Test
    public void testScanFile_FL_audio_aac() throws Exception {
        Assume.assumeTrue(MediaProvider.ENABLE_MODERN_SCANNER);
        // copy raw file into test folder
        final File dm = new File(mDir, sDrmTestDmFileName);
        final File dcf = new File(mDir, sDrmTestDcfFileName);
        stage(R.raw.FL_audio_acc, dm);
        // create drm process session
        int sessionId = getConvertSessionId(sDrmTestFileMimeType);
        if(processConvertDrmInfo(dm, dcf, sessionId) == DRM_PROCESS_OK) {
            mModern.scanDirectory(mDir);
            // Confirm that we found new file and scanned it
            try (Cursor cursor = getFileMimeType(sDrmTestDcfFileName, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)) {
                assertEquals(1, cursor.getCount());
                cursor.moveToFirst();
                final String mimeType = cursor.getString(0);
                assertEquals("audio/x-aac", mimeType);
                // Delete raw file and confirm it's cleaned up
                assertQueryCountAfterClean(dcf, sDrmTestDcfFileName);
            }
        }else{
            fail("Drm file processing failed .");
        }
    }

    /**
     * Test file : FL_audio_wav.dcf
     * @throws Exception
     */
    @Test
    public void testScanFile_FL_audio_wav() throws Exception {
        Assume.assumeTrue(MediaProvider.ENABLE_MODERN_SCANNER);
        // copy raw file into test folder
        final File dm = new File(mDir, sDrmTestDmFileName);
        final File dcf = new File(mDir, sDrmTestDcfFileName);
        stage(R.raw.FL_audio_wav, dm);
        // create drm process session
        int sessionId = getConvertSessionId(sDrmTestFileMimeType);
        if(processConvertDrmInfo(dm, dcf, sessionId) == DRM_PROCESS_OK) {
            mModern.scanDirectory(mDir);
            // Confirm that we found new file and scanned it
            try (Cursor cursor = getFileMimeType(sDrmTestDcfFileName, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)) {
                assertEquals(1, cursor.getCount());
                cursor.moveToFirst();
                final String mimeType = cursor.getString(0);
                assertEquals("audio/wav", mimeType);
                // Delete raw file and confirm it's cleaned up
                assertQueryCountAfterClean(dcf, sDrmTestDcfFileName);
            }
        }else{
            fail("Drm file processing failed .");
        }
    }

    /**
     * Test file : FL_38_audio_8bit.dcf
     * @throws Exception
     */
    @Test
    public void testScanFile_FL_audio_8bit() throws Exception {
        Assume.assumeTrue(MediaProvider.ENABLE_MODERN_SCANNER);
        // copy raw file into test folder
        final File dm = new File(mDir, sDrmTestDmFileName);
        final File dcf = new File(mDir, sDrmTestDcfFileName);
        stage(R.raw.FL_38_audio_8bit, dm);
        // create drm process session
        int sessionId = getConvertSessionId(sDrmTestFileMimeType);
        if(processConvertDrmInfo(dm, dcf, sessionId) == DRM_PROCESS_OK) {
            mModern.scanDirectory(mDir);
            // Confirm that we found new file and scanned it
            try (Cursor cursor = getFileMimeType(sDrmTestDcfFileName, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)) {
                assertEquals(1, cursor.getCount());
                cursor.moveToFirst();
                final String mimeType = cursor.getString(0);
                assertEquals("audio/amr", mimeType);
                // Delete raw file and confirm it's cleaned up
                assertQueryCountAfterClean(dcf, sDrmTestDcfFileName);
            }
        }else{
            fail("Drm file processing failed .");
        }
    }

    /**
     * Test file : FL_01_binary_bmp_58k_140x140.dcf
     * @throws Exception
     */
    @Test
    public void testScanFile_FL_image_bmp() throws Exception {
        Assume.assumeTrue(MediaProvider.ENABLE_MODERN_SCANNER);
        // copy raw file into test folder
        final File dm = new File(mDir, sDrmTestDmFileName);
        final File dcf = new File(mDir, sDrmTestDcfFileName);
        stage(R.raw.FL_01_binary_bmp_58k_140x140, dm);
        // create drm process session
        int sessionId = getConvertSessionId(sDrmTestFileMimeType);
        if(processConvertDrmInfo(dm, dcf, sessionId) == DRM_PROCESS_OK) {
            mModern.scanDirectory(mDir);
            // Confirm that we found new file and scanned it
            try (Cursor cursor = getFileMimeType(sDrmTestDcfFileName, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)) {
                assertEquals(1, cursor.getCount());
                cursor.moveToFirst();
                final String mimeType = cursor.getString(0);
                assertEquals("image/bmp", mimeType);
                // Delete raw file and confirm it's cleaned up
                assertQueryCountAfterClean(dcf, sDrmTestDcfFileName);
            }
        }else{
            fail("Drm file processing failed .");
        }
    }

    /**
     * Test file : FL_03_binary_gif_51k_369x369.dcf
     * @throws Exception
     */
    @Test
    public void testScanFile_FL_image_gif() throws Exception {
        Assume.assumeTrue(MediaProvider.ENABLE_MODERN_SCANNER);
        // copy raw file into test folder
        final File dm = new File(mDir, sDrmTestDmFileName);
        final File dcf = new File(mDir, sDrmTestDcfFileName);
        stage(R.raw.FL_03_binary_gif_51k_369x369, dm);
        // create drm process session
        int sessionId = getConvertSessionId(sDrmTestFileMimeType);
        if(processConvertDrmInfo(dm, dcf, sessionId) == DRM_PROCESS_OK) {
            mModern.scanDirectory(mDir);
            // Confirm that we found new file and scanned it
            try (Cursor cursor = getFileMimeType(sDrmTestDcfFileName, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)) {
                assertEquals(1, cursor.getCount());
                cursor.moveToFirst();
                final String mimeType = cursor.getString(0);
                assertEquals("image/gif", mimeType);
                // Delete raw file and confirm it's cleaned up
                assertQueryCountAfterClean(dcf, sDrmTestDcfFileName);
            }
        }else{
            fail("Drm file processing failed .");
        }
    }

    /**
     * Test file : FL_05_binary_jpg_43k_320x240.dcf
     * @throws Exception
     */
    @Test
    public void testScanFile_FL_image_jpg() throws Exception {
        Assume.assumeTrue(MediaProvider.ENABLE_MODERN_SCANNER);
        // copy raw file into test folder
        final File dm = new File(mDir, sDrmTestDmFileName);
        final File dcf = new File(mDir, sDrmTestDcfFileName);
        stage(R.raw.FL_05_binary_jpg_43k_320x240, dm);
        // create drm process session
        int sessionId = getConvertSessionId(sDrmTestFileMimeType);
        if(processConvertDrmInfo(dm, dcf, sessionId) == DRM_PROCESS_OK) {
            mModern.scanDirectory(mDir);
            // Confirm that we found new file and scanned it
            try (Cursor cursor = getFileMimeType(sDrmTestDcfFileName, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)) {
                assertEquals(1, cursor.getCount());
                cursor.moveToFirst();
                final String mimeType = cursor.getString(0);
                assertEquals("image/jpeg", mimeType);
                // Delete raw file and confirm it's cleaned up
                assertQueryCountAfterClean(dcf, sDrmTestDcfFileName);
            }
        }else{
            fail("Drm file processing failed .");
        }
    }

    /**
     * Test file : FL_07_base_png_97k_300x256.dcf
     * @throws Exception
     */
    @Test
    public void testScanFile_FL_image_png() throws Exception {
        Assume.assumeTrue(MediaProvider.ENABLE_MODERN_SCANNER);
        // copy raw file into test folder
        final File dm = new File(mDir, sDrmTestDmFileName);
        final File dcf = new File(mDir, sDrmTestDcfFileName);
        stage(R.raw.FL_07_base_png_97k_300x256, dm);
        // create drm process session
        int sessionId = getConvertSessionId(sDrmTestFileMimeType);
        if(processConvertDrmInfo(dm, dcf, sessionId) == DRM_PROCESS_OK) {
            mModern.scanDirectory(mDir);
            // Confirm that we found new file and scanned it
            try (Cursor cursor = getFileMimeType(sDrmTestDcfFileName, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)) {
                assertEquals(1, cursor.getCount());
                cursor.moveToFirst();
                final String mimeType = cursor.getString(0);
                assertEquals("image/png", mimeType);
                // Delete raw file and confirm it's cleaned up
                assertQueryCountAfterClean(dcf, sDrmTestDcfFileName);
            }
        }else{
            fail("Drm file processing failed .");
        }
    }

    /**
     * Test file : FL_3GP_H263_128X96_Binary_83K_.dcf
     * @throws Exception
     */
    @Test
    public void testScanFile_FL_video_3gp() throws Exception {
        Assume.assumeTrue(MediaProvider.ENABLE_MODERN_SCANNER);
        // copy raw file into test folder
        final File dm = new File(mDir, sDrmTestDmFileName);
        final File dcf = new File(mDir, sDrmTestDcfFileName);
        stage(R.raw.FL_3GP_H263_128X96_Binary_83K_, dm);
        // create drm process session
        int sessionId = getConvertSessionId(sDrmTestFileMimeType);
        if(processConvertDrmInfo(dm, dcf, sessionId) == DRM_PROCESS_OK) {
            mModern.scanDirectory(mDir);
            // Confirm that we found new file and scanned it
            try (Cursor cursor = getFileMimeType(sDrmTestDcfFileName, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)) {
                assertEquals(1, cursor.getCount());
                cursor.moveToFirst();
                final String mimeType = cursor.getString(0);
                assertEquals("video/3gpp", mimeType);
                // Delete raw file and confirm it's cleaned up
                assertQueryCountAfterClean(dcf, sDrmTestDcfFileName);
            }
        }else{
            fail("Drm file processing failed .");
        }
    }

    /**
     * Test file : FL_33_video_8bit.dcf
     * @throws Exception
     */
    @Test
    public void testScanFile_FL_video_8bit() throws Exception {
        Assume.assumeTrue(MediaProvider.ENABLE_MODERN_SCANNER);
        // copy raw file into test folder
        final File dm = new File(mDir, sDrmTestDmFileName);
        final File dcf = new File(mDir, sDrmTestDcfFileName);
        stage(R.raw.FL_33_video_8bit, dm);
        // create drm process session
        int sessionId = getConvertSessionId(sDrmTestFileMimeType);
        if(processConvertDrmInfo(dm, dcf, sessionId) == DRM_PROCESS_OK) {
            mModern.scanDirectory(mDir);
            // Confirm that we found new file and scanned it
            try (Cursor cursor = getFileMimeType(sDrmTestDcfFileName, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)) {
                assertEquals(1, cursor.getCount());
                cursor.moveToFirst();
                final String mimeType = cursor.getString(0);
                assertEquals("video/3gpp", mimeType);
                // Delete raw file and confirm it's cleaned up
                assertQueryCountAfterClean(dcf, sDrmTestDcfFileName);
            }
        }else{
            fail("Drm file processing failed .");
        }
    }

    /**
     * Test file : CD_20_bin_norights_png_97k_300x256.dcf
     * @throws Exception
     */
    @Test
    public void testScanFile_CD_image_png() throws Exception {
        Assume.assumeTrue(MediaProvider.ENABLE_MODERN_SCANNER);
        // copy raw file into test folder
        final File dm = new File(mDir, sDrmTestDmFileName);
        final File dcf = new File(mDir, sDrmTestDcfFileName);
        stage(R.raw.CD_20_bin_norights_png_97k_300x256, dm);
        // create drm process session
        int sessionId = getConvertSessionId(sDrmTestFileMimeType);
        if(processConvertDrmInfo(dm, dcf, sessionId) == DRM_PROCESS_OK) {
            mModern.scanDirectory(mDir);
            // Confirm that we found new file and scanned it
            try (Cursor cursor = getFileMimeType(sDrmTestDcfFileName, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)) {
                assertEquals(1, cursor.getCount());
                cursor.moveToFirst();
                final String mimeType = cursor.getString(0);
                assertEquals("image/png", mimeType);
                // Delete raw file and confirm it's cleaned up
                assertQueryCountAfterClean(dcf, sDrmTestDcfFileName);
            }
        }else{
            fail("Drm file processing failed .");
        }
    }

    /**
     * Test file : CD_2_bas_3count_bmp_58k_140x140.dcf
     * @throws Exception
     */
    @Test
    public void testScanFile_CD_image_bmp() throws Exception {
        Assume.assumeTrue(MediaProvider.ENABLE_MODERN_SCANNER);
        // copy raw file into test folder
        final File dm = new File(mDir, sDrmTestDmFileName);
        final File dcf = new File(mDir, sDrmTestDcfFileName);
        stage(R.raw.CD_2_bas_3count_bmp_58k_140x140, dm);
        // create drm process session
        int sessionId = getConvertSessionId(sDrmTestFileMimeType);
        if(processConvertDrmInfo(dm, dcf, sessionId) == DRM_PROCESS_OK) {
            mModern.scanDirectory(mDir);
            // Confirm that we found new file and scanned it
            try (Cursor cursor = getFileMimeType(sDrmTestDcfFileName, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)) {
                assertEquals(1, cursor.getCount());
                cursor.moveToFirst();
                final String mimeType = cursor.getString(0);
                assertEquals("image/bmp", mimeType);
                // Delete raw file and confirm it's cleaned up
                assertQueryCountAfterClean(dcf, sDrmTestDcfFileName);
            }
        }else{
            fail("Drm file processing failed .");
        }
    }

    /**
     * Test file : CD_2_bas_5count_aac_207k_16kh_VBR.dcf
     * @throws Exception
     */
    @Test
    public void testScanFile_CD_audio_aac() throws Exception {
        Assume.assumeTrue(MediaProvider.ENABLE_MODERN_SCANNER);
        // copy raw file into test folder
        final File dm = new File(mDir, sDrmTestDmFileName);
        final File dcf = new File(mDir, sDrmTestDcfFileName);
        stage(R.raw.CD_2_bas_5count_aac_207k_16kh_VBR, dm);
        // create drm process session
        int sessionId = getConvertSessionId(sDrmTestFileMimeType);
        if(processConvertDrmInfo(dm, dcf, sessionId) == DRM_PROCESS_OK) {
            mModern.scanDirectory(mDir);
            // Confirm that we found new file and scanned it
            try (Cursor cursor = getFileMimeType(sDrmTestDcfFileName, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)) {
                assertEquals(1, cursor.getCount());
                cursor.moveToFirst();
                final String mimeType = cursor.getString(0);
                assertEquals("audio/x-aac", mimeType);
                // Delete raw file and confirm it's cleaned up
                assertQueryCountAfterClean(dcf, sDrmTestDcfFileName);
            }
        }else{
            fail("Drm file processing failed .");
        }
    }

    /**
     * Test file : CD_34_bas_10min_2014_3_3_2015_3_3_MIDI_13k.dcf
     * @throws Exception
     */
    @Test
    public void testScanFile_CD_audio_midi() throws Exception {
        Assume.assumeTrue(MediaProvider.ENABLE_MODERN_SCANNER);
        // copy raw file into test folder
        final File dm = new File(mDir, sDrmTestDmFileName);
        final File dcf = new File(mDir, sDrmTestDcfFileName);
        stage(R.raw.CD_34_bas_10min_2014_3_3_2015_3_3_MIDI_13k, dm);
        // create drm process session
        int sessionId = getConvertSessionId(sDrmTestFileMimeType);
        if(processConvertDrmInfo(dm, dcf, sessionId) == DRM_PROCESS_OK) {
            mModern.scanDirectory(mDir);
            // Confirm that we found new file and scanned it
            try (Cursor cursor = getFileMimeType(sDrmTestDcfFileName, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)) {
                assertEquals(1, cursor.getCount());
                cursor.moveToFirst();
                final String mimeType = cursor.getString(0);
                assertEquals("audio/sp-midi", mimeType);
                // Delete raw file and confirm it's cleaned up
                assertQueryCountAfterClean(dcf, sDrmTestDcfFileName);
            }
        }else{
            fail("Drm file processing failed .");
        }
    }

    /**
     * Test file : CD_13_bas_min6_count_3gp_936k_H263.dcf
     * @throws Exception
     */
    @Test
    public void testScanFile_CD_video_3gp() throws Exception {
        Assume.assumeTrue(MediaProvider.ENABLE_MODERN_SCANNER);
        // copy raw file into test folder
        final File dm = new File(mDir, sDrmTestDmFileName);
        final File dcf = new File(mDir, sDrmTestDcfFileName);
        stage(R.raw.CD_13_bas_min6_count_3gp_936k_H263, dm);
        // create drm process session
        int sessionId = getConvertSessionId(sDrmTestFileMimeType);
        if(processConvertDrmInfo(dm, dcf, sessionId) == DRM_PROCESS_OK) {
            mModern.scanDirectory(mDir);
            // Confirm that we found new file and scanned it
            try (Cursor cursor = getFileMimeType(sDrmTestDcfFileName, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)) {
                assertEquals(1, cursor.getCount());
                cursor.moveToFirst();
                final String mimeType = cursor.getString(0);
                assertEquals("video/3gpp", mimeType);
                // Delete raw file and confirm it's cleaned up
                assertQueryCountAfterClean(dcf, sDrmTestDcfFileName);
            }
        }else{
            fail("Drm file processing failed .");
        }
    }
}
