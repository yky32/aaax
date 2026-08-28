package com.aaax.server.config;

import com.aaax.core.api.DiscordApiClient;
import com.aaax.core.config.api_handler.InServiceElkHandler;
import com.aaax.core.filter.AppFilter;
import com.aaax.core.security.AaaxAuditAware;
import com.aaax.core.utils.ApplicationContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This class is to centralize all the app configuration bean here.
 */
@Configuration
public class AppConfig {

    @Value("${spring.application.name}")
    private String serviceName;
    @Autowired
    private DiscordApiClient discordApiClient;


    @Bean
    public ApplicationContextUtil applicationContextUtil() {
        return new ApplicationContextUtil();
    }

    @Bean
    public InServiceElkHandler inServiceElkHandler() {
        return new InServiceElkHandler(this.discordApiClient);
    }

    @Bean
    public AppFilter appFilter() {
        return AppFilter.builder()
                .serviceName(serviceName)
                .build();
    }

    @Bean
    public AaaxAuditAware aaaxAuditAware() {
        return AaaxAuditAware.builder()
                .defaultUsername(serviceName)
                .build();
    }
}
