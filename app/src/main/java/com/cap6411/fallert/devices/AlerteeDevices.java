package com.cap6411.fallert.devices;

import android.content.Context;
import android.widget.ListView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class AlerteeDevices {
    private List<AlerteeDevice> mDevices;
    private AlerteeListAdapter mAdapter;

    public AlerteeDevices(Context context, ListView listView, Consumer<String> onDeviceDelete) {
        mDevices = new ArrayList<>();
        mAdapter = new AlerteeListAdapter(context, (ArrayList<AlerteeDevice>) mDevices, onDeviceDelete);
        listView.setAdapter(mAdapter);
    }

    public void addDevice(String title, String lastIP) {
        AlerteeDevice device = new AlerteeDevice();
        device.mTitle = title;
        device.mLastIP = lastIP;
        mDevices.add(device);
        mAdapter.notifyDataSetChanged();
    }

    public void updateDevice(String title, String lastIP) {
        for (AlerteeDevice device : mDevices) {
            if (device.mLastIP.equals(lastIP)) {
                device.mTitle = title;
                break;
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    public void removeDevice(String ipAddress) {
        for (AlerteeDevice device : mDevices) {
            if (device.mLastIP.equals(ipAddress)) {
                mDevices.remove(device);
                break;
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    public void _parse(String comma_divided_device_string) {
        if(comma_divided_device_string == null || comma_divided_device_string.equals("")) return;
        String[] deviceInfo = comma_divided_device_string.split(",");
        addDevice(deviceInfo[0], deviceInfo[1]);
    }

    public void parse(String bar_divided_client_devices_string) {
        if(bar_divided_client_devices_string == null || bar_divided_client_devices_string.equals("")) return;
        String[] deviceInfo = bar_divided_client_devices_string.split("\\|");
        for (String device : deviceInfo) {
            _parse(device);
        }
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder deviceString = new StringBuilder();
        for (AlerteeDevice device : mDevices) {
            deviceString.append(device.mTitle).append(",").append(device.mLastIP).append("|");
        }
        return deviceString.toString();
    }
}