package com.w9jds.gallery4glass;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.w9jds.gallery4glass.Classes.Gallery4Glass;
import com.w9jds.gallery4glass.Classes.SingleMediaScanner;
import com.w9jds.gallery4glass.Widget.PreviewSurface;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CameraActivity extends Activity
{
    public static final String ACTION_WINK = "com.google.glass.action.EYE_GESTURE";

    // Declare a new Gesture Detector
    private GestureDetector mGestureDetector;
    // Declare a new Camera Preview Surface
    private PreviewSurface mPreviewSurface;
    //create an audio manager for sounds
    private AudioManager maManager;
    // Zoom level of the camera
    private int mnZoom = 0;

    @Override
    public void onCreate(Bundle bSavedInstanceState)
    {
        super.onCreate(bSavedInstanceState);

        Gallery4Glass.CameraOpened();

        // Turn on Gestures
        mGestureDetector = createGestureDetector(this);

        maManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        setupReceivers();

        setPreviewSurface();

    }

    private void setPreviewSurface()
    {
        // Initiate CameraView
        mPreviewSurface = new PreviewSurface(this);

        setContentView(mPreviewSurface);

    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // Do not hold the camera during onResume
        if (mPreviewSurface != null)
            mPreviewSurface.releaseCamera();

        // Set the view
        setPreviewSurface();
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        // Do not hold the camera during onPause
        if (mPreviewSurface != null)
            mPreviewSurface.releaseCamera();
    }

    private GestureDetector createGestureDetector(final Context cContext)
    {
        GestureDetector gestureDetector = new GestureDetector(cContext);

        //Create a base listener for generic gestures
        gestureDetector.setBaseListener( new GestureDetector.BaseListener()
        {
            @Override
            public boolean onGesture(Gesture gGesture)
            {
                // Make sure view is initiated
                if (mPreviewSurface != null)
                {
                    if (gGesture == Gesture.TAP)
                    {
                        // Play the tap sound
                        maManager.playSoundEffect(Sounds.TAP);
                        // Get the camera from the preview surface
                        Camera cCamera = mPreviewSurface.getCamera();
                        // Take a picture
                        cCamera.takePicture(shutterCallback, null, jpgPictureCallback);
                    }

                    else if (gGesture == Gesture.SWIPE_DOWN)
                    {
                        // Play the dismiss sound
                        maManager.playSoundEffect(Sounds.DISMISSED);
                        // If the preview surface isn't null release the camera
                        if (mPreviewSurface != null)
                            mPreviewSurface.releaseCamera();
                        // Close activity
                        finish();
                    }

                    else if (gGesture == Gesture.SWIPE_RIGHT)
                    {
                        // Get the camera from the preview surface
                        Camera cCamera = mPreviewSurface.getCamera();

                        if ((mnZoom + 5) < cCamera.getParameters().getMaxZoom())
                            // Zoom the camera in 5
                            cCamera.startSmoothZoom(mnZoom += 5);
                    }

                    else if (gGesture == Gesture.SWIPE_LEFT)
                    {
                        // Get the camera from the preview surface
                        Camera cCamera = mPreviewSurface.getCamera();

                        if (mnZoom != 0)
                            //zoom the camera out 5
                            cCamera.startSmoothZoom(mnZoom -= 5);
                    }

                    else if (gGesture == Gesture.TWO_SWIPE_RIGHT)
                    {
                        if (mPreviewSurface != null)
                            mPreviewSurface.releaseCamera();

                        Gallery4Glass.CameraClosed();

                        Intent iGallery = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(iGallery);
                    }
                }

                return false;
            }
        });

        return gestureDetector;
    }

    private final Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback()
    {
        public void onShutter()
        {

        }
    };

    /**
     * This will be called after taking picture from camera.
     */
    final transient private Camera.PictureCallback jpgPictureCallback = new Camera.PictureCallback()
    {
        /**
         * After taking picture, onPictureTaken() will be called where image
         * will be saved.
         */
        @Override
        public void onPictureTaken(final byte[] data, final Camera camera)
        {
            new SavePhotoTask().execute(data);
            setPreviewSurface();
        }

    };

    @Override
    public boolean onGenericMotionEvent(MotionEvent event)
    {
        if (mGestureDetector != null)
            return mGestureDetector.onMotionEvent(event);

        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {

        if (keyCode == KeyEvent.KEYCODE_CAMERA)
        {
            // Play the tap sound
            maManager.playSoundEffect(Sounds.TAP);
            // Get the camera from the preview surface
            Camera cCamera = mPreviewSurface.getCamera();
            // Take a picture
            cCamera.takePicture(shutterCallback, null, jpgPictureCallback);

            return true;
        }

        else
            return super.onKeyDown(keyCode, event);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (ACTION_WINK.equals(intent.getAction()))
            {
                if (Gallery4Glass.isUsingCamera())
                {
                    // Get the camera from the preview surface
                    Camera cCamera = mPreviewSurface.getCamera();
                    // Take a picture
                    cCamera.takePicture(shutterCallback, null, jpgPictureCallback);
                }

                //abort the Broadcast so the wink doesn't crash both the camera and my app
                abortBroadcast();
            }
        }
    };

    private void setupReceivers()
    {
        // Create Intent Filter
        IntentFilter filter = new IntentFilter(ACTION_WINK);
        // Set the Priority
        filter.setPriority(3000);
        // Register the broadcast receiver
        registerReceiver(mReceiver, filter);
    }

    @Override
    public void onDestroy()
    {
        // Unregister the Receiver
        unregisterReceiver(mReceiver);
        // Run normal onDestroy
        super.onDestroy();
    }

    class SavePhotoTask extends AsyncTask<byte[], String, String>
    {
        @Override
        protected String doInBackground(byte[]... jpeg)
        {
            try
            {

                Bitmap bitFinal = BitmapFactory.decodeByteArray(jpeg[0], 0, jpeg[0].length);

                //get the path to the camera directory
                String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/Camera";

                //create new file
                File fImage;

                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
                String currentDateandTime = sdf.format(new Date());

                //create a new file with the added _effectname for the vignette to be stored in
                fImage = new File(path, currentDateandTime + ".jpg");

                //create an output stream with the new file
                FileOutputStream fOut = new FileOutputStream(fImage);

                //compress the image we just made
                bitFinal.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                fOut.flush();
                fOut.close();

                //add to media scanner
                new SingleMediaScanner(getApplicationContext(), fImage);

            }

            catch (Exception e)
            {
                Log.d("PhotoSaver", e.getCause().toString());
            }

            return null;
        }
    }
}
