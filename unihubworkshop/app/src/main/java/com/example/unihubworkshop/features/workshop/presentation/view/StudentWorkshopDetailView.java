package com.example.unihubworkshop.features.workshop.presentation.view;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.unihubworkshop.R;
import com.example.unihubworkshop.features.notification.NotificationPoller;
import com.example.unihubworkshop.features.workshop.data.repository.WorkshopRepositoryImpl;
import com.example.unihubworkshop.features.workshop.domain.entity.WorkShop;
import com.example.unihubworkshop.features.workshop.domain.usecase.GetWorkshopDetailUseCase;
import com.example.unihubworkshop.features.workshop.domain.usecase.GetWorkshopsUseCase;
import com.example.unihubworkshop.features.workshop.presentation.viewmodel.WorkshopViewModel;

public class StudentWorkshopDetailView extends AppCompatActivity {
    private WorkshopViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_workshop_detail);

        // Manual DI
        WorkshopRepositoryImpl repo = new WorkshopRepositoryImpl(this);
        viewModel = new WorkshopViewModel(repo, new GetWorkshopsUseCase(repo), new GetWorkshopDetailUseCase(repo));

        String workshopId = getIntent().getStringExtra("workshop_id");
        if (workshopId != null) {
            viewModel.selectWorkshop(workshopId);
        }

        setupUI();
        observeViewModel();
    }

    private void setupUI() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        Button btnRegister = findViewById(R.id.btnRegister);
        // Default click listener will be overridden in observeViewModel based on state
    }

    private void observeViewModel() {
        viewModel.selectedWorkshop.observe(this, workshop -> {
            if (workshop != null) {
                ((TextView) findViewById(R.id.tvWorkshopTitle)).setText(workshop.getTitle());
                ((TextView) findViewById(R.id.tvPresenterName)).setText(workshop.getAuthor());
                ((TextView) findViewById(R.id.tvWorkshopTime)).setText(workshop.getTime());
                ((TextView) findViewById(R.id.tvWorkshopLocation)).setText(workshop.getAddress());
                ((TextView) findViewById(R.id.tvSummaryContent)).setText(workshop.getDescription());
                ((TextView) findViewById(R.id.tvWorkshopSlots)).setText(String.format("%d/%d", workshop.getAttendanceCount(), workshop.getMaxAttendance()));
                ((TextView) findViewById(R.id.tvRoomMapText)).setText(workshop.getRoomMapText());
                
                Button btnRegister = findViewById(R.id.btnRegister);
                
                if (workshop.isRegistered()) {
                    if ("registering...".equals(workshop.getRegistrationId())) {
                        btnRegister.setEnabled(false);
                        btnRegister.setText("Registering...");
                    } else {
                        btnRegister.setEnabled(true);
                        btnRegister.setText("View Your Ticket");
                        btnRegister.setOnClickListener(v -> {
                            android.content.Intent intent = new android.content.Intent(this, StudentTicketView.class);
                            intent.putExtra("registration_id", workshop.getRegistrationId());
                            startActivity(intent);
                        });
                    }
                } else if (workshop.getAttendanceCount() >= workshop.getMaxAttendance()) {
                    btnRegister.setEnabled(false);
                    btnRegister.setText("Workshop Full");
                } else {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Register Now");
                    btnRegister.setOnClickListener(v -> registerWorkshop(workshop.getId()));
                }
            }
        });
    }

    private void registerWorkshop(String workshopId) {
        if (workshopId == null) return;
        
        // Optimistic UI Update
        WorkShop current = viewModel.selectedWorkshop.getValue();
        if (current != null && current.isRegistered()) return; // Already registered
        
        viewModel.updateRegistrationStatus(workshopId, true, "registering...");
        
        com.example.unihubworkshop.features.workshop.data.datasource.RegistrationApi regApi = 
            com.example.unihubworkshop.core.network.RetrofitClient.getClient(this)
            .create(com.example.unihubworkshop.features.workshop.data.datasource.RegistrationApi.class);
        
        android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String studentId = prefs.getString("userId", null);
        if (studentId == null) {
            viewModel.updateRegistrationStatus(workshopId, false, null);
            return;
        }
        
        com.example.unihubworkshop.features.workshop.data.datasource.RegistrationRequestDto req = 
            new com.example.unihubworkshop.features.workshop.data.datasource.RegistrationRequestDto(workshopId, studentId);
        
        regApi.createRegistration(java.util.UUID.randomUUID().toString(), req).enqueue(new retrofit2.Callback<com.example.unihubworkshop.features.workshop.data.datasource.RegistrationResponseDto>() {
            @Override
            public void onResponse(retrofit2.Call<com.example.unihubworkshop.features.workshop.data.datasource.RegistrationResponseDto> call, retrofit2.Response<com.example.unihubworkshop.features.workshop.data.datasource.RegistrationResponseDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    android.widget.Toast.makeText(StudentWorkshopDetailView.this, "Registration successful!", android.widget.Toast.LENGTH_SHORT).show();
                    finalizeRegistration(workshopId, response.body().getRegistrationId());
                    NotificationPoller.getInstance(StudentWorkshopDetailView.this).pollAfterDelay(2000);
                } else {
                    android.widget.Toast.makeText(StudentWorkshopDetailView.this, "Registration failed: " + response.code(), android.widget.Toast.LENGTH_SHORT).show();
                    // Rollback
                    viewModel.updateRegistrationStatus(workshopId, false, null);
                }
            }

            @Override
            public void onFailure(retrofit2.Call<com.example.unihubworkshop.features.workshop.data.datasource.RegistrationResponseDto> call, Throwable t) {
                android.widget.Toast.makeText(StudentWorkshopDetailView.this, "Network Error", android.widget.Toast.LENGTH_SHORT).show();
                // Rollback
                viewModel.updateRegistrationStatus(workshopId, false, null);
            }
        });
    }

    private void finalizeRegistration(String workshopId, String registrationId) {
        viewModel.finalizeRegistration(workshopId, registrationId);
    }
}
