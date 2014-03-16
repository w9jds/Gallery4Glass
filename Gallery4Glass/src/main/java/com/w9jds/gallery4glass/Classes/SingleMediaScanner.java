package com.w9jds.gallery4glass.Classes;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;

import java.io.File;

public class SingleMediaScanner implements MediaScannerConnection.MediaScannerConnectionClient
{
    private MediaScannerConnection mmscCon;
    private File mfFile;

    public SingleMediaScanner(Context cContext, File file)
    {
        mfFile = file;
        mmscCon = new MediaScannerConnection(cContext, this);
        mmscCon.connect();
    }

    @Override
    public void onMediaScannerConnected()
    {
        mmscCon.scanFile(mfFile.getAbsolutePath(), null);
    }

    @Override
    public void onScanCompleted(String path, Uri uri)
    {
        mmscCon.disconnect();
    }

}