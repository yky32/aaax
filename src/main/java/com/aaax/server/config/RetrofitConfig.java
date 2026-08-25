package com.aaax.server.config;

import java.util.concurrent.TimeUnit;

import com.aaax.core.api.BaseRetrofitInClusterInterceptor;
import com.aaax.core.api.DiscordApiClient;
import com.aaax.core.api.UaaApiClient;
import com.aaax.core.api.UtilApiClient;
import com.aaax.core.utils.RedisUtil;
import com.aaax.server.ext.api.client.idv.IdvApiClient;
import com.aaax.server.ext.api.client.tenant.TenantApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * HTTP clients for optional mesh services.
 * <p>
 * GrandPay / Onboarding / Profile clients were removed for OSS (Quinsic-specific).
 * Tenant / IDV / Util / UAA remain for optional integration; default base URL is a
 * black-hole placeholder when env is unset.
 */
@Configuration
public class RetrofitConfig {

    private static final String DISABLED_BASE = "http://127.0.0.1:1/";

    @Autowired
    private RedisUtil redisUtil;

    private final HttpLoggingInterceptor logging =
            new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC);

    @Bean
    public JacksonConverterFactory getFactory() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return JacksonConverterFactory.create(objectMapper);
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
    public IdvApiClient idvApiClient(@Value("${ext.api.client.idv-svc.url:}") String endpoint) {
        Retrofit build = new Retrofit.Builder()
                .baseUrl(baseUrl(endpoint))
                .client(http().addInterceptor(new BaseRetrofitInClusterInterceptor()).build())
                .addConverterFactory(getFactory())
                .build();
        return build.create(IdvApiClient.class);
    }

    @Bean
    public UaaApiClient uaaApiClient(@Value("${ext.api.client.uaa-svc.url:}") String endpoint) {
        Retrofit build = new Retrofit.Builder()
                .baseUrl(baseUrl(endpoint))
                .client(http().build())
                .addConverterFactory(getFactory())
                .build();
        return build.create(UaaApiClient.class);
    }

    @Bean
    public TenantApiClient tenantApiClient(@Value("${ext.api.client.tenant-svc.url:}") String endpoint) {
        Retrofit build = new Retrofit.Builder()
                .baseUrl(baseUrl(endpoint))
                .client(http().addInterceptor(new BaseRetrofitInClusterInterceptor()).build())
                .addConverterFactory(getFactory())
                .build();
        return build.create(TenantApiClient.class);
    }

    @Bean
    public DiscordApiClient discordApiClient() {
        Retrofit build = new Retrofit.Builder()
                .baseUrl("https://discord.com/")
                .client(http().build())
                .addConverterFactory(JacksonConverterFactory.create())
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
