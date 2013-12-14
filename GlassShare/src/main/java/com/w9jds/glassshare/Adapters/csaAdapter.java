package com.w9jds.glassshare.Adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.glass.widget.CardScrollAdapter;
import com.w9jds.glassshare.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by w9jds on 12/13/13.
 */
public class csaAdapter extends CardScrollAdapter
{
    private Context mcContext;
    private ArrayList<String> mlsPaths;
    private Bitmap mPlaceHolderBitmap;

    public csaAdapter(Context cContext, ArrayList<String> alsPaths)
    {
        mcContext = cContext;
        mlsPaths = alsPaths;
        mPlaceHolderBitmap = BitmapFactory.decodeResource(mcContext.getResources(), R.drawable.ic_placeholder_photo_150);
    }

    @Override
    public int findIdPosition(Object id)
    {
        return -1;
    }

    @Override
    public int findItemPosition(Object item)
    {
        return mlsPaths.indexOf(item);
    }

    @Override
    public int getCount()
    {
        return mlsPaths.size();
    }

    @Override
    public Object getItem(int position)
    {
        return mlsPaths.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        LayoutInflater inflater = (LayoutInflater) mcContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View vCard = inflater.inflate(R.layout.card_layout, parent, false);

        //get the imageview we are going to populate
        ImageView ivPic = (ImageView) vCard.findViewById(R.id.cardImage);


        if (cancelPotentialWork(position, ivPic))
        {
            final BitmapWorkerTask task = new BitmapWorkerTask(ivPic, mlsPaths, position);

            final AsyncDrawable asyncDrawable = new AsyncDrawable(mcContext.getResources(), mPlaceHolderBitmap, task);
            ivPic.setScaleType(ImageView.ScaleType.CENTER);
            ivPic.setImageDrawable(asyncDrawable);

            task.execute();
        }

        return vCard;
    }

    public static boolean cancelPotentialWork(int iPosition, ImageView ivCard)
    {
        final BitmapWorkerTask bwtTask = getBitmapWorkerTask(ivCard);

        if (bwtTask != null)
        {
            final int position = bwtTask.miPosition;
            if (position != iPosition)
                // Cancel previous task
                bwtTask.cancel(true);
            else
                // The same work is already in progress
                return false;
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }

    static class AsyncDrawable extends BitmapDrawable
    {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask)
        {
            super(res, bitmap);
            bitmapWorkerTaskReference =  new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask()
        {
            return bitmapWorkerTaskReference.get();
        }
    }


    private static BitmapWorkerTask getBitmapWorkerTask(ImageView ivCard)
    {
        if (ivCard != null)
        {
            final Drawable drawable = ivCard.getDrawable();
            if (drawable instanceof AsyncDrawable)
            {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    public class BitmapWorkerTask extends AsyncTask<Integer, Void, Bitmap>
    {
        private final ArrayList<String> mlsPaths;
        private final WeakReference<ImageView> mivCard;
        private final int miPosition;

        public BitmapWorkerTask(ImageView ivCard, ArrayList<String> lsPaths, int iPosition)
        {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            mivCard = new WeakReference<ImageView>(ivCard);
            miPosition = iPosition;
            mlsPaths = lsPaths;

        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(Integer... params)
        {
            //create new options for bitmap import
            BitmapFactory.Options bfoOptions = new BitmapFactory.Options();
            //set it so you only get bounds (no pixels)
            bfoOptions.inJustDecodeBounds = true;
            //pull the info from the file
            BitmapFactory.decodeFile(mlsPaths.get(miPosition), bfoOptions);

            if (bfoOptions.outWidth > 640)
            {
                //turn off the bounds only so you get the pixels this time
                bfoOptions.inJustDecodeBounds = false;
                //calculate the sample size and set it in the options
                bfoOptions.inSampleSize = calculateInSampleSize(bfoOptions, 640, 360);

                Bitmap bImage = BitmapFactory.decodeFile(mlsPaths.get(miPosition), bfoOptions);

                double dRatio = ((double)bImage.getWidth()) / 640;
                return Bitmap.createScaledBitmap(bImage, 640, (int) Math.round(bImage.getHeight() / dRatio), true);
            }
            else
                return BitmapFactory.decodeFile(mlsPaths.get(miPosition));

        }

        public int calculateInSampleSize( BitmapFactory.Options bfoOptions, int iReqWidth, int iReqHeight)
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
                final int halfHeight = height / 2;
                final int halfWidth = width / 2;

                //white both half dimensions divided by the sample size are still greater than the requested dimensions
                while ((halfHeight / inSampleSize) > iReqHeight && (halfWidth / inSampleSize) > iReqWidth)
                    //multiply the sample size by 2
                    inSampleSize *= 2;
            }

            //once the loop is done return the sample size
            return inSampleSize;
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bPic)
        {
            if (isCancelled())
                bPic = null;

            if (mivCard != null && bPic != null)
            {
                final ImageView ivCard = mivCard.get();
                final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(ivCard);
                if (this == bitmapWorkerTask && ivCard != null)
                {
                    ivCard.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    ivCard.setImageBitmap(bPic);
                }
            }
        }
    }
}
