package com.w9jds.gallery4glass.Classes;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by w9jds on 1/12/14.
 */
public class cPaths implements Parcelable
{

    private ArrayList<String> ImagePaths = new ArrayList<String>();
    private int MainPosition;

    public cPaths() { }


    /***
     * Insert the passed in string to the indicated index
     * @param sString
     * @param nIndex
     */
    public void insertString(String sString, int nIndex) { ImagePaths.add(nIndex, sString); }

    /***
     * get the selected card index
     * @return MainPosition
     */
    public int getMainPosition() { return MainPosition; }

    /***
     * set the selected position in the array
     * @param i the index of selected card
     */
    public void setMainPosition(int i) { MainPosition = i; }

    /***
     * Get the arraylist of image paths
     */
    public ArrayList<String> getImagePaths() { return ImagePaths; }

    /***
     * Get the image path at the passed in index
     * @param i index to get from array list
     * @return image string path
     */
    public String getImagePathsIndex(int i) { return ImagePaths.get(i); }

    /***
     * Get the image path for current position
     * @return image string path
     */
    public String getCurrentPositionPath() { return ImagePaths.get(MainPosition); }

    /***
     * Set the passed in arraylist as the imagepaths for this object
     * @param alsPaths arraylist to store
     */
    public void setImagePaths(ArrayList<String> alsPaths) { ImagePaths = alsPaths; }

    /***
     * add image path string to arraylist
     * @param sPath the string to add to the arraylist
     */
    public void addImagePath(String sPath) { ImagePaths.add(sPath); }

    /***
     * Remove the passed in index path
     * @param i index to remove
     */
    public void removeImagePath(int i) { ImagePaths.remove(i); }

    /***
     * Removes the currently selected Image Path from the ArrayList
     */
    public void removeCurrentPositionPath() { ImagePaths.remove(MainPosition); }



    @Override
    public void writeToParcel(Parcel parcel, int i)
    {
        try
        {
            parcel.writeInt(this.getImagePaths().size());
            for (int j = 0; j < this.getImagePaths().size(); j++)
                parcel.writeString(this.getImagePaths().get(j));

            parcel.writeInt(this.getMainPosition());
        }
        catch(Exception e)
        {
            Log.d("cPathsParcel", e.toString());
        }
    }

    private cPaths(Parcel in)
    {
        try
        {
            int nPathsCount = in.readInt();
            for (int j = 0; j < nPathsCount; j++)
                this.addImagePath(in.readString());

            this.setMainPosition(in.readInt());
        }
        catch(Exception e)
        {
            Log.d("cPathsParcel", e.toString());
        }
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    public static final Creator<cPaths> CREATOR = new Creator<cPaths>()
    {
        public cPaths createFromParcel(Parcel in)
        {
            return new cPaths(in);
        }

        public cPaths[] newArray(int size)
        {
            return new cPaths[size];
        }
    };
}
