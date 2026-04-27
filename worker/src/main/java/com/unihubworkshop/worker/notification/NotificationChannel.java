package com.unihubworkshop.worker.notification;

public interface NotificationChannel {
    public String getType();
    public void send(NotificationData data);
}
