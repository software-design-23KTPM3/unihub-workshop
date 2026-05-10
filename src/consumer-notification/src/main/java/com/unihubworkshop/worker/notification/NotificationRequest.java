package com.unihubworkshop.worker.notification;

public class NotificationRequest {
    private String type;
    private NotificationData data;

    public String getType() {
        return type;
    }

    public NotificationData getData() {
        return data;
    }

    public static class NotificationRequestBuilder {
        private NotificationRequest req = new NotificationRequest();

        public NotificationRequestBuilder type(String type) {
            req.type = type;
            return this;
        }

        public NotificationRequestBuilder data(NotificationData data) {
            req.data = data;
            return this;
        }

        public NotificationRequest build() {
            return req;
        }
    }

    public static NotificationRequestBuilder builder() {
        return new NotificationRequestBuilder();
    }

}
