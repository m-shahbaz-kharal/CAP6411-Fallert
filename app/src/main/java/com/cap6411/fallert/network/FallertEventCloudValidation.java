package com.cap6411.fallert.network;

import android.graphics.Bitmap;

public class FallertEventCloudValidation extends FallertEvent{
    private Bitmap[] mFallImages = null;
    private boolean mIsValidated = false;
    private boolean mIsFall = false;

    public FallertEventCloudValidation(String eventTime, Bitmap[] fallImages, boolean isValidated, boolean isFall) {
        super(FallertEventType.CLOUD_VALIDATION, eventTime);
        mFallImages = fallImages;
        mIsValidated = isValidated;
        mIsFall = isFall;
    }
    public Bitmap[] getFallImages() {
        return mFallImages;
    }
    public boolean isValidated() {
        return mIsValidated;
    }
    public boolean isFall() {
        return mIsFall;
    }

    public String toString() {
        StringBuilder fallImagesString = new StringBuilder();
        for (int i = 0; i < mFallImages.length; i++) {
            fallImagesString.append(StringNetwork.bitmapToString(mFallImages[i]));
            if (i != mFallImages.length - 1) fallImagesString.append(":");
        }
        return super.toString() + ":" + fallImagesString.toString();
    }
    public static FallertEventCloudValidation parse(String eventString) {
        try {
            String[] eventStringArray = eventString.split(":");
            String type = eventStringArray[0];
            String time = eventStringArray[1];
            Bitmap image = StringNetwork.stringToBitmap(eventStringArray[2]);
            boolean isValidated = true;
            boolean isFall = Boolean.parseBoolean(eventStringArray[3]);
            return new FallertEventCloudValidation(time, new Bitmap[]{image}, isValidated, isFall);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
