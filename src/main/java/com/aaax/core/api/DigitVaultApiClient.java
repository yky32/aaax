package com.aaax.core.api;

import com.aaax.core.entity.dto.util.response.GetVaultKeyResponseDto;
import com.aaax.core.response.Result;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

import java.util.Map;

public interface DigitVaultApiClient {

    @GET("keys/public-keys")
    Call<Result<GetVaultKeyResponseDto>> getPublicKeys();

    @POST("encryption")
    Call<Result<String>> doEncryption(@Body Map<String, Object> dto);

    @POST("decryption")
    Call<Result<String>> doDecryption(@Body Map<String, Object> dto);
}
