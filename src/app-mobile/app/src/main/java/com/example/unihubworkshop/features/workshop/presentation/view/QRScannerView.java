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
import com.example.unihubworkshop.core.network.RetrofitClient;
import com.example.unihubworkshop.features.workshop.data.datasource.RegistrationApi;
import com.example.unihubworkshop.features.workshop.data.datasource.RegistrationDetailResponseDto;
import com.example.unihubworkshop.features.workshop.data.local.db.AppDatabase;
import com.example.unihubworkshop.features.workshop.data.local.entity.RegistrationEntity;
import com.example.unihubworkshop.features.workshop.data.sync.CheckinSyncWorker;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.UUID;
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

        CheckinSyncWorker.enqueue(this);

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

        String registrationId = extractRegistrationId(rawData);
        if (registrationId == null) {
            Toast.makeText(this, "Invalid QR Code.", Toast.LENGTH_SHORT).show();
            isProcessing = false;
            return;
        }

        // --- OFFLINE FIRST LOGIC ---
        Executors.newSingleThreadExecutor().execute(() -> {
            com.example.unihubworkshop.features.workshop.domain.repository.WorkshopRepository repo = 
                new com.example.unihubworkshop.features.workshop.data.repository.WorkshopRepositoryImpl(QRScannerView.this);
                
            com.example.unihubworkshop.features.workshop.domain.repository.WorkshopRepository.CheckinResult result = 
                repo.verifyOfflineCheckinDetailed(registrationId, currentWorkshopId);

            if (result == com.example.unihubworkshop.features.workshop.domain.repository.WorkshopRepository.CheckinResult.INVALID_TICKET) {
                result = fetchAndCacheRegistrationThenCheckIn(registrationId, currentWorkshopId, repo);
            }
            final com.example.unihubworkshop.features.workshop.domain.repository.WorkshopRepository.CheckinResult finalResult = result;
            
            runOnUiThread(() -> {
                if (finalResult == com.example.unihubworkshop.features.workshop.domain.repository.WorkshopRepository.CheckinResult.SUCCESS) {
                    Toast.makeText(QRScannerView.this, "Offline check-in recorded: " + registrationId, Toast.LENGTH_LONG).show();
                    CheckinSyncWorker.enqueue(QRScannerView.this);
                    finish();
                } else if (finalResult == com.example.unihubworkshop.features.workshop.domain.repository.WorkshopRepository.CheckinResult.ALREADY_CHECKED_IN) {
                    Toast.makeText(QRScannerView.this, "Ticket already checked in.", Toast.LENGTH_LONG).show();
                    isProcessing = false;
                } else {
                    Toast.makeText(QRScannerView.this, "Invalid ticket.", Toast.LENGTH_LONG).show();
                    isProcessing = false;
                }
            });
        });
    }

    private com.example.unihubworkshop.features.workshop.domain.repository.WorkshopRepository.CheckinResult fetchAndCacheRegistrationThenCheckIn(
            String registrationId,
            String currentWorkshopId,
            com.example.unihubworkshop.features.workshop.domain.repository.WorkshopRepository repo) {
        try {
            RegistrationApi registrationApi = RetrofitClient.getClient(this).create(RegistrationApi.class);
            retrofit2.Response<RegistrationDetailResponseDto> response = registrationApi.getRegistrationById(registrationId).execute();
            if (!response.isSuccessful() || response.body() == null) {
                return com.example.unihubworkshop.features.workshop.domain.repository.WorkshopRepository.CheckinResult.INVALID_TICKET;
            }

            RegistrationDetailResponseDto registration = response.body();
            String ticketWorkshopId = registration.getWorkshop() == null ? null : registration.getWorkshop().getId();
            if (ticketWorkshopId == null || !currentWorkshopId.equalsIgnoreCase(ticketWorkshopId)) {
                return com.example.unihubworkshop.features.workshop.domain.repository.WorkshopRepository.CheckinResult.INVALID_TICKET;
            }

            if (!"SUCCESS".equalsIgnoreCase(registration.getStatus())
                    && !"CHECKED_IN".equalsIgnoreCase(registration.getStatus())) {
                return com.example.unihubworkshop.features.workshop.domain.repository.WorkshopRepository.CheckinResult.INVALID_TICKET;
            }

            AppDatabase.getInstance(this).registrationDao().insertAll(java.util.Collections.singletonList(
                    new RegistrationEntity(
                            registration.getId(),
                            currentWorkshopId,
                            registration.getStudentId(),
                            registration.getStudentName(),
                            registration.getQrCode() == null ? registration.getId() : registration.getQrCode(),
                            registration.getStatus())));

            return repo.verifyOfflineCheckinDetailed(registrationId, currentWorkshopId);
        } catch (Exception e) {
            Log.e("QRScanner", "Online QR fallback failed", e);
            return com.example.unihubworkshop.features.workshop.domain.repository.WorkshopRepository.CheckinResult.INVALID_TICKET;
        }
    }

    private String extractRegistrationId(String rawData) {
        String value = rawData == null ? "" : rawData.trim();
        if (isUuid(value)) {
            return value;
        }
        try {
            JSONObject json = new JSONObject(value);
            String registrationId = json.optString("registrationId", "");
            if (isUuid(registrationId)) {
                return registrationId;
            }
            String qrCode = json.optString("qrCode", "");
            if (isUuid(qrCode)) {
                return qrCode;
            }
        } catch (JSONException ignored) {
        }
        return null;
    }

    private boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
