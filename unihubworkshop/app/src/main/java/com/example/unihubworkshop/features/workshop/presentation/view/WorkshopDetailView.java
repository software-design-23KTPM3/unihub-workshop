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
        btnScan.setOnClickListener(v -> {
            startActivity(new Intent(this, QRScannerView.class));
        });
    }

    private void observeViewModel() {
        viewModel.selectedWorkshop.observe(this, workshop -> {
            if (workshop != null) {
                ((TextView) findViewById(R.id.tvWorkshopTitle)).setText(workshop.getTitle());
                ((TextView) findViewById(R.id.tvPresenterName)).setText(workshop.getAuthor());
                ((TextView) findViewById(R.id.tvAttendanceCount)).setText(String.valueOf(workshop.getAttendanceCount()));
                ((TextView) findViewById(R.id.tvSummaryContent)).setText(workshop.getDescription());
                ((TextView) findViewById(R.id.tvAddress)).setText(workshop.getRoomNumber());
            }
        });
    }
}
