package com.cap6411.fallert;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.fragment.app.Fragment;

import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.cap6411.fallert.devices.AlerteeDevices;

import net.glxn.qrgen.android.QRCode;

@ExperimentalCamera2Interop
public class SettingsFragment extends Fragment implements View.OnClickListener{
    private SharedPreferences sharedPreferences;
    private String barDividedIPString;
    private Context mContext;
    private TextView mIPAddress;
    private ImageView mQRCode;
    private Button mSettingsOK;

    private AlerteeDevices mAlerteeDevices;
    public SettingsFragment() {}

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        sharedPreferences = mContext.getSharedPreferences("com.cap6411.fallert", Context.MODE_PRIVATE);
        barDividedIPString = sharedPreferences.getString("barDividedIPString", null);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("barDividedIPString", mAlerteeDevices.getBarDividedString()).apply();
        mAlerteeDevices = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        WifiManager wm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        mIPAddress = view.findViewById(R.id.ip_address);
        mIPAddress.setText(ip);
        Bitmap qr_code = QRCode.from(ip).bitmap();
        mQRCode = view.findViewById(R.id.qr_code);
        mQRCode.setImageBitmap(qr_code);
        mSettingsOK = view.findViewById(R.id.settings_ok);
        mSettingsOK.setOnClickListener(this);

        mAlerteeDevices = new AlerteeDevices(mContext, view.findViewById(R.id.alertee_devices_list));
        mAlerteeDevices.addViaBarDividedString(barDividedIPString);
        mAlerteeDevices.addDevice("Alertee 1", "192.168.1.0", "00:00:00:00:00:00");
        mAlerteeDevices.addDevice("Alertee 1", "192.168.1.1", "00:00:00:00:00:00");
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.settings_ok) {
            FragmentChangeListener fc = (FragmentChangeListener)getActivity();
            fc.replaceFragment(new DetectionFragment());
        }
    }
}