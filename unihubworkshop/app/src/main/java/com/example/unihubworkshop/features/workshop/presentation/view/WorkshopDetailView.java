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
            viewModel.selectWorkshop(workshopId);
        }

        setupListeners();
        observeViewModel();
    }

    private void setupListeners() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        View btnScan = findViewById(R.id.scanContainer);
        
        android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String role = prefs.getString("userRole", "");
        
        if ("CHECKIN_STAFF".equals(role) || "STAFF".equals(role) || "ORGANIZER".equals(role)) {
            btnScan.setVisibility(View.VISIBLE);
            findViewById(R.id.registerContainer).setVisibility(View.GONE);
            btnScan.setOnClickListener(v -> {
                Intent intent = new Intent(this, QRScannerView.class);
                intent.putExtra("workshop_id", getIntent().getStringExtra("workshop_id"));
                startActivity(intent);
            });
        } else if ("STUDENT".equals(role)) {
            btnScan.setVisibility(View.GONE);
            View btnRegister = findViewById(R.id.registerContainer);
            btnRegister.setVisibility(View.VISIBLE);
            btnRegister.setOnClickListener(v -> {
                String id = getIntent().getStringExtra("workshop_id");
                if (id != null) {
                    // Update UI immediately (optimistic UI)
                    TextView tvRegisterText = findViewById(R.id.tvRegisterText);
                    tvRegisterText.setText("Registering...");
                    btnRegister.setEnabled(false);
                    
                    viewModel.registerForWorkshop(id, success -> {
                        runOnUiThread(() -> {
                            if (success) {
                                tvRegisterText.setText("Registered");
                                btnRegister.setBackgroundResource(R.drawable.bg_button_secondary); // assuming there's a secondary bg, or keep same
                            } else {
                                tvRegisterText.setText("Register");
                                btnRegister.setEnabled(true);
                                android.widget.Toast.makeText(WorkshopDetailView.this, "Registration failed", android.widget.Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                }
            });
        } else {
            btnScan.setVisibility(View.GONE);
            findViewById(R.id.registerContainer).setVisibility(View.GONE);
        }
    }

    private void observeViewModel() {
        viewModel.selectedWorkshop.observe(this, workshop -> {
            if (workshop != null) {
                ((TextView) findViewById(R.id.tvWorkshopTitle)).setText(workshop.getTitle());
                ((TextView) findViewById(R.id.tvPresenterName)).setText(workshop.getAuthor());
                // Fallback to server attendance count initially
                ((TextView) findViewById(R.id.tvAttendanceCount)).setText(String.valueOf(workshop.getAttendanceCount()));
                // Render AI Summary / Description as Markdown
                io.noties.markwon.Markwon markwon = io.noties.markwon.Markwon.create(WorkshopDetailView.this);
                markwon.setMarkdown((TextView) findViewById(R.id.tvSummaryContent), workshop.getDescription());
                ((TextView) findViewById(R.id.tvAddress)).setText(workshop.getRoomNumber());
                
                // Observe local checkin count
                viewModel.getLocalAttendanceCount(workshop.getId()).observe(this, localCount -> {
                    if (localCount != null && localCount > 0) {
                        ((TextView) findViewById(R.id.tvAttendanceCount)).setText(String.valueOf(localCount));
                    }
                });
                
                // Update register button if already registered
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
