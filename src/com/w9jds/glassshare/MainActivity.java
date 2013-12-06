package com.w9jds.glassshare;

import java.util.ArrayList;

import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

@SuppressLint("DefaultLocale")
public class MainActivity extends Activity 
{
	public static final String CAMERA_IMAGE_BUCKET_NAME = Environment.getExternalStorageDirectory().toString() + "/DCIM/Camera";
	public static final String CAMERA_IMAGE_BUCKET_ID = getBucketId(CAMERA_IMAGE_BUCKET_NAME);
	
	private ArrayList<String> mlsPaths = new ArrayList<String>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
			
		mlsPaths = getCameraImages(this);
		
		CardScrollView csvCardsView = new CardScrollView(this);
		csaAdapter cvAdapter = new csaAdapter(this);
		csvCardsView.setAdapter(cvAdapter);
		csvCardsView.activate();
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
	            return true;
	        case R.id.share_menu_item:
	        	return true;
	        default:
	            return super.onOptionsItemSelected(item);
		}
	};

//	private class ClickListener implements OnItemClickListener
//	{
//		@Override
//		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) 
//		{
//			// TODO Auto-generated method stub
//			openOptionsMenu();
//			
//		}
//	}

    private class csaAdapter extends CardScrollAdapter 
    {	
    	private Context mcContext;
    	
    	public csaAdapter(Context cContext)
    	{
    		mcContext = cContext;
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

	        ImageView ivPic = (ImageView) vCard.findViewById(R.id.cardImage);
	        ivPic.setImageBitmap(Bitmap.createScaledBitmap(BitmapFactory.decodeFile(mlsPaths.get(position)), 640, 470, true));

	        return vCard;
	    }
    }
}

