package com.unisoc.providers.media;

import static android.media.MediaFile.getFormatCode;
import static android.media.MediaFile.getMimeType;
import static android.media.MediaFile.isAudioMimeType;
import static android.media.MediaFile.isImageMimeType;
import static android.media.MediaFile.isPlayListMimeType;
import static android.media.MediaFile.isVideoMimeType;
import static android.media.MediaFile.isDrmMimeType;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import java.util.Locale;
import android.mtp.MtpConstants;

import androidx.test.runner.AndroidJUnit4;

import libcore.net.MimeUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Objects;

@RunWith(AndroidJUnit4.class)
public class MediaFileAssistTest {

    @Test
    public void testImageType() throws Exception {
        assertTrue(isImageMimeType("image/jpeg"));
        assertTrue(isImageMimeType("image/bmp"));
    }

    @Test
    public void testAudioType() throws Exception {
        assertTrue(isAudioMimeType("audio/x-mp3"));
        assertTrue(isAudioMimeType("audio/mpeg3"));
        assertTrue(isAudioMimeType("audio/mp3"));
        assertTrue(isAudioMimeType("audio/mpg3"));
        assertTrue(isAudioMimeType("audio/mp4"));
        //assertTrue(isAudioMimeType("audio/3gpp"));
        //assertTrue(isAudioMimeType("audio/3gpp2"));
        assertTrue(isAudioMimeType("audio/opus"));
        assertTrue(isAudioMimeType("audio/mpeg"));
        assertTrue(isAudioMimeType("audio/mpeg4"));
        assertTrue(isAudioMimeType("audio/mid"));
        assertTrue(isAudioMimeType("audio/x-midi"));
        assertTrue(isAudioMimeType("audio/imy"));
        assertTrue(isAudioMimeType("audio/bp3"));
        assertTrue(isAudioMimeType("audio/wav"));
        assertTrue(isAudioMimeType("audio/x-aac"));
    }

    @Test
    public void testVideoType() throws Exception {
        assertTrue(isVideoMimeType("video/3g2"));
        assertTrue(isVideoMimeType("video/flv"));
        assertTrue(isVideoMimeType("video/mp4"));
        assertTrue(isVideoMimeType("video/avi"));
        assertTrue(isVideoMimeType("video/amc"));
        assertTrue(isVideoMimeType("video/k3g"));
        assertTrue(isVideoMimeType("video/mp2p"));
    }

    @Test
    public void testDrmType() throws Exception {
        assertTrue(isDrmMimeType("application/vnd.oma.drm.content"));
    }

    private static void assertMimeTypeFromExtension(String mimeType, String extension) {
        final String actual = MimeUtils.guessMimeTypeFromExtension(extension);
        if (!Objects.equals(mimeType, actual)) {
            fail("Expected " + mimeType + " but was " + actual + " for extension " + extension);
        }
    }

    private static void assertExtensionFromMimeType(String extension, String mimeType) {
        final String actual = MimeUtils.guessExtensionFromMimeType(mimeType);
        if (!Objects.equals(extension, actual)) {
            fail("Expected " + extension + " but was " + actual + " for type " + mimeType);
        }
    }

    /**
     *      audio/opus opus
     *      audio/mid mmid
     *      audio/x-midi xmid
     *      audio/bp3 bp3
     *      audio/wav wav
     *      audio/x-aac aac
     *      audio/imy imy
     *      audio/mpeg4 dm
     *      video/flv flv
     *      video/amc amc
     *      video/k3g k3g
     *      application/application apk
     */
    @Test
    public void testSingleExtensionFromSingleMimeType(){
        assertExtensionFromMimeType("opus","audio/opus");
        assertExtensionFromMimeType("mmid","audio/mid");
        assertExtensionFromMimeType("xmid","audio/x-midi");
        assertExtensionFromMimeType("bp3","audio/bp3");
        assertExtensionFromMimeType("wav","audio/wav");
        assertExtensionFromMimeType("aac","audio/x-aac");
        assertExtensionFromMimeType("imy","audio/imy");
        assertExtensionFromMimeType("flv","video/flv");
        assertExtensionFromMimeType("amc","video/amc");
        assertExtensionFromMimeType("k3g","video/k3g");
        assertExtensionFromMimeType("apk","application/application");
    }

