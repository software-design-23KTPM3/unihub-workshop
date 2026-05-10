package com.example.unihubworkshop.core.session;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.work.WorkManager;

import com.example.unihubworkshop.features.notification.NotificationPoller;
import com.example.unihubworkshop.features.workshop.data.local.db.AppDatabase;

import java.util.concurrent.Executors;

public final class LogoutManager {
    private LogoutManager() {}

    public static void logout(Context context, Runnable afterCleanup) {
        Context appContext = context.getApplicationContext();
        NotificationPoller.getInstance(appContext).stopPolling();

        Executors.newSingleThreadExecutor().execute(() -> {
            WorkManager.getInstance(appContext).cancelUniqueWork("checkin-sync");
            WorkManager.getInstance(appContext).cancelUniqueWork("NotificationWorker");

            AppDatabase.getInstance(appContext).clearAllTables();
            appContext.deleteDatabase("unihub_checkin.db");

            appContext.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).edit().clear().commit();
            appContext.getSharedPreferences("RegistrationPrefs", Context.MODE_PRIVATE).edit().clear().commit();
            appContext.getSharedPreferences("CheckinPrefs", Context.MODE_PRIVATE).edit().clear().commit();

            if (afterCleanup != null) {
                new Handler(Looper.getMainLooper()).post(afterCleanup);
            }
        });
    }
}
