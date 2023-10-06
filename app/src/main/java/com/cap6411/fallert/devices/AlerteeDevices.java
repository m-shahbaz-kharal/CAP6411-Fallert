package com.cap6411.fallert.devices;

import android.content.Context;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class AlerteeDevices {
    private List<AlerteeDevice> mDevices;
    private AlerteeListAdapter mAdapter;

    public AlerteeDevices(Context context, ListView listView) {
        mDevices = new ArrayList<AlerteeDevice>();
        mAdapter = new AlerteeListAdapter(context, (ArrayList<AlerteeDevice>) mDevices);
        listView.setAdapter(mAdapter);
    }

    public void addDevice(String title, String lastIP, String macAddress) {
        AlerteeDevice device = new AlerteeDevice();
        device.mTitle = title;
        device.mLastIP = lastIP;
        device.mMACAddress = macAddress;
        mDevices.add(device);
        mAdapter.notifyDataSetChanged();
    }

    public void removeDevice(String macAddress) {
        for (AlerteeDevice device : mDevices) {
            if (device.mMACAddress.equals(macAddress)) {
                mDevices.remove(device);
                break;
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    public void _addViaString(String deviceString) {
        String[] deviceInfo = deviceString.split(",");
        addDevice(deviceInfo[0], deviceInfo[1], deviceInfo[2]);
    }

    public void addViaBarDividedString(String deviceString) {
        if(deviceString == null) return;
        String[] deviceInfo = deviceString.split("\\|");
        for (String device : deviceInfo) {
            _addViaString(device);
        }
    }

    public String getBarDividedString() {
        String deviceString = "";
        for (AlerteeDevice device : mDevices) {
            deviceString += device.mTitle + "," + device.mLastIP + "," + device.mMACAddress + "|";
        }
        return deviceString;
    }
}