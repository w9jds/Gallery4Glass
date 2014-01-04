package com.w9jds.glassshare.Classes;

import android.hardware.Camera;

public class Size
{
    public final int height;
    public final int width;

    public Size(int paramInt1, int paramInt2)
    {
        this.width = paramInt1;
        this.height = paramInt2;
    }

    public Size(Camera.Size paramSize)
    {
    this(paramSize.width, paramSize.height);
    }

    public Size(Size paramSize)
    {
    this(paramSize.width, paramSize.height);
    }

    public boolean equals(Object paramObject)
    {
        if (!(paramObject instanceof Size));
        Size localSize;
        do
        {
//            return false;
            localSize = (Size)paramObject;
        }
        while ((this.width != localSize.width) || (this.height != localSize.height));
        return true;
    }

    public int hashCode()
    {
    return 32713 * this.width + this.height;
    }

    public String toString()
    {
    return this.width + "x" + this.height;
    }
}