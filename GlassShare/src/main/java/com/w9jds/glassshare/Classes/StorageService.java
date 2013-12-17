package com.w9jds.glassshare.Classes;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.util.Pair;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.MobileServiceJsonTable;
import com.microsoft.windowsazure.mobileservices.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.TableDeleteCallback;
import com.microsoft.windowsazure.mobileservices.TableJsonOperationCallback;
import com.microsoft.windowsazure.mobileservices.TableJsonQueryCallback;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by w9jds on 12/15/13.
 */
public class StorageService
{
    private MobileServiceClient mClient;
    private MobileServiceJsonTable mTableTables;
    private MobileServiceJsonTable mTableTableRows;
    private MobileServiceJsonTable mTableContainers;
    private MobileServiceJsonTable mTableBlobs;
    private Context mContext;
    private final String TAG = "StorageService";
    private List<Map<String, String>> mTables;
    private ArrayList<JsonElement> mTableRows;
    private List<Map<String, String>> mContainers;
    private List<Map<String, String>> mBlobNames;
    private ArrayList<JsonElement> mBlobObjects;
    private JsonObject mLoadedBlob;

    /***
     * Initialize our service
     * @param context
     */
    public StorageService(Context context)
    {
        mContext = context;
        try
        {
            
            mTableTables = mClient.getTable("Tables");
            mTableTableRows = mClient.getTable("TableRows");
            mTableContainers = mClient.getTable("BlobContainers");
            mTableBlobs = mClient.getTable("BlobBlobs");
        }
        catch (MalformedURLException e)
        {
            Log.e(TAG, "There was an error creating the Mobile Service. Verify the URL");
        }
    }

    public List<Map<String, String>> getLoadedTables()
    {
        return this.mTables;
    }

    public JsonElement[] getLoadedTableRows()
    {
        return this.mTableRows.toArray(new JsonElement[this.mTableRows.size()]);
    }

    public List<Map<String, String>> getLoadedContainers()
    {
        return this.mContainers;
    }

    public List<Map<String, String>> getLoadedBlobNames()
    {
        return this.mBlobNames;
    }

    public JsonElement[] getLoadedBlobObjects()
    {
        return this.mBlobObjects.toArray(new JsonElement[this.mBlobObjects.size()]);
    }

    public JsonObject getLoadedBlob()
    {
        return this.mLoadedBlob;
    }

    /***
     * Gets all of the containers from storage
     */
    public void getContainers()
    {
        mTableContainers.where().execute(new TableJsonQueryCallback()
        {
            @Override
            public void onCompleted(JsonElement result, int count, Exception exception, ServiceFilterResponse response)
            {
                if (exception != null)
                {
                    Log.e(TAG, exception.getCause().getMessage());
                    return;
                }
                //Loop through and build an array of container names
                JsonArray results = result.getAsJsonArray();
                mContainers = new ArrayList<Map<String, String>>();

                for (int i = 0; i < results.size(); i ++)
                {
                    JsonElement item = results.get(i);
                    Map<String, String> map = new HashMap<String, String>();
                    map.put("ContainerName", item.getAsJsonObject().getAsJsonPrimitive("name").getAsString());
                    mContainers.add(map);
                }

                //Broadcast that the containers have been loaded
                Intent broadcast = new Intent();
                broadcast.setAction("containers.loaded");
                mContext.sendBroadcast(broadcast);
            }
        });
    }

    /***
     * Adds a new container
     * @param containerName
     * @param isPublic - specifies ithe container should be public or not
     */
    public void addContainer(String containerName, boolean isPublic)
    {
        //Creating a json object with the container name
        JsonObject newContainer = new JsonObject();

        newContainer.addProperty("containerName", containerName);

        //Passing over the public flag as a parameter
        List<Pair<String,String>> parameters = new ArrayList<Pair<String, String>>();

        parameters.add(new Pair<String, String>("isPublic", isPublic ? "1" : "0"));

        mTableContainers.insert(newContainer, parameters, new TableJsonOperationCallback()
        {
            @Override
            public void onCompleted(JsonObject jsonObject, Exception exception, ServiceFilterResponse response)
            {
                if (exception != null)
                {
                    Log.e(TAG, exception.getCause().getMessage());
                    return;
                }
                //Refetch the containers from the server
                getContainers();
            }
        });
    }

