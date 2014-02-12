package com.w9jds.gallery4glass;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardScrollView;
import com.w9jds.gallery4glass.Adapters.csaAdapter;
import com.w9jds.gallery4glass.Classes.cPaths;

import java.util.ArrayList;
import java.util.Arrays;


public class EffectActivity extends Activity
{

    //create an adapter for the cardscrollviewer
    private csaAdapter mcvAdapter;
    //special packageable list of all the image paths
    private cPaths mcpPaths = new cPaths();
    //create an audio manager for sounds
    private AudioManager maManager;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_effect);

        maManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        Intent iThis = getIntent();
        iThis.getExtras();
        mcpPaths = iThis.getParcelableExtra("PathsObject");

        setMainView();
    }



    private void setMainView()
    {
        ArrayList<String> alsEffects = new ArrayList<String>(Arrays.asList(
                "AutoFix",
                "Back Dropper",
                "Black White",
                "Fill Light",
                "Fisheye",
                "Flip",
                "Grain",
                "GrayScale",
                "Lomoish",
                "Negative",
                "Posterize",
                "Rotate",
                "Saturate",
                "Sepia",
                "Sharpen",
                "Tint" ));



        //create a new card scroll viewer for this context
        CardScrollView csvCardsView = new CardScrollView(this);
        //create a new adapter for the scroll viewer
        mcvAdapter = new csaAdapter(this, mcpPaths.getImagePaths());
        //set this adapter as the adapter for the scroll viewer
        csvCardsView.setAdapter(mcvAdapter);
        //activate this scroll viewer
        csvCardsView.activate();
        //add a listener to the scroll viewer that is fired when an item is clicked
        csvCardsView.setOnItemClickListener(new AdapterView.OnItemClickListener()
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
        setContentView(csvCardsView);
    }

}
