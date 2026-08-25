package com.aaax.core.config;


import com.aaax.core.config.prop.AppApiProp;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final AppApiProp appApiProp;

    @Bean(name = "utility-service")
    public WebClient utilityServiceWebClient() {
        String uri = Optional.ofNullable(appApiProp)
                .map(AppApiProp::getExt)
                .map(ext -> ext.get("utility-service"))
                .map(AppApiProp.ApiProp::getUri)
                .orElse("http://utility-service.default.svc.cluster.local:9000");
        return WebClient.builder()
                .baseUrl(uri)
                .build();
    }
}
