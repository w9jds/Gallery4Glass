package com.w9jds.gallery4glass;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
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
import com.w9jds.gallery4glass.Adapters.csaAdapter;
import com.w9jds.gallery4glass.Classes.cPaths;
import com.w9jds.gallery4glass.Widget.SliderView;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;


public class EffectActivity extends Activity
{

    //create an adapter for the cardscrollviewer
    private csaAdapter mcvAdapter;
    //special packageable list of all the image paths
    private cPaths mcpPaths = new cPaths();
    //create an audio manager for sounds
    private AudioManager maManager;
    //create ArrayList for all the effect names
    private ArrayList<String> malsEffects = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_effect);

        maManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        Intent iThis = getIntent();
        iThis.getExtras();
        mcpPaths = iThis.getParcelableExtra("PathsObject");

        setMainView();
    }

    private void setMainView()
    {

        malsEffects.add(getString(R.string.grayscale_effect));
        malsEffects.add(getString(R.string.sepia_effect));


        //create a new card scroll viewer for this context
        CardScrollView csvCardsView = new CardScrollView(this);
        //create a new adapter for the scroll viewer
        mcvAdapter = new csaAdapter(this, malsEffects);
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
                //play the tap sound
                maManager.playSoundEffect(Sounds.TAP);

                //set the view to a new menu layout
                setContentView(R.layout.menu_layout);
                //set the icon to the effects icon
                ((ImageView)findViewById(R.id.icon)).setImageResource(R.drawable.ic_effects_50);
                //and set the label
                ((TextView)findViewById(R.id.label)).setText(getString(R.string.apply_label));

                //make sure it has the slider view in it
                SliderView svProgress = (SliderView)findViewById(R.id.slider);
                //and start the progressbar as indeterminate
                svProgress.startIndeterminate();

                //pass in the index of the selected item and start making the new bitmap
                startEffectCompositeCreation(position);
            }
        });

        //set the view of this activity
        setContentView(csvCardsView);
    }

    private void startEffectCompositeCreation(int nSelected)
    {
        (new CreateEffectComposite(mcpPaths, nSelected, this)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }


    public class CreateEffectComposite extends AsyncTask<Void, Void, Boolean>
    {
        private cPaths mcpPaths;
        private Context mcContext;
        private int mnEffect;

        //Constructor
        public CreateEffectComposite(cPaths cpPaths, int nSelected, Context cContext)
        {
            mnEffect = nSelected;
            mcpPaths = cpPaths;
            mcContext = cContext;
        }

        @Override
        protected Boolean doInBackground(Void... params)
        {

            Bitmap bitMain = null;

            switch(mnEffect)
            {
                case 0:
                    bitMain = toGrayscale(mcpPaths.getCurrentPositionPath());
                    break;
                case 1:
                    bitMain = toSephia(mcpPaths.getCurrentPositionPath());
                default:
                    break;




            }

            if (bitMain != null)
            {
                try
                {
                    //get the path to the camera directory
                    String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Camera";
                    //create a new output stream
                    OutputStream fOut;

                    String[] saPath = mcpPaths.getImagePathsIndex(mcpPaths.getMainPosition() + 1).split("/|\\.");

                    //create a new file with the added _x for the vignette to be stored in
                    java.io.File file = new java.io.File(path, saPath[saPath.length - 1] + "_" + mnEffect + ".jpg");
                    //create an output stream with the new file
                    fOut = new FileOutputStream(file);

                    //compress the image we just made
                    bitMain.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                    fOut.flush();
                    fOut.close();

                    //store the new file
                    MediaStore.Images.Media.insertImage(mcContext.getContentResolver(),file.getAbsolutePath(),file.getName(),file.getName());
                }
                catch (Exception e)
                {
                    Log.d("VignetteMaker", e.getCause().toString());
                }
            }

            return false;
        }

        public Bitmap toGrayscale(String sPath)
        {
            Bitmap bOriginal = BitmapFactory.decodeFile(sPath);

            Bitmap bGrayscale = Bitmap.createBitmap(bOriginal.getWidth(), bOriginal.getHeight(), Bitmap.Config.RGB_565);
            Canvas cCanvas = new Canvas(bGrayscale);
            Paint pPaint = new Paint();
            ColorMatrix cmMatrix = new ColorMatrix();
            cmMatrix.setSaturation(0);
            ColorMatrixColorFilter cmcFilter = new ColorMatrixColorFilter(cmMatrix);
            pPaint.setColorFilter(cmcFilter);
            cCanvas.drawBitmap(bOriginal, 0, 0, pPaint);
            return bGrayscale;
        }

        public Bitmap toSephia(String sPath)
        {
            Bitmap bOriginal = BitmapFactory.decodeFile(sPath);

            Bitmap bSephia = Bitmap.createBitmap(bOriginal.getWidth(), bOriginal.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas cCanvas = new Canvas(bSephia);
            Paint pPaint = new Paint();
            ColorMatrix cmMatrix = new ColorMatrix();
            cmMatrix.setScale(.3f, .3f, .3f, 1.0f);
            ColorMatrixColorFilter cmcFilter = new ColorMatrixColorFilter(cmMatrix);
            pPaint.setColorFilter(cmcFilter);
            cCanvas.drawBitmap(bOriginal, 0, 0, pPaint);

            int [] naPixels  = new int [bOriginal.getWidth() * bOriginal.getHeight()] ;
            bOriginal.getPixels(naPixels, 0, bOriginal.getWidth(), 0, 0, bOriginal.getWidth(), bOriginal.getHeight());
            int nPixelLength = naPixels.length;

            int nR, nG, nB, nGry;
            int nDepth = 20;

            for (int i = 0; i < nPixelLength; i++)
            {
                nR = (naPixels[i] >> 16) & 0xFF; //Isolate Red Channel value...
                nG = (naPixels[i] >> 8) & 0xFF; //Isolate Green Channel value...
                nB = naPixels[i] & 0xFF; //Isolate Blue Channel value...

                nGry = (nR + nG + nB) / 3;
                nR = nG = nB = nGry;

                nR = nR + (nDepth * 2);
                nG = nG + nDepth;

                if (nR > 255)
                    nR = 255;

                if (nG > 255)
                    nG = 255;

                naPixels[i] =  Color.rgb(nR, nG, nB) ;
            }

            return Bitmap.createBitmap(naPixels, bOriginal.getWidth(), bOriginal.getHeight(), Bitmap.Config.ARGB_8888) ;
        }

        @Override
        protected void onPostExecute(Boolean uploaded)
        {
            setContentView(R.layout.menu_layout);
            ((ImageView)findViewById(R.id.icon)).setImageResource(R.drawable.ic_done_50);
            ((TextView)findViewById(R.id.label)).setText(getString(R.string.applied_label));

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
