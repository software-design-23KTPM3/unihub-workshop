package com.example.unihubworkshop.features.workshop.data.datasource;

import retrofit2.Call;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface RegistrationApi {
    @POST("api/registrations")
    Call<RegistrationResponseDto> createRegistration(
            @Header("Idempotency-Key") String idempotencyKey,
            @Body RegistrationRequestDto request
    );

    @GET("api/registrations/{id}")
    Call<RegistrationDetailResponseDto> getRegistrationById(@Path("id") String id);

    @GET("api/registrations/{id}/qr.png")
    Call<ResponseBody> getRegistrationQrImage(@Path("id") String id);

    @GET("api/me/registrations")
    Call<java.util.List<RegistrationDetailResponseDto>> getMyRegistrations();

    @GET("api/admin/registrations")
    Call<java.util.List<RegistrationDetailResponseDto>> getRegistrationsByWorkshop(
        @retrofit2.http.Query("workshopId") String workshopId,
        @retrofit2.http.Query("status") String status
    );
}
