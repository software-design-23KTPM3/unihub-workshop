package com.example.unihubworkshop.features.workshop.data.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.unihubworkshop.core.network.RetrofitClient;
import com.example.unihubworkshop.features.workshop.data.datasource.CheckinApi;
import com.example.unihubworkshop.features.workshop.data.datasource.CheckinEvent;

import java.util.List;

import retrofit2.Response;

public class CheckinSyncWorker extends Worker {
    private static final String UNIQUE_WORK_NAME = "checkin-sync";

    public CheckinSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static void enqueue(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(CheckinSyncWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, request);
    }

    @NonNull
    @Override
    public Result doWork() {
        com.example.unihubworkshop.features.workshop.data.local.db.AppDatabase db = 
            com.example.unihubworkshop.features.workshop.data.local.db.AppDatabase.getInstance(getApplicationContext());
        List<com.example.unihubworkshop.features.workshop.data.local.entity.CheckinEventEntity> pendingEntities = db.checkinEventDao().getPendingEvents();
        
        if (pendingEntities.isEmpty()) {
            return Result.success();
        }

        List<CheckinEvent> pending = new java.util.ArrayList<>();
        List<String> pendingIds = new java.util.ArrayList<>();
        for (com.example.unihubworkshop.features.workshop.data.local.entity.CheckinEventEntity entity : pendingEntities) {
            pending.add(new CheckinEvent(
                entity.clientEventId,
                entity.studentId,
                entity.workshopId,
                entity.registrationId,
                entity.qrCode,
                entity.staffId,
                entity.deviceId,
                entity.checkinAt
            ));
            pendingIds.add(entity.clientEventId);
        }

        try {
            CheckinApi api = RetrofitClient.getClient(getApplicationContext()).create(CheckinApi.class);
            Response<Void> response = api.syncCheckins(pending).execute();
            if (response.isSuccessful()) {
                db.checkinEventDao().deleteSynced(pendingIds);
                return Result.success();
            }
            if (response.code() >= 500 || response.code() == 429) {
                return Result.retry();
            }
            return Result.failure();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
