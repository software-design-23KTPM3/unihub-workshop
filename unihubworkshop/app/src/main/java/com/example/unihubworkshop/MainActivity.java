package com.example.unihubworkshop;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;

import com.example.unihubworkshop.features.workshop.presentation.view.StaffPortalView;
import com.example.unihubworkshop.features.workshop.presentation.view.WorkshopView;

public class MainActivity extends AppCompatActivity {

    private View inputUsername;
    private View inputPassword;
    private android.widget.Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        android.content.SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isLogged", false);
        String accessToken = sharedPreferences.getString("accessToken", null);

        android.util.Log.d("MainActivity", "isLoggedIn: " + isLoggedIn + ", hasToken: " + (accessToken != null));

        // If logged in AND has token, redirect to Home immediately
        if (isLoggedIn && accessToken != null) {
            redirectToHome(sharedPreferences.getString("userRole", "STUDENT"));
            return;
        }

        // If not logged in, show Login UI
        setContentView(R.layout.activity_login);
        setupLoginUI();
    }

    private void setupLoginUI() {
        inputUsername = findViewById(R.id.inputUsername);
        inputPassword = findViewById(R.id.inputPassword);
        btnLogin = findViewById(R.id.btnLogin);

        android.widget.TextView tvInputUserName = inputUsername.findViewById(R.id.tvInput);
        android.widget.TextView tvInputPassword = inputPassword.findViewById(R.id.tvInput);
        tvInputUserName.setText("Username");
        tvInputPassword.setText("Password");

        android.widget.EditText etUsername = inputUsername.findViewById(R.id.etInput);
        android.widget.EditText etPassword = inputPassword.findViewById(R.id.etPasswordInput);
        etUsername.setHint("Enter username");
        etPassword.setHint("Enter password");

        // Ensure password masking
        etPassword.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            com.example.unihubworkshop.features.auth.data.datasource.AuthApi authApi = 
                com.example.unihubworkshop.core.network.RetrofitClient.getKeycloakClient().create(com.example.unihubworkshop.features.auth.data.datasource.AuthApi.class);
            
            authApi.login("unihub-client", username, password, "password").enqueue(new retrofit2.Callback<com.example.unihubworkshop.features.auth.data.datasource.TokenResponse>() {
                @Override
                public void onResponse(retrofit2.Call<com.example.unihubworkshop.features.auth.data.datasource.TokenResponse> call, retrofit2.Response<com.example.unihubworkshop.features.auth.data.datasource.TokenResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String token = response.body().getAccessToken();
                        android.content.SharedPreferences.Editor editor = getSharedPreferences("UserPrefs", MODE_PRIVATE).edit();
                        editor.putBoolean("isLogged", true);
                        editor.putString("accessToken", token);
                        editor.putString("userId", username);

                        String role = resolveRole(token);
                        editor.putString("userRole", role);
                        editor.apply();

                        redirectToHome(role);
                    } else {
                        android.widget.Toast.makeText(MainActivity.this, "Login Failed", android.widget.Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<com.example.unihubworkshop.features.auth.data.datasource.TokenResponse> call, Throwable t) {
                    android.widget.Toast.makeText(MainActivity.this, "Network Error", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void redirectToHome(String role) {
        Intent intent = new Intent(
                MainActivity.this,
                "CHECKIN_STAFF".equals(role) || "STAFF".equals(role) || "ORGANIZER".equals(role)
                        ? StaffPortalView.class
                        : WorkshopView.class
        );
        startActivity(intent);
        finish();
    }

    private String resolveRole(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return "STUDENT";
            byte[] decoded = android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE | android.util.Base64.NO_WRAP | android.util.Base64.NO_PADDING);
            org.json.JSONObject payload = new org.json.JSONObject(new String(decoded, java.nio.charset.StandardCharsets.UTF_8));
            org.json.JSONObject realmAccess = payload.optJSONObject("realm_access");
            org.json.JSONArray roles = realmAccess == null ? new org.json.JSONArray() : realmAccess.optJSONArray("roles");
            
            for (int i = 0; i < roles.length(); i++) {
                String r = roles.optString(i);
                if ("CHECKIN_STAFF".equals(r) || "STAFF".equals(r) || "ORGANIZER".equals(r)) return r;
            }
        } catch (Exception ignored) {}
        return "STUDENT";
    }
}
