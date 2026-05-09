package com.example.unihubworkshop.features.workshop.presentation.view;

import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.unihubworkshop.R;
import com.example.unihubworkshop.features.workshop.data.repository.WorkshopRepositoryImpl;
import com.example.unihubworkshop.features.workshop.domain.usecase.GetWorkshopDetailUseCase;
import com.example.unihubworkshop.features.workshop.domain.usecase.GetWorkshopsUseCase;
import com.example.unihubworkshop.features.workshop.presentation.viewmodel.WorkshopViewModel;

public class RegisteredStudentsView extends AppCompatActivity {
    private WorkshopViewModel viewModel;
    private RegistrationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registered_students);

        // Manual DI
        WorkshopRepositoryImpl repo = new WorkshopRepositoryImpl(this);
        viewModel = new WorkshopViewModel(repo, new GetWorkshopsUseCase(repo), new GetWorkshopDetailUseCase(repo));

        String workshopId = getIntent().getStringExtra("workshop_id");
        
        setupUI();
        
        if (workshopId != null) {
            viewModel.getRegistrationDetails(workshopId).observe(this, registrations -> {
                if (registrations != null) {
                    adapter.setRegistrations(registrations);
                }
            });
        }
    }

    private void setupUI() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        RecyclerView rvRegistrations = findViewById(R.id.rvRegistrations);
        rvRegistrations.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RegistrationAdapter();
        rvRegistrations.setAdapter(adapter);
    }
}
