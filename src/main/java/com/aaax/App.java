package com.aaax;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration;

/**
 * Boot entry (qs/uaa style: {@code App}).
 * <p>Redis is only wired when {@code aaax.otp.store=redis} and/or {@code aaax.qr.store=redis}
 * ({@link com.aaax.config.RedisTokenStoreConfig}). Default memory stores need no broker.
 */
@SpringBootApplication(exclude = {
        DataRedisAutoConfiguration.class,
        DataRedisRepositoriesAutoConfiguration.class
})
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
