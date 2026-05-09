package com.example.unihubworkshop.features.notification.data;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface NotificationApi {
    @GET("api/notifications/unread")
    Call<List<NotificationDto>> getUnreadNotifications();

    @PUT("api/notifications/{id}/read")
    Call<Void> markAsRead(@Path("id") String id);
}
