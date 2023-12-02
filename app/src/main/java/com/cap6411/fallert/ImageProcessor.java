package com.cap6411.fallert;

import android.graphics.Bitmap;

import java.nio.FloatBuffer;

public class ImageProcessor {

    private static final int DIM_BATCH_SIZE = 1;
    private static final int DIM_PIXEL_SIZE = 3;
    private static final int IMAGE_SIZE_X = 640;
    private static final int IMAGE_SIZE_Y = 640;

    public static FloatBuffer preProcess(Bitmap bitmap) {
        int bufferSize = DIM_BATCH_SIZE * DIM_PIXEL_SIZE * IMAGE_SIZE_X * IMAGE_SIZE_Y;
        FloatBuffer imgData = FloatBuffer.allocate(bufferSize);

        int stride = IMAGE_SIZE_X * IMAGE_SIZE_Y;
        int[] bmpData = new int[stride];
        bitmap.getPixels(bmpData, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (int i = 0; i < IMAGE_SIZE_X; i++) {
            for (int j = 0; j < IMAGE_SIZE_Y; j++) {
                int idx = IMAGE_SIZE_Y * i + j;
                int pixelValue = bmpData[idx];

                imgData.put(idx, ((float) ((pixelValue >> 16) & 0xFF) / 255f - 0.485f) / 0.229f);
                imgData.put(idx + stride, ((float) ((pixelValue >> 8) & 0xFF) / 255f - 0.456f) / 0.224f);
                imgData.put(idx + stride * 2, ((float) (pixelValue & 0xFF) / 255f - 0.406f) / 0.225f);
            }
        }

        imgData.rewind();
        return imgData;
    }
}