package com.w9jds.glassshare.Adapters;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.glass.widget.CardScrollAdapter;
import com.w9jds.glassshare.R;

public class csaAdapter extends CardScrollAdapter
{	
	private Context mcContext;
	private ArrayList<String> mlsPaths;
	
	public csaAdapter(Context cContext, ArrayList<String> alsPaths)
	{
		mcContext = cContext;
		mlsPaths = alsPaths;
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

//        BitmapFactory.Options bfoOptions = new BitmapFactory.Options();
//        bfoOptions.inJustDecodeBounds = true;
//        BitmapFactory.decodeFile(mlsPaths.get(position), bfoOptions);
//
//        bfoOptions.inSampleSize = (bfoOptions.outWidth / 2) - (640 / 2);
        
        Bitmap bImage = BitmapFactory.decodeFile(mlsPaths.get(position));
        
        ImageView ivPic = (ImageView) vCard.findViewById(R.id.cardImage);
        
        if (bImage != null)
        {
	        if (bImage.getWidth() > 640)
	        {
	            double dRatio = ((double)bImage.getWidth()) / 640;
	            ivPic.setImageBitmap(Bitmap.createScaledBitmap(bImage, 640, (int) Math.round(bImage.getHeight() / dRatio) , true));
	        }
	        else
	            ivPic.setImageBitmap(bImage);
        }

        return vCard;
    }
}
