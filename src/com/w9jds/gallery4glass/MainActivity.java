package com.w9jds.gallery4glass;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.widget.CardScrollView;
import com.google.gson.JsonObject;
import com.w9jds.gallery4glass.Adapters.csaAdapter;
import com.w9jds.gallery4glass.Classes.Gallery4Glass;
import com.w9jds.gallery4glass.Classes.SingleMediaScanner;
import com.w9jds.gallery4glass.Classes.StorageService;
import com.w9jds.gallery4glass.Classes.cPaths;
import com.w9jds.gallery4glass.Services.LiveCardService;
import com.w9jds.gallery4glass.Widget.SliderView;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

import com.google.analytics.tracking.android.EasyTracker;

import static com.google.android.gms.common.GooglePlayServicesUtil.isGooglePlayServicesAvailable;

@SuppressLint("DefaultLocale")
public class MainActivity extends Activity
{
    private final String CAMERA_IMAGE_BUCKET_NAME = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Camera";
    private final String CAMERA_IMAGE_BUCKET_ID = getBucketId(CAMERA_IMAGE_BUCKET_NAME);

    private ConnectivityManager mcmCon;
    private AudioManager maManager;

    //create member variables for Azure
    private StorageService mStorageService;
    //gesture detector
    private GestureDetector mGestureDetector;
    //cardscrollview
    private CardScrollView mcsvCardsView;


