package com.cap6411.fallert.network;

public class FallertEvent {
    public enum FallertEventType {
        FALL,
        INFORMATION,
        REMOVE_DEVICE
    }
    private FallertEventType mEventType;
    private String mEventTime;
    public FallertEvent(FallertEventType eventType, String eventTime) {
        mEventType = eventType;
        mEventTime = eventTime;
    }
    public FallertEventType getEventType() {
        return mEventType;
    }
    public String getEventTime() {
        return mEventTime;
    }
    public String toString() {
        return mEventType.toString() + ":" + mEventTime;
    }
}
