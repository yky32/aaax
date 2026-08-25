package com.aaax.core.config;

import com.aaax.core.config.prop.AppApiProp;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AppApiProp.class)
public class CoreAutoConfig {
}