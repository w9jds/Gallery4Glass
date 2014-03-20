package com.w9jds.gallery4glass;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.FileObserver;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.google.android.glass.media.CameraManager;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.w9jds.gallery4glass.Widget.PreviewSurface;

import java.io.File;

public class CameraActivity extends Activity
{
    private static final int TAKE_PICTURE_REQUEST = 1;
    private GestureDetector mGestureDetector;
    private PreviewSurface mPreviewSurface;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Initiate CameraView
        mPreviewSurface = new PreviewSurface(this);
        // Turn on Gestures
        mGestureDetector = createGestureDetector(this);

        this.setContentView(mPreviewSurface);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // Do not hold the camera during onResume
        if (mPreviewSurface != null)
            mPreviewSurface.releaseCamera();

        // Set the view
        this.setContentView(mPreviewSurface);
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        // Do not hold the camera during onPause
        if (mPreviewSurface != null)
            mPreviewSurface.releaseCamera();
    }

    private GestureDetector createGestureDetector(Context cContext)
    {
        GestureDetector gestureDetector = new GestureDetector(cContext);

        //Create a base listener for generic gestures
        gestureDetector.setBaseListener( new GestureDetector.BaseListener()
        {
            @Override
            public boolean onGesture(Gesture gesture)
            {
                // Make sure view is initiated
                if (mPreviewSurface != null)
                {

                    if (gesture == Gesture.TAP)
                    {
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                        if (intent != null)
                            startActivityForResult(intent, TAKE_PICTURE_REQUEST);

                        return true;
                    }

                    else if (gesture == Gesture.TWO_SWIPE_RIGHT)
                    {

                        if (mPreviewSurface != null)
                            mPreviewSurface.releaseCamera();

                        Intent iGallery = new Intent(getApplicationContext(), MainActivity.class);
                        finish();
                        startActivity(iGallery);

                    }

//                    else if (gesture == Gesture.TWO_TAP)
//                    {
//                        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
//
//                        if (intent != null)
//                            startActivityForResult(intent, TAKE_VIDEO_REQUEST);
//
//                        return true;
//                    }
                }

                return false;
            }
        });

        return gestureDetector;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event)
    {
        if (mGestureDetector != null)
            return mGestureDetector.onMotionEvent(event);

        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode == RESULT_OK)
        {
            switch (requestCode)
            {
                case TAKE_PICTURE_REQUEST:
                    String sPath = data.getStringExtra(CameraManager.EXTRA_PICTURE_FILE_PATH);
                    processPictureWhenReady(sPath);
                    break;

            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Process picture - from example GDK
     * @param picturePath
     */
    private void processPictureWhenReady(final String picturePath)
    {
        final File fPicture = new File(picturePath);

        if (fPicture.exists())
        {
            // The picture is ready; process it.
        }
        else
        {
            // The file does not exist yet. Before starting the file observer, you
            // can update your UI to let the user know that the application is
            // waiting for the picture (for example, by displaying the thumbnail
            // image and a progress indicator).

            final File fParent = fPicture.getParentFile();

            FileObserver foObserver = new FileObserver(fParent.getPath())
            {
                // Protect against additional pending events after CLOSE_WRITE is
                // handled.
                private boolean isFileWritten;

                @Override
                public void onEvent(int event, String path)
                {
                    if (! isFileWritten)
                    {
                        // For safety, make sure that the file that was created in
                        // the directory is actually the one that we're expecting.
                        File fAffected = new File(fParent, path);
                        isFileWritten = (event == FileObserver.CLOSE_WRITE && fAffected.equals(fPicture));

                        if (isFileWritten)
                        {
                            stopWatching();

                            // Now that the file is ready, recursively call
                            // processPictureWhenReady again (on the UI thread).
                            runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    processPictureWhenReady(picturePath);
                                }
                            });
                        }
                    }
                }
            };

            foObserver.startWatching();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {

        if (keyCode == KeyEvent.KEYCODE_CAMERA)
        {
            // Stop the preview and release the camera.
            // Execute your logic as quickly as possible
            // so the capture happens quickly.
            return false;
        }

        else
        {
            return super.onKeyDown(keyCode, event);
        }

    }
}