    //custom adapter
    private csaAdapter mcvAdapter;
    //custom object
    private cPaths mcpPaths = new cPaths();

//    private boolean mIsBound = false;
//    private LiveCardService mliveCardService;

//    private ServiceConnection serviceConnection = new ServiceConnection()
//    {
//        public void onServiceConnected(ComponentName componentName, IBinder service)
//        {
//            mliveCardService = ((LiveCardService.LiveCardBinder)service).getService();
//        }
//        public void onServiceDisconnected(ComponentName className)
//        {
//            mliveCardService = null;
//        }
//    };
//
//    private void bindService()
//    {
//        bindService(new Intent(this, LiveCardService.class), serviceConnection, Context.BIND_AUTO_CREATE);
//        mIsBound = true;
//    }
//
//    private void unbindService()
//    {
//        if (mIsBound)
//        {
//            unbindService(serviceConnection);
//            mIsBound = false;
//        }
//    }
//
//    private void startService()
//    {
//        startService(new Intent(this, LiveCardService.class));
//    }
//
//    private void stopService()
//    {
//        stopService(new Intent(this, LiveCardService.class));
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mcmCon = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        maManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        // Turn on Gestures
        mGestureDetector = createGestureDetector(this);

//        startService();
        CreatePictureView();

    }

    @Override
    public void onStart()
    {
        super.onStart();
        // The rest of your onStart() code.
        EasyTracker.getInstance(this).activityStart(this);  // Add this method.
    }

    @Override
    public void onStop()
    {
        super.onStop();
        // The rest of your onStop() code.
        EasyTracker.getInstance(this).activityStop(this);  // Add this method.
    }

    @Override
    protected void onDestroy()
    {
//        unbindService();
        super.onDestroy();
    }

    private void CreatePictureView()
    {
        if (mcmCon.getActiveNetworkInfo().isConnected())
        {
            Gallery4Glass myApp = (Gallery4Glass) getApplication();
            mStorageService = myApp.getStorageService();
        }

        //get all the images from the camera folder (paths)
        mcpPaths.setImagePaths(getCameraImages());
        //sort the paths of pictures
//        sortPaths(mcpPaths.getImagePaths());
        Collections.reverse(mcpPaths.getImagePaths());

        //create a new card scroll viewer for this context
        mcsvCardsView = new CardScrollView(this);
        //create a new adapter for the scroll viewer
        mcvAdapter = new csaAdapter(this, mcpPaths.getImagePaths());
        //set this adapter as the adapter for the scroll viewer
        mcsvCardsView.setAdapter(mcvAdapter);
        //activate this scroll viewer
        mcsvCardsView.activate();
        //add a listener to the scroll viewer that is fired when an item is clicked
        mcsvCardsView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                maManager.playSoundEffect(Sounds.TAP);
                //save the card index that was selected
                mcpPaths.setMainPosition(position);
                //open the menu
                openOptionsMenu();
            }
        });

        //set the view of this activity
        setContentView(mcsvCardsView);
    }

    /***
     * Register for broadcasts
     */
    @Override
    protected void onResume()
    {
        IntentFilter filter = new IntentFilter();
        filter.addAction("blob.created");
        registerReceiver(receiver, filter);
        super.onResume();
    }

    /***
     * Unregister for broadcasts
     */
    @Override
    protected void onPause()
    {
        unregisterReceiver(receiver);
        super.onPause();
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event)
    {
        if (mGestureDetector != null)
            return mGestureDetector.onMotionEvent(event);

        return false;
    }

    private GestureDetector createGestureDetector(Context cContext)
    {
        GestureDetector gestureDetector = new GestureDetector(cContext);

        //Create a base listener for generic gestures
        gestureDetector.setBaseListener( new GestureDetector.BaseListener()
        {
            @Override
            public boolean onGesture(Gesture gesture)
            {

                if (gesture == Gesture.TWO_SWIPE_LEFT)
                {
                    Intent iCamera = new Intent(getApplicationContext(), CameraActivity.class);
                    startActivity(iCamera);
                    finish();
                }

                return false;
            }
        });

        return gestureDetector;
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
                iVignette.putExtra("PathsObject", mcpPaths);
                startActivityForResult(iVignette, 1);

                return true;

            case R.id.effects_menu_item:

                Intent iEffects = new Intent(this, EffectActivity.class);
                iEffects.putExtra("PathsObject", mcpPaths);
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
                        java.io.File fPic = new java.io.File(mcpPaths.getCurrentPositionPath());
                        //delete the image
                        fPic.delete();
                        //refresh the folder
                        new SingleMediaScanner(getApplicationContext(), fPic);
                        //remove the selected item from the list of images
                        mcpPaths.removeCurrentPositionPath();
                        //let the adapter know that the list of images has changed
                        mcvAdapter.notifyDataSetChanged();
                        //handled

                        setContentView(R.layout.menu_layout);
                        ((ImageView)findViewById(R.id.icon)).setImageResource(R.drawable.ic_done_50);
                        ((TextView)findViewById(R.id.label)).setText(getString(R.string.deleted_label));

                        maManager.playSoundEffect(Sounds.SUCCESS);

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

//            case R.id.upload_menu_item:
//
//                if (mcmCon.getActiveNetworkInfo().isConnected())
//                {
//                    //get google account credentials and store to member variable
//                    mgacCredential = GoogleAccountCredential.usingOAuth2(this, Arrays.asList(DriveScopes.DRIVE));
//                    //get a list of all the accounts on the device
//                    Account[] myAccounts = AccountManager.get(this).getAccounts();
//                    //for each account
//                    for (int i = 0; i < myAccounts.length; i++) {
//                        //if the account type is google
//                        if (myAccounts[i].type.equals("com.google"))
//                            //set this as the selected Account
//                            mgacCredential.setSelectedAccountName(myAccounts[i].name);
//                    }
//                    //get the drive service
//                    mdService = getDriveService(mgacCredential);
//                    //save the selected item to google drive
//                    saveFileToDrive(mcpPaths.getCurrentPositionPath());
//                }
//
//                return true;

            case R.id.uploadphone_menu_item:

                if (mcmCon.getActiveNetworkInfo().isConnected())
                {
                    setContentView(R.layout.menu_layout);

                    svProgress = (SliderView)findViewById(R.id.slider);

                    svProgress.startIndeterminate();

                    ((ImageView)findViewById(R.id.icon)).setImageResource(R.drawable.ic_mobile_phone_50);
                    ((TextView)findViewById(R.id.label)).setText(getString(R.string.uploading_label));

                    String sContainer = "";
                    String[] saImage = mcpPaths.getCurrentPositionPath().split("/|\\.");

                    Account[] myAccounts = AccountManager.get(this).getAccounts();
                    //for each account
                    for (int i = 0; i < myAccounts.length; i++)
                    {
                        //if the account type is google
                        if (myAccounts[i].type.equals("com.google"))
                        {
                            //set this as the selected Account
                            String[] saAccount = myAccounts[i].name.split("@|\\.");
                            sContainer = saAccount[0] + saAccount[1] + saAccount[2];
                        }
                    }

                    mStorageService.addContainer(sContainer, false);
                    mStorageService.getSasForNewBlob(sContainer, saImage[saImage.length - 2]);
                }
                else
                    maManager.playSoundEffect(Sounds.DISALLOWED);
                return true;

            default:
                return super.onOptionsItemSelected(iItem);
        }
    }

    /***
     * Broadcast receiver handles blobs being loaded or a new blob being created
     */
    private BroadcastReceiver receiver = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent)
        {
            String intentAction = intent.getAction();

            if (intentAction.equals("blob.created"))
            {
                //If a blob has been created, upload the image
                JsonObject blob = mStorageService.getLoadedBlob();
                String sasUrl = blob.getAsJsonPrimitive("sasUrl").toString();
                (new ImageUploaderTask(sasUrl, mcpPaths.getMainPosition(), mcpPaths.getImagePaths())).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            }
        }
    };

    public class ImageUploaderTask extends AsyncTask<Void, Void, Boolean>
    {
        private String mUrl;
        private ArrayList<String> mlsPaths;
        private int miPosition;

        public ImageUploaderTask(String url, int iPosition, ArrayList<String> lsPath)
        {
            mUrl = url;
            miPosition = iPosition;
            mlsPaths = lsPath;
        }

        @Override
        protected Boolean doInBackground(Void... params)
        {
            try
            {
                java.io.File fImage = new java.io.File(mlsPaths.get(miPosition));
                FileInputStream fisStream = new FileInputStream(fImage);
                byte[] byteArray = new byte[(int)fImage.length()];
                fisStream.read(byteArray);

                // Post our image data (byte array) to the server
                URL url = new URL(mUrl.replace("\"", ""));
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("PUT");
                urlConnection.addRequestProperty("Content-Type", "image/jpeg");
                urlConnection.setRequestProperty("Content-Length", ""+ byteArray.length);
                // Write image data to server
                DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
                wr.write(byteArray);
                wr.flush();
                wr.close();
                int response = urlConnection.getResponseCode();
                urlConnection.disconnect();
                //If we successfully uploaded, return true
                if (response == 201 && urlConnection.getResponseMessage().equals("Created"))
                    return true;

            }

            catch (Exception ex)
            {
                Log.e("Gallery4GlassImageUploadTask", ex.getMessage());
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean uploaded)
        {
            if (uploaded)
            {
                setContentView(R.layout.menu_layout);
                ((ImageView)findViewById(R.id.icon)).setImageResource(R.drawable.ic_done_50);
                ((TextView)findViewById(R.id.label)).setText(getString(R.string.uploaded_label));

                maManager.playSoundEffect(Sounds.SUCCESS);
            }

            new Handler().postDelayed(new Runnable()
            {
                public void run()
                {
                    CreatePictureView();
                }
            }, 1000);
        }
    }

}



