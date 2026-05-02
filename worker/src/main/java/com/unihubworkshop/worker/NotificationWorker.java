package com.unihubworkshop.worker;

import com.unihubworkshop.worker.config.RabbitConfig;
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
            System.out.println(channel.getType());
            this.channels.put(channel.getType(), channel);
        }
    }

    @RabbitListener(queues = RabbitConfig.NOTIFICATION_QUEUE)
    public void handle(NotificationRequest request) {
        System.out.println("NOTIFICATION CALL");
        NotificationChannel channel = channels.get(request.getType());
        System.out.println(request.getType());
        System.out.println(channel);
        if (channel != null) {
            System.out.println("Notification request received: " + request.getData());
            channel.send(request.getData());
        }
    }
}
