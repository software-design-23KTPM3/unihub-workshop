package com.example.unihubworkshop.features.auth.presentation.view;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.unihubworkshop.R;

import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Button;
import android.widget.EditText;
import com.example.unihubworkshop.features.workshop.presentation.view.StaffPortalView;
import com.example.unihubworkshop.features.workshop.presentation.view.WorkshopView;

public class LoginView extends AppCompatActivity {
    private View inputUsername;
    private View inputPassword;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle saveInstance) {
        super.onCreate(saveInstance);
        setContentView(R.layout.activity_login);

        inputUsername = findViewById(R.id.inputUsername);
        inputPassword = findViewById(R.id.inputPassword);
        btnLogin = findViewById(R.id.btnLogin);

        TextView tvInputUserName = (TextView) inputUsername.findViewById(R.id.tvInput);
        TextView tvInputPassword = (TextView) inputPassword.findViewById(R.id.tvInput);
        tvInputUserName.setText("Username");
        tvInputPassword.setText("Password");

        //TEST NOT BACKEND

        EditText etUsername = inputUsername.findViewById(R.id.etInput);
        EditText etPassword = inputPassword.findViewById(R.id.etInput);
        etUsername.setHint("Enter username");
        etPassword.setHint("Enter password");

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            
            SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isLogged", true);
            
            Intent intent;
            if ("staff".equalsIgnoreCase(username)) {
                editor.putString("userRole", "STAFF");
                intent = new Intent(this, StaffPortalView.class);
            } else {
                editor.putString("userRole", "STUDENT");
                intent = new Intent(this, WorkshopView.class);
            }
            editor.apply();
            
            startActivity(intent);
            finish();
        });
    }

}
