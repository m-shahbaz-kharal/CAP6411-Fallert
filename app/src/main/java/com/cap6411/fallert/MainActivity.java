package com.cap6411.fallert;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Bundle;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

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

@ExperimentalGetImage @ExperimentalCamera2Interop
public class MainActivity extends AppCompatActivity {

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    int PERMISSION_REQUESTS = 1;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        previewView = findViewById(R.id.previewView);
        previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);

        display = findViewById(R.id.displayOverlay);

        mPaint.setColor(Color.RED);
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaint.setStrokeWidth(1);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (Exception ignored) {}
        },ContextCompat.getMainExecutor(this));

        if (!allPermissionsGranted()) {
            getRuntimePermissions();
        }
    }

    Runnable RunMlkit = () -> poseDetector.process(InputImage.fromBitmap(bitmapArrayList.get(0),0)).addOnSuccessListener(pose -> poseArrayList.add(pose)).addOnFailureListener(e -> {});
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

        imageAnalysis.setAnalyzer(ActivityCompat.getMainExecutor(this), imageProxy -> {
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


    private String[] getRequiredPermissions() {
        try {
            PackageInfo info =
                    this.getPackageManager()
                            .getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) {
                return ps;
            } else {
                return new String[0];
            }
        } catch (Exception e) {
            return new String[0];
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (isPermissionGranted(this, permission)) return false;
        }
        return true;
    }

    private static boolean isPermissionGranted(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED;
    }

    private void getRuntimePermissions() {
        List<String> allNeededPermissions = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            if (isPermissionGranted(this, permission)) {
                allNeededPermissions.add(permission);
            }
        }

        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
        }
    }
}