package com.example.unihubworkshop.features.workshop.presentation.view;

import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.unihubworkshop.R;
import com.example.unihubworkshop.features.workshop.data.repository.WorkshopRepositoryImpl;
import com.example.unihubworkshop.features.workshop.domain.usecase.GetWorkshopDetailUseCase;
import com.example.unihubworkshop.features.workshop.domain.usecase.GetWorkshopsUseCase;
import com.example.unihubworkshop.features.workshop.presentation.viewmodel.WorkshopViewModel;

import com.example.unihubworkshop.features.auth.presentation.view.LoginView;

public class WorkshopView extends AppCompatActivity {
    private WorkshopViewModel viewModel;
    private WorkshopAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workshop);
        
        // Security check
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        if (!sharedPreferences.getBoolean("isLogged", false)) {
            Intent intent = new Intent(this, com.example.unihubworkshop.MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // Manual DI
        WorkshopRepositoryImpl repo = new WorkshopRepositoryImpl(this);
        viewModel = new WorkshopViewModel(repo, new GetWorkshopsUseCase(repo), new GetWorkshopDetailUseCase(repo));

        setupUI();
        setupRecyclerView();
        observeViewModel();
        createNotificationChannel();
        requestNotificationPermission();
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String channelId = "workshop_notifications";
            CharSequence name = "Workshop Notifications";
            String description = "Notifications for workshop registrations";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            android.app.NotificationChannel channel = new android.app.NotificationChannel(channelId, name, importance);
            channel.setDescription(description);
            android.app.NotificationManager notificationManager = getSystemService(android.app.NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.loadWorkshops();
    }

    private void setupUI() {
        findViewById(R.id.btnLogout).setOnClickListener(v -> logout());
    }

    private void logout() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();
        
        Intent intent = new Intent(this, com.example.unihubworkshop.MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupRecyclerView() {
        RecyclerView rv = findViewById(R.id.rvWorkshop);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WorkshopAdapter(null);
        adapter.setOnItemClickListener(workshop -> {
            Intent intent = new Intent(this, StudentWorkshopDetailView.class);
            intent.putExtra("workshop_id", workshop.getId());
            intent.putExtra("is_registered", workshop.isRegistered());
            intent.putExtra("registration_id", workshop.getRegistrationId());
            startActivity(intent);
        });
        rv.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.workshops.observe(this, workshops -> {
            adapter.setWorkshops(workshops);
        });
    }
}
