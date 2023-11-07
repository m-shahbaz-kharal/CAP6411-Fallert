package com.cap6411.fallert;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;

import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cap6411.fallert.devices.AlerteeDevices;
import com.cap6411.fallert.network.FallertEvent;
import com.cap6411.fallert.network.FallertEventFall;
import com.cap6411.fallert.network.FallertInformationEvent;
import com.cap6411.fallert.network.FallertNetworkService;
import com.cap6411.fallert.network.FallertRemoveDeviceEvent;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

import net.glxn.qrgen.android.QRCode;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

@ExperimentalCamera2Interop
public class DetectionFragment extends Fragment implements View.OnClickListener{
    private Context mContext;

    // PoseDetection elements
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    PreviewView previewView;
    PoseDetectorOptions options = new PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.STREAM_MODE).build();
    PoseDetector poseDetector = PoseDetection.getClient(options);
    Canvas canvas;
    Paint mPaint = new Paint();
    Display display;
    Bitmap bitmap4Save;
    ArrayList<Bitmap> bitmapArrayList = new ArrayList<>();
    ArrayList<Bitmap> bitmap4DisplayArrayList = new ArrayList<>();
    ArrayList<Pose> poseArrayList = new ArrayList<>();
    ArrayList<FallertDetection> fallertDetectionArrayList = new ArrayList<>();
    boolean isRunning = false;

    // UI elements
    private Button mSettingsButton;
    private LinearLayout mSettingsRootView;
    private SharedPreferences sharedPreferences;
    private EditText mSettingsServerTitle;
    private TextView mSettingsServerIPAddress;
    private ImageView mQRCode;
    private Button mSettingsSave;

    // Network elements
    private FallertNetworkService mFallertNetworkService;
    private Thread mRecievedFallertEventHandler;
    private AlerteeDevices mAlerteeDevices;
    public DetectionFragment() {}

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        sharedPreferences = mContext.getSharedPreferences("com.cap6411.fallert", Context.MODE_PRIVATE);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("server_title", mSettingsServerTitle.getText().toString()).apply();
        editor.putString("client_devices", mAlerteeDevices.toString()).apply();
        mFallertNetworkService.stopServerThread();
        mAlerteeDevices = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cameraProviderFuture = ProcessCameraProvider.getInstance(mContext);

        mPaint.setColor(Color.RED);
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaint.setStrokeWidth(1);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            }
            catch (Exception ignored) {}
        }, ContextCompat.getMainExecutor(mContext));
        return inflater.inflate(R.layout.fragment_detection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String serverIPAddress = getServerIPAddress();
        mFallertNetworkService = new FallertNetworkService(getContext());

        mAlerteeDevices = new AlerteeDevices(mContext, view.findViewById(R.id.alertee_devices_list), serverIPAddress, mFallertNetworkService::removeClient);
        mAlerteeDevices.parse(sharedPreferences.getString("client_devices", null));

        previewView = view.findViewById(R.id.previewView);
        previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);
        display = view.findViewById(R.id.displayOverlay);

        mSettingsButton = view.findViewById(R.id.settings_button);
        mSettingsButton.setOnClickListener(this);

        mSettingsRootView = view.findViewById(R.id.settings_root_view);
        mSettingsServerTitle = view.findViewById(R.id.settings_server_title);
        mSettingsServerTitle.setText(sharedPreferences.getString("server_title", "Room 01"));
        mSettingsServerIPAddress = view.findViewById(R.id.settings_server_ip_address);
        mSettingsServerIPAddress.setText(serverIPAddress);
        Bitmap qr_code = QRCode.from(serverIPAddress).bitmap();
        mQRCode = view.findViewById(R.id.qr_code);
        mQRCode.setImageBitmap(qr_code);
        mSettingsSave = view.findViewById(R.id.settings_save);
        mSettingsSave.setOnClickListener(this);

        mSettingsRootView.setVisibility(View.VISIBLE);

        startReceivedFallertEventHandler();
    }

    private void startReceivedFallertEventHandler() {
        mRecievedFallertEventHandler = new Thread(() -> {
            while (true) {
                if (mFallertNetworkService == null) continue;
                if (FallertNetworkService.mServerRecvFallertEventQueue.size() == 0) continue;
                FallertEvent event = FallertNetworkService.mServerRecvFallertEventQueue.poll();
                if (event == null) continue;
                switch (event.getEventType()) {
                    case INFORMATION:
                        FallertInformationEvent infoEvent = (FallertInformationEvent) event;
                        new Handler(mContext.getMainLooper()).post(() -> {
                            mAlerteeDevices.updateDevice(infoEvent.getInformation(), infoEvent.getIPAddress());
                        });
                        break;
                    case REMOVE_DEVICE:
                        FallertRemoveDeviceEvent removeEvent = (FallertRemoveDeviceEvent) event;
                        new Handler(mContext.getMainLooper()).post(() -> {
                            mAlerteeDevices.removeDevice(removeEvent.getIPAddress());
                        });
                        break;
                }
            }
        });
        mRecievedFallertEventHandler.start();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.settings_button) {
            mSettingsRootView.setVisibility(View.VISIBLE);
            mSettingsServerTitle.setEnabled(true);
            mSettingsSave.setText("Start");
            mFallertNetworkService.stopServerThread();
            mRecievedFallertEventHandler.interrupt();
        }
        else if(v.getId() == R.id.settings_save){
            SharedPreferences.Editor editor = sharedPreferences.edit();

            if (mSettingsSave.getText().equals("Start")) {
                mSettingsServerTitle.setEnabled(false);
                mSettingsSave.setText("Save");
                String serverIPAddress = getServerIPAddress();
                mFallertNetworkService.startServerThread(serverIPAddress, mSettingsServerTitle.getText().toString(), mAlerteeDevices);
                startReceivedFallertEventHandler();
            }
            else if(mSettingsSave.getText().equals("Save")) {
                mSettingsRootView.setVisibility(View.INVISIBLE);
                editor.putString("server_title", mSettingsServerTitle.getText().toString()).apply();
                editor.putString("client_devices", mAlerteeDevices.toString()).apply();
            }
        }
    }

    public String getServerIPAddress() {
        try {
            String ip = "";
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface networkInterface = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = networkInterface.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        ip = inetAddress.getHostAddress();
                    }
                }
            }
            return ip;
        } catch (SocketException ex) {
            Log.e("IP", ex.toString());
        }
        return null;
    }

    @androidx.camera.camera2.interop.ExperimentalCamera2Interop
    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().setTargetResolution(new Size(640, 360)).build();

        List<CameraInfo> allCameraInfos = cameraProvider.getAvailableCameraInfos();
        String ourCamID = null;
        CameraSelector cameraSelector;
        for(CameraInfo camInfo: allCameraInfos) {
            Camera2CameraInfo cam2CamInfo = Camera2CameraInfo.from(camInfo);
            if (cam2CamInfo.getCameraCharacteristic(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL)
            {
                ourCamID = cam2CamInfo.getCameraId();
                break;
            }
        }

        if (ourCamID != null)
        {
            String finalOurCamID = ourCamID;
            cameraSelector = new CameraSelector.Builder().addCameraFilter(cameraInfos -> {
                List<CameraInfo> valids = new ArrayList<>();
                for(CameraInfo camInfo:cameraInfos)
                {
                    Camera2CameraInfo cam2CamInfo = Camera2CameraInfo.from((camInfo));
                    if(cam2CamInfo.getCameraId().equals(finalOurCamID))
                    {
                        valids.add((camInfo));
                        break;
                    }
                }
                return valids;
            }).build();
        }
        else
        {
            cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        }

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis =
                new ImageAnalysis
                        .Builder()
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .setTargetResolution(new Size(640, 360))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

        imageAnalysis.setAnalyzer(ActivityCompat.getMainExecutor(mContext), imageProxy -> {
            Matrix matrix = imageProxy.getImageInfo().getSensorToBufferTransformMatrix();
            ByteBuffer byteBuffer = imageProxy.getImage().getPlanes()[0].getBuffer();
            byteBuffer.rewind();
            Bitmap bitmap = Bitmap.createBitmap(imageProxy.getWidth(), imageProxy.getHeight(), Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(byteBuffer);
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap,0,0,imageProxy.getWidth(), imageProxy.getHeight(),matrix,false);

            bitmapArrayList.add(rotatedBitmap);

            if (poseArrayList.size() >= 1) {
                canvas = new Canvas(bitmapArrayList.get(0));

                for (PoseLandmark poseLandmark : poseArrayList.get(0).getAllPoseLandmarks()) {
                    canvas.drawCircle(poseLandmark.getPosition().x, poseLandmark.getPosition().y,2,mPaint);
                }
                if(fallertDetectionArrayList.get(0).hasPose) {
                    if (fallertDetectionArrayList.get(0).hasFall) {
                        mPaint.setColor(Color.RED);
                        String time = String.valueOf(System.currentTimeMillis());
                        FallertEventFall event = new FallertEventFall(time, sharedPreferences.getString("server_title", "Room 01"), "A person (or multiple) has fallen. Please check.", bitmapArrayList.get(0));
                        FallertNetworkService.mServerSendFallertEventQueue.add(event);
                    } else {
                        mPaint.setColor(Color.GREEN);
                    }
                    mPaint.setStyle(Paint.Style.STROKE);
                    mPaint.setStrokeWidth(2);
                    canvas.drawRect(fallertDetectionArrayList.get(0).minX, fallertDetectionArrayList.get(0).minY, fallertDetectionArrayList.get(0).maxX, fallertDetectionArrayList.get(0).maxY, mPaint);
                }

                bitmap4DisplayArrayList.clear();
                bitmap4DisplayArrayList.add(bitmapArrayList.get(0));
                bitmap4Save = bitmapArrayList.get(bitmapArrayList.size()-1);
                bitmapArrayList.clear();
                bitmapArrayList.add(bitmap4Save);
                poseArrayList.clear();
                fallertDetectionArrayList.clear();
                isRunning = false;

            }

            poseArrayList.size();
            if (bitmapArrayList.size() >= 1 && !isRunning) {
                RunMlkit.run();
                isRunning = true;
            }

            if (bitmap4DisplayArrayList.size() >= 1) {
                display.getBitmap(bitmap4DisplayArrayList.get(0));
            }

            imageProxy.close();
        });

        cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, imageAnalysis, preview);
    }

    public class FallertDetection {
        boolean hasPose, hasFall;
        float minX, maxX, minY, maxY;
        Pose pose;
        // constructor
        public FallertDetection(boolean hasPose, boolean hasFall, float minX, float minY, float maxX, float maxY, Pose pose){
            this.hasPose = hasPose;
            this.hasFall = hasFall;
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.pose = pose;
        }
    }

    private FallertDetection detectFallertEvents(Pose pose) {
        List<PoseLandmark> landmarks = pose.getAllPoseLandmarks();

        boolean hasPose = pose.getAllPoseLandmarks().size() > 0;

        float xmin = 1000000f;
        float xmax = -1f;
        float ymin = 1000000f;
        float ymax = -1f;

        if(hasPose) {
            for (PoseLandmark lm : landmarks) {
                if (lm.getPosition().x < xmin) xmin = lm.getPosition().x;
                if (lm.getPosition().x > xmax) xmax = lm.getPosition().x;
                if (lm.getPosition().y < ymin) ymin = lm.getPosition().y;
                if (lm.getPosition().y > ymax) ymax = lm.getPosition().y;
            }
            float left_shoulder_y = landmarks.get(11).getPosition().y;
            float left_shoulder_x = landmarks.get(11).getPosition().x;
            float right_shoulder_y = landmarks.get(12).getPosition().y;
            float left_body_y = landmarks.get(23).getPosition().y;
            float left_body_x = landmarks.get(23).getPosition().x;
            float right_body_y = landmarks.get(24).getPosition().y;
            double len_factor = Math.sqrt((Math.pow((left_shoulder_y - left_body_y), 2) + Math.pow((left_shoulder_x - left_body_x), 2)));
            float left_foot_y = landmarks.get(27).getPosition().y;
            float right_foot_y = landmarks.get(28).getPosition().y;
            float dx = xmax - xmin;
            float dy = ymax - ymin;
            float difference = dy - dx;

            boolean hasFall = (left_shoulder_y > left_foot_y - len_factor
                    && left_body_y > left_foot_y - (len_factor / 2)
                    && left_shoulder_y > left_body_y - (len_factor / 2)
                    || (right_shoulder_y > right_foot_y - len_factor && right_body_y > right_foot_y - (len_factor / 2) && right_shoulder_y > right_body_y - (len_factor / 2))
                    || difference < 0);
            return new FallertDetection(true, hasFall, xmin, ymin, xmax, ymax, pose);
        }

        return new FallertDetection(false, false, xmin, ymin, xmax, ymax, pose);
    }

    Runnable RunMlkit = () -> {
        poseDetector.process(InputImage.fromBitmap(bitmapArrayList.get(0),0)).addOnSuccessListener(pose -> {
            poseArrayList.add(pose);
            fallertDetectionArrayList.add(detectFallertEvents(pose));
        }).addOnFailureListener(e -> {});
    };
}