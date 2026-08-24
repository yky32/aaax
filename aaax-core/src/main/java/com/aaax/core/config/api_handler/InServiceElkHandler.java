package com.aaax.core.config.api_handler;


import com.aaax.core.api.ApiClient;
import com.aaax.core.api.DiscordApiClient;
import com.aaax.core.api.dto.DiscordWebhookMessage;
import com.aaax.core.exception.BizException;
import com.aaax.core.response.R;
import com.aaax.core.response.Response;
import com.aaax.core.response.Result;
import com.aaax.core.response.SystemResponse;
import com.aaax.core.utils.JSONUtil;
import com.aaax.core.utils.RetrofitCallHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InServiceElkHandler implements ApiClient<String, Result<Object>> {

    @Value("${ext.api.client.elk.webhookId}")
    private String webhookId;
    @Value("${ext.api.client.elk.webhookToken}")
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
            result = R.fail(new BizException(SystemResponse.SYS9998, "This Exception throw to ELK handler [incomplete]. Plz do some code review......."));
        }

        if (result.getResponse().getHttpStatus().equals(HttpStatus.MOVED_PERMANENTLY)) {
            log.info("--- Redirection for client ---> @ {}", result);
            return; // quick return
        }

        try {
            log.info("--- ELK --- {}", result.getResponse());
            // for Discord
            String content = JSONUtil.writeValue(result);
            DiscordWebhookMessage message = DiscordWebhookMessage.builder()
                    .username("ELK - Police")
                    .content("```".concat(content).concat("```"))
                    .build();

            boolean isByPassELK = this.checkResponseIsElk(result.getResponse());
            if (isByPassELK) {
                return;
            }
            RetrofitCallHandler._void_execute(discordApiClient.sendWebhookMessage(message, webhookId, webhookToken));
            log.info("--- ELK END --- {}", result.getResponse());
        } catch (Exception exception) {
            DiscordWebhookMessage message = DiscordWebhookMessage.builder()
                    .username("ELK - Police")
                    .content("```".concat("The Error was in [InServiceElkHandler.catch] block of code. plz check.").concat("```"))
                    .build();
            RetrofitCallHandler._void_execute(discordApiClient.sendWebhookMessage(message, webhookId, webhookToken));
            log.error("--- ELK END --- {}", result.getResponse());
        }
    }

    private boolean checkResponseIsElk(Response response) {
        for (Response byPassResponse : byPassResponses) {
            if (response.equals(byPassResponse)) {
                return true;
            }
        }
        return false;
    }
}
