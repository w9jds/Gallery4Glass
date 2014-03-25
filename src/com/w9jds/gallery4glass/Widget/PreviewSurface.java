package com.w9jds.gallery4glass.Widget;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import javax.xml.transform.Result;

/**
 * Created by w9jds on 3/18/14.
 */
public class PreviewSurface extends SurfaceView implements SurfaceHolder.Callback
{
    private SurfaceHolder mshSurfaceHolder = null;
    private Camera mcCamera = null;

    public PreviewSurface(Context context)
    {
        super(context);

        this.setKeepScreenOn(true);

        mshSurfaceHolder = this.getHolder();
        mshSurfaceHolder.addCallback(this);
        mshSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        mcCamera = Camera.open();

        // Show the Camera display
        try
        {
            mcCamera.setPreviewDisplay(holder);
        }

        catch (Exception e)
        {
            this.releaseCamera();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        // Start the preview for surfaceChanged
//        initPreview(width, height);
//
        // Set the Hotfix for Google Glass
        this.setCameraParameters(mcCamera);

        if (mcCamera != null)
            mcCamera.startPreview();
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        // Do not hold the camera during surfaceDestroyed - view should be gone
        this.releaseCamera();
    }

    /**
     * Important HotFix for Google Glass (post-XE11) update
     *
     * @param cCamera Object
     */
    public void setCameraParameters(Camera cCamera)
    {
        if (cCamera != null)
        {
            Camera.Parameters cParams = cCamera.getParameters();
            cParams.setPreviewFpsRange(30000, 30000);
//            cParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

            cParams.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
            cParams.setJpegQuality(100);

            cCamera.setParameters(cParams);
        }
    }

    /**
     * Release the camera from use
     */
    public void releaseCamera()
    {

        if (mcCamera != null)
        {
            //stop the preview
            mcCamera.stopPreview();
            //release the camera
            mcCamera.release();
            //clear out the camera
            mcCamera = null;
        }
    }

    public Camera getCamera()
    {
        return mcCamera;
    }

}
