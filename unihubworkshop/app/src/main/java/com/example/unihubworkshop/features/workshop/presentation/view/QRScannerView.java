package com.example.unihubworkshop.features.workshop.presentation.view;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.unihubworkshop.R;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;
import org.json.JSONException;

public class QRScannerView extends AppCompatActivity {
    private static final int PERMISSION_CODE = 1001;
    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private boolean isProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        previewView = findViewById(R.id.previewView);
        cameraExecutor = Executors.newSingleThreadExecutor();

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_CODE);
        }
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                BarcodeScanner scanner = BarcodeScanning.getClient();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, image -> {
                    @SuppressWarnings("UnsafeOptInUsageError")
                    android.media.Image mediaImage = image.getImage();
                    if (mediaImage != null) {
                        InputImage inputImage = InputImage.fromMediaImage(mediaImage, image.getImageInfo().getRotationDegrees());
                        scanner.process(inputImage)
                                .addOnSuccessListener(barcodes -> {
                                    for (Barcode barcode : barcodes) {
                                        String rawValue = barcode.getRawValue();
                                        if (rawValue != null && !isProcessing) {
                                            isProcessing = true;
                                            runOnUiThread(() -> {
                                                processScannedData(rawValue);
                                            });
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> Log.e("QRScanner", "Barcode scan failed", e))
                                .addOnCompleteListener(task -> image.close());
                    } else {
                        image.close();
                    }
                });

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Log.e("QRScanner", "Error starting camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void processScannedData(String rawData) {
        String currentWorkshopId = getIntent().getStringExtra("workshop_id");
        if (currentWorkshopId == null) {
            Toast.makeText(this, "Error: Current Workshop ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            JSONObject json = new JSONObject(rawData);
            String studentId = json.getString("studentId");
            String qrWorkshopId = json.getString("workshopId");

            // KIỂM TRA: Workshop của QR có đúng là workshop đang chọn không?
            if (!currentWorkshopId.equalsIgnoreCase(qrWorkshopId)) {
                Toast.makeText(this, "Lỗi: Vé này thuộc về workshop khác!", Toast.LENGTH_LONG).show();
                isProcessing = false; // Cho phép quét lại
                return;
            }

            performCheckin(studentId, currentWorkshopId);

        } catch (JSONException e) {
            Toast.makeText(this, "Lỗi: Mã QR không đúng định dạng UniHub!", Toast.LENGTH_SHORT).show();
            isProcessing = false; // Cho phép quét lại
        }
    }

    private void performCheckin(String studentId, String workshopId) {
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(java.time.ZoneId.of("UTC"));
        String timestamp = now.format(java.time.format.DateTimeFormatter.ISO_INSTANT);

        com.example.unihubworkshop.features.workshop.data.datasource.CheckinEvent event = 
            new com.example.unihubworkshop.features.workshop.data.datasource.CheckinEvent(studentId, workshopId, timestamp);
        
        com.example.unihubworkshop.features.workshop.data.datasource.CheckinApi checkinApi = 
            com.example.unihubworkshop.core.network.RetrofitClient.getClient(this).create(com.example.unihubworkshop.features.workshop.data.datasource.CheckinApi.class);

        // Sử dụng checkinSingle để nhận feedback trực tiếp
        checkinApi.checkinSingle(event).enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(QRScannerView.this, "Check-in thành công cho: " + studentId, Toast.LENGTH_LONG).show();
                } else {
                    String errorMsg = "Check-in thất bại!";
                    try {
                        // Cố gắng lấy message từ error body
                        String errorBody = response.errorBody().string();
                        JSONObject errorJson = new JSONObject(errorBody);
                        if (errorJson.has("message")) {
                            errorMsg = errorJson.getString("message");
                        }
                    } catch (Exception e) {
                        errorMsg += " (Code: " + response.code() + ")";
                    }
                    Toast.makeText(QRScannerView.this, errorMsg, Toast.LENGTH_LONG).show();
                }
                finish();
            }

            @Override
            public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                Toast.makeText(QRScannerView.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
