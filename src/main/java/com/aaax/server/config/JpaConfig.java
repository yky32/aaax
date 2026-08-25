package com.aaax.server.config;

import com.aaax.core.utils.StringUtil;
import com.aaax.core.utils.generator.id.SnowflakeIdGeneratorConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JpaConfig {

    @Bean
    public SnowflakeIdGeneratorConfiguration snowflakeIdGeneratorConfiguration(
            @Value("${pod.ip}") String podIp
    ) {
        long machineId = StringUtil.ipToLong(podIp) % 1024;
        return new SnowflakeIdGeneratorConfiguration(Math.toIntExact(machineId), podIp);
    }

}
