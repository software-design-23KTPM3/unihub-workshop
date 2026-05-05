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

        // Manual DI
        WorkshopRepositoryImpl repo = new WorkshopRepositoryImpl(this);
        viewModel = new WorkshopViewModel(repo, new GetWorkshopsUseCase(repo), new GetWorkshopDetailUseCase(repo));

        setupUI();
        setupRecyclerView();
        observeViewModel();
        createNotificationChannel();
        requestNotificationPermission();
        registerFcmToken();
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

    private void registerFcmToken() {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    return;
                }
                String token = task.getResult();
                sendTokenToServer(token);
            });
    }

    private void sendTokenToServer(String token) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String studentId = sharedPreferences.getString("userId", null);
        String role = sharedPreferences.getString("userRole", "STUDENT");

        if (studentId == null || !"STUDENT".equals(role)) return;

        com.example.unihubworkshop.features.workshop.data.datasource.StudentApi api = 
            com.example.unihubworkshop.core.network.RetrofitClient.getClient(this).create(com.example.unihubworkshop.features.workshop.data.datasource.StudentApi.class);
        
        com.example.unihubworkshop.features.workshop.data.datasource.StudentApi.FcmTokenRequest request = 
            new com.example.unihubworkshop.features.workshop.data.datasource.StudentApi.FcmTokenRequest(studentId, token);
        
        api.updateFcmToken(request).enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                android.util.Log.d("FCM", "Token updated: " + response.isSuccessful());
            }

            @Override
            public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                android.util.Log.e("FCM", "Token update failed", t);
            }
        });
    }


    private void setupUI() {
        findViewById(R.id.btnLogout).setOnClickListener(v -> logout());
    }

    private void logout() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();
        
        Intent intent = new Intent(this, LoginView.class);
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

