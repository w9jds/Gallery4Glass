package com.w9jds.gallery4glass.Widget;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;

import org.opencv.android.JavaCameraView;

/**
 * Created by w9jds on 3/25/14.
 */
public class OpenCVSurface extends JavaCameraView
{

    public OpenCVSurface(Context cContext, AttributeSet args)
    {
        super(cContext, args);

        this.setKeepScreenOn(true);
    }

    public Camera getCamera()
    {
        return mCamera;
    }

}
