package com.example.unihubworkshop.features.workshop.data.datasource;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface CheckinApi {
    @POST("api/sync")
    Call<Void> syncCheckins(@Body List<CheckinEvent> events);
}
