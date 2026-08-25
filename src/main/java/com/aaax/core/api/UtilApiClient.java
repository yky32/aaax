package com.aaax.core.api;

import com.aaax.core.entity.dto.util.request.CreateFeatureFlagRequestDto;
import com.aaax.core.entity.dto.util.request.PutFeatureFlagRequestDto;
import com.aaax.core.entity.dto.util.response.GetCdnResponseDto;
import com.aaax.core.entity.dto.util.response.GetFeatureFlagResponseDto;
import com.aaax.core.entity.dto.util.response.GetPreSignedCdnResponseDto;
import com.aaax.core.entity.dto.util.response.GetRefDataResponseDto;
import com.aaax.core.response.Result;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface UtilApiClient {

    @Multipart
    @POST("cdn/files")
    Call<Result<List<GetCdnResponseDto>>> upload(@Part("files") MultipartBody.Part[] files);

    @GET("cdn/files/pre-signed")
    Call<Result<List<GetPreSignedCdnResponseDto>>> generatePreSignedUrlByKeys(
            @Query("keys") List<String> keys,
            @Query("ttlInSeconds") Long ttlInSeconds
    );

    @Multipart
    @POST("cdn/files/path/pre-signed")
    Call<Result<List<GetPreSignedCdnResponseDto>>> uploadForPreSignedUrl(
            @Part("files") MultipartBody.Part[] files,
            @Query("path") String path,
            @Query("ttlInSeconds") Long ttlInSeconds
    );

    @Multipart
    @POST("cdn/files")
    Call<Result<List<GetCdnResponseDto>>> upload(
            @Query("path") String path,
            @Query("isPublic") Boolean isPublic,
            @Part MultipartBody.Part files
    );

    @Multipart
    @POST("cdn/files")
    Call<Result<List<GetCdnResponseDto>>> upload(
            @Query("path") String path,
            @Part MultipartBody.Part files
    );

    @Multipart
    @POST("cdn/files/customize-upload")
    Call<Result<List<GetCdnResponseDto>>> uploadCustomize(
            @Part("files") MultipartBody.Part[] files,
            @Part("custom-type") RequestBody customType,
            @Part("feature") RequestBody feature,
            @Part("service-domain") RequestBody serviceDomain,
            @Part("resource") RequestBody resource,
            @Part("use-case") RequestBody useCase
    );

    @POST("feature-flags")
    Call<Result<GetFeatureFlagResponseDto>> createFeatureFlags(@Body CreateFeatureFlagRequestDto dto);

    @GET("feature-flags/{owner}/{key}")
    Call<Result<GetFeatureFlagResponseDto>> getFeatureFlags(@Path(value = "owner") String owner,
                                                            @Path(value = "key") String key);

    @GET("feature-flags/{owner}/{key}")
    Call<Result<GetFeatureFlagResponseDto>> updateFeatureFlags(
            @Path(value = "owner") String owner,
            @Path(value = "key") String key,
            @Body PutFeatureFlagRequestDto updateDto
    );

    @GET("ref-data/key/{key}")
    Call<Result<GetRefDataResponseDto>> getRefDataByKey(@Path(value = "key") String key);
}
