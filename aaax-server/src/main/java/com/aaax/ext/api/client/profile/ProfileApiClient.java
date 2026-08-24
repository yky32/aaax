package com.aaax.ext.api.client.profile;

import com.aaax.core.response.Result;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

import java.util.Map;

/**
 * Temporarily use.
 */
public interface ProfileApiClient {

    @POST("uaa-profiles")
    Call<Result<Object>> createPmsProfile(@Body Map requestDto);
}
