package com.w9jds.gallery4glass.Activities;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardScrollView;
import com.w9jds.gallery4glass.Adapters.GalleryAdapter;
import com.w9jds.gallery4glass.R;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;

public class MainActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * {@link CardScrollView} to use as the main content view.
     */
    private CardScrollView mCardScroller;

    private ConnectivityManager mConnectionManager;
    private GalleryAdapter mGalleryAdapter;
    private AudioManager mAudioManager;

    public static int COLUMN_DATA = 1;
    public static int COLUMN_DATE = 2;

    private View mView;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        mConnectionManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mCardScroller = new CardScrollView(this);

        //create a new adapter for the scroll viewer
        mGalleryAdapter = new GalleryAdapter(this, null, 0);
        //set this adapter as the adapter for the scroll viewer
        mCardScroller.setAdapter(mGalleryAdapter);
        //activate this scroll viewer
        mCardScroller.activate();
        //add a listener to the scroll viewer that is fired when an item is clicked
        mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mAudioManager.playSoundEffect(Sounds.TAP);
                //open the menu
                openOptionsMenu();
            }
        });

        //set the view of this activity
        setContentView(mCardScroller);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCardScroller.activate();
    }

    @Override
    protected void onPause() {
        mCardScroller.deactivate();
        super.onPause();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Get relevant columns for use later.
        String[] projection = {
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.TITLE
        };

        String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
//                + " OR "
//                + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
//                + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

        Uri queryUri = MediaStore.Files.getContentUri("external");


        return new CursorLoader(this, queryUri, projection, selection,
                null, MediaStore.Files.FileColumns.DATE_ADDED + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mGalleryAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mGalleryAdapter.swapCursor(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.gallery, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem menuItem)
    {

        switch (menuItem.getItemId())
        {
//            case R.id.vignette_menu_item:
//                Intent iVignette = new Intent(this, VignetteActivity.class);
//                iVignette.putExtra("PathsObject", mPaths);
//                startActivityForResult(iVignette, 1);
//                return true;
//
//            case R.id.effects_menu_item:
//                Intent iEffects = new Intent(this, EffectActivity.class);
//                iEffects.putExtra("PathsObject", mPaths);
//                startActivityForResult(iEffects, 1);
//                return true;
//
            case R.id.delete_menu_item:


//                //set the text as deleting
//                setContentView(R.layout.menu_layout);
//                ((ImageView)findViewById(R.id.icon)).setImageResource(R.drawable.ic_delete_50);
//                ((TextView)findViewById(R.id.label)).setText(getString(R.string.deleting_label));
//
//                svProgress = (SliderView)findViewById(R.id.slider);
//                svProgress.startProgress(1000, new Animator.AnimatorListener()
//                {
//                    @Override
//                    public void onAnimationStart(Animator animation)
//                    {
//
//                    }
//
//                    @Override
//                    public void onAnimationEnd(Animator animation)
//                    {
//                        //pull the file from the path of the selected item
//                        java.io.File fPic = new java.io.File(mPaths.getCurrentPositionPath());
//                        //delete the image
//                        fPic.delete();
//                        //refresh the folder
//                        new SingleMediaScanner(getApplicationContext(), fPic);
//                        //remove the selected item from the list of images
//                        mPaths.removeCurrentPositionPath();
//                        //let the adapter know that the list of images has changed
//                        mGalleryAdapter.notifyDataSetChanged();
//                        //handled
//
//                        setContentView(R.layout.menu_layout);
//                        ((ImageView)findViewById(R.id.icon)).setImageResource(R.drawable.ic_done_50);
//                        ((TextView)findViewById(R.id.label)).setText(getString(R.string.deleted_label));
//
//                        mAudioManager.playSoundEffect(Sounds.SUCCESS);
//
//                        new Handler().postDelayed(new Runnable()
//                        {
//                            public void run()
//                            {
//                                CreatePictureView();
//                            }
//                        }, 1000);
//                    }
//
//                    @Override
//                    public void onAnimationCancel(Animator animation)
//                    {
//                        CreatePictureView();
//                    }
//
//                    @Override
//                    public void onAnimationRepeat(Animator animation)
//                    {
//
//                    }
//                });
//                return true;

            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }
}
