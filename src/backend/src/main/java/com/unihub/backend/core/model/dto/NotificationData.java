package com.unihub.backend.core.model.dto;

public class NotificationData {
    private String title;
    private String msg;
    private String to;
    private String qrPayload;
    private String qrImageBase64;
    private String workshopTitle;
    private String workshopTime;
    private String workshopRoom;
    private String workshopSpeaker;

    public String getTitle() {
        return title;
    }

    public String getMsg() {
        return msg;
    }

    public String getTo() {
        return to;
    }

    public String getQrPayload() {
        return qrPayload;
    }

    public String getQrImageBase64() {
        return qrImageBase64;
    }

    public String getWorkshopTitle() {
        return workshopTitle;
    }

    public String getWorkshopTime() {
        return workshopTime;
    }

    public String getWorkshopRoom() {
        return workshopRoom;
    }

    public String getWorkshopSpeaker() {
        return workshopSpeaker;
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

        public NotificationDataBuilder qrPayload(String qrPayload) {
            data.qrPayload = qrPayload;
            return this;
        }

        public NotificationDataBuilder qrImageBase64(String qrImageBase64) {
            data.qrImageBase64 = qrImageBase64;
            return this;
        }

        public NotificationDataBuilder workshopTitle(String workshopTitle) {
            data.workshopTitle = workshopTitle;
            return this;
        }

        public NotificationDataBuilder workshopTime(String workshopTime) {
            data.workshopTime = workshopTime;
            return this;
        }

        public NotificationDataBuilder workshopRoom(String workshopRoom) {
            data.workshopRoom = workshopRoom;
            return this;
        }

        public NotificationDataBuilder workshopSpeaker(String workshopSpeaker) {
            data.workshopSpeaker = workshopSpeaker;
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
