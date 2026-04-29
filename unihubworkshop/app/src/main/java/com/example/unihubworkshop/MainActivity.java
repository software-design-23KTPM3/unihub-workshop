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

        // If logged in, go to WorkshopView (role-based detail navigation is handled there)
        Intent intent = new Intent(MainActivity.this, WorkshopView.class);
        startActivity(intent);
        finish();
    }
}