package com.aaax.server;

import com.aaax.core.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class Initialization implements CommandLineRunner {

    private final RedisUtil redisUtil;

    @Override
    public void run(String... args) {
        this.doResetRedis();
    }

    private void doResetRedis() {
        log.info("--- [{}] resetRedis start", this.getClass().getSimpleName());
    }
}
