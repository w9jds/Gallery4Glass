package com.w9jds.glassshare.Classes;

import java.util.ArrayList;

/**
 * Created by w9jds on 1/12/14.
 */
public class cPaths
{

    private ArrayList<String> ImagePaths = new ArrayList<String>();
    private int MainPosition;

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
     * Set the passed in arraylist as the imagepaths for this object
     * @param alsPaths arraylist to store
     */
    public void setImagePaths(ArrayList<String> alsPaths) { ImagePaths = alsPaths; }

    /***
     * add image path string to arraylist
     * @param sPath the string to add to the arraylist
     */
    public void addImagePath(String sPath) { ImagePaths.add(sPath); }

}
