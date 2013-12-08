package com.w9jds.glassshare;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import android.accounts.*;
import com.google.android.glass.widget.CardScrollView;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.w9jds.glassshare.Adapters.csaAdapter;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

@SuppressLint("DefaultLocale")
public class MainActivity extends Activity 
{
    static final int REQUEST_ACCOUNT_PICKER = 1;
    static final int REQUEST_AUTHORIZATION = 2;

	public static final String CAMERA_IMAGE_BUCKET_NAME = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Camera";
	public static final String CAMERA_IMAGE_BUCKET_ID = getBucketId(CAMERA_IMAGE_BUCKET_NAME);

    private static Drive service;
    private GoogleAccountCredential credential;
	
	//custom adapter
	private csaAdapter mcvAdapter;
	//list for all the paths of the images on google glass
	private ArrayList<String> mlsPaths = new ArrayList<String>();
	//variable for the last selected index
	private int iPosition;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		//get all the images from the camera folder (paths)
		mlsPaths = getCameraImages(this);
		//create a new card scroll viewer for this context
		CardScrollView csvCardsView = new CardScrollView(this);
		//create a new adapter for the scroll viewer
		mcvAdapter = new csaAdapter(this, mlsPaths);
		//set this adapter as the adapter for the scroll viewer
		csvCardsView.setAdapter(mcvAdapter);
		//activate this scroll viewer
		csvCardsView.activate();
		//add a listener to the scroll viewer that is fired when an item is clicked
        csvCardsView.setOnItemClickListener(new OnItemClickListener() 
        {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
			{
				//save the card index that was selected
				iPosition = position;
				//open the menu
				openOptionsMenu();
	    		}
         });
		
        //set the view of this activity
		setContentView(csvCardsView);
	}

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null)
                {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null)
                    {
                        credential.setSelectedAccountName(accountName);
                        service = getDriveService(credential);
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == Activity.RESULT_OK)
                    saveFileToDrive(mlsPaths.get(iPosition));
                else
                    startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);

                break;
        }
    }

	
	public static String getBucketId(String path) 
	{
	    return String.valueOf(path.toLowerCase().hashCode());
	}

	public static ArrayList<String> getCameraImages(Context context) 
	{
	    final String[] projection = { MediaStore.Images.Media.DATA };
	    final String selection = MediaStore.Images.Media.BUCKET_ID + " = ?";
	    final String[] selectionArgs = { CAMERA_IMAGE_BUCKET_ID };
	    final Cursor cursor = context.getContentResolver().query(Images.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null);
	    ArrayList<String> result = new ArrayList<String>(cursor.getCount());
	    
	    if (cursor.moveToFirst()) 
	    {
	        final int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
	        do 
	        {
	            final String data = cursor.getString(dataColumn);
	            result.add(data);
	        } while (cursor.moveToNext());
	    }
	    
	    cursor.close();
	    return result;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(android.view.MenuItem item) 
	{
		switch (item.getItemId()) 
		{
	        case R.id.delete_menu_item:
	        	//pull the file from the path of the selected item
	        	File fPic = new File(mlsPaths.get(iPosition));
	        	//delete the image
	        	fPic.delete();
	        	//refresh the folder
	        	sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" +  Environment.getExternalStorageDirectory())));
	        	//remove the selected item from the list of images
	        	mlsPaths.remove(iPosition);
	        	//let the adapter know that the list of images has changed
	        	mcvAdapter.notifyDataSetChanged();
	        	//handled
	            return true;
	        case R.id.upload_menu_item:
                credential = GoogleAccountCredential.usingOAuth2(this, Arrays.asList(DriveScopes.DRIVE));
                startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
	        	return true;
            case R.id.share_menu_item:
                return true;
	        default:
	            return super.onOptionsItemSelected(item);
		}
	};

    private void saveFileToDrive(String sPath)
    {
        final String msPath = sPath;

        Thread t = new Thread(new Runnable()
        {
            @Override
            public void run() {
                try {
                    // File's binary content
                    File fContent = new File(msPath);
                    FileContent mediaContent = new FileContent("image/jpeg", fContent);

                    // File's metadata.
                    com.google.api.services.drive.model.File gdfBody = new com.google.api.services.drive.model.File();
                    gdfBody.setTitle(fContent.getName());
                    gdfBody.setMimeType("image/jpeg");

                    com.google.api.services.drive.model.File gdfFile = service.files().insert(gdfBody, mediaContent).execute();
                    if (gdfFile != null)
                    {
//                        showToast("Photo uploaded: " + file.getTitle());
                    }
                }
                catch (UserRecoverableAuthIOException e)
                {
                    startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }

    private Drive getDriveService(GoogleAccountCredential credential)
    {
        return new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).build();
    }

}



