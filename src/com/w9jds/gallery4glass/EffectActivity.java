package com.w9jds.gallery4glass;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
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
import com.w9jds.gallery4glass.Classes.SingleMediaScanner;
import com.w9jds.gallery4glass.Classes.cPaths;
import com.w9jds.gallery4glass.Widget.SliderView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


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

    private Mat mOriginal;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_effect);

//        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2, this, mLoaderCallback);
        OpenCVLoader.initDebug();
        maManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        Intent iThis = getIntent();
        iThis.getExtras();
        mcpPaths = iThis.getParcelableExtra("PathsObject");

        setMainView();
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this)
    {
        @Override
        public void onManagerConnected(int status)
        {
            switch (status)
            {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i("EffectMaker", "OpenCV loaded successfully");
                    mOriginal = new Mat();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }


    };

    private void setMainView()
    {

        malsEffects.add(getString(R.string.grayscale_effect));
        malsEffects.add(getString(R.string.sepia_effect));
        malsEffects.add(getString(R.string.invert_effect));
        malsEffects.add(getString(R.string.emboss_effect));
        malsEffects.add(getString(R.string.engrave_effect));
        malsEffects.add(getString(R.string.sharpen_effect));


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
                    bitMain = toSepia(mcpPaths.getCurrentPositionPath());
                    break;
                case 2:
                    bitMain = toInvert(mcpPaths.getCurrentPositionPath());
                    break;
//                case 3:
//                    bitMain = toEmboss(mcpPaths.getCurrentPositionPath());
//                    break;
//                case 4:
//                    bitMain = toEngrave(mcpPaths.getCurrentPositionPath());
//                    break;
                case 5:
                    bitMain = toSharpen(mcpPaths.getCurrentPositionPath());
                    break;
                default:
                    break;
            }

            if (bitMain != null)
            {
                try
                {
                    //get the path to the camera directory
                    String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/Camera";

                    String[] saPath = mcpPaths.getCurrentPositionPath().split("/|\\.");

                    //create a new file with the added _x for the vignette to be stored in
                    java.io.File fImage = new java.io.File(path, saPath[saPath.length - 2] + "_" + malsEffects.get(mnEffect) + ".jpg");
                    //create an output stream with the new file
                    FileOutputStream fOut = new FileOutputStream(fImage);

                    //compress the image we just made
                    bitMain.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                    fOut.flush();
                    fOut.close();

                    //store the new file
                    MediaStore.Images.Media.insertImage(mcContext.getContentResolver(), fImage.getAbsolutePath(), fImage.getName(), fImage.getName());

                    //add to media scanner
                    new SingleMediaScanner(mcContext, fImage);
                }
                catch (Exception e)
                {
                    Log.d("EffectMaker", e.getCause().toString());
                }
            }

            return false;
        }

        public Bitmap toGrayscale(String sPath)
        {
            mOriginal = new Mat();

            Utils.bitmapToMat(BitmapFactory.decodeFile(sPath), mOriginal);

            Imgproc.cvtColor(mOriginal, mOriginal, Imgproc.COLOR_BGR2GRAY);
            Imgproc.cvtColor(mOriginal, mOriginal, Imgproc.COLOR_GRAY2RGBA, 4);

            Bitmap bGrayscale = Bitmap.createBitmap(mOriginal.width(), mOriginal.height(), Bitmap.Config.RGB_565);

            Utils.matToBitmap(mOriginal, bGrayscale);
            return bGrayscale;
        }

        public Bitmap toSepia(String sPath)
        {
            Mat mOriginal = new Mat();
            Utils.bitmapToMat(BitmapFactory.decodeFile(sPath), mOriginal);

            // Fill sepia kernel
            Mat mSepiaKernel = new Mat(4, 4, CvType.CV_32F);
            mSepiaKernel.put(0, 0, /* R */0.189f, 0.769f, 0.393f, 0f);
            mSepiaKernel.put(1, 0, /* G */0.168f, 0.686f, 0.349f, 0f);
            mSepiaKernel.put(2, 0, /* B */0.131f, 0.534f, 0.272f, 0f);
            mSepiaKernel.put(3, 0, /* A */0.000f, 0.000f, 0.000f, 1f);

            Core.transform(mOriginal, mOriginal, mSepiaKernel);

            Bitmap bSepia = Bitmap.createBitmap(mOriginal.width(), mOriginal.height(), Bitmap.Config.RGB_565);
            Utils.matToBitmap(mOriginal, bSepia);
            return bSepia;
        }

        public Bitmap toInvert(String sPath)
        {
            //create new mat
            Mat mOriginal = new Mat();
            //populate mat with passed in image
            Utils.bitmapToMat(BitmapFactory.decodeFile(sPath), mOriginal);
            //invert image
            Core.bitwise_not(mOriginal, mOriginal);
            //make new bitmap
            Bitmap bInvert = Bitmap.createBitmap(mOriginal.width(), mOriginal.height(), Bitmap.Config.RGB_565);
            //return image to bitmap
            Utils.matToBitmap(mOriginal, bInvert);
            //return bitmap
            return bInvert;
        }

        public Bitmap toSharpen(String sPath)
        {
            //create mat and populate with the image to be edited
            Mat mOriginal = new Mat();
            Utils.bitmapToMat(BitmapFactory.decodeFile(sPath), mOriginal);
            //create mat for the blur destination
            Mat mGaussian = new Mat();
            //blur then sharpen image
            Imgproc.GaussianBlur(mOriginal, mGaussian, new Size(5, 5), 5);
            Core.addWeighted(mOriginal, 1.5, mGaussian, -0.5, 0, mOriginal);
            //create bitmap for the result
            Bitmap bSharpen = Bitmap.createBitmap(mOriginal.width(), mOriginal.height(), Bitmap.Config.RGB_565);
            //return the mat as a bitmap
            Utils.matToBitmap(mOriginal, bSharpen);
            //return the new image
            return bSharpen;
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
