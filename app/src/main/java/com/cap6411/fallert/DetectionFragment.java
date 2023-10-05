package com.cap6411.fallert;

import android.content.Context;
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

import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@ExperimentalCamera2Interop public class DetectionFragment extends Fragment {
    private Context mContext;
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
    boolean isRunning = false;

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
    }

    public static DetectionFragment newInstance(String param1, String param2) {
        DetectionFragment fragment = new DetectionFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

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
            } catch (Exception ignored) {}
        }, ContextCompat.getMainExecutor(mContext));
        return inflater.inflate(R.layout.fragment_detection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        previewView = view.findViewById(R.id.previewView);
        previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);

        display = view.findViewById(R.id.displayOverlay);
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
                    canvas.drawCircle(poseLandmark.getPosition().x, poseLandmark.getPosition().y,5,mPaint);
                }

                bitmap4DisplayArrayList.clear();
                bitmap4DisplayArrayList.add(bitmapArrayList.get(0));
                bitmap4Save = bitmapArrayList.get(bitmapArrayList.size()-1);
                bitmapArrayList.clear();
                bitmapArrayList.add(bitmap4Save);
                poseArrayList.clear();
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

    Runnable RunMlkit = () -> poseDetector.process(InputImage.fromBitmap(bitmapArrayList.get(0),0)).addOnSuccessListener(pose -> poseArrayList.add(pose)).addOnFailureListener(e -> {});
}