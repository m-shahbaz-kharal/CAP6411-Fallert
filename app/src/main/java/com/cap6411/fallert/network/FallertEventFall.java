package com.cap6411.fallert.network;

import android.graphics.Bitmap;

public class FallertEventFall extends FallertEvent{
    private String mTitle = "";
    private String mDescription = "";
    private Bitmap mFallImage = null;

    public FallertEventFall(String eventTime, String title, String description, Bitmap fallImage) {
        super(FallertEventType.FALL, eventTime);
        mTitle = title;
        mDescription = description;
        mFallImage = fallImage;
    }
    public String getTitle() {
        return mTitle;
    }
    public String getDescription() {
        return mDescription;
    }
    public Bitmap getFallImage() {
        return mFallImage;
    }
    public String toString() {
        return super.toString() + ":" + mTitle + ":" + mDescription + ":" + StringNetwork.bitmapToString(mFallImage);
    }
    public static FallertEventFall parse(String eventString) {
        String[] eventStringArray = eventString.split(":");
        if (eventStringArray.length != 5) return null;
        return new FallertEventFall(eventStringArray[1], eventStringArray[2], eventStringArray[3], StringNetwork.stringToBitmap(eventStringArray[4]));
    }
}
