package com.example.unihubworkshop.features.workshop.presentation.view;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.unihubworkshop.R;
import com.example.unihubworkshop.core.network.RetrofitClient;
import com.example.unihubworkshop.features.workshop.data.datasource.RegistrationApi;
import com.example.unihubworkshop.features.workshop.data.datasource.RegistrationDetailResponseDto;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentTicketView extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_ticket);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        String registrationId = getIntent().getStringExtra("registration_id");
        if (registrationId != null) {
            loadTicket(registrationId);
        }
    }

    private void loadTicket(String id) {
        RegistrationApi api = RetrofitClient.getClient(this).create(RegistrationApi.class);
        api.getRegistrationById(id).enqueue(new Callback<RegistrationDetailResponseDto>() {
            @Override
            public void onResponse(Call<RegistrationDetailResponseDto> call, Response<RegistrationDetailResponseDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displayTicket(response.body());
                } else {
                    Toast.makeText(StudentTicketView.this, "Failed to load ticket", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RegistrationDetailResponseDto> call, Throwable t) {
                Toast.makeText(StudentTicketView.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayTicket(RegistrationDetailResponseDto ticket) {
        ((TextView) findViewById(R.id.tvWorkshopTitle)).setText(ticket.getWorkshop().getTitle());
        ((TextView) findViewById(R.id.tvWorkshopTime)).setText(ticket.getWorkshop().getDate() + " | " + ticket.getWorkshop().getStartTime());
        ((TextView) findViewById(R.id.tvRegistrationId)).setText("ID: " + ticket.getId());

        RegistrationApi api = RetrofitClient.getClient(this).create(RegistrationApi.class);
        api.getRegistrationQrImage(ticket.getId()).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Bitmap qrBitmap = BitmapFactory.decodeStream(response.body().byteStream());
                    ((ImageView) findViewById(R.id.ivQrCode)).setImageBitmap(qrBitmap);
                } else {
                    Toast.makeText(StudentTicketView.this, "Failed to load QR code", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(StudentTicketView.this, "Network error while loading QR code", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
