package com.unihubworkshop.worker.notification;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NotificationApp implements NotificationChannel {

    @Autowired
    private FirebaseMessaging firebaseMessaging;

    public NotificationApp() {
        System.out.println("NotificationApp initialized");
    }

    @Override
    public String getType() {
        return "APP";
    }

    @Override
    public void send(NotificationData data) {
        System.out.println("APP NOTIFICATION CALL (FCM)");
        
        try {
            // For demo purposes, if 'to' is an email, we send to a generic topic
            // In production, 'to' should be the FCM Device Token
            String target = data.getTo();
            
            Message.Builder messageBuilder = Message.builder()
                    .setNotification(Notification.builder()
                            .setTitle(data.getTitle())
                            .setBody(data.getMsg()) // HTML body might not look good in push, consider stripping it
                            .build());

            if (target == null || target.contains("@") || target.isEmpty()) {
                System.out.println("Cannot send Push Notification: FCM Token is missing (got email or null). Skipping APP channel.");
                return;
            }

            Message message = Message.builder()
                    .setNotification(Notification.builder()
                            .setTitle(data.getTitle())
                            .setBody(data.getMsg())
                            .build())
                    .setToken(target)
                    .build();

            String response = firebaseMessaging.send(message);
            System.out.println("Successfully sent FCM message: " + response);

        } catch (Exception e) {
            System.err.println("Error sending FCM notification: " + e.getMessage());
        }
    }
}
