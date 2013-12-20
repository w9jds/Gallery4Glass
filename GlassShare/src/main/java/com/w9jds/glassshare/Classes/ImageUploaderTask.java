package com.w9jds.glassshare.Classes;

/**
 * Created by w9jds on 12/20/13.
 */

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/***
 * Handles uploading an image to a specified url
 */
public class ImageUploaderTask extends AsyncTask<Void, Void, Boolean>
{
    private String mUrl;
    private ArrayList<String> mlsPaths;
    private int miPosition;

    public ImageUploaderTask(String url, int iPosition, ArrayList<String> lsPath)
    {
        mUrl = url;
        miPosition = iPosition;
        mlsPaths = lsPath;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try
        {
            //get the image data
            Bitmap bmp = BitmapFactory.decodeFile(mlsPaths.get(miPosition));
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            // Post our image data (byte array) to the server
            URL url = new URL(mUrl.replace("\"", ""));
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("PUT");
            urlConnection.addRequestProperty("Content-Type", "image/jpeg");
            urlConnection.setRequestProperty("Content-Length", ""+ byteArray.length);
            // Write image data to server
            DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
            wr.write(byteArray);
            wr.flush();
            wr.close();
            int response = urlConnection.getResponseCode();
            //If we successfully uploaded, return true
            if (response == 201 && urlConnection.getResponseMessage().equals("Created"))
                return true;
        }

        catch (Exception ex)
        {
            Log.e("GlassShareImageUploadTask", ex.getMessage());
        }
        return false;
    }

    @Override
    protected void onPostExecute(Boolean uploaded)
    {
//            if (uploaded)
//            {
//                mAlertDialog.cancel();
//                mStorageService.getBlobsForContainer(mContainerName);
//            }
    }
}