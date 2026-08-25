package com.aaax.core.api;

import com.aaax.core.common.PushSettingDto;
import com.aaax.core.entity.dto.uaa.response.GetKeysResponseDto;
import com.aaax.core.entity.dto.uaa.response.GetMyRolesResponseDto;
import com.aaax.core.entity.dto.uaa.response.GetUserMetricsResponseDto;
import com.aaax.core.entity.dto.uaa.response.GetUserPreferenceResponseDto;
import com.aaax.core.entity.dto.uaa.response.GetUserProfileResponseDto;
import com.aaax.core.entity.dto.uaa.response.GetUserResponseDto;
import com.aaax.core.response.Result;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;
import java.util.Map;

public interface UaaApiClient {

    @FormUrlEncoded
    @POST("oauth2/token")
    Call<Result<Jwt>> oauth2Login(
            @Header("Authorization") String authorization,
            @Field("grant_type") String grantType,
            @Field("username") String username,
            @Field("credentials") String credentials
    );

    @GET("user-profiles/my-profiles")
    Call<Result<GetUserProfileResponseDto>> getMyProfile(
            @Query("a") List<String> a
    );

    @GET("user-profiles/users/{id}/my-profiles")
    Call<Result<GetUserProfileResponseDto>> getUserProfile(
            @Path(value = "id") String id,
            @Query("a") List<String> a
    );

    @GET("users/{id}")
    Call<Result<GetUserResponseDto>> getUserById(@Path(value = "id") String id);

    @GET("users/{id}")
    Call<Result<GetUserResponseDto>> getUserWithAspectsById(
            @Path(value = "id") String id,
            @Query("a") List<String> a,
            @Query("ss") String ss
    );

    @GET("users/{id}/my-metrics")
    Call<Result<GetUserMetricsResponseDto>> getMyMetricsByUserId(
            @Path(value = "id") String id,
            @Query("ss") String ss
    );

    @GET("users/{id}")
    Call<Result<GetUserResponseDto>> getUserById(
            @Path(value = "id") String id,
            @Query("identifierType") String identifierType
    );

    @GET("keys/public-keys")
    Call<Result<GetKeysResponseDto>> publicKeys();

    @GET("keys/private-keys")
    Call<Result<GetKeysResponseDto>> privateKeys();

    @GET("users")
    Call<Result<List<GetUserResponseDto>>> getUser();

    @GET("users")
    Call<Result<List<GetUserResponseDto>>> getUser(
            @Query("query") String query
    );

    @GET("users")
    Call<Result<List<GetUserResponseDto>>> getUser(
            @Query("ids") List<String> userIds
    );

    @GET("users/me")
    Call<Result<GetUserResponseDto>> me();

    @GET("users/my-roles")
    Call<Result<GetMyRolesResponseDto>> getMyRoles();

    @GET("user-engagements/my-push-data")
    Call<Result<PushSettingDto>> myPushData();

    @GET("user-engagements/{userId}/my-push-data")
    Call<Result<PushSettingDto>> userPushData(@Path(value = "userId") String userId);

    @GET("user-preferences/my-preferences")
    Call<Result<GetUserPreferenceResponseDto>> getMyUserPreference(@Query("key") String key);

    @GET("user-preferences/users/{userId}/my-preferences")
    Call<Result<GetUserPreferenceResponseDto>> getUserPreference(
            @Path(value = "userId") String userId,
            @Query("key") String key
    );

    @Multipart
    @PATCH("user-customizations/avatar")
    Call<Result<GetUserProfileResponseDto>> updateAvatar(@Part List<MultipartBody.Part> files);

    @PATCH("user-customizations/avatar-urls")
    Call<Result<GetUserProfileResponseDto>> updateAvatarUrl(@Body Map<String, Object> dto);

    @DELETE("mgt/users/id/{id}")
    Call<Result<String>> deleteUserById(
            @Path("id") String id,
            @Query("isSoftDelete") Boolean isSoftDelete
    );
}
