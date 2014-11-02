package com.w9jds.gallery4glass.Activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardScrollView;
import com.w9jds.gallery4glass.Adapters.GalleryAdapter;
import com.w9jds.gallery4glass.Classes.Paths;
import com.w9jds.gallery4glass.Classes.SingleMediaScanner;
import com.w9jds.gallery4glass.R;
import com.w9jds.gdk_progress_widget.SliderView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class EffectActivity extends Activity
{
    private GalleryAdapter mGalleryAdapter;
    private Paths mPaths = new Paths();
    private AudioManager maManager;
    private ArrayList<String> malsEffects = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //start openCV manager
        OpenCVLoader.initDebug();

        maManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        Intent iThis = getIntent();
        iThis.getExtras();
        mPaths = iThis.getParcelableExtra("PathsObject");

        setMainView();
    }

    //    @Override
//    public void onStart()
//    {
//        super.onStart();
//        // The rest of your onStart() code.
//        EasyTracker.getInstance(this).activityStart(this);  // Add this method.
//    }
//
//    @Override
//    public void onStop()
//    {
//        super.onStop();
//        // The rest of your onStop() code.
//        EasyTracker.getInstance(this).activityStop(this);  // Add this method.
//    }

    private void setMainView()
    {

        //add a card to the card scroll view for each effect that is available
        malsEffects.add(getString(R.string.grayscale_effect));
        malsEffects.add(getString(R.string.sepia_effect));
        malsEffects.add(getString(R.string.invert_effect));
        malsEffects.add(getString(R.string.canny_effect));
        malsEffects.add(getString(R.string.pixelize_effect));
        malsEffects.add(getString(R.string.sharpen_effect));

        CardScrollView csvCardsView = new CardScrollView(this);
        mGalleryAdapter = new GalleryAdapter(this, malsEffects);
        csvCardsView.setAdapter(mGalleryAdapter);
        csvCardsView.activate();

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
        (new CreateEffectComposite(nSelected, this)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public class CreateEffectComposite extends AsyncTask<Void, Void, Boolean>
    {
        private Context mContext;
        private int mEffect;

        public CreateEffectComposite(int selected, Context context)
        {
            mEffect = selected;
            mContext = context;
        }

        @Override
        protected Boolean doInBackground(Void... params)
        {
            Bitmap bitMain = null;

            switch(mEffect)
            {
                case 0:
//                    EasyTracker.getInstance(getApplicationContext()).send(MapBuilder.createEvent(
//                            "Effects",
//                            "Made",
//                            "grayscale_effect",
//                            null).build());
                    bitMain = toGrayscale(mPaths.getCurrentPositionPath());
                    break;
                case 1:
//                    EasyTracker.getInstance(getApplicationContext()).send(MapBuilder.createEvent(
//                            "Effects",
//                            "Made",
//                            "sepia_effect",
//                            null).build());
                    bitMain = toSepia(mPaths.getCurrentPositionPath());
                    break;
                case 2:
//                    EasyTracker.getInstance(getApplicationContext()).send(MapBuilder.createEvent(
//                            "Effects",
//                            "Made",
//                            "invert_effect",
//                            null).build());
                    bitMain = toInvert(mPaths.getCurrentPositionPath());
                    break;
                case 3:
//                    EasyTracker.getInstance(getApplicationContext()).send(MapBuilder.createEvent(
//                            "Effects",
//                            "Made",
//                            "canny_effect",
//                            null).build());
                    bitMain = toCanny(mPaths.getCurrentPositionPath());
                    break;
                case 4:
//                    EasyTracker.getInstance(getApplicationContext()).send(MapBuilder.createEvent(
//                            "Effects",
//                            "Made",
//                            "pixelize_effect",
//                            null).build());
                    bitMain = toPixelize(mPaths.getCurrentPositionPath());
                    break;
                case 5:
//                    EasyTracker.getInstance(getApplicationContext()).send(MapBuilder.createEvent(
//                            "Effects",
//                            "Made",
//                            "sharpen_effect",
//                            null).build());
                    bitMain = toSharpen(mPaths.getCurrentPositionPath());
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

                    String[] paths = mPaths.getCurrentPositionPath().split("/|\\.");

                    //create new file
                    File image;

                    if (malsEffects.get(mEffect).length() > 6)
                        //create a new file with the added _effectname for the vignette to be stored in
                        image = new File(path, paths[paths.length - 2] + "_" + malsEffects.get(mEffect).substring(0,6) + ".jpg");
                    else
                        //create a new file with the added _effectname for the vignette to be stored in
                        image = new File(path, paths[paths.length - 2] + "_" + malsEffects.get(mEffect) + ".jpg");

                    //create an output stream with the new file
                    FileOutputStream fileOut = new FileOutputStream(image);

                    //compress the image we just made
                    bitMain.compress(Bitmap.CompressFormat.JPEG, 100, fileOut);
                    fileOut.flush();
                    fileOut.close();

                    //add to media scanner
                    new SingleMediaScanner(mContext, image);
                }
                catch (Exception e)
                {
                    Log.d("EffectMaker", e.getCause().toString());
                }
            }

            return false;
        }

        public Bitmap toGrayscale(String path)
        {
            Mat original = new Mat();

            Utils.bitmapToMat(BitmapFactory.decodeFile(path), original);

            Imgproc.cvtColor(original, original, Imgproc.COLOR_BGR2GRAY);
            Imgproc.cvtColor(original, original, Imgproc.COLOR_GRAY2RGBA, 4);

            Bitmap grayscaled = Bitmap.createBitmap(original.width(), original.height(), Bitmap.Config.RGB_565);

            Utils.matToBitmap(original, grayscaled);
            return grayscaled;
        }

        public Bitmap toSepia(String path)
        {
            Mat original = new Mat();

            Utils.bitmapToMat(BitmapFactory.decodeFile(path), original);

            // Fill sepia kernel
            Mat mSepiaKernel = new Mat(4, 4, CvType.CV_32F);
            mSepiaKernel.put(0, 0, /* R */0.189f, 0.769f, 0.393f, 0f);
            mSepiaKernel.put(1, 0, /* G */0.168f, 0.686f, 0.349f, 0f);
            mSepiaKernel.put(2, 0, /* B */0.131f, 0.534f, 0.272f, 0f);
            mSepiaKernel.put(3, 0, /* A */0.000f, 0.000f, 0.000f, 1f);

            Core.transform(original, original, mSepiaKernel);

            Bitmap sepia = Bitmap.createBitmap(original.width(), original.height(), Bitmap.Config.RGB_565);
            Utils.matToBitmap(original, sepia);
            return sepia;
        }

        public Bitmap toInvert(String path)
        {
            //create new mat
            Mat original = new Mat();
            //populate mat with passed in image
            Utils.bitmapToMat(BitmapFactory.decodeFile(path), original);
            //invert image
            Core.bitwise_not(original, original);
            //make new bitmap
            Bitmap inverted = Bitmap.createBitmap(original.width(), original.height(), Bitmap.Config.RGB_565);
            //return image to bitmap
            Utils.matToBitmap(original, inverted);
            //return bitmap
            return inverted;
        }

        public Bitmap toSharpen(String path)
        {
            //create mat and populate with the image to be edited
            Mat original = new Mat();
            Utils.bitmapToMat(BitmapFactory.decodeFile(path), original);
            //create mat for the blur destination
            Mat gaussian = new Mat();
            //blur then sharpen image
            Imgproc.GaussianBlur(original, gaussian, new Size(5, 5), 5);
            Core.addWeighted(original, 1.5, gaussian, -0.5, 0, original);
            //create bitmap for the result
            Bitmap sharpened = Bitmap.createBitmap(original.width(), original.height(), Bitmap.Config.RGB_565);
            //return the mat as a bitmap
            Utils.matToBitmap(original, sharpened);
            //return the new image
            return sharpened;
        }

        public Bitmap toCanny(String path)
        {
            Mat original = new Mat();

            Utils.bitmapToMat(BitmapFactory.decodeFile(path), original);

            Mat intermediateMat = new Mat();

            Imgproc.Canny(original, intermediateMat, 80, 90);
            Imgproc.cvtColor(intermediateMat, original, Imgproc.COLOR_GRAY2BGRA, 4);

            Bitmap canny = Bitmap.createBitmap(original.width(), original.height(), Bitmap.Config.RGB_565);

            Utils.matToBitmap(original, canny);

            return canny;
        }

        public Bitmap toPixelize(String path)
        {
            Mat original = new Mat();
            //bring in the bitmap into the mat
            Utils.bitmapToMat(BitmapFactory.decodeFile(path), original);
            //intermediate mat
            Mat intermediateMat = new Mat();

            Imgproc.resize(original, intermediateMat, new Size(), 0.1, 0.1, Imgproc.INTER_NEAREST);
            Imgproc.resize(intermediateMat, original, original.size(), 0., 0., Imgproc.INTER_NEAREST);

            Bitmap pixelized = Bitmap.createBitmap(original.width(), original.height(), Bitmap.Config.RGB_565);

            Utils.matToBitmap(original, pixelized);

            return pixelized;

        }

        public Bitmap toPosterize(String path)
        {
            Mat original = new Mat();

            Utils.bitmapToMat(BitmapFactory.decodeFile(path), original);

            Mat intermediateMat = new Mat();

            Imgproc.Canny(original, intermediateMat, 80, 90);
            original.setTo(new Scalar(0, 0, 0, 255), intermediateMat);
            Core.convertScaleAbs(original, intermediateMat, 1./16, 0);
            Core.convertScaleAbs(intermediateMat, original, 16, 0);

            Bitmap posterized = Bitmap.createBitmap(original.width(), original.height(), Bitmap.Config.RGB_565);

            Utils.matToBitmap(original, posterized);

            return posterized;
        }

//        public Bitmap toHist(String sPath)
//        {
//            Mat hist = new Mat();
//            int thikness = (int) (sizeRgba.width / (mHistSizeNum + 10) / 5);
//            if(thikness > 5) thikness = 5;
//            int offset = (int) ((sizeRgba.width - (5*mHistSizeNum + 4*10)*thikness)/2);
//            // RGB
//            for(int c=0; c<3; c++) {
//                Imgproc.calcHist(Arrays.asList(rgba), mChannels[c], mMat0, hist, mHistSize, mRanges);
//                Core.normalize(hist, hist, sizeRgba.height/2, 0, Core.NORM_INF);
//                hist.get(0, 0, mBuff);
//                for(int h=0; h<mHistSizeNum; h++) {
//                    mP1.x = mP2.x = offset + (c * (mHistSizeNum + 10) + h) * thikness;
//                    mP1.y = sizeRgba.height-1;
//                    mP2.y = mP1.y - 2 - (int)mBuff[h];
//                    Core.line(rgba, mP1, mP2, mColorsRGB[c], thikness);
//                }
//            }
//            // Value and Hue
//            Imgproc.cvtColor(rgba, mIntermediateMat, Imgproc.COLOR_RGB2HSV_FULL);
//            // Value
//            Imgproc.calcHist(Arrays.asList(mIntermediateMat), mChannels[2], mMat0, hist, mHistSize, mRanges);
//            Core.normalize(hist, hist, sizeRgba.height/2, 0, Core.NORM_INF);
//            hist.get(0, 0, mBuff);
//            for(int h=0; h<mHistSizeNum; h++) {
//                mP1.x = mP2.x = offset + (3 * (mHistSizeNum + 10) + h) * thikness;
//                mP1.y = sizeRgba.height-1;
//                mP2.y = mP1.y - 2 - (int)mBuff[h];
//                Core.line(rgba, mP1, mP2, mWhilte, thikness);
//            }
//            // Hue
//            Imgproc.calcHist(Arrays.asList(mIntermediateMat), mChannels[0], mMat0, hist, mHistSize, mRanges);
//            Core.normalize(hist, hist, sizeRgba.height/2, 0, Core.NORM_INF);
//            hist.get(0, 0, mBuff);
//            for(int h=0; h<mHistSizeNum; h++) {
//                mP1.x = mP2.x = offset + (4 * (mHistSizeNum + 10) + h) * thikness;
//                mP1.y = sizeRgba.height-1;
//                mP2.y = mP1.y - 2 - (int)mBuff[h];
//                Core.line(rgba, mP1, mP2, mColorsHue[h], thikness);
//            }
//
//        }

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
