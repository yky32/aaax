package com.aaax.server.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "aaax.security")
public class AaaxSecurityProperties {

    /** Failed password checks before {@link com.aaax.server.service.AuthenticationService#check} refuses login. */
    private int maxLoginAttempts = 5;

    /** Applied when creating/updating passwords if system config has no regex list. */
    private List<String> passwordPatterns = new ArrayList<>(List.of(".{8,}"));
}