    @Test
    public void testSingleMimeTypeFromSingleExtension(){
        assertMimeTypeFromExtension("audio/opus","opus");
        assertMimeTypeFromExtension("audio/mid","mmid");
        assertMimeTypeFromExtension("audio/x-midi","xmid");
        assertMimeTypeFromExtension("audio/bp3","bp3");
        assertMimeTypeFromExtension("audio/wav","wav");
        assertMimeTypeFromExtension("audio/aac","aac");//override by aosp
        assertMimeTypeFromExtension("audio/imelody","imy");//override by aosp
        assertMimeTypeFromExtension("video/x-flv","flv");//override by mime.type
        assertMimeTypeFromExtension("video/amc","amc");
        assertMimeTypeFromExtension("video/k3g","k3g");
        assertMimeTypeFromExtension("application/vnd.android.package-archive","apk");//override by aosp
    }

    /**
     *      audio/3gpp2 3g2
     *      video/3g2 3g2
     *      video/3gpp2 3g2
     *      audio/x-mp3 mp3
     *      audio/mpeg3 mp3
     *      audio/mp3 mp3
     *      audio/mpg3 mp3
     */
    @Test
    public void testSingleExtensionFromMultiMimeType(){
        assertExtensionFromMimeType("3g2","audio/3gpp2");
        assertExtensionFromMimeType("3g2","video/3g2");
        assertExtensionFromMimeType("mp3","audio/x-mp3");
        assertExtensionFromMimeType("mp3","audio/mpeg3");
        assertExtensionFromMimeType("mp3","audio/mp3");
        assertExtensionFromMimeType("mp3","audio/mpg3");
    }

    @Test
    public void testMultiMimeTypeFromSingleExtension(){
        assertMimeTypeFromExtension("video/3gpp2","3g2");//override by aosp
        assertMimeTypeFromExtension("audio/mpeg","mp3");//override by aosp
    }


    /**
     *      image/bmp bmp drmbmp
     *      image/jpeg jpg! jpe
     *      audio/mp4 m4a m4b m4r
     *      audio/3gpp 3gpp 3gp
     *      audio/mpeg mp3! m4a m4r mp2
     *      video/mp4 mp4 m4v
     *      video/avi avi divx
     *      video/mp2p mpeg vob
     */
    @Test
    public void testMultiExtensionFromSingleMimeType(){
        assertExtensionFromMimeType("bmp","image/bmp");
        assertExtensionFromMimeType("jpg","image/jpeg");
        assertExtensionFromMimeType("m4a","audio/mp4");
        assertExtensionFromMimeType("3gpp","audio/3gpp");
        assertExtensionFromMimeType("mp3","audio/mpeg");
        assertExtensionFromMimeType("mp4","video/mp4");
        assertExtensionFromMimeType("avi","video/avi");
        assertExtensionFromMimeType("mpeg","video/mp2p");
    }

    @Test
    public void testSingleMimeTypeFromMultiExtension(){
        assertMimeTypeFromExtension("image/x-ms-bmp","bmp");//override by aosp
        assertMimeTypeFromExtension("image/bmp","drmbmp");
        assertMimeTypeFromExtension("audio/mpeg","m4a");
        assertMimeTypeFromExtension("audio/mp4","m4b");
        assertMimeTypeFromExtension("audio/mpeg","m4r");
        assertMimeTypeFromExtension("audio/mpeg","mp3");
        assertMimeTypeFromExtension("audio/mpeg","m4a");
        assertMimeTypeFromExtension("audio/mpeg","m4r");
        assertMimeTypeFromExtension("audio/mpeg","mp2");//support mp2 from Bug #923889
        assertMimeTypeFromExtension("video/mp4","mp4");
        assertMimeTypeFromExtension("video/mp4","m4v");
        assertMimeTypeFromExtension("video/avi","avi");
        assertMimeTypeFromExtension("video/avi","divx");
        assertMimeTypeFromExtension("video/mpeg","mpeg");//override by aosp
        assertMimeTypeFromExtension("video/3gpp","3gpp");//override by aosp
        assertMimeTypeFromExtension("video/mp2p","vob");
    }

    /**
     * Drm file extension test
     */
    @Test
    public void testDrmExtensionFromMimeType(){
        assertExtensionFromMimeType("dm","application/vnd.oma.drm.message");
        assertExtensionFromMimeType("dcf","application/vnd.oma.drm.content");
    }

    /**
     * Drm file mimetype test
     */
    @Test
    public void testDrmMimeTypeFromExtension(){
        assertMimeTypeFromExtension("application/vnd.oma.drm.message","dm");
        assertMimeTypeFromExtension("application/vnd.oma.drm.content","dcf");
    }

