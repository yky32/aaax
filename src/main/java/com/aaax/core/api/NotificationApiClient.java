package com.aaax.core.api;

import com.aaax.core.entity.dto.notification.response.GetNotificationTemplateResponseDto;
import com.aaax.core.response.Result;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Retrofit client for notification-service. Wire with
 * {@code ext.api.client.notification-svc.url} like {@link AaaxApiClient} / {@link UtilApiClient}.
 */
public interface NotificationApiClient {

    @GET("notification-templates/name/{name}")
    Call<Result<GetNotificationTemplateResponseDto>> getByName(@Path("name") String name);
}
