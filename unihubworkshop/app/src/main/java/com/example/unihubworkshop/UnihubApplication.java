package com.example.unihubworkshop;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class UnihubApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Firebase
        FirebaseApp.initializeApp(this);
    }
}
