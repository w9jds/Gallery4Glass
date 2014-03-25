package com.w9jds.gallery4glass.Classes;

import android.app.Application;

public class Gallery4Glass extends Application
{


    private StorageService mStorageService;

    public StorageService getStorageService()
    {
        if (mStorageService == null)
            mStorageService = new StorageService(this);

        return mStorageService;
    }

    public static boolean isUsingCamera()
    {
        return CameraInUse;
    }

    public static void CameraOpened()
    {
        CameraInUse = true;
    }

    public static void CameraClosed()
    {
        CameraInUse = false;
    }

    private static boolean CameraInUse;


}