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
import android.widget.Toast;

import com.example.unihubworkshop.core.network.RetrofitClient;
import com.example.unihubworkshop.features.auth.data.datasource.AuthApi;
import com.example.unihubworkshop.features.auth.data.datasource.TokenResponse;
import com.example.unihubworkshop.features.workshop.presentation.view.StaffPortalView;
import com.example.unihubworkshop.features.workshop.presentation.view.WorkshopView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

        EditText etUsername = inputUsername.findViewById(R.id.etInput);
        EditText etPassword = inputPassword.findViewById(R.id.etInput);
        etUsername.setHint("Enter username");
        etPassword.setHint("Enter password");

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            
            AuthApi authApi = RetrofitClient.getKeycloakClient().create(AuthApi.class);
            authApi.login("unihub-client", username, password, "password").enqueue(new Callback<TokenResponse>() {
                @Override
                public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String token = response.body().getAccessToken();
                        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean("isLogged", true);
                        editor.putString("accessToken", token);
                        editor.putString("userId", username);
                        
                        // Just a simple hack for roles based on username
                        Intent intent;
                        if (username.startsWith("hr")) {
                            editor.putString("userRole", "HUMAN_RESOURCE");
                            intent = new Intent(LoginView.this, StaffPortalView.class);
                        } else if (username.startsWith("organizer")) {
                            editor.putString("userRole", "ORGANIZER");
                            intent = new Intent(LoginView.this, StaffPortalView.class);
                        } else {
                            editor.putString("userRole", "STUDENT");
                            intent = new Intent(LoginView.this, WorkshopView.class);
                        }
                        editor.apply();
                        
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginView.this, "Login Failed", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<TokenResponse> call, Throwable t) {
                    Toast.makeText(LoginView.this, "Network Error", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
