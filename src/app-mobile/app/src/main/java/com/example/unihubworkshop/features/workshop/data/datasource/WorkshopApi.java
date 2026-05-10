package com.example.unihubworkshop.features.workshop.data.datasource;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface WorkshopApi {
    @GET("api/workshops")
    Call<List<WorkshopResponseDto>> getWorkshops();

    @GET("api/workshops/{id}")
    Call<WorkshopResponseDto> getWorkshopById(@Path("id") String id);
}
