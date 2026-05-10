package com.example.unihubworkshop.features.notification;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class NotificationWorker extends Worker {

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Just trigger one poll synchronously or asynchronously.
        // Since Retrofit is async, we can just trigger it and let it show notification.
        // But better to use synchronous call for Worker.
        try {
            com.example.unihubworkshop.features.notification.data.NotificationApi api = 
                com.example.unihubworkshop.core.network.RetrofitClient.getClient(getApplicationContext())
                .create(com.example.unihubworkshop.features.notification.data.NotificationApi.class);
            
            retrofit2.Response<java.util.List<com.example.unihubworkshop.features.notification.data.NotificationDto>> response = api.getUnreadNotifications().execute();
            if (response.isSuccessful() && response.body() != null) {
                for (com.example.unihubworkshop.features.notification.data.NotificationDto notif : response.body()) {
                    showLocalNotification(notif);
                    api.markAsRead(notif.getId()).execute();
                }
            }
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
    
    private void showLocalNotification(com.example.unihubworkshop.features.notification.data.NotificationDto notif) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.app.ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        android.content.Intent intent = new android.content.Intent(getApplicationContext(), com.example.unihubworkshop.MainActivity.class);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(getApplicationContext(), 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE);

        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(getApplicationContext(), "workshop_notifications")
                .setSmallIcon(com.example.unihubworkshop.R.drawable.ic_scan)
                .setContentTitle("UniHub")
                .setContentText(notif.getContent())
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        androidx.core.app.NotificationManagerCompat notificationManager = androidx.core.app.NotificationManagerCompat.from(getApplicationContext());
        notificationManager.notify(notif.getId().hashCode(), builder.build());
    }
}
