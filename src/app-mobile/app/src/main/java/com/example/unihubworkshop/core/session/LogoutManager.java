package com.example.unihubworkshop.core.session;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.work.WorkManager;

import com.example.unihubworkshop.core.network.RetrofitClient;
import com.example.unihubworkshop.features.notification.NotificationPoller;
import com.example.unihubworkshop.features.workshop.data.datasource.CheckinApi;
import com.example.unihubworkshop.features.workshop.data.datasource.CheckinEvent;
import com.example.unihubworkshop.features.workshop.data.local.db.AppDatabase;
import com.example.unihubworkshop.features.workshop.data.local.entity.CheckinEventEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public final class LogoutManager {
    private LogoutManager() {}

    public static void logout(Context context, Runnable afterCleanup) {
        Context appContext = context.getApplicationContext();
        NotificationPoller.getInstance(appContext).stopPolling();

        Executors.newSingleThreadExecutor().execute(() -> {
            // 1. Sync pending checkins before logout if internet is available
            try {
                AppDatabase db = AppDatabase.getInstance(appContext);
                List<CheckinEventEntity> pendingEntities = db.checkinEventDao().getPendingEvents();
                if (!pendingEntities.isEmpty()) {
                    List<CheckinEvent> pending = new ArrayList<>();
                    List<String> pendingIds = new ArrayList<>();
                    for (CheckinEventEntity entity : pendingEntities) {
                        pending.add(new CheckinEvent(
                            entity.clientEventId, entity.studentId, entity.workshopId,
                            entity.registrationId, entity.qrCode, entity.staffId,
                            entity.deviceId, entity.checkinAt
                        ));
                        pendingIds.add(entity.clientEventId);
                    }
                    
                    CheckinApi api = RetrofitClient.getClient(appContext).create(CheckinApi.class);
                    retrofit2.Response<Void> response = api.syncCheckins(pending).execute();
                    if (response.isSuccessful()) {
                        db.checkinEventDao().deleteSynced(pendingIds);
                        for (CheckinEventEntity entity : pendingEntities) {
                            if (entity.registrationId != null) {
                                db.registrationDao().updateIsOfflineOnly(entity.registrationId, false);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore sync errors during logout, user wants to leave
            }

            WorkManager.getInstance(appContext).cancelUniqueWork("checkin-sync");
            WorkManager.getInstance(appContext).cancelUniqueWork("NotificationWorker");

            AppDatabase.destroyInstance();

            appContext.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).edit().clear().commit();
            appContext.getSharedPreferences("RegistrationPrefs", Context.MODE_PRIVATE).edit().clear().commit();
            appContext.getSharedPreferences("CheckinPrefs", Context.MODE_PRIVATE).edit().clear().commit();

            if (afterCleanup != null) {
                new Handler(Looper.getMainLooper()).post(afterCleanup);
            }
        });
    }
}
