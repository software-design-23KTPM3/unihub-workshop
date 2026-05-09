package com.example.unihubworkshop.features.auth.presentation.view;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.unihubworkshop.R;

import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Base64;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

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
        EditText etPassword = inputPassword.findViewById(R.id.etPasswordInput);
        etUsername.setHint("Enter username");
        etPassword.setHint("Enter password");
        
        // Masking is now handled by TextInputLayout and its EditText in XML
        // but we ensure it's set correctly here as well for robustness
        etPassword.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
        
        etUsername.setSingleLine(true);
        etPassword.setSingleLine(true);
        etUsername.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);
        etPassword.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);

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

                        String role = resolveRole(token);
                        Intent intent;
                        if ("CHECKIN_STAFF".equals(role) || "STAFF".equals(role) || "ORGANIZER".equals(role)) {
                            editor.putString("userRole", role);
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

    private String resolveRole(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return "STUDENT";
            }
            byte[] decoded = Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            JSONObject payload = new JSONObject(new String(decoded, StandardCharsets.UTF_8));
            JSONObject realmAccess = payload.optJSONObject("realm_access");
            JSONArray roles = realmAccess == null ? new JSONArray() : realmAccess.optJSONArray("roles");
            if (hasRole(roles, "CHECKIN_STAFF")) {
                return "CHECKIN_STAFF";
            }
            if (hasRole(roles, "STAFF")) {
                return "STAFF";
            }
            if (hasRole(roles, "ORGANIZER")) {
                return "ORGANIZER";
            }
        } catch (Exception ignored) {
        }
        return "STUDENT";
    }

    private boolean hasRole(JSONArray roles, String targetRole) {
        if (roles == null) {
            return false;
        }
        for (int i = 0; i < roles.length(); i++) {
            if (targetRole.equals(roles.optString(i))) {
                return true;
            }
        }
        return false;
    }
}
