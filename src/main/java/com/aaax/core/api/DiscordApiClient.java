package com.aaax.core.api;

import com.aaax.core.api.dto.DiscordWebhookMessage;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface DiscordApiClient {

    @POST("api/webhooks/{webhookId}/{webhookToken}")
    Call<Void> sendWebhookMessage(
            @Body DiscordWebhookMessage message,
            @Path("webhookId") String webhookId,
            @Path("webhookToken") String webhookToken
    );
}
