package com.unihubworkshop.worker.notification;

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

    public NotificationData() {
    }

    public NotificationData(String title, String msg, String to) {
        this(title, msg, to, null);
    }

    public NotificationData(String title, String msg, String to, String qrPayload) {
        this(title, msg, to, qrPayload, null);
    }

    public NotificationData(String title, String msg, String to, String qrPayload, String qrImageBase64) {
        this(title, msg, to, qrPayload, qrImageBase64, null, null, null, null);
    }

    public NotificationData(
            String title,
            String msg,
            String to,
            String qrPayload,
            String qrImageBase64,
            String workshopTitle,
            String workshopTime,
            String workshopRoom,
            String workshopSpeaker) {
        this.title = title;
        this.msg = msg;
        this.to = to;
        this.qrPayload = qrPayload;
        this.qrImageBase64 = qrImageBase64;
        this.workshopTitle = workshopTitle;
        this.workshopTime = workshopTime;
        this.workshopRoom = workshopRoom;
        this.workshopSpeaker = workshopSpeaker;
    }

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
}
