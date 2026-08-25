package com.aaax.server.ext.api.client.onboarding;


import com.aaax.core.response.Result;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

import java.util.Map;

/**
 * Temporarily use.
 */
public interface OnboardingApiClient {

    @POST("applications")
    Call<Result<Object>> createOnboardingForm(
            @Header("Authorization") String authorization,
            @Body Map requestDto
    );
}
