package com.example.unihubworkshop.features.workshop.presentation.view;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.unihubworkshop.R;
import com.example.unihubworkshop.features.workshop.data.repository.WorkshopRepositoryImpl;
import com.example.unihubworkshop.features.workshop.domain.usecase.GetWorkshopDetailUseCase;
import com.example.unihubworkshop.features.workshop.domain.usecase.GetWorkshopsUseCase;
import com.example.unihubworkshop.features.workshop.presentation.viewmodel.WorkshopViewModel;

public class StudentWorkshopDetailView extends AppCompatActivity {
    private WorkshopViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_workshop_detail);

        // Simple manual DI
        WorkshopRepositoryImpl repo = new WorkshopRepositoryImpl();
        viewModel = new WorkshopViewModel(new GetWorkshopsUseCase(repo), new GetWorkshopDetailUseCase(repo));

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
        btnRegister.setOnClickListener(v -> {
            android.widget.Toast.makeText(this, "Registration successful!", android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    private void observeViewModel() {
        viewModel.selectedWorkshop.observe(this, workshop -> {
            if (workshop != null) {
                ((TextView) findViewById(R.id.tvWorkshopTitle)).setText(workshop.getTitle());
                ((TextView) findViewById(R.id.tvPresenterName)).setText(workshop.getAuthor());
                ((TextView) findViewById(R.id.tvWorkshopTime)).setText(workshop.getTime());
                ((TextView) findViewById(R.id.tvWorkshopLocation)).setText(workshop.getAddress());
                ((TextView) findViewById(R.id.tvSummaryContent)).setText(workshop.getDescription());
            }
        });
    }
}
