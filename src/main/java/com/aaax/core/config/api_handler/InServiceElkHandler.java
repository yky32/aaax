package com.aaax.core.config.api_handler;

import java.util.ArrayList;
import java.util.List;

import com.aaax.core.api.ApiClient;
import com.aaax.core.api.DiscordApiClient;
import com.aaax.core.api.DiscordWebhookSupport;
import com.aaax.core.exception.BizException;
import com.aaax.core.response.R;
import com.aaax.core.response.Response;
import com.aaax.core.response.Result;
import com.aaax.core.response.SystemResponse;
import com.aaax.core.utils.JSONUtil;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Optional Discord mirror of exception payloads. No-op when webhook not configured.
 */
@Component
@Slf4j
public class InServiceElkHandler implements ApiClient<String, Result<Object>> {

    @Value("${ext.api.client.elk.webhookId:}")
    private String webhookId;

    @Value("${ext.api.client.elk.webhookToken:}")
    private String webhookToken;

    private final DiscordApiClient discordApiClient;
    private final List<Response> byPassResponses;

    public InServiceElkHandler(DiscordApiClient discordApiClient) {
        this.discordApiClient = discordApiClient;
        this.byPassResponses = new ArrayList<>();
    }

    @Override
    public void executeOnly(Result<Object> result) {
        if (result == null) {
            result = R.fail(new BizException(
                    SystemResponse.SYS9998,
                    "This Exception throw to ELK handler [incomplete]. Plz do some code review......."));
        }

        if (result.getResponse() != null
                && HttpStatus.MOVED_PERMANENTLY.equals(result.getResponse().getHttpStatus())) {
            log.info("--- Redirection for client ---> @ {}", result);
            return;
        }

        try {
            log.info("--- exception sink --- {}", result.getResponse());
            if (checkResponseIsElk(result.getResponse())) {
                return;
            }
            String content = "```" + JSONUtil.writeValue(result) + "```";
            DiscordWebhookSupport.sendSafe(
                    discordApiClient, webhookId, webhookToken, "AAAX", content);
        } catch (Exception exception) {
            log.error("--- exception sink failed --- {}", exception.getMessage());
        }
    }

    private boolean checkResponseIsElk(Response response) {
        if (response == null) {
            return false;
        }
        for (Response byPassResponse : byPassResponses) {
            if (response.equals(byPassResponse)) {
                return true;
            }
        }
        return false;
    }
}
