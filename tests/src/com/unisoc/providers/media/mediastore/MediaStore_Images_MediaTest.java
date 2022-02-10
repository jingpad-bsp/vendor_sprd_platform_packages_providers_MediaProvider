/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.unisoc.providers.media.mediastore;

import static com.unisoc.providers.media.mediastore.MediaStoreTest.TAG;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.storage.StorageManager;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Images.Media;
import com.unisoc.providers.media.mediastore.MediaStoreUtils.PendingParams;
import com.unisoc.providers.media.mediastore.MediaStoreUtils.PendingSession;
import android.util.Log;
import android.util.Size;
import java.io.FileOutputStream;
import androidx.test.InstrumentationRegistry;
import com.android.providers.media.tests.R;


import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@RunWith(Parameterized.class)
public class MediaStore_Images_MediaTest {
    private static final String MIME_TYPE_JPEG = "image/jpeg";

    private Context mContext;
    private ContentResolver mContentResolver;

    private Uri mExternalImages;

    @Parameter(0)
    public String mVolumeName;

    @Parameters
    public static Iterable<? extends Object> data() {
        return ProviderTestUtils.getSharedVolumeNames();
    }

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getContext();
        mContentResolver = mContext.getContentResolver();

        Log.d(TAG, "Using volume " + mVolumeName);
        mExternalImages = MediaStore.Images.Media.getContentUri(mVolumeName);
    }

    

   

    private void cleanExternalMediaFile(String path) {
        mContentResolver.delete(mExternalImages, "_data=?", new String[] { path });
        new File(path).delete();
    }


    public static void createFile(File file, int numBytes) throws IOException {
        Log.d(TAG,"createFile -> file.exists: "+ file.exists());
        Log.d(TAG,"createFile -> file.length: "+ file.length());
        Log.d(TAG,"createFile -> file.canWrite: "+ file.canWrite());
        Log.d(TAG,"createFile -> file.getAbsolutePath: "+ file.getAbsolutePath());
//        File parentFile = file.getParentFile();
//        if (parentFile != null) {
//            Log.d(TAG,"createFile -> parentFile.mkdirs(): "+ parentFile.getAbsolutePath());
//            parentFile.mkdirs();
//        }
        byte[] buffer = new byte[numBytes];
        FileOutputStream output = new FileOutputStream(file);
        try {
            output.write(buffer);
        } finally {
            output.close();
        }
    }

    @Test
    public void testStoreImagesMediaExternal() throws Exception {
        final String externalPath = new File(ProviderTestUtils.stageDir(mVolumeName),
                "testimage.jpg").getAbsolutePath();
        final String externalPath2 = new File(ProviderTestUtils.stageDir(mVolumeName),
                "testimage1.jpg").getAbsolutePath();

				
	    Log.d(TAG,"externalPath:"+ externalPath);
				
        // clean up any potential left over entries from a previous aborted run
        cleanExternalMediaFile(externalPath);
        cleanExternalMediaFile(externalPath2);

        int numBytes = 1337;
        File audioFile = new File(externalPath);
        audioFile.delete();
       // createFile(audioFile, numBytes);
        createFile(audioFile, numBytes);

        Log.d(TAG,"end createFile -> file.exists: "+ audioFile.exists());
        Log.d(TAG,"end createFile -> file.length: "+ audioFile.length());
        Log.d(TAG,"end createFile -> file.canWrite: "+ audioFile.canWrite());
        Log.d(TAG,"end createFile -> file.getAbsolutePath: "+ audioFile.getAbsolutePath());


        ContentValues values = new ContentValues();
        values.put(Media.ORIENTATION, 0);
        values.put(Media.PICASA_ID, 0);
        long dateTaken = System.currentTimeMillis();
        values.put(Media.DATE_TAKEN, dateTaken);
        values.put(Media.DESCRIPTION, "This is a image");
        values.put(Media.IS_PRIVATE, 1);
        values.put(Media.MINI_THUMB_MAGIC, 0);
        values.put(Media.DATA, externalPath);
        values.put(Media.DISPLAY_NAME, "testimage");
        values.put(Media.MIME_TYPE, "image/jpeg");
        values.put(Media.SIZE, numBytes);
        values.put(Media.TITLE, "testimage");
        long dateAdded = System.currentTimeMillis() / 1000;
        values.put(Media.DATE_ADDED, dateAdded);
        long dateModified = System.currentTimeMillis() / 1000;
        values.put(Media.DATE_MODIFIED, dateModified);

        // insert
        Uri uri = mContentResolver.insert(mExternalImages, values);
        assertNotNull(uri);

        try {
            // query
            Cursor c = mContentResolver.query(uri, null, null, null, null);
            assertEquals(1, c.getCount());
            c.moveToFirst();
            long id = c.getLong(c.getColumnIndex(Media._ID));
            assertTrue(id > 0);
            assertEquals(0, c.getInt(c.getColumnIndex(Media.ORIENTATION)));
            assertEquals(0, c.getLong(c.getColumnIndex(Media.PICASA_ID)));
            assertEquals(dateTaken, c.getLong(c.getColumnIndex(Media.DATE_TAKEN)));
            assertEquals("This is a image",
                    c.getString(c.getColumnIndex(Media.DESCRIPTION)));
            assertEquals(1, c.getInt(c.getColumnIndex(Media.IS_PRIVATE)));
            assertEquals(0, c.getLong(c.getColumnIndex(Media.MINI_THUMB_MAGIC)));
            assertEquals(externalPath, c.getString(c.getColumnIndex(Media.DATA)));
            assertEquals("testimage.jpg", c.getString(c.getColumnIndex(Media.DISPLAY_NAME)));
            assertEquals("image/jpeg", c.getString(c.getColumnIndex(Media.MIME_TYPE)));
            assertEquals("testimage", c.getString(c.getColumnIndex(Media.TITLE)));
            assertEquals(numBytes, c.getInt(c.getColumnIndex(Media.SIZE)));
            long realDateAdded = c.getLong(c.getColumnIndex(Media.DATE_ADDED));
            assertTrue(realDateAdded >= dateAdded);
            // there can be delay as time is read after creation
            assertTrue(Math.abs(dateModified - c.getLong(c.getColumnIndex(Media.DATE_MODIFIED)))
                       < 5);
            c.close();
        } finally {
            // delete
            assertEquals(1, mContentResolver.delete(uri, null, null));
            new File(externalPath).delete();
        }
    }

    private void assertInsertionSuccess(String stringUrl) throws IOException {
        final Uri uri = Uri.parse(stringUrl);

        // check whether the thumbnails are generated
        try (Cursor c = mContentResolver.query(uri, null, null, null)) {
            assertEquals(1, c.getCount());
        }

        assertNotNull(mContentResolver.loadThumbnail(uri, new Size(512, 384), null));
        assertNotNull(mContentResolver.loadThumbnail(uri, new Size(96, 96), null));
    }

}