    /**
     * Empty extension/mimetype test
     */
    @Test
    public void test_invalid_empty() {
        checkInvalidExtension("");
        checkInvalidMimeType("");
    }

    /**
     * Null extension/mimetype test
     */
    @Test
    public void test_invalid_null() {
        checkInvalidExtension(null);
        checkInvalidMimeType(null);
    }

    /**
     * Invalid extension/mimetype test
     */
    @Test
    public void test_invalid() {
        checkInvalidMimeType("invalid mime type");
        checkInvalidExtension("invalid extension");
    }

    /**
     * ====================================================================
     *                           MimeUtils CTS TESTS
     * ====================================================================
     */
    @Test
    public void test_15715370() {
        assertEquals("audio/flac", MimeUtils.guessMimeTypeFromExtension("flac"));
        assertEquals("flac", MimeUtils.guessExtensionFromMimeType("audio/flac"));
        assertEquals("flac", MimeUtils.guessExtensionFromMimeType("application/x-flac"));
    }

    @Test
    // https://code.google.com/p/android/issues/detail?id=78909
    public void test_78909() {
        assertEquals("mka", MimeUtils.guessExtensionFromMimeType("audio/x-matroska"));
        assertEquals("mkv", MimeUtils.guessExtensionFromMimeType("video/x-matroska"));
    }

    @Test
    public void test_16978217() {
        assertEquals("image/x-ms-bmp", MimeUtils.guessMimeTypeFromExtension("bmp"));
        assertEquals("image/x-icon", MimeUtils.guessMimeTypeFromExtension("ico"));
        assertEquals("video/mp2ts", MimeUtils.guessMimeTypeFromExtension("ts"));
    }

    @Test
    public void testCommon() {
        assertEquals("audio/mpeg", MimeUtils.guessMimeTypeFromExtension("mp3"));
        assertEquals("image/png", MimeUtils.guessMimeTypeFromExtension("png"));
        assertEquals("application/zip", MimeUtils.guessMimeTypeFromExtension("zip"));

        assertEquals("mp3", MimeUtils.guessExtensionFromMimeType("audio/mpeg"));
        assertEquals("png", MimeUtils.guessExtensionFromMimeType("image/png"));
        assertEquals("zip", MimeUtils.guessExtensionFromMimeType("application/zip"));
    }

    @Test
    public void test_18390752() {
        assertEquals("jpg", MimeUtils.guessExtensionFromMimeType("image/jpeg"));
    }

    @Test
    public void test_30207891() {
        assertTrue(MimeUtils.hasMimeType("IMAGE/PNG"));
        assertTrue(MimeUtils.hasMimeType("IMAGE/png"));
        assertFalse(MimeUtils.hasMimeType(""));
        assertEquals("png", MimeUtils.guessExtensionFromMimeType("IMAGE/PNG"));
        assertEquals("png", MimeUtils.guessExtensionFromMimeType("IMAGE/png"));
        assertNull(MimeUtils.guessMimeTypeFromExtension(""));
        assertNull(MimeUtils.guessMimeTypeFromExtension("doesnotexist"));
        assertTrue(MimeUtils.hasExtension("PNG"));
        assertTrue(MimeUtils.hasExtension("PnG"));
        assertFalse(MimeUtils.hasExtension(""));
        assertFalse(MimeUtils.hasExtension(".png"));
        assertEquals("image/png", MimeUtils.guessMimeTypeFromExtension("PNG"));
        assertEquals("image/png", MimeUtils.guessMimeTypeFromExtension("PnG"));
        assertNull(MimeUtils.guessMimeTypeFromExtension(".png"));
        assertNull(MimeUtils.guessMimeTypeFromExtension(""));
        assertNull(MimeUtils.guessExtensionFromMimeType("doesnotexist"));
    }

    @Test
    public void test_30793548() {
        assertEquals("video/3gpp", MimeUtils.guessMimeTypeFromExtension("3gpp"));
        assertEquals("video/3gpp", MimeUtils.guessMimeTypeFromExtension("3gp"));
        assertEquals("video/3gpp2", MimeUtils.guessMimeTypeFromExtension("3gpp2"));
        assertEquals("video/3gpp2", MimeUtils.guessMimeTypeFromExtension("3g2"));
    }

    @Test
    public void test_37167977() {
        // https://tools.ietf.org/html/rfc5334#section-10.1
        assertEquals("audio/ogg", MimeUtils.guessMimeTypeFromExtension("ogg"));
        assertEquals("audio/ogg", MimeUtils.guessMimeTypeFromExtension("oga"));
        assertEquals("audio/ogg", MimeUtils.guessMimeTypeFromExtension("spx"));
        assertEquals("video/ogg", MimeUtils.guessMimeTypeFromExtension("ogv"));
    }

    @Test
    public void test_70851634_mimeTypeFromExtension() {
        assertEquals("video/vnd.youtube.yt", MimeUtils.guessMimeTypeFromExtension("yt"));
    }

    @Test
    public void test_70851634_extensionFromMimeType() {
        assertEquals("yt", MimeUtils.guessExtensionFromMimeType("video/vnd.youtube.yt"));
        assertEquals("yt", MimeUtils.guessExtensionFromMimeType("application/vnd.youtube.yt"));
    }

    @Test
    public void test_112162449_audio() {
        // According to https://en.wikipedia.org/wiki/M3U#Internet_media_types
        // this is a giant mess, so we pick "audio/x-mpegurl" because a similar
        // playlist format uses "audio/x-scpls".
        assertMimeTypeFromExtension("audio/x-mpegurl", "m3u");
        assertMimeTypeFromExtension("audio/x-mpegurl", "m3u8");
        assertExtensionFromMimeType("m3u", "audio/x-mpegurl");

        assertExtensionFromMimeType("m4a", "audio/mp4");
        assertMimeTypeFromExtension("audio/mpeg", "m4a");

        assertBidirectional("audio/aac", "aac");
    }

    @Test
    public void test_112162449_video() {
        assertBidirectional("video/x-flv", "flv");
        assertBidirectional("video/quicktime", "mov");
        assertBidirectional("video/mpeg", "mpeg");
    }

    @Test
    public void test_112162449_image() {
        assertBidirectional("image/heif", "heif");
        assertBidirectional("image/heif-sequence", "heifs");
        assertBidirectional("image/heic", "heic");
        assertBidirectional("image/heic-sequence", "heics");
        assertMimeTypeFromExtension("image/heif", "hif");

        assertBidirectional("image/x-adobe-dng", "dng");
        assertBidirectional("image/x-photoshop", "psd");

        assertBidirectional("image/jp2", "jp2");
        assertMimeTypeFromExtension("image/jp2", "jpg2");
    }

    @Test
    public void test_120135571_audio() {
        assertMimeTypeFromExtension("audio/mpeg", "m4r");
    }

    @Test
    public void testWifiConfig_xml() {
        assertExtensionFromMimeType("xml", "application/x-wifi-config");
        assertMimeTypeFromExtension("text/xml", "xml");
    }

    @Test// http://b/122734564
    public void testNonLowercaseMimeType() {
        // A mixed-case mimeType that appears in mime.types; we expect guessMimeTypeFromExtension()
        // to return it in lowercase because MimeUtils considers lowercase to be the canonical form.
        String mimeType = "application/vnd.ms-word.document.macroEnabled.12".toLowerCase(Locale.US);
        assertBidirectional(mimeType, "docm");
    }

    @Test// Check that the keys given for lookups in either direction are not case sensitive
    public void testCaseInsensitiveKeys() {
        String mimeType = MimeUtils.guessMimeTypeFromExtension("apk");
        assertNotNull(mimeType);

        assertEquals(mimeType, MimeUtils.guessMimeTypeFromExtension("APK"));
        assertEquals(mimeType, MimeUtils.guessMimeTypeFromExtension("aPk"));

        assertEquals("apk", MimeUtils.guessExtensionFromMimeType(mimeType));
        assertEquals("apk", MimeUtils.guessExtensionFromMimeType(mimeType.toUpperCase(Locale.US)));
        assertEquals("apk", MimeUtils.guessExtensionFromMimeType(mimeType.toLowerCase(Locale.US)));
    }

    private static void checkInvalidExtension(String s) {
        assertFalse(MimeUtils.hasExtension(s));
        assertNull(MimeUtils.guessMimeTypeFromExtension(s));
    }

    private static void checkInvalidMimeType(String s) {
        assertFalse(MimeUtils.hasMimeType(s));
        assertNull(MimeUtils.guessExtensionFromMimeType(s));
    }

    private static void assertBidirectional(String mimeType, String extension) {
        assertMimeTypeFromExtension(mimeType, extension);
        assertExtensionFromMimeType(extension, mimeType);
    }
}