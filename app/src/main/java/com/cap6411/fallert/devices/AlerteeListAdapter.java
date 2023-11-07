package com.cap6411.fallert.devices;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.cap6411.fallert.R;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

public class AlerteeListAdapter extends ArrayAdapter<AlerteeDevice> {
        private Context mContext = null;
        private String mServerIP = null;
        private Consumer<Pair<String, String>> mOnDeviceDelete = null;

        public AlerteeListAdapter(Context context, ArrayList<AlerteeDevice> devices, String serverIP, Consumer<Pair<String, String>> onDeviceDelete) {
            super(context, 0, devices);
            mContext = context;
            mServerIP = serverIP;
            mOnDeviceDelete = onDeviceDelete;
        }

        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            // Get the data item for this position
            AlerteeDevice device = getItem(position);
            if (device == null) return convertView;
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.alertee_device, parent, false);
            }
            // Lookup view for data population
            TextView mTitle = (TextView) convertView.findViewById(R.id.alertee_device_title);
            TextView mLastIP = (TextView) convertView.findViewById(R.id.alertee_device_last_ip);
            ImageView mDelete = (ImageView) convertView.findViewById(R.id.alertee_device_delete);
            // Populate the data into the template view using the data object
            mTitle.setText(device.mTitle);
            mLastIP.setText(device.mLastIP);
            mDelete.setOnClickListener(v -> {
                remove(device);
                mOnDeviceDelete.accept(new Pair<>(device.mLastIP, mServerIP));
                notifyDataSetChanged();
            });
            // Return the completed view to render on screen
            return convertView;
        }
    }