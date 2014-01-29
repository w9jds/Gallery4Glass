package com.w9jds.gallery4glass;

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
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.glass.app.Card;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardScrollView;
import com.w9jds.gallery4glass.Adapters.csaAdapter;
import com.w9jds.gallery4glass.Classes.Size;
import com.w9jds.gallery4glass.Classes.cPaths;
import com.w9jds.gallery4glass.Widget.SliderView;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

public class VignetteActivity extends Activity
{
    private static final int SPEECH_REQUEST = 0;

    static final Size FULL_COMPOSITE_SIZE = new Size(1920, 1080);
    private static final Paint SCALE_PAINT;
    private static final Paint SCREEN_PAINT;
    private static final RectF SCREEN_POSITION = new RectF(0.645833F, 0.037037F, 0.979167F, 0.37037F);

    private String msSpoken;

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

        mcpPaths.insertString("Select what to use for the second image.", 0);
        mcpPaths.insertString("Text", 1);

        CreatePictureView();
    }

    private void displaySpeechRecognizer()
    {

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        startActivityForResult(intent, SPEECH_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == SPEECH_REQUEST && resultCode == RESULT_OK)
        {
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            msSpoken = results.get(0);

            //set the view to a new menu layout
            setContentView(R.layout.menu_layout);
            //set the icon to the vignette icon
            ((ImageView)findViewById(R.id.icon)).setImageResource(R.drawable.ic_vignette_medium);
            //and set the label
            ((TextView)findViewById(R.id.label)).setText("Making Vignette");

            //make sure it has the slider view in it
            SliderView svProgress = (SliderView)findViewById(R.id.slider);
            //and start the progressbar as indeterminate
            svProgress.startIndeterminate();

            //create composite
            startCompositeCreation();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public Bitmap loadBitmapFromView()
    {
        Card txtCard = new Card(this);
        txtCard.setText(msSpoken);

        View v = txtCard.toView();

        Bitmap bView = Bitmap.createBitmap( 640, 360, Bitmap.Config.ARGB_8888);
        Canvas cView = new Canvas(bView);

        v.layout(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
        v.draw(cView);
        return bView;
    }

    private void startCompositeCreation()
    {
        (new CreateComposite(mcpPaths, miVignettePosition, this)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

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
                switch(position)
                {
                    case 0:
                        maManager.playSoundEffect(Sounds.DISALLOWED);
                        break;
                    case 1:
                        maManager.playSoundEffect(Sounds.TAP);
                        //save the card index that was selected
                        miVignettePosition = position;

                        //display speech recognition screen
                        displaySpeechRecognizer();
                        break;
                    default:
                        maManager.playSoundEffect(Sounds.TAP);
                        //save the card index that was selected
                        miVignettePosition = position;

                        //set the view to a new menu layout
                        setContentView(R.layout.menu_layout);
                        //set the icon to the vignette icon
                        ((ImageView)findViewById(R.id.icon)).setImageResource(R.drawable.ic_vignette_medium);
                        //and set the label
                        ((TextView)findViewById(R.id.label)).setText("Making Vignette");

                        //make sure it has the slider view in it
                        SliderView svProgress = (SliderView)findViewById(R.id.slider);
                        //and start the progressbar as indeterminate
                        svProgress.startIndeterminate();

                        //create the vignette and save it
                        startCompositeCreation();
                        break;
                }
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

        //Constructor
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

            //pull in just the info for the main image of the vignette
            Bitmap bitMain = BitmapFactory.decodeFile(mcpPaths.getImagePathsIndex(mcpPaths.getMainPosition() + 2), bfoOptions);
            //set the size on the image
            Size sWhole = FULL_COMPOSITE_SIZE;

            //now pull the whole image at the scaled size
            Bitmap bitWhole = Bitmap.createScaledBitmap(bitMain, sWhole.Width, sWhole.Height, false);
            //create a canvas with the bitmap we just pulled
            Canvas cBuild = new Canvas(bitWhole);

            //draw the vignette overlay on top of that main image
            cBuild.drawBitmap(BitmapFactory.decodeResource(mcContext.getResources(), R.drawable.vignette_overlay), null, new Rect(0, 0, sWhole.Width, sWhole.Height), SCALE_PAINT);

            //if user selected the text option
            if (miVignettePosition == 1)
                //turn the view into a bitmap and draw it in the top right hand corner
                cBuild.drawBitmap(loadBitmapFromView(), null, new Rect(Math.round(SCREEN_POSITION.left * sWhole.Width), Math.round(SCREEN_POSITION.top * sWhole.Height), Math.round(SCREEN_POSITION.right * sWhole.Width), Math.round(SCREEN_POSITION.bottom * sWhole.Height)), SCREEN_PAINT);
            //otherwise
            else
                //take the second image and draw it in the top right hand corner
                cBuild.drawBitmap(BitmapFactory.decodeFile(mcpPaths.getImagePathsIndex(miVignettePosition)), null, new Rect(Math.round(SCREEN_POSITION.left * sWhole.Width), Math.round(SCREEN_POSITION.top * sWhole.Height), Math.round(SCREEN_POSITION.right * sWhole.Width), Math.round(SCREEN_POSITION.bottom * sWhole.Height)), SCREEN_PAINT);

            try
            {
                //get the path to the camera directory
                String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Camera";
                //create a new output stream
                OutputStream fOut;

                //create a new file with the added _x for the vignette to be stored in
                java.io.File file = new java.io.File(path, mcpPaths.getImagePathsIndex(mcpPaths.getMainPosition() + 1).split("/|\\.")[5] + "_x.jpg");
                //create an output stream with the new file
                fOut = new FileOutputStream(file);

                //compress the image we just made
                bitWhole.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                fOut.flush();
                fOut.close();

                //store the new file
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
            setContentView(R.layout.menu_layout);
            ((ImageView)findViewById(R.id.icon)).setImageResource(R.drawable.ic_done_50);
            ((TextView)findViewById(R.id.label)).setText("Made Vignette");

            maManager.playSoundEffect(Sounds.SUCCESS);

            new Handler().postDelayed(new Runnable()
            {
                public void run()
                {
                    Intent returnIntent = new Intent();
                    setResult(RESULT_OK, returnIntent);
                    finish();
                }
            }, 1000);
        }
    }




}
