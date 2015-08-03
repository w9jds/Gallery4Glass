package com.w9jds.gallery4glass.Adapters;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.squareup.picasso.Picasso;
import com.w9jds.gallery4glass.Activities.MainActivity;
import com.w9jds.gallery4glass.R;
import com.w9jds.gallery4glass.Widgets.CardCursorAdapter;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by JeremyShore on 4/5/15.
 */
public class GalleryAdapter extends CardCursorAdapter {

    public GalleryAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    public static class ViewHolder {
        ImageView picture;
        TextView timestamp;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        final int layout = getItemViewType(cursor.getPosition());

        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View convertView = layoutInflater.inflate(layout, parent, false);

        ViewHolder viewHolder = new ViewHolder();
        viewHolder.picture = (ImageView) convertView.findViewById(R.id.cardImage);
        viewHolder.timestamp = (TextView) convertView.findViewById(R.id.datestamp);
        convertView.setTag(viewHolder);

        return convertView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder)view.getTag();

//        SimpleDateFormat dateFormat = new SimpleDateFormat("LLL dd");
//        Date added = new Date(cursor.getLong(MainActivity.COLUMN_DATE));
//        viewHolder.timestamp.setText(dateFormat.format(added));

        Glide.with(context)
                .load("file://" + cursor.getString(MainActivity.COLUMN_DATA))
                .centerCrop()
                .placeholder(R.drawable.ic_placeholder_photo_150)
                .crossFade()
                .thumbnail(0.2f)
                .into(viewHolder.picture);

    }

    @Override
    public int getItemViewType(int position) {
        return R.layout.image_layout;
    }

    @Override
    public int getPosition(Object o) {
        return 0;
    }
}
