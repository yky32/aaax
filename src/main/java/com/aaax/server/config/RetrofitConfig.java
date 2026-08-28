package com.aaax.server.config;

import java.util.concurrent.TimeUnit;

import com.aaax.core.api.BaseRetrofitInClusterInterceptor;
import com.aaax.core.api.DiscordApiClient;
import com.aaax.core.api.Jackson3ConverterFactory;
import com.aaax.core.api.AaaxApiClient;
import com.aaax.core.api.UtilApiClient;
import com.aaax.core.utils.RedisUtil;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;

/**
 * HTTP clients. Tenant / IDV / GrandPay / Onboarding / Profile removed for OSS.
 */
@Configuration
public class RetrofitConfig {

    private static final String DISABLED_BASE = "http://127.0.0.1:1/";

    @Autowired
    private RedisUtil redisUtil;

    private final HttpLoggingInterceptor logging =
            new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC);

    @Bean
    public Jackson3ConverterFactory getFactory() {
        return Jackson3ConverterFactory.create(JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build());
    }

    private static String baseUrl(String endpoint) {
        if (endpoint == null || endpoint.isBlank() || endpoint.contains("127.0.0.1:1")) {
            return DISABLED_BASE;
        }
        return endpoint.endsWith("/") ? endpoint : endpoint + "/";
    }

    private OkHttpClient.Builder http() {
        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(logging);
    }

    @Bean
    public AaaxApiClient aaaxApiClient(@Value("${ext.api.client.aaax-svc.url:}") String endpoint) {
        Retrofit build = new Retrofit.Builder()
                .baseUrl(baseUrl(endpoint))
                .client(http().build())
                .addConverterFactory(getFactory())
                .build();
        return build.create(AaaxApiClient.class);
    }

    @Bean
    public DiscordApiClient discordApiClient() {
        Retrofit build = new Retrofit.Builder()
                .baseUrl("https://discord.com/")
                .client(http().build())
                .addConverterFactory(Jackson3ConverterFactory.create())
                .build();
        return build.create(DiscordApiClient.class);
    }

    @Bean
    public UtilApiClient utilApiClient(@Value("${ext.api.client.util-svc.url:}") String endpoint) {
        Retrofit build = new Retrofit.Builder()
                .baseUrl(baseUrl(endpoint))
                .client(http().addInterceptor(new BaseRetrofitInClusterInterceptor()).build())
                .addConverterFactory(getFactory())
                .build();
        return build.create(UtilApiClient.class);
    }
}
