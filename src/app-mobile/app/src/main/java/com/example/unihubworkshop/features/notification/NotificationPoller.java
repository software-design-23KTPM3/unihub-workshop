package com.example.unihubworkshop.features.notification;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.example.unihubworkshop.MainActivity;
import com.example.unihubworkshop.R;
import com.example.unihubworkshop.core.network.RetrofitClient;
import com.example.unihubworkshop.features.notification.data.NotificationApi;
import com.example.unihubworkshop.features.notification.data.NotificationDto;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationPoller {
    private static final String TAG = "NotificationPoller";
    private static final String CHANNEL_ID = "workshop_notifications";
    private static final long POLLING_INTERVAL_MS = 15000; // 15 seconds

    private static NotificationPoller instance;
    private final Context context;
    private final NotificationApi notificationApi;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isPolling = false;

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            pollNotifications();
            if (isPolling) {
                handler.postDelayed(this, POLLING_INTERVAL_MS);
            }
        }
    };

    private NotificationPoller(Context context) {
        this.context = context.getApplicationContext();
        this.notificationApi = RetrofitClient.getClient(this.context).create(NotificationApi.class);
        createNotificationChannel();
    }

    public static synchronized NotificationPoller getInstance(Context context) {
        if (instance == null) {
            instance = new NotificationPoller(context);
        }
        return instance;
    }

    public void startPolling() {
        if (!isPolling) {
            isPolling = true;
            handler.post(pollRunnable);
        }
    }

    public void stopPolling() {
        isPolling = false;
        handler.removeCallbacks(pollRunnable);
    }

    public void pollNow() {
        pollNotifications();
    }

    public void pollAfterDelay(long delayMs) {
        handler.postDelayed(this::pollNotifications, delayMs);
    }

    private void pollNotifications() {
        notificationApi.getUnreadNotifications().enqueue(new Callback<List<NotificationDto>>() {
            @Override
            public void onResponse(Call<List<NotificationDto>> call, Response<List<NotificationDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (NotificationDto notif : response.body()) {
                        showLocalNotification(notif);
                        markAsRead(notif.getId());
                    }
                }
            }

            @Override
            public void onFailure(Call<List<NotificationDto>> call, Throwable t) {
                Log.e(TAG, "Failed to poll notifications", t);
            }
        });
    }

    private void markAsRead(String id) {
        notificationApi.markAsRead(id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {}
            @Override
            public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Workshop Notifications";
            String description = "Workshop registration and updates";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void showLocalNotification(NotificationDto notif) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_scan)
                .setContentTitle("UniHub")
                .setContentText(notif.getContent())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(notif.getId().hashCode(), builder.build());
    }
}
