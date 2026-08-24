package com.aaax.ext.api.client.tenant;
import com.aaax.core.response.Result;
import com.aaax.entity.dto.request.CreateTenantAccessRequestDto;
import com.aaax.entity.dto.response.GetTenantRoleWithRouteResponseDto;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;
import java.util.Map;

public interface TenantApiClient {

    @GET("users/trr/{trrId}/tenant-context")
    Call<Result<Object>> getTenantContextByTrrId(
            @Path(value = "trrId") Long trrId
    );

    @GET("route-templates")
    Call<Result<Object>> getAllRouteTemplates(
            @Query(value = "name") String name
    );

    @GET("route-templates/{id}")
    Call<Result<Object>> getOneRouteTemplates(
            @Path(value = "id") Long id
    );

    @GET("tenant-role-routes/{id}")
    Call<Result<Object>> getTenantRoleRoute(
            @Path(value = "id") Long id
    );

    @GET("tenant-role-routes/k/{tenantKey}")
    Call<Result<List<GetTenantRoleWithRouteResponseDto>>> getTenantRoleRouteByTenantKey(
            @Path(value = "tenantKey") String tenantKey
    );

    @GET("tenant-role-routes/tenants/{tenantId}")
    Call<Result<List<GetTenantRoleWithRouteResponseDto>>> getTenantRoleRouteByTenantId(
            @Path(value = "tenantId") String tenantId
    );

    @POST("/mgt/tenant-access")
    Call<Result<Map>> createTenantAccess(@Body CreateTenantAccessRequestDto dto);
}
