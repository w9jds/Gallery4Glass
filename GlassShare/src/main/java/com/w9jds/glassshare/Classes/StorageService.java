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

//            mTableTables = mClient.getTable("");
//            mTableTableRows = mClient.getTable("");
            mTableContainers = mClient.getTable("");
//            mTableBlobs = mClient.getTable("");
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
     * Fetches all of the tables from storage
     */
    public void getTables()
    {
        mTableTables.where().execute(new TableJsonQueryCallback()
        {
            @Override
            public void onCompleted(JsonElement result, int count, Exception exception, ServiceFilterResponse response)
            {
                if (exception != null)
                {
                    Log.e(TAG, exception.getCause().getMessage());
                    return;
                }
                JsonArray results = result.getAsJsonArray();

                mTables = new ArrayList<Map<String, String>>();
                //Loop through the results and get the name of each table
                for (int i = 0; i < results.size(); i ++)
                {
                    JsonElement item = results.get(i);
                    Map<String, String> map = new HashMap<String, String>();
                    map.put("TableName", item.getAsJsonObject().getAsJsonPrimitive("TableName").getAsString());
                    mTables.add(map);
                }
                //Broadcast that tables have been loaded
                Intent broadcast = new Intent();
                broadcast.setAction("tables.loaded");
                mContext.sendBroadcast(broadcast);
            }
        });
    }

    /***
     * Adds a new table
     * @param tableName
     */
    public void addTable(String tableName)
    {
        JsonObject newTable = new JsonObject();
        newTable.addProperty("tableName", tableName);
        mTableTables.insert(newTable, new TableJsonOperationCallback()
        {
            @Override
            public void onCompleted(JsonObject jsonObject, Exception exception, ServiceFilterResponse response)
            {
                if (exception != null)
                {
                    Log.e(TAG, exception.getCause().getMessage());
                    return;
                }
                //Refetch the tables from the server
                getTables();
            }
        });
    }

    /***
     * Handles deleting a table from storage
     * @param tableName
     */
    public void deleteTable(String tableName)
    {
        //Create the json Object we'll send over and fill it with the required
        //id property - otherwise we'll get kicked back
        JsonObject table = new JsonObject();
        table.addProperty("id", 0);
        //Create parameters to pass in the table name.  We do this with params
        //because it would be stripped out if we put it on the table object
        List<Pair<String,String>> parameters = new ArrayList<Pair<String, String>>();
        parameters.add(new Pair<String, String>("tableName", tableName));
        mTableTables.delete(table, parameters, new TableDeleteCallback()
        {
            @Override
            public void onCompleted(Exception exception, ServiceFilterResponse response)
            {
                if (exception != null)
                {
                    Log.e(TAG, exception.getCause().getMessage());
                    return;
                }
                //Refetch the tables from the server
                getTables();
            }
        });
    }


    /***
     * Gets all of the rows for a specific table
     * @param tableName
     */
    public void getTableRows(String tableName)
    {
        //Executes a read request with parameters
        //We have to do it in this way to ensure it shows up correctly on the server
        mTableTableRows.execute(mTableTableRows.parameter("table", tableName), new TableJsonQueryCallback() {
            @Override
            public void onCompleted(JsonElement result, int count, Exception exception, ServiceFilterResponse response)
            {
                if (exception != null)
                {
                    Log.e(TAG, exception.getCause().getMessage());
                    return;
                }
                //Loop through the results and add them to our local collection
                JsonArray results = result.getAsJsonArray();
                mTableRows = new ArrayList<JsonElement>();
                for (int i = 0; i < results.size(); i ++)
                {
                    JsonElement item = results.get(i);
                    mTableRows.add(item);
                }
                //Broadcast that table rows have been loaded
                Intent broadcast = new Intent();
                broadcast.setAction("tablerows.loaded");
                mContext.sendBroadcast(broadcast);
            }
        });
    }

    /***
     * Delets an individual table row
     * @param tableName
     * @param partitionKey
     * @param rowKey
     */
    public void deleteTableRow(final String tableName, String partitionKey, String rowKey)
    {
        //Create the json Object we'll send over and fill it with the required
        //id property - otherwise we'll get kicked back
        JsonObject row = new JsonObject();
        row.addProperty("id", 0);
        //Create parameters to pass in the table row details.  We do this with params
        //because it would be stripped out if we put it on the row object
        List<Pair<String,String>> parameters = new ArrayList<Pair<String, String>>();
        parameters.add(new Pair<String, String>("tableName", tableName));
        parameters.add(new Pair<String, String>("rowKey", rowKey));
        parameters.add(new Pair<String, String>("partitionKey", partitionKey));

        mTableTableRows.delete(row, parameters, new TableDeleteCallback()
        {
            @Override
            public void onCompleted(Exception exception, ServiceFilterResponse response)
            {
                if (exception != null)
                {
                    Log.e(TAG, exception.getCause().getMessage());
                    return;
                }
                //Refetch the table rows for the table
                getTableRows(tableName);
            }
        });
    }

    /***
     * Adds a new row to a table
     * @param tableName
     * @param tableRowData
     */
    public void addTableRow(final String tableName, List<Pair<String,String>> tableRowData)
    {
        //Create a new json object with the key value pairs
        JsonObject newRow = new JsonObject();
        for (Pair<String,String> pair : tableRowData)
        {
            newRow.addProperty(pair.first, pair.second);
        }
        //Pass the table name over in parameters
        List<Pair<String,String>> parameters = new ArrayList<Pair<String, String>>();
        parameters.add(new Pair<String, String>("table", tableName));
        mTableTableRows.insert(newRow, parameters, new TableJsonOperationCallback()
        {
            @Override
            public void onCompleted(JsonObject jsonObject, Exception exception, ServiceFilterResponse response)
            {
                if (exception != null)
                {
                    Log.e(TAG, exception.getCause().getMessage());
                    return;
                }
                //Refetch the table rows from the server
                getTableRows(tableName);
            }
        });
    }

    /***
     * Updates an existing table row
     * @param tableName
     * @param tableRowData
     */
    public void updateTableRow(final String tableName, List<Pair<String,String>> tableRowData)
    {
        //Create a new json object with the key value pairs
        JsonObject newRow = new JsonObject();
        for (Pair<String,String> pair : tableRowData)
        {
            newRow.addProperty(pair.first, pair.second);
        }
        //Add ID Parameter since it's required on the server side
        newRow.addProperty("id", 1);
        //Pass the table name over in parameters
        List<Pair<String,String>> parameters = new ArrayList<Pair<String, String>>();
        parameters.add(new Pair<String, String>("table", tableName));
        mTableTableRows.update(newRow, parameters, new TableJsonOperationCallback()
        {
            @Override
            public void onCompleted(JsonObject jsonObject, Exception exception, ServiceFilterResponse response)
            {
                if (exception != null) {
                    Log.e(TAG, exception.getCause().getMessage());
                    return;
                }
                //Refetch the table rows
                getTableRows(tableName);
            }
        });
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
     * Get all of the blobs for a container
     * @param containerName
     */
    public void getBlobsForContainer(String containerName)
    {
        //Pass the container name as a parameter
        //We have to do it in this way for it to show up properly on the server
        mTableBlobs.execute(mTableBlobs.parameter("container", containerName), new TableJsonQueryCallback()
        {
            @Override
            public void onCompleted(JsonElement result, int count, Exception exception, ServiceFilterResponse response)
            {
                if (exception != null)
                {
                    Log.e(TAG, exception.getCause().getMessage());
                    return;
                }
                JsonArray results = result.getAsJsonArray();
                //Store a local array of both the JsonElements and the blob names
                mBlobNames = new ArrayList<Map<String, String>>();
                mBlobObjects = new ArrayList<JsonElement>();
                for (int i = 0; i < results.size(); i ++)
                {
                    JsonElement item = results.get(i);
                    mBlobObjects.add(item);
                    Map<String, String> map = new HashMap<String, String>();
                    map.put("BlobName", item.getAsJsonObject().getAsJsonPrimitive("name").getAsString());
                    mBlobNames.add(map);
                }
                //Broadcast that blobs are loaded
                Intent broadcast = new Intent();
                broadcast.setAction("blobs.loaded");
                mContext.sendBroadcast(broadcast);
            }
        });
    }

    /***
     * Handles deleting a blob
     * @param containerName
     * @param blobName
     */
    public void deleteBlob(final String containerName, String blobName) {
        //Create the json Object we'll send over and fill it with the required
        //id property - otherwise we'll get kicked back
        JsonObject blob = new JsonObject();
        blob.addProperty("id", 0);

        //Create parameters to pass in the blob details.  We do this with params
        //because it would be stripped out if we put it on the blob object
        List<Pair<String,String>> parameters = new ArrayList<Pair<String, String>>();

        parameters.add(new Pair<String, String>("containerName", containerName));
        parameters.add(new Pair<String, String>("blobName", blobName));

        mTableBlobs.delete(blob, parameters, new TableDeleteCallback()
        {
            @Override
            public void onCompleted(Exception exception, ServiceFilterResponse response)
            {
                if (exception != null)
                {
                    Log.e(TAG, exception.getCause().getMessage());
                    return;
                }

                //Refetch the blobs from the server
//                getBlobsForContainer(containerName);
            }
        });
    }

    /***
     * Gets a SAS URL for an existing blob
     * @param containerName
     * @param blobName
     * NOTE THIS IS DONE AS A SEPARATE METHOD FROM getSasForNewBlob BECAUSE IT
     * BROADCASTS A DIFFERENT ACTION
     */
    public void getBlobSas(String containerName, String blobName)
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
            public void onCompleted(JsonObject jsonObject, Exception exception, ServiceFilterResponse response)
            {
                if (exception != null)
                {
                    Log.e(TAG, exception.getCause().getMessage());
                    return;
                }
                //Set the loaded blob
                mLoadedBlob = jsonObject;
                //Broadcast that the blob is loaded
                Intent broadcast = new Intent();
                broadcast.setAction("blob.loaded");
                mContext.sendBroadcast(broadcast);
            }
        });
    }

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
