package com.example.unihubworkshop.features.workshop.presentation.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.unihubworkshop.R;
import com.example.unihubworkshop.features.workshop.data.repository.WorkshopRepositoryImpl;
import com.example.unihubworkshop.features.workshop.domain.usecase.GetWorkshopDetailUseCase;
import com.example.unihubworkshop.features.workshop.domain.usecase.GetWorkshopsUseCase;
import com.example.unihubworkshop.features.workshop.presentation.viewmodel.WorkshopViewModel;

public class WorkshopDetailView extends AppCompatActivity {
    private WorkshopViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workshop_detail);

        // Manual DI
        WorkshopRepositoryImpl repo = new WorkshopRepositoryImpl(this);
        viewModel = new WorkshopViewModel(repo, new GetWorkshopsUseCase(repo), new GetWorkshopDetailUseCase(repo));

        String workshopId = getIntent().getStringExtra("workshop_id");
        if (workshopId != null) {
            boolean isRegistered = getIntent().getBooleanExtra("is_registered", false);
            String registrationId = getIntent().getStringExtra("registration_id");
            if (isRegistered) {
                viewModel.updateRegistrationStatus(workshopId, true, registrationId);
            }
            viewModel.selectWorkshop(workshopId);
        }

        setupListeners();
        observeViewModel();
    }

    private void setupListeners() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        View btnScan = findViewById(R.id.scanContainer);
        View btnViewList = findViewById(R.id.btnViewList);
        
        android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String role = prefs.getString("userRole", "");
        
        if ("CHECKIN_STAFF".equals(role) || "STAFF".equals(role) || "ORGANIZER".equals(role)) {
            if (btnScan != null) btnScan.setVisibility(View.VISIBLE);
            View registerContainer = findViewById(R.id.registerContainer);
            if (registerContainer != null) registerContainer.setVisibility(View.GONE);
            
            if (btnScan != null) {
                btnScan.setOnClickListener(v -> {
                    Intent intent = new Intent(this, QRScannerView.class);
                    intent.putExtra("workshop_id", getIntent().getStringExtra("workshop_id"));
                    startActivity(intent);
                });
            }

            if (btnViewList != null) {
                btnViewList.setVisibility(View.VISIBLE);
                btnViewList.setOnClickListener(v -> {
                    Intent intent = new Intent(this, RegisteredStudentsView.class);
                    intent.putExtra("workshop_id", getIntent().getStringExtra("workshop_id"));
                    startActivity(intent);
                });
            }
        } else if ("STUDENT".equals(role)) {
            if (btnScan != null) btnScan.setVisibility(View.GONE);
            if (btnViewList != null) btnViewList.setVisibility(View.GONE);
            
            View btnRegister = findViewById(R.id.registerContainer);
            if (btnRegister != null) {
                btnRegister.setVisibility(View.VISIBLE);
                btnRegister.setOnClickListener(v -> {
                    String id = getIntent().getStringExtra("workshop_id");
                    if (id != null) {
                        TextView tvRegisterText = findViewById(R.id.tvRegisterText);
                        if (tvRegisterText != null) tvRegisterText.setText("Registering...");
                        btnRegister.setEnabled(false);
                        
                        viewModel.registerForWorkshop(id, success -> {
                            runOnUiThread(() -> {
                                if (success) {
                                    if (tvRegisterText != null) tvRegisterText.setText("Registered");
                                    btnRegister.setBackgroundResource(R.drawable.bg_button_secondary);
                                } else {
                                    if (tvRegisterText != null) tvRegisterText.setText("Register");
                                    btnRegister.setEnabled(true);
                                    android.widget.Toast.makeText(WorkshopDetailView.this, "Registration failed", android.widget.Toast.LENGTH_SHORT).show();
                                }
                            });
                        });
                    }
                });
            }
        } else {
            if (btnScan != null) btnScan.setVisibility(View.GONE);
            if (btnViewList != null) btnViewList.setVisibility(View.GONE);
            View registerContainer = findViewById(R.id.registerContainer);
            if (registerContainer != null) registerContainer.setVisibility(View.GONE);
        }
    }

    private void observeViewModel() {
        viewModel.selectedWorkshop.observe(this, workshop -> {
            if (workshop != null) {
                TextView tvTitle = findViewById(R.id.tvWorkshopTitle);
                if (tvTitle != null) tvTitle.setText(workshop.getTitle());
                
                TextView tvPresenter = findViewById(R.id.tvPresenterName);
                if (tvPresenter != null) tvPresenter.setText(workshop.getAuthor());

                TextView tvTime = findViewById(R.id.tvWorkshopTime);
                if (tvTime != null) tvTime.setText(workshop.getTime());

                TextView tvLocation = findViewById(R.id.tvWorkshopLocation);
                if (tvLocation != null) tvLocation.setText(workshop.getAddress());

                TextView tvSlots = findViewById(R.id.tvWorkshopSlots);
                if (tvSlots != null) tvSlots.setText(workshop.getAttendanceCount() + "/" + workshop.getMaxAttendance());

                TextView tvMaxAttendance = findViewById(R.id.tvMaxAttendance);
                if (tvMaxAttendance != null) tvMaxAttendance.setText("/ " + workshop.getAttendanceCount());

                TextView tvAttendance = findViewById(R.id.tvAttendanceCount);
                // Initial text is set to 0 or current local count if available
                if (tvAttendance != null) tvAttendance.setText("0");
                
                TextView tvSummary = findViewById(R.id.tvSummaryContent);
                if (tvSummary != null) {
                    io.noties.markwon.Markwon markwon = io.noties.markwon.Markwon.create(WorkshopDetailView.this);
                    String desc = workshop.getDescription() != null ? workshop.getDescription() : "No summary available.";
                    markwon.setMarkdown(tvSummary, desc);
                }

                TextView tvAddress = findViewById(R.id.tvAddress);
                if (tvAddress != null) tvAddress.setText(workshop.isFree() ? "FREE WORKSHOP" : "PAID WORKSHOP");
                
                viewModel.getLocalAttendanceCount(workshop.getId()).observe(this, localCount -> {
                    if (localCount != null) {
                        if (tvAttendance != null) tvAttendance.setText(String.valueOf(localCount));
                    }
                });
                
                if (workshop.isRegistered()) {
                    View btnRegister = findViewById(R.id.registerContainer);
                    TextView tvRegisterText = findViewById(R.id.tvRegisterText);
                    if (btnRegister != null && tvRegisterText != null) {
                        tvRegisterText.setText("Registered");
                        btnRegister.setEnabled(false);
                    }
                }
            }
        });
    }
}