    /***
     * Deletes a container
     * @param containerName
     */
    public void deleteContainer(String containerName)
    {
        //Create the json Object we'll send over and fill it with the required
        //id property - otherwise we'll get kicked back
        JsonObject container = new JsonObject();
        container.addProperty("id", 0);
        //Create parameters to pass in the container details.  We do this with params
        //because it would be stripped out if we put it on the container object
        List<Pair<String,String>> parameters = new ArrayList<Pair<String, String>>();
        parameters.add(new Pair<String, String>("containerName", containerName));
        mTableContainers.delete(container, parameters, new TableDeleteCallback()
        {
            @Override
            public void onCompleted(Exception exception, ServiceFilterResponse response)
            {
                if (exception != null)
                {
                    Log.e(TAG, exception.getCause().getMessage());
                    return;
                }
                //Refetch containers from the server
                getContainers();
            }
        });
    }

    /***
     * Handles deleting a blob
     * @param containerName
     * @param blobName
     */
//    public void deleteBlob(final String containerName, String blobName) {
//        //Create the json Object we'll send over and fill it with the required
//        //id property - otherwise we'll get kicked back
//        JsonObject blob = new JsonObject();
//        blob.addProperty("id", 0);
//
//        //Create parameters to pass in the blob details.  We do this with params
//        //because it would be stripped out if we put it on the blob object
//        List<Pair<String,String>> parameters = new ArrayList<Pair<String, String>>();
//
//        parameters.add(new Pair<String, String>("containerName", containerName));
//        parameters.add(new Pair<String, String>("blobName", blobName));
//
//        mTableBlobs.delete(blob, parameters, new TableDeleteCallback()
//        {
//            @Override
//            public void onCompleted(Exception exception, ServiceFilterResponse response)
//            {
//                if (exception != null)
//                {
//                    Log.e(TAG, exception.getCause().getMessage());
//                    return;
//                }
//
//                //Refetch the blobs from the server
////                getBlobsForContainer(containerName);
//            }
//        });
//    }

    /***
     * Gets a SAS URL for an existing blob
     * @param containerName
     * @param blobName
     * NOTE THIS IS DONE AS A SEPARATE METHOD FROM getSasForNewBlob BECAUSE IT
     * BROADCASTS A DIFFERENT ACTION
     */
//    public void getBlobSas(String containerName, String blobName)
//    {
//        //Create the json Object we'll send over and fill it with the required
//        //id property - otherwise we'll get kicked back
//        JsonObject blob = new JsonObject();
//        blob.addProperty("id", 0);
//        //Create parameters to pass in the blob details.  We do this with params
//        //because it would be stripped out if we put it on the blob object
//        List<Pair<String,String>> parameters = new ArrayList<Pair<String, String>>();
//        parameters.add(new Pair<String, String>("containerName", containerName));
//        parameters.add(new Pair<String, String>("blobName", blobName));
//        mTableBlobs.insert(blob, parameters, new TableJsonOperationCallback()
//        {
//            @Override
//            public void onCompleted(JsonObject jsonObject, Exception exception, ServiceFilterResponse response)
//            {
//                if (exception != null)
//                {
//                    Log.e(TAG, exception.getCause().getMessage());
//                    return;
//                }
//                //Set the loaded blob
//                mLoadedBlob = jsonObject;
//                //Broadcast that the blob is loaded
//                Intent broadcast = new Intent();
//                broadcast.setAction("blob.loaded");
//                mContext.sendBroadcast(broadcast);
//            }
//        });
//    }

    /***
     * Gets a SAS URL for a new blob so we can upload it to the server
     * @param containerName
     * @param blobName
     * NOTE THIS IS DONE AS A SEPARATE METHOD FROM getSasForNewBlob BECAUSE IT
     * BROADCASTS A DIFFERENT ACTION
     */
    public void getSasForNewBlob(String containerName, String blobName)
    {
        //Create the json Object we'll send over and fill it with the required
        //id property - otherwise we'll get kicked back
        JsonObject blob = new JsonObject();

        blob.addProperty("id", 0);

        //Create parameters to pass in the blob details.  We do this with params
        //because it would be stripped out if we put it on the blob object
        List<Pair<String,String>> parameters = new ArrayList<Pair<String, String>>();

        parameters.add(new Pair<String, String>("containerName", containerName));
        parameters.add(new Pair<String, String>("blobName", blobName));

        mTableBlobs.insert(blob, parameters, new TableJsonOperationCallback()
        {
            @Override
            public void onCompleted(JsonObject jsonObject, Exception exception, ServiceFilterResponse response) {
                if (exception != null)
                {
                    Log.e(TAG, exception.getCause().getMessage());
                    return;
                }
                //Set the loaded blob
                mLoadedBlob = jsonObject;
                //Broadcast that we are ready to upload the blob data
                Intent broadcast = new Intent();
                broadcast.setAction("blob.created");
                mContext.sendBroadcast(broadcast);
            }
        });
    }
}
