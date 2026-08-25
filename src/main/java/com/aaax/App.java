package com.aaax;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableJpaAuditing
@EnableTransactionManagement
@EnableMethodSecurity // -> for use @PreAuthorize, @PreAuthorize("permitAll()"),
@EntityScan(basePackages = {
        "com.aaax.entity.po",
        "com.aaax.core.entity"
})
public class App {

    // 20260415
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
