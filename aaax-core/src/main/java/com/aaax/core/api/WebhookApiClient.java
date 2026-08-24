package com.aaax.core.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface WebhookApiClient {
    @POST
    Call<Void> send(@Url String url, @Body Object payload, @Header("Authorization") String authToken);

    @POST
    Call<Void> send(@Url String url, @Body Object payload, @Header("Authorization") String authToken, @Header("x-api-key") String apiKey);
}
