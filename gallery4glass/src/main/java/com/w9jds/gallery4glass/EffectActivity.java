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
import android.media.effect.Effect;
import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
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
import com.w9jds.gallery4glass.Classes.ConvolutionMatrix;
import com.w9jds.gallery4glass.Classes.cPaths;
import com.w9jds.gallery4glass.Widget.SliderView;

import java.io.FileOutputStream;
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
                    bitMain = toSephia(mcpPaths.getCurrentPositionPath());
                    break;
                case 2:
                    bitMain = toInvert(mcpPaths.getCurrentPositionPath());
                    break;
                case 3:
                    bitMain = toEmboss(mcpPaths.getCurrentPositionPath());
                    break;
                case 4:
                    bitMain = toEngrave(mcpPaths.getCurrentPositionPath());
                    break;
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
                    String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Camera";

                    String[] saPath = mcpPaths.getCurrentPositionPath().split("/|\\.");

                    //create a new file with the added _x for the vignette to be stored in
                    java.io.File fImage = new java.io.File(path, saPath[saPath.length - 2] + "_" + malsEffects.get(mnEffect) + ".jpg");
                    //create the file
                    fImage.createNewFile();
                    //create an output stream with the new file
                    FileOutputStream fOut = new FileOutputStream(fImage);

                    //compress the image we just made
                    bitMain.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                    fOut.flush();
                    fOut.close();

                    //store the new file
                    MediaStore.Images.Media.insertImage(mcContext.getContentResolver(), fImage.getAbsolutePath(), fImage.getName(), fImage.getName());
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
            int nWidth = bOriginal.getWidth();
            int nHeight = bOriginal.getHeight();

            Bitmap bSephia = Bitmap.createBitmap(nWidth, nHeight, Bitmap.Config.RGB_565);
            Canvas cCanvas = new Canvas(bSephia);
            Paint pPaint = new Paint();
            ColorMatrix cmMatrix = new ColorMatrix();
            cmMatrix.setScale(.3f, .3f, .3f, 1.0f);
            ColorMatrixColorFilter cmcFilter = new ColorMatrixColorFilter(cmMatrix);
            pPaint.setColorFilter(cmcFilter);
            cCanvas.drawBitmap(bOriginal, 0, 0, pPaint);

            int [] naPixels  = new int [nWidth * nHeight] ;
            bOriginal.getPixels(naPixels, 0, nWidth, 0, 0, nWidth, nHeight);
            int nPixelLength = naPixels.length;

            int nDepth = 20;

            for (int i = 0; i < nPixelLength; i++)
            {
                int nR = (naPixels[i] >> 16) & 0xFF;
                int nG = (naPixels[i] >> 8) & 0xFF;
                int nB = naPixels[i] & 0xFF;

                int nGry = (nR + nG + nB) / 3;
                nR = nG = nB = nGry;

                nR = nR + (nDepth * 2);
                nG = nG + nDepth;

                if (nR > 255)
                    nR = 255;

                if (nG > 255)
                    nG = 255;

                naPixels[i] =  Color.rgb(nR, nG, nB) ;
            }

            return Bitmap.createBitmap(naPixels, nWidth, nHeight, Bitmap.Config.ARGB_8888) ;
        }

        public int loadTexture(Bitmap bOriginal)
        {
            final int[] textureHandle = new int[1];

            GLES20.glGenTextures(1, textureHandle, 0);

            if (textureHandle[0] != 0)
            {
                // Bind to the texture in OpenGL
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

                // Set filtering
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

                // Load the bitmap into the bound texture.
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bOriginal, 0);

                // Recycle the bitmap, since its data has been loaded into OpenGL.
                bOriginal.recycle();
            }

            if (textureHandle[0] == 0)
            {
                throw new RuntimeException("Error loading texture.");
            }

            return textureHandle[0];
        }

        public Bitmap toInvert(String sPath)
        {
            Bitmap bOriginal = BitmapFactory.decodeFile(sPath);

            // image size
            int nHeight = bOriginal.getHeight();
            int nWidth = bOriginal.getWidth();

            int nTexId = loadTexture(bOriginal);



            EffectContext ecContext = EffectContext.createWithCurrentGlContext();
            EffectFactory efFactory = ecContext.getFactory();
            Effect eInvert = efFactory.createEffect(EffectFactory.EFFECT_NEGATIVE);
            eInvert.apply(nTexId, nWidth, nHeight, nTexId);

            GLES20.

            // create new bitmap with the same settings as source bitmap
            Bitmap bInvert = Bitmap.createBitmap(nWidth, nHeight, bOriginal.getConfig());
            // color info
            int nA, nR, nG, nB;
            int nPixelColor;

            // scan through every pixel
            for (int y = 0; y < nHeight; y++)
            {
                for (int x = 0; x < nWidth; x++)
                {
                    // get one pixel
                    nPixelColor = bOriginal.getPixel(x, y);
                    // saving alpha channel
                    nA = Color.alpha(nPixelColor);
                    // inverting byte for each R/G/B channel
                    nR = 255 - Color.red(nPixelColor);
                    nG = 255 - Color.green(nPixelColor);
                    nB = 255 - Color.blue(nPixelColor);
                    // set newly-inverted pixel to output image
                    bInvert.setPixel(x, y, Color.argb(nA, nR, nG, nB));
                }
            }

            // return final bitmap
            return bInvert;
        }

        public Bitmap toEmboss(String sPath)
        {
            Bitmap bOriginal = BitmapFactory.decodeFile(sPath);

            double[][] naEmbossMatrix = new double[][]
            {
                    { -1 ,  0, -1 },
                    {  0 ,  4,  0 },
                    { -1 ,  0, -1 }
            };

            ConvolutionMatrix cmMatrix = new ConvolutionMatrix(3);
            cmMatrix.applyConfig(naEmbossMatrix);
            cmMatrix.Factor = 1;
            cmMatrix.Offset = 127;
            return ConvolutionMatrix.computeConvolution3x3(bOriginal, cmMatrix);
        }

        public Bitmap toEngrave(String sPath)
        {
            Bitmap bOriginal = BitmapFactory.decodeFile(sPath);

            //configure the convolution matrix
            ConvolutionMatrix cmMatrix = new ConvolutionMatrix(3);
            cmMatrix.setAll(0);
            cmMatrix.Matrix[0][0] = -2;
            cmMatrix.Matrix[1][1] = 2;
            cmMatrix.Factor = 1;
            cmMatrix.Offset = 95;

            //use it to create the new image
            return ConvolutionMatrix.computeConvolution3x3(bOriginal, cmMatrix);
        }

        public Bitmap toSharpen(String sPath)
        {
            Bitmap bOriginal = BitmapFactory.decodeFile(sPath);

            double[][] daSharpenMatrix = new double[][]
            {
//            { 0 , -2    , 0  },
//            { -2, weight, -2 },
//            { 0 , -2    , 0  }
            {0, -1, 0},
            {-1, 5, -1},
            {0, -1, 0}
            };

            ConvolutionMatrix cmMatrix = new ConvolutionMatrix(3);
            cmMatrix.applyConfig(daSharpenMatrix);
            cmMatrix.Factor = 5 - 8;
            return ConvolutionMatrix.computeConvolution3x3(bOriginal, cmMatrix);
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
