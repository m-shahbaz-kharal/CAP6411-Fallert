package com.cap6411.fallert.devices;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.cap6411.fallert.R;

import java.util.ArrayList;

public class AlerteeListAdapter extends ArrayAdapter<AlerteeDevice> {
        private Context mContext;

        public AlerteeListAdapter(Context context, ArrayList<AlerteeDevice> devices) {
            super(context, 0, devices);
            mContext = context;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            AlerteeDevice device = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.alertee_device, parent, false);
            }
            // Lookup view for data population
            TextView mTitle = (TextView) convertView.findViewById(R.id.alertee_device_title);
            TextView mLastIP = (TextView) convertView.findViewById(R.id.alertee_device_last_ip);
            TextView mMACAddress = (TextView) convertView.findViewById(R.id.alertee_device_last_mac);
            ImageView mDelete = (ImageView) convertView.findViewById(R.id.alertee_device_delete);
            // Populate the data into the template view using the data object
            mTitle.setText(device.mTitle);
            mLastIP.setText(device.mLastIP);
            mMACAddress.setText(device.mMACAddress);
            mDelete.setOnClickListener(v -> {
                remove(device);
                notifyDataSetChanged();
            });
            // Return the completed view to render on screen
            return convertView;
        }
    }