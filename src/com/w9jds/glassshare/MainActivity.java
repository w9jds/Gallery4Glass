package com.w9jds.glassshare;

import java.io.File;
import java.util.ArrayList;

import android.accounts.*;
import com.google.android.glass.widget.CardScrollView;
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
	public static final String CAMERA_IMAGE_BUCKET_NAME = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Camera";
	public static final String CAMERA_IMAGE_BUCKET_ID = getBucketId(CAMERA_IMAGE_BUCKET_NAME);
	
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
	        case R.id.share_menu_item:
//                Account[] accounts = AccountManager.get(this).getAccounts();

                Account[] accounts = AccountManager.get(this).getAccounts();
                String[] names = new String[accounts.length];

                for (int i = 0; i < names.length; i++)
                    names[i] = accounts[i].name;

	        	return true;
	        default:
	            return super.onOptionsItemSelected(item);
		}
	};

}



