package com.unihub.backend.core.model.dto;

public class NotificationData {
    private String title;
    private String msg;
    private String to;

    public String getTitle() {
        return title;
    }

    public String getMsg() {
        return msg;
    }

    public String getTo() {
        return to;
    }

    public static class NotificationDataBuilder {
        private NotificationData data = new NotificationData();

        public NotificationDataBuilder title(String title) {
            data.title = title;
            return this;
        }

        public NotificationDataBuilder msg(String msg) {
            data.msg = msg;
            return this;
        }

        public NotificationDataBuilder to(String to) {
            data.to = to;
            return this;
        }

        public NotificationData build() {
            return data;
        }
    }

    public static NotificationDataBuilder builder() {
        return new NotificationDataBuilder();
    }
}
