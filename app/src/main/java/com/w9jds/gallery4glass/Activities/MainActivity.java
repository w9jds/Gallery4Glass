package com.w9jds.gallery4glass.Activities;

import android.animation.Animator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.gesture.Gesture;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MotionEvent;
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

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends Activity
{
    private final String CAMERA_IMAGE_BUCKET_NAME = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Camera";
    private final String CAMERA_IMAGE_BUCKET_ID = getBucketId(CAMERA_IMAGE_BUCKET_NAME);

    private ConnectivityManager mConnectionManager;
    private AudioManager mAudioManager;

    //cardscrollview
    private CardScrollView mCardsView;


    //custom adapter
    private GalleryAdapter mGalleryAdapter;
    //custom object
    private Paths mPaths = new Paths();


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mConnectionManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        CreatePictureView();
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

    @Override
    protected void onDestroy()
    {
//        unbindService();
        super.onDestroy();
    }

    private void CreatePictureView()
    {
        //get all the images from the camera folder (paths)
        mPaths.setImagePaths(getCameraImages());
        //sort the paths of pictures
//        sortPaths(mcpPaths.getImagePaths());
        Collections.reverse(mPaths.getImagePaths());

        //create a new card scroll viewer for this context
        mCardsView = new CardScrollView(this);
        //create a new adapter for the scroll viewer
        mGalleryAdapter = new GalleryAdapter(this, mPaths.getImagePaths());
        //set this adapter as the adapter for the scroll viewer
        mCardsView.setAdapter(mGalleryAdapter);
        //activate this scroll viewer
        mCardsView.activate();
        //add a listener to the scroll viewer that is fired when an item is clicked
        mCardsView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                mAudioManager.playSoundEffect(Sounds.TAP);
                //save the card index that was selected
                mPaths.setMainPosition(position);
                //open the menu
                openOptionsMenu();
            }
        });

        //set the view of this activity
        setContentView(mCardsView);
    }

    public String getBucketId(String path)
    {
        return String.valueOf(path.toLowerCase().hashCode());
    }

    /***
     * Get all the image file paths on this device (from the camera folder)
     * @return an arraylist of all the file paths
     */
    public ArrayList<String> getCameraImages()
    {
        final String[] saProj = {MediaStore.Images.Media.DATA};
        final String sSelection = MediaStore.Images.Media.BUCKET_ID + " = ?";
        final String[] saSelectionArgs = { CAMERA_IMAGE_BUCKET_ID };
        final Cursor cCursor = this.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, saProj, sSelection, saSelectionArgs, null);
        ArrayList<String> result = new ArrayList<String>(cCursor.getCount());

        if (cCursor.moveToFirst())
        {
            final int dataColumn = cCursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            do
            {
                final String data = cCursor.getString(dataColumn);
                result.add(data);
            }

            while (cCursor.moveToNext());
        }

        cCursor.close();
        return result;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode == RESULT_OK)
        {
            switch(requestCode)
            {
                case 1:
                    CreatePictureView();
                    break;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem iItem)
    {
        SliderView svProgress;

        switch (iItem.getItemId())
        {
            case R.id.vignette_menu_item:
                Intent iVignette = new Intent(this, VignetteActivity.class);
                iVignette.putExtra("PathsObject", mPaths);
                startActivityForResult(iVignette, 1);
                return true;

            case R.id.effects_menu_item:
                Intent iEffects = new Intent(this, EffectActivity.class);
                iEffects.putExtra("PathsObject", mPaths);
                startActivityForResult(iEffects, 1);
                return true;

            case R.id.delete_menu_item:
                //set the text as deleting
                setContentView(R.layout.menu_layout);
                ((ImageView)findViewById(R.id.icon)).setImageResource(R.drawable.ic_delete_50);
                ((TextView)findViewById(R.id.label)).setText(getString(R.string.deleting_label));

                svProgress = (SliderView)findViewById(R.id.slider);
                svProgress.startProgress(1000, new Animator.AnimatorListener()
                {
                    @Override
                    public void onAnimationStart(Animator animation)
                    {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation)
                    {
                        //pull the file from the path of the selected item
                        java.io.File fPic = new java.io.File(mPaths.getCurrentPositionPath());
                        //delete the image
                        fPic.delete();
                        //refresh the folder
                        new SingleMediaScanner(getApplicationContext(), fPic);
                        //remove the selected item from the list of images
                        mPaths.removeCurrentPositionPath();
                        //let the adapter know that the list of images has changed
                        mGalleryAdapter.notifyDataSetChanged();
                        //handled

                        setContentView(R.layout.menu_layout);
                        ((ImageView)findViewById(R.id.icon)).setImageResource(R.drawable.ic_done_50);
                        ((TextView)findViewById(R.id.label)).setText(getString(R.string.deleted_label));

                        mAudioManager.playSoundEffect(Sounds.SUCCESS);

                        new Handler().postDelayed(new Runnable()
                        {
                            public void run()
                            {
                                CreatePictureView();
                            }
                        }, 1000);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation)
                    {
                        CreatePictureView();
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation)
                    {

                    }
                });
                return true;

            default:
                return super.onOptionsItemSelected(iItem);
        }
    }
}



