package com.w9jds.glassshare;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.w9jds.glassshare.Classes.Size;

import java.io.FileOutputStream;
import java.io.OutputStream;

public class VignetteActivity extends Activity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vignette);

    }


//    @Override
//    public boolean onCreateOptionsMenu(Menu menu)
//    {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.vignette, menu);
//        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item)
//    {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//        if (id == R.id.action_settings)
//        {
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }

    static final Size FULL_COMPOSITE_SIZE;
    //    static final Size PREVIEW_COMPOSITE_SIZE;
    private static final Paint SCALE_PAINT;
    private static final Paint SCREEN_PAINT;
    private static final RectF SCREEN_POSITION;

    static
    {
        FULL_COMPOSITE_SIZE = new Size(1920, 1080);
//        PREVIEW_COMPOSITE_SIZE = new Size(640, 360);
        SCREEN_POSITION = new RectF(0.645833F, 0.037037F, 0.979167F, 0.37037F);

        SCALE_PAINT = new Paint();
        SCALE_PAINT.setFilterBitmap(true);
        SCALE_PAINT.setDither(true);

        SCREEN_PAINT = new Paint(SCALE_PAINT);
        SCREEN_PAINT.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
    }

    protected void createComposite()
    {
        BitmapFactory.Options bfoOptions = new BitmapFactory.Options();
        //turn off the bounds only so you get the pixels this time
        bfoOptions.inJustDecodeBounds = false;
        //calculate the sample size and set it in the options
        bfoOptions.inSampleSize = calculateInSampleSize(bfoOptions, FULL_COMPOSITE_SIZE.Height, FULL_COMPOSITE_SIZE.Width);

        Bitmap bitMain = BitmapFactory.decodeFile(mlsPaths.get(miPosition), bfoOptions);
        Size sWhole = FULL_COMPOSITE_SIZE;

        Bitmap bitWhole = Bitmap.createScaledBitmap(bitMain, sWhole.Width, sWhole.Height, false);
        Canvas cBuild = new Canvas(bitWhole);

        cBuild.drawBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.vignette_overlay), null, new Rect(0, 0, sWhole.Width, sWhole.Height), SCALE_PAINT);
        cBuild.drawBitmap(BitmapFactory.decodeFile(mlsPaths.get(miPosition + 1)), null, new Rect(Math.round(SCREEN_POSITION.left * sWhole.Width), Math.round(SCREEN_POSITION.top * sWhole.Height), Math.round(SCREEN_POSITION.right * sWhole.Width), Math.round(SCREEN_POSITION.bottom * sWhole.Height)), SCREEN_PAINT);

        try
        {
            String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Camera";
            OutputStream fOut;

            java.io.File file = new java.io.File(path, mlsPaths.get(miPosition).split("/|\\.")[5] + "_x.jpg");
            fOut = new FileOutputStream(file);

            bitWhole.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            fOut.flush();
            fOut.close();

            MediaStore.Images.Media.insertImage(this.getContentResolver(),file.getAbsolutePath(),file.getName(),file.getName());
        }
        catch (Exception e)
        {
            Log.d("MyGlassShare", e.getCause().toString());
        }

        CreatePictureView();

    }

    public int calculateInSampleSize(BitmapFactory.Options bfoOptions, int iReqWidth, int iReqHeight)
    {
        //pull out the images height
        final int height = bfoOptions.outHeight;
        //pull out the images width
        final int width = bfoOptions.outWidth;
        //set the samle size to 1 for initialization
        int inSampleSize = 1;

        //if te height or the width of the image is greater than the requested sizes
        if (height > iReqHeight || width > iReqWidth)
        {
            //set the half dimensions for the image
            final int iHalfHeight = height / 2;
            final int iHalfWidth = width / 2;

            //white both half dimensions divided by the sample size are still greater than the requested dimensions
            while ((iHalfHeight / inSampleSize) > iReqHeight && (iHalfWidth / inSampleSize) > iReqWidth)
                //multiply the sample size by 2
                inSampleSize *= 2;
        }

        //once the loop is done return the sample size
        return inSampleSize;
    }


}
