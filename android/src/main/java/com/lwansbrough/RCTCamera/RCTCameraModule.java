/**
 * Created by Fabrice Armisen (farmisen@gmail.com) on 1/4/16.
 * Android video recording support by Marc Johnson (me@marc.mn) 4/2016
 */

package com.lwansbrough.RCTCamera;

import android.content.ContentValues;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaActionSound;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Surface;
import com.facebook.react.bridge.*;

import javax.annotation.Nullable;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class RCTCameraModule extends ReactContextBaseJavaModule implements MediaRecorder.OnInfoListener {
    private static final String TAG = "RCTCameraModule";
    private static RCTCameraModule ourInstance;

    public static final int RCT_CAMERA_ASPECT_FILL = 0;
    public static final int RCT_CAMERA_ASPECT_FIT = 1;
    public static final int RCT_CAMERA_ASPECT_STRETCH = 2;
    public static final int RCT_CAMERA_CAPTURE_MODE_STILL = 0;
    public static final int RCT_CAMERA_CAPTURE_MODE_VIDEO = 1;
    public static final int RCT_CAMERA_CAPTURE_TARGET_MEMORY = 0;
    public static final int RCT_CAMERA_CAPTURE_TARGET_DISK = 1;
    public static final int RCT_CAMERA_CAPTURE_TARGET_CAMERA_ROLL = 2;
    public static final int RCT_CAMERA_CAPTURE_TARGET_TEMP = 3;
    public static final int RCT_CAMERA_ORIENTATION_AUTO = Integer.MAX_VALUE;
    public static final int RCT_CAMERA_ORIENTATION_PORTRAIT = Surface.ROTATION_0;
    public static final int RCT_CAMERA_ORIENTATION_PORTRAIT_UPSIDE_DOWN = Surface.ROTATION_180;
    public static final int RCT_CAMERA_ORIENTATION_LANDSCAPE_LEFT = Surface.ROTATION_90;
    public static final int RCT_CAMERA_ORIENTATION_LANDSCAPE_RIGHT = Surface.ROTATION_270;
    public static final int RCT_CAMERA_TYPE_FRONT = 1;
    public static final int RCT_CAMERA_TYPE_BACK = 2;
    public static final int RCT_CAMERA_FLASH_MODE_OFF = 0;
    public static final int RCT_CAMERA_FLASH_MODE_ON = 1;
    public static final int RCT_CAMERA_FLASH_MODE_AUTO = 2;
    public static final int RCT_CAMERA_TORCH_MODE_OFF = 0;
    public static final int RCT_CAMERA_TORCH_MODE_ON = 1;
    public static final int RCT_CAMERA_TORCH_MODE_AUTO = 2;
    public static final String RCT_CAMERA_CAPTURE_QUALITY_HIGH = "high";
    public static final String RCT_CAMERA_CAPTURE_QUALITY_MEDIUM = "medium";
    public static final String RCT_CAMERA_CAPTURE_QUALITY_LOW = "low";
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    public static final String CODE_TYPE_AZTEC = "AZTEC";
    public static final String CODE_TYPE_CODABAR = "CODABAR";
    public static final String CODE_TYPE_CODE_128 = "CODE_128";
    public static final String CODE_TYPE_CODE_93= "CODE_93";
    public static final String CODE_TYPE_CODE_39 = "CODE_39";
    public static final String CODE_TYPE_DATA_MATRIX = "DATA_MATRIX";
    public static final String CODE_TYPE_EAN_13 = "EAN_13";
    public static final String CODE_TYPE_EAN_8 = "EAN_8";
    public static final String CODE_TYPE_ITF = "ITF";
    public static final String CODE_TYPE_MAXICODE = "MAXICODE";
    public static final String CODE_TYPE_PDF_417 = "PDF_417";
    public static final String CODE_TYPE_QR_CODE = "QR_CODE";
    public static final String CODE_TYPE_RSS_14 = "RSS_14";
    public static final String CODE_TYPE_RSS_EXPANDED = "RSS_EXPANDED";
    public static final String CODE_TYPE_UPC_A = "UPC_A";
    public static final String CODE_TYPE_UPC_E = "UPC_E";
    public static final String CODE_UPC_EAN_EXTENSION = "UPC_EAN_EXTENSION";


    private final ReactApplicationContext _reactContext;
    private RCTSensorOrientationChecker _sensorOrientationChecker;

    private MediaRecorder mMediaRecorder = new MediaRecorder();
    private long MRStartTime;
    private File mVideoFile;
    private Camera mCamera = null;
    private Promise mRecordingPromise = null;
    private ReadableMap mRecordingOptions;


    public RCTCameraModule(ReactApplicationContext reactContext) {
        super(reactContext);
        ourInstance = this;
        _reactContext = reactContext;
        _sensorOrientationChecker = new RCTSensorOrientationChecker(_reactContext);
    }


    public static RCTCameraModule getInstance() {
      return ourInstance;
    }

    public void onInfo(MediaRecorder mr, int what, int extra) {
        if ( what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED ||
                what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
            if (mRecordingPromise != null) {
                releaseMediaRecorder(); // release the MediaRecorder object and resolve promise
            }
        }
    }

    @Override
    public String getName() {
        return "RCTCameraModule";
    }

    @Nullable
    @Override
    public Map<String, Object> getConstants() {
        return Collections.unmodifiableMap(new HashMap<String, Object>() {
            {
                put("Aspect", getAspectConstants());
                put("BarCodeType", getBarCodeConstants());
                put("Type", getTypeConstants());
                put("CaptureQuality", getCaptureQualityConstants());
                put("CaptureMode", getCaptureModeConstants());
                put("CaptureTarget", getCaptureTargetConstants());
                put("Orientation", getOrientationConstants());
                put("FlashMode", getFlashModeConstants());
                put("TorchMode", getTorchModeConstants());
            }

            private Map<String, Object> getAspectConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("stretch", RCT_CAMERA_ASPECT_STRETCH);
                        put("fit", RCT_CAMERA_ASPECT_FIT);
                        put("fill", RCT_CAMERA_ASPECT_FILL);
                    }
                });
            }

            private Map<String, Object> getBarCodeConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("aztec", CODE_TYPE_AZTEC);
                        put("codabar", CODE_TYPE_CODABAR);
                        put("code128", CODE_TYPE_CODE_128);
                        put("code93", CODE_TYPE_CODE_93);
                        put("code39", CODE_TYPE_CODE_39);
                        put("datamatrix", CODE_TYPE_DATA_MATRIX);
                        put("ean13", CODE_TYPE_EAN_13);
                        put("ean8", CODE_TYPE_EAN_8);
                        put("itf", CODE_TYPE_ITF);
                        put("maxicode", CODE_TYPE_MAXICODE);
                        put("pdf417", CODE_TYPE_PDF_417);
                        put("qr", CODE_TYPE_QR_CODE);
                        put("rss14", CODE_TYPE_RSS_14);
                        put("rss", CODE_TYPE_RSS_EXPANDED);
                        put("upca", CODE_TYPE_UPC_A);
                        put("upce", CODE_TYPE_UPC_E);
                        put("upc", CODE_UPC_EAN_EXTENSION);
                    }
                });
            }

            private Map<String, Object> getTypeConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("front", RCT_CAMERA_TYPE_FRONT);
                        put("back", RCT_CAMERA_TYPE_BACK);
                    }
                });
            }

            private Map<String, Object> getCaptureQualityConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                      put("low", RCT_CAMERA_CAPTURE_QUALITY_LOW);
                      put("medium", RCT_CAMERA_CAPTURE_QUALITY_MEDIUM);
                      put("high", RCT_CAMERA_CAPTURE_QUALITY_HIGH);
                      put("photo", RCT_CAMERA_CAPTURE_QUALITY_HIGH);
                    }
                });
            }

            private Map<String, Object> getCaptureModeConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("still", RCT_CAMERA_CAPTURE_MODE_STILL);
                        put("video", RCT_CAMERA_CAPTURE_MODE_VIDEO);
                    }
                });
            }

            private Map<String, Object> getCaptureTargetConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("memory", RCT_CAMERA_CAPTURE_TARGET_MEMORY);
                        put("disk", RCT_CAMERA_CAPTURE_TARGET_DISK);
                        put("cameraRoll", RCT_CAMERA_CAPTURE_TARGET_CAMERA_ROLL);
                        put("temp", RCT_CAMERA_CAPTURE_TARGET_TEMP);
                    }
                });
            }

            private Map<String, Object> getOrientationConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("auto", RCT_CAMERA_ORIENTATION_AUTO);
                        put("landscapeLeft", RCT_CAMERA_ORIENTATION_LANDSCAPE_LEFT);
                        put("landscapeRight", RCT_CAMERA_ORIENTATION_LANDSCAPE_RIGHT);
                        put("portrait", RCT_CAMERA_ORIENTATION_PORTRAIT);
                        put("portraitUpsideDown", RCT_CAMERA_ORIENTATION_PORTRAIT_UPSIDE_DOWN);
                    }
                });
            }

            private Map<String, Object> getFlashModeConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("off", RCT_CAMERA_FLASH_MODE_OFF);
                        put("on", RCT_CAMERA_FLASH_MODE_ON);
                        put("auto", RCT_CAMERA_FLASH_MODE_AUTO);
                    }
                });
            }

            private Map<String, Object> getTorchModeConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("off", RCT_CAMERA_TORCH_MODE_OFF);
                        put("on", RCT_CAMERA_TORCH_MODE_ON);
                        put("auto", RCT_CAMERA_TORCH_MODE_AUTO);
                    }
                });
            }
        });
    }

    private Throwable prepareMediaRecorder(ReadableMap options) {
        CamcorderProfile cm = RCTCamera.getInstance().setCaptureVideoQuality(options.getInt("type"), options.getString("quality"));

        // Attach callback to handle maxDuration (@see onInfo method in this file)
        mMediaRecorder.setOnInfoListener(this);
        mMediaRecorder.setCamera(mCamera);

        mCamera.unlock();  // make available for mediarecorder

        // Set AV sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        mMediaRecorder.setOrientationHint(RCTCamera.getInstance().getAdjustedDeviceOrientation());

        if (cm == null) {
            return new RuntimeException("CamcorderProfile not found in prepareMediaRecorder.");
        }

        cm.fileFormat = MediaRecorder.OutputFormat.MPEG_4;
        mMediaRecorder.setProfile(cm);

        mVideoFile = null;
        switch (options.getInt("target")) {
            case RCT_CAMERA_CAPTURE_TARGET_MEMORY:
                mVideoFile = getTempMediaFile(MEDIA_TYPE_VIDEO); // temporarily
                break;
            case RCT_CAMERA_CAPTURE_TARGET_CAMERA_ROLL:
                mVideoFile = getOutputCameraRollFile(MEDIA_TYPE_VIDEO);
                break;
            case RCT_CAMERA_CAPTURE_TARGET_TEMP:
                mVideoFile = getTempMediaFile(MEDIA_TYPE_VIDEO);
                break;
            default:
            case RCT_CAMERA_CAPTURE_TARGET_DISK:
                mVideoFile = getOutputMediaFile(MEDIA_TYPE_VIDEO);
                break;
        }

        if (mVideoFile == null) {
            return new RuntimeException("Error while preparing output file in prepareMediaRecorder.");
        }

        mMediaRecorder.setOutputFile(mVideoFile.getPath());

        if (options.hasKey("totalSeconds")) {
            int totalSeconds = options.getInt("totalSeconds");
            mMediaRecorder.setMaxDuration(totalSeconds * 1000);
        }

        if (options.hasKey("maxFileSize")) {
            int maxFileSize = options.getInt("maxFileSize");
            mMediaRecorder.setMaxFileSize(maxFileSize);
        }

        try {
            mMediaRecorder.prepare();
        } catch (Exception ex) {
            Log.e(TAG, "Media recorder prepare error.", ex);
            releaseMediaRecorder();
            return ex;
        }

        return null;
    }

    @ReactMethod
    private void record(final ReadableMap options, final Promise promise) {
        if (mRecordingPromise != null) {
            return;
        }

        mCamera = RCTCamera.getInstance().acquireCameraInstance(options.getInt("type"));
        if (mCamera == null) {
            promise.reject(new RuntimeException("No camera found."));
            return;
        }

        Throwable prepareError = prepareMediaRecorder(options);
        if (prepareError != null) {
            promise.reject(prepareError);
            return;
        }

        try {
            mMediaRecorder.start();
            MRStartTime =  System.currentTimeMillis();
            mRecordingOptions = options;
            mRecordingPromise = promise;  // only got here if mediaRecorder started
        } catch (Exception ex) {
            Log.e(TAG, "Media recorder start error.", ex);
            promise.reject(ex);
        }
    }

    private void releaseMediaRecorder() {
        // Must record at least a second or MediaRecorder throws exceptions on some platforms
        long duration = System.currentTimeMillis() - MRStartTime;
        if (duration < 1500) {
            try {
                Thread.sleep(1500 - duration);
            } catch(InterruptedException ex) {
                Log.e(TAG, "releaseMediaRecorder thread sleep error.", ex);
            }
        }

        try {
            mMediaRecorder.stop(); // stop the recording
        } catch (RuntimeException ex) {
            Log.e(TAG, "Media recorder stop error.", ex);
        }

        mMediaRecorder.reset(); // clear recorder configuration

        if (mCamera != null) {
            mCamera.lock(); // relock camera for later use since we unlocked it
        }

        if (mRecordingPromise == null) {
            return;
        }

        File f = new File(mVideoFile.getPath());
        if (!f.exists()) {
            mRecordingPromise.reject(new RuntimeException("There is nothing recorded."));
            mRecordingPromise = null;
            return;
        }

        f.setReadable(true, false); // so mediaplayer can play it
        f.setWritable(true, false); // so can clean it up

        WritableMap response = new WritableNativeMap();
        switch (mRecordingOptions.getInt("target")) {
            case RCT_CAMERA_CAPTURE_TARGET_MEMORY:
                byte[] encoded = convertFileToByteArray(mVideoFile);
                response.putString("data", new String(encoded, Base64.DEFAULT));
                mRecordingPromise.resolve(response);
                f.delete();
                break;
            case RCT_CAMERA_CAPTURE_TARGET_CAMERA_ROLL:
                ContentValues values = new ContentValues();
                values.put(MediaStore.Video.Media.DATA, mVideoFile.getPath());
                values.put(MediaStore.Video.Media.TITLE, mRecordingOptions.hasKey("title") ? mRecordingOptions.getString("title") : "video");

                if (mRecordingOptions.hasKey("description")) {
                    values.put(MediaStore.Video.Media.DESCRIPTION, mRecordingOptions.hasKey("description"));
                }

                if (mRecordingOptions.hasKey("latitude")) {
                    values.put(MediaStore.Video.Media.LATITUDE, mRecordingOptions.getString("latitude"));
                }

                if (mRecordingOptions.hasKey("longitude")) {
                    values.put(MediaStore.Video.Media.LONGITUDE, mRecordingOptions.getString("longitude"));
                }

                values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
                _reactContext.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
                addToMediaStore(mVideoFile.getAbsolutePath());
                response.putString("path", Uri.fromFile(mVideoFile).toString());
                mRecordingPromise.resolve(response);
                break;
            case RCT_CAMERA_CAPTURE_TARGET_TEMP:
            case RCT_CAMERA_CAPTURE_TARGET_DISK:
                response.putString("path", Uri.fromFile(mVideoFile).toString());
                mRecordingPromise.resolve(response);
        }

        mRecordingPromise = null;
    }

    public static byte[] convertFileToByteArray(File f)
    {
        byte[] byteArray = null;
        try
        {
            InputStream inputStream = new FileInputStream(f);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024*8];
            int bytesRead;

            while ((bytesRead = inputStream.read(b)) != -1) {
                bos.write(b, 0, bytesRead);
            }

            byteArray = bos.toByteArray();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return byteArray;
    }

    @ReactMethod
    public void capture(final ReadableMap options, final Promise promise) {
        int orientation = options.hasKey("orientation") ? options.getInt("orientation") : RCTCamera.getInstance().getOrientation();
        if (orientation == RCT_CAMERA_ORIENTATION_AUTO) {
            _sensorOrientationChecker.onResume();
            _sensorOrientationChecker.registerOrientationListener(new RCTSensorOrientationListener() {
                @Override
                public void orientationEvent() {
                    int deviceOrientation = _sensorOrientationChecker.getOrientation();
                    _sensorOrientationChecker.unregisterOrientationListener();
                    _sensorOrientationChecker.onPause();
                    captureWithOrientation(options, promise, deviceOrientation);
                }
            });
        } else {
            captureWithOrientation(options, promise, orientation);
        }
    }

    private void captureWithOrientation(final ReadableMap options, final Promise promise, int deviceOrientation) {
        Camera camera = RCTCamera.getInstance().acquireCameraInstance(options.getInt("type"));
        if (null == camera) {
            promise.reject("No camera found.");
            return;
        }

        if (options.getInt("mode") == RCT_CAMERA_CAPTURE_MODE_VIDEO) {
            record(options, promise);
            return;
        }

        RCTCamera.getInstance().setCaptureQuality(options.getInt("type"), options.getString("quality"));

        if (options.hasKey("playSoundOnCapture") && options.getBoolean("playSoundOnCapture")) {
            MediaActionSound sound = new MediaActionSound();
            sound.play(MediaActionSound.SHUTTER_CLICK);
        }

        if (options.hasKey("quality")) {
            RCTCamera.getInstance().setCaptureQuality(options.getInt("type"), options.getString("quality"));
        }

        RCTCamera.getInstance().adjustCameraRotationToDeviceOrientation(options.getInt("type"), deviceOrientation);
        camera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                camera.stopPreview();
                camera.startPreview();
                WritableMap response = new WritableNativeMap();
                switch (options.getInt("target")) {
                    case RCT_CAMERA_CAPTURE_TARGET_MEMORY:
                        String encoded = Base64.encodeToString(data, Base64.DEFAULT);
                        response.putString("data", encoded);
                        promise.resolve(response);
                        break;
                    case RCT_CAMERA_CAPTURE_TARGET_CAMERA_ROLL: {
                        File cameraRollFile = getOutputCameraRollFile(MEDIA_TYPE_IMAGE);
                        if (cameraRollFile == null) {
                            promise.reject("Error creating media file.");
                            return;
                        }

                        Throwable error = writeDataToFile(data, cameraRollFile);
                        if (error != null) {
                            promise.reject(error);
                            return;
                        }

                        addToMediaStore(cameraRollFile.getAbsolutePath());
                        response.putString("path", Uri.fromFile(cameraRollFile).toString());
                        promise.resolve(response);
                        break;
                    }
                    case RCT_CAMERA_CAPTURE_TARGET_DISK: {
                        File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
                        if (pictureFile == null) {
                            promise.reject("Error creating media file.");
                            return;
                        }

                        Throwable error = writeDataToFile(data, pictureFile);
                        if (error != null) {
                            promise.reject(error);
                            return;
                        }

                        addToMediaStore(pictureFile.getAbsolutePath());
                        response.putString("path", Uri.fromFile(pictureFile).toString());
                        promise.resolve(response);
                        break;
                    }
                    case RCT_CAMERA_CAPTURE_TARGET_TEMP: {
                        File tempFile = getTempMediaFile(MEDIA_TYPE_IMAGE);
                        if (tempFile == null) {
                            promise.reject("Error creating media file.");
                            return;
                        }

                        Throwable error = writeDataToFile(data, tempFile);
                        if (error != null) {
                            promise.reject(error);
                        }

                        response.putString("path", Uri.fromFile(tempFile).toString());
                        promise.resolve(response);
                        break;
                    }
                }
            }
        });
    }

    @ReactMethod
    public void stopCapture(final Promise promise) {
        if (mRecordingPromise != null) {
            releaseMediaRecorder(); // release the MediaRecorder object
            promise.resolve("Finished recording.");
        } else {
            promise.resolve("Not recording.");
        }
    }

    @ReactMethod
    public void hasFlash(ReadableMap options, final Promise promise) {
        Camera camera = RCTCamera.getInstance().acquireCameraInstance(options.getInt("type"));
        if (null == camera) {
            promise.reject("No camera found.");
            return;
        }
        List<String> flashModes = camera.getParameters().getSupportedFlashModes();
        promise.resolve(null != flashModes && !flashModes.isEmpty());
    }

    private Throwable writeDataToFile(byte[] data, File file) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.close();
        } catch (FileNotFoundException e) {
            return e;
        } catch (IOException e) {
            return e;
        }

        return null;
    }

    private File getOutputMediaFile(int type) {
        return getOutputFile(
                type,
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        );
    }

    private File getOutputCameraRollFile(int type) {
        return getOutputFile(
                type,
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        );
    }

    private File getOutputFile(int type, File storageDir) {
        // Create the storage directory if it does not exist
        if (!storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                Log.e(TAG, "failed to create directory:" + storageDir.getAbsolutePath());
                return null;
            }
        }

        // Create a media file name
        String photoName = String.format("%s", new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()));

        if (type == MEDIA_TYPE_IMAGE) {
            photoName = String.format("IMG_%s.jpg", photoName);
        } else if (type == MEDIA_TYPE_VIDEO) {
            photoName = String.format("VID_%s.mp4", photoName);
        } else {
            Log.e(TAG, "Unsupported media type:" + type);
            return null;
        }

        return new File(String.format("%s%s%s", storageDir.getPath(), File.separator, photoName));
    }

    private File getTempMediaFile(int type) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File outputDir = _reactContext.getCacheDir();
            File outputFile;

            if (type == MEDIA_TYPE_IMAGE) {
                outputFile = File.createTempFile("IMG_" + timeStamp, ".jpg", outputDir);
            } else if (type == MEDIA_TYPE_VIDEO) {
                outputFile = File.createTempFile("VID_" + timeStamp, ".mp4", outputDir);
            } else {
                Log.e(TAG, "Unsupported media type:" + type);
                return null;
            }
            return outputFile;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    private void addToMediaStore(String path) {
        MediaScannerConnection.scanFile(_reactContext, new String[] { path }, null, null);
    }
}
