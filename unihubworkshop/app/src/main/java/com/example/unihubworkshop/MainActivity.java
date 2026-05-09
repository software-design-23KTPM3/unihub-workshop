package com.example.unihubworkshop;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.unihubworkshop.features.auth.presentation.view.LoginView;
import com.example.unihubworkshop.features.workshop.presentation.view.StaffPortalView;
import com.example.unihubworkshop.features.workshop.presentation.view.WorkshopView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isLogged", false);

        if (!isLoggedIn) {
            Intent intent = new Intent(MainActivity.this, LoginView.class);
            startActivity(intent);
            finish();
            return;
        }

        String role = sharedPreferences.getString("userRole", "STUDENT");
        Intent intent = new Intent(
                MainActivity.this,
                "CHECKIN_STAFF".equals(role) || "STAFF".equals(role) || "ORGANIZER".equals(role)
                        ? StaffPortalView.class
                        : WorkshopView.class
        );
        startActivity(intent);
        finish();
    }
}
