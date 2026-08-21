package com.aaax.client;

import com.aaax.config.SecurityConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class DemoClientSeed implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoClientSeed.class);

    private final RegisteredClientRepository clients;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;

    public DemoClientSeed(
            RegisteredClientRepository clients,
            PasswordEncoder passwordEncoder,
            @Value("${aaax.demo.seed-client:true}") boolean enabled) {
        this.clients = clients;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        if (clients.findByClientId("aaax-demo") == null) {
            clients.save(SecurityConfig.demoClient(passwordEncoder));
            log.info("Seeded OAuth client aaax-demo");
        }
        if (clients.findByClientId("aaax-spa") == null) {
            clients.save(SecurityConfig.spaPublicClient());
            log.info("Seeded public SPA client aaax-spa (PKCE)");
        }
    }
}
