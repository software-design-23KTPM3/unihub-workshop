package com.unihubworkshop.worker;

import com.unihubworkshop.worker.notification.NotificationChannel;
import com.unihubworkshop.worker.notification.NotificationRequest;
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
            this.channels.put(channel.getType(), channel);
        }
    }

    @RabbitListener(queues = "notification.queue")
    public void handle(NotificationRequest request) {
        NotificationChannel channel = channels.get(request.getType());
        if (channel != null) {
            channel.send(request.getData());
        }
    }
}
