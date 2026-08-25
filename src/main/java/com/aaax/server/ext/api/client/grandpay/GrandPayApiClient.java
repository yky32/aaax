package com.aaax.server.ext.api.client.grandpay;

import com.aaax.core.response.Result;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

import java.util.Map;

public interface GrandPayApiClient {

    @FormUrlEncoded
    @POST("sso/token-exchange")
    Call<Result<Map<String, Object>>> tokenExchange(
            @Field("code") String code,
            @Field("client_id") String clientId,
            @Field("state") String state
    );
}

