package com.w9jds.gallery4glass.Classes;

import android.graphics.Bitmap;
import android.graphics.Color;
 
public class ConvolutionMatrix
{
    public static final int SIZE = 3;
 
    public double[][] Matrix;
    public double Factor = 1;
    public double Offset = 1;
 
    public ConvolutionMatrix(int size) 
    {
        Matrix = new double[size][size];
    }
 
    public void setAll(double value) 
    {
        for (int x = 0; x < SIZE; x++)
        {
            for (int y = 0; y < SIZE; y++)
                Matrix[x][y] = value;
        }
    }
 
    public void applyConfig(double[][] config) 
    {
        for(int x = 0; x < SIZE; x++)
        {
            for(int y = 0; y < SIZE; y++)
                Matrix[x][y] = config[x][y];
        }
    }
 
    public static Bitmap computeConvolution3x3(Bitmap bOriginal, ConvolutionMatrix cmMatrix)
    {
        int nWidth = bOriginal.getWidth();
        int nHeight = bOriginal.getHeight();
        Bitmap bResult = Bitmap.createBitmap(nWidth, nHeight, bOriginal.getConfig());

        int[][] naPixels = new int[SIZE][SIZE];
 
        for (int y = 0; y < nHeight - 2; y++)
        {
            for (int x = 0; x < nWidth - 2; x++)
            {
                int nA, nR, nG, nB;
                int nSumR, nSumG, nSumB;
 
                // get pixel matrix
                for (int i = 0; i < SIZE; i++)
                {
                    for (int j = 0; j < SIZE; j++)
                        naPixels[i][j] = bOriginal.getPixel(x + i, y + j);
                }
 
                // get alpha of center pixel
                nA = Color.alpha(naPixels[1][1]);
 
                // init color sum
                nSumR = nSumG = nSumB = 0;
 
                // get sum of RGB on matrix
                for (int i = 0; i < SIZE; i++)
                {
                    for (int j = 0; j < SIZE; j++)
                    {
                        nSumR += (Color.red(naPixels[i][j]) * cmMatrix.Matrix[i][j]);
                        nSumG += (Color.green(naPixels[i][j]) * cmMatrix.Matrix[i][j]);
                        nSumB += (Color.blue(naPixels[i][j]) * cmMatrix.Matrix[i][j]);
                    }
                }
 
                // get final Red
                nR = (int)(nSumR / cmMatrix.Factor + cmMatrix.Offset);
                if (nR < 0)
                    nR = 0;
                else if (nR > 255)
                    nR = 255;
 
                // get final Green
                nG = (int)(nSumG / cmMatrix.Factor + cmMatrix.Offset);
                if (nG < 0)
                    nG = 0;
                else if (nG > 255)
                    nG = 255;
 
                // get final Blue
                nB = (int)(nSumB / cmMatrix.Factor + cmMatrix.Offset);
                if (nB < 0)
                    nB = 0;
                else if (nB > 255)
                    nB = 255;
 
                // apply new pixel
                bResult.setPixel(x + 1, y + 1, Color.argb(nA, nR, nG, nB));
            }
        }
 
        // final image
        return bResult;
    }
}