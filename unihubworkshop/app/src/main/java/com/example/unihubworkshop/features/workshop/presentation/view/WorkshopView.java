package com.example.unihubworkshop.features.workshop.presentation.view;

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
        WorkshopRepositoryImpl repo = new WorkshopRepositoryImpl();
        viewModel = new WorkshopViewModel(new GetWorkshopsUseCase(repo), new GetWorkshopDetailUseCase(repo));

        setupUI();
        setupRecyclerView();
        observeViewModel();
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
            SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String role = sharedPreferences.getString("userRole", "STUDENT");
            
            Intent intent;
            if ("STAFF".equals(role)) {
                intent = new Intent(this, WorkshopDetailView.class);
            } else {
                intent = new Intent(this, StudentWorkshopDetailView.class);
            }
            
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

