package com.w9jds.glassshare.Classses;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by w9jds on 12/9/13.
 */
public class ConnectedThread extends Thread
{
    private final BluetoothSocket mmSocket;
    private final OutputStream mmOutStream;

    public ConnectedThread(BluetoothSocket socket)
    {
        mmSocket = socket;

        OutputStream tmpOut = null;

        // Get the input and output streams, using temp objects because
        // member streams are final
        try
        {
            tmpOut = socket.getOutputStream();
        }

        catch (IOException e) { }

        mmOutStream = tmpOut;
    }

    /* Call this from the main activity to send data to the remote device */
    public void write(byte[] bytes)
    {
        try
        {
            mmOutStream.write(bytes);
        }

        catch (IOException e) { }
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel()
    {

        try
        {
            mmSocket.close();
        }

        catch (IOException e) { }
    }
}
