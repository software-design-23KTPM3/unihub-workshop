package com.unihubworkshop.worker;

import com.unihubworkshop.worker.config.RabbitConfig;
import com.unihubworkshop.worker.notification.NotificationChannel;
import com.unihubworkshop.worker.notification.NotificationData;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class NotificationWorker {
    private final Map<String, NotificationChannel> channels;

    public NotificationWorker(List<NotificationChannel> list) {
        this.channels = new HashMap<>();
        for (NotificationChannel channel : list) {
            System.out.println(channel.getType());
            this.channels.put(channel.getType(), channel);
        }
    }

    @RabbitListener(queues = RabbitConfig.NOTIFICATION_QUEUE)
    public void handle(Map<String, Object> request) {
        String type = stringValue(request.get("type"));
        NotificationData data = toNotificationData(request.get("data"));
        NotificationChannel channel = channels.get(type);
        if (channel != null) {
            channel.send(data);
        }
    }

    @SuppressWarnings("unchecked")
    private NotificationData toNotificationData(Object value) {
        if (value instanceof NotificationData data) {
            return data;
        }
        if (value instanceof Map<?, ?> map) {
            return new NotificationData(
                    stringValue(map.get("title")),
                    stringValue(map.get("msg")),
                    stringValue(map.get("to")),
                    nullableString(map.get("qrPayload")),
                    nullableString(map.get("qrImageBase64")),
                    nullableString(map.get("workshopTitle")),
                    nullableString(map.get("workshopTime")),
                    nullableString(map.get("workshopRoom")),
                    nullableString(map.get("workshopSpeaker")));
        }
        return new NotificationData("", "", "");
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String nullableString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
