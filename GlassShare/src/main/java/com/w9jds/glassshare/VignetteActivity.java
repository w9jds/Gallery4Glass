package com.w9jds.glassshare;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardScrollView;
import com.w9jds.glassshare.Adapters.csaAdapter;
import com.w9jds.glassshare.Classes.Size;
import com.w9jds.glassshare.Classes.cPaths;

import java.io.FileOutputStream;
import java.io.OutputStream;

public class VignetteActivity extends Activity
{
    static final Size FULL_COMPOSITE_SIZE = new Size(1920, 1080);
    private static final Paint SCALE_PAINT;
    private static final Paint SCREEN_PAINT;
    private static final RectF SCREEN_POSITION = new RectF(0.645833F, 0.037037F, 0.979167F, 0.37037F);

    //create an adapter for the cardscrollviewer
    private csaAdapter mcvAdapter;
    //list of image paths
    private cPaths mcpPaths = new cPaths();
    //create variable to store the vignette position in
    private int miVignettePosition;
    //create an audio manager for sounds
    private AudioManager maManager;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vignette);

        maManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        Intent iThis = getIntent();
        iThis.getExtras();
        mcpPaths = iThis.getParcelableExtra("PathsObject");

        CreatePictureView();
    }

    private void startCompositeCreation()
    {
        (new CreateComposite(mcpPaths, miVignettePosition, this)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
//    private void loadMainView()
//    {
//        Intent i = new Intent(this, MainActivity.class);
//        startActivity(i);
//    }

    private void CreatePictureView()
    {
        //create a new card scroll viewer for this context
        CardScrollView csvCardsView = new CardScrollView(this);
        //create a new adapter for the scroll viewer
        mcvAdapter = new csaAdapter(this, mcpPaths.getImagePaths());
        //set this adapter as the adapter for the scroll viewer
        csvCardsView.setAdapter(mcvAdapter);
        //activate this scroll viewer
        csvCardsView.activate();
        //add a listener to the scroll viewer that is fired when an item is clicked
        csvCardsView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                if (position != 0)
                {
                    maManager.playSoundEffect(Sounds.TAP);
                    //save the card index that was selected
                    miVignettePosition = position;
                    //create the vignette and save it
                    startCompositeCreation();
                }
                else
                    maManager.playSoundEffect(Sounds.DISALLOWED);
            }
        });

        //set the view of this activity
        setContentView(csvCardsView);
    }

    static
    {
        SCALE_PAINT = new Paint();
        SCALE_PAINT.setFilterBitmap(true);
        SCALE_PAINT.setDither(true);

        SCREEN_PAINT = new Paint(SCALE_PAINT);
        SCREEN_PAINT.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
    }

    public class CreateComposite extends AsyncTask<Void, Void, Boolean>
    {
        private cPaths mcpPaths;
        private int miVignettePosition;
        private Context mcContext;

        public CreateComposite(cPaths cpPaths, int iVignettePosition, Context cContext)
        {
            mcpPaths = cpPaths;
            miVignettePosition = iVignettePosition;
            mcContext = cContext;
        }

        @Override
        protected Boolean doInBackground(Void... params)
        {
            BitmapFactory.Options bfoOptions = new BitmapFactory.Options();
            //turn off the bounds only so you get the pixels this time
            bfoOptions.inJustDecodeBounds = false;
            //calculate the sample size and set it in the options
            bfoOptions.inSampleSize = calculateInSampleSize(bfoOptions, FULL_COMPOSITE_SIZE.Height, FULL_COMPOSITE_SIZE.Width);

            Bitmap bitMain = BitmapFactory.decodeFile(mcpPaths.getCurrentPositionPath(), bfoOptions);
            Size sWhole = FULL_COMPOSITE_SIZE;

            Bitmap bitWhole = Bitmap.createScaledBitmap(bitMain, sWhole.Width, sWhole.Height, false);
            Canvas cBuild = new Canvas(bitWhole);

            cBuild.drawBitmap(BitmapFactory.decodeResource(mcContext.getResources(), R.drawable.vignette_overlay), null, new Rect(0, 0, sWhole.Width, sWhole.Height), SCALE_PAINT);
            cBuild.drawBitmap(BitmapFactory.decodeFile(mcpPaths.getImagePathsIndex(miVignettePosition)), null, new Rect(Math.round(SCREEN_POSITION.left * sWhole.Width), Math.round(SCREEN_POSITION.top * sWhole.Height), Math.round(SCREEN_POSITION.right * sWhole.Width), Math.round(SCREEN_POSITION.bottom * sWhole.Height)), SCREEN_PAINT);

            try
            {
                String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Camera";
                OutputStream fOut;

                java.io.File file = new java.io.File(path, mcpPaths.getCurrentPositionPath().split("/|\\.")[5] + "_x.jpg");
                fOut = new FileOutputStream(file);

                bitWhole.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                fOut.flush();
                fOut.close();

                MediaStore.Images.Media.insertImage(mcContext.getContentResolver(),file.getAbsolutePath(),file.getName(),file.getName());
            }
            catch (Exception e)
            {
                Log.d("VignetteMaker", e.getCause().toString());
            }

            return false;
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

        @Override
        protected void onPostExecute(Boolean uploaded)
        {
            if (uploaded)
            {

                setContentView(R.layout.menu_layout);
                ((ImageView)findViewById(R.id.icon)).setImageResource(R.drawable.ic_done_50);
                ((TextView)findViewById(R.id.label)).setText("Made Vignette");

                maManager.playSoundEffect(Sounds.SUCCESS);
            }

            new Handler().postDelayed(new Runnable()
            {
                public void run()
                {
                    Intent returnIntent = new Intent();
                    setResult(RESULT_CANCELED, returnIntent);
                    finish();
                }
            }, 1000);
        }
    }




}
