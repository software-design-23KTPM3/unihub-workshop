package com.example.unihubworkshop.features.workshop.presentation.view;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.unihubworkshop.R;
import com.example.unihubworkshop.features.workshop.data.repository.WorkshopRepositoryImpl;
import com.example.unihubworkshop.features.workshop.domain.usecase.GetWorkshopDetailUseCase;
import com.example.unihubworkshop.features.workshop.domain.usecase.GetWorkshopsUseCase;
import com.example.unihubworkshop.features.workshop.presentation.viewmodel.WorkshopViewModel;

import android.content.SharedPreferences;
import com.example.unihubworkshop.features.auth.presentation.view.LoginView;

public class StaffPortalView extends AppCompatActivity {
    private WorkshopViewModel viewModel;
    private WorkshopAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_portal);
        
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
        RecyclerView rv = findViewById(R.id.rvStaffWorkshops);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WorkshopAdapter(null);
        adapter.setShowRegistrationStatus(false);
        adapter.setOnItemClickListener(workshop -> {
            Intent intent = new Intent(this, WorkshopDetailView.class);
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
