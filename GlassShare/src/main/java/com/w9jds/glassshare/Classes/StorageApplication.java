package com.w9jds.glassshare.Classes;

import android.app.Application;

/**
 * Created by w9jds on 12/15/13.
 */
public class StorageApplication extends Application
{

    private StorageService mStorageService;

    public StorageApplication() {}

    public StorageService getStorageService()
    {
        if (mStorageService == null)
            mStorageService = new StorageService(this);

        return mStorageService;
    }

}
