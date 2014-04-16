package com.w9jds.gallery4glass.Services;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.RemoteViews;

import com.google.android.glass.timeline.LiveCard;
//import com.google.android.glass.timeline.TimelineManager;
import com.w9jds.gallery4glass.MainActivity;
import com.w9jds.gallery4glass.R;

/**
 * Created by w9jds on 4/8/14.
 */
public class LiveCardService extends Service
{

    public class LiveCardBinder extends Binder
    {
        public LiveCardService getService()
        {
            return LiveCardService.this;
        }
    }

    private final IBinder mBinder = new LiveCardBinder();

    // For live card
    private LiveCard liveCard;

    public LiveCardService()
    {
        super();
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
//
//        currentState = STATE_NORMAL;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        serviceStart();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        serviceStart();
        return mBinder;
    }

    @Override
    public void onDestroy()
    {
        serviceStop();
        super.onDestroy();
    }

    private boolean serviceStart()
    {
        publishCard(this);
        return true;
    }

//    private boolean servicePause()
//    {
//        return true;
//    }
//    private boolean serviceResume()
//    {
//        return true;
//    }

    private boolean serviceStop()
    {
        unpublishCard(this);
        return true;
    }

    private void publishCard(Context context)
    {
        if (liveCard == null)
        {
            String sCardId = "Gallery_Card";
//            TimelineManager tm = TimelineManager.from(context);
//            liveCard = tm.createLiveCard(sCardId);

            liveCard.setViews(new RemoteViews(context.getPackageName(), R.layout.livecard_layout));

            Intent intent = new Intent(context, MainActivity.class);
            liveCard.setAction(PendingIntent.getActivity(context, 0, intent, 0));
            liveCard.publish(LiveCard.PublishMode.SILENT);
        }

        else
            return;
    }

    private void unpublishCard(Context context)
    {
        if (liveCard != null)
        {
            liveCard.unpublish();
            liveCard = null;
        }
    }
}