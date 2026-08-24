package com.aaax.config;

import com.aaax.core.api.BaseRetrofitInClusterInterceptor;
import com.aaax.core.api.DiscordApiClient;
import com.aaax.core.api.UaaApiClient;
import com.aaax.core.api.UtilApiClient;
import com.aaax.core.utils.RedisUtil;
import com.aaax.ext.api.client.grandpay.GrandPayApiClient;
import com.aaax.ext.api.client.idv.IdvApiClient;
import com.aaax.ext.api.client.onboarding.OnboardingApiClient;
import com.aaax.ext.api.client.profile.ProfileApiClient;
import com.aaax.ext.api.client.tenant.TenantApiClient;
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

import java.util.concurrent.TimeUnit;

@Configuration
public class RetrofitConfig {

    @Autowired
    private RedisUtil redisUtil;

    HttpLoggingInterceptor logging = new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY);

    @Bean
    public JacksonConverterFactory getFactory() {
        ObjectMapper objectMapper = new ObjectMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule(); // Register the JavaTimeModule for Java 8 date/time types
        objectMapper.registerModule(javaTimeModule);
        return JacksonConverterFactory.create(objectMapper);
    }

    @Bean
    public IdvApiClient idvApiClient(
            @Value("${ext.api.client.idv-svc.url}")
            String endpoint
    ) {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(600, TimeUnit.SECONDS) // Set connection timeout
                .readTimeout(600, TimeUnit.SECONDS) // Set read timeout
                .writeTimeout(600, TimeUnit.SECONDS) // Set write timeout
                .addInterceptor(logging)
                .addInterceptor(new BaseRetrofitInClusterInterceptor())
                .build();
        Retrofit build = new Retrofit.Builder()
                .baseUrl(endpoint)
                .client(httpClient)
                .addConverterFactory(getFactory())
                .build();
        return build.create(IdvApiClient.class);
    }

    @Bean
    public UaaApiClient uaaApiClient(
            @Value("${ext.api.client.uaa-svc.url}")
            String endpoint
    ) {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(600, TimeUnit.SECONDS) // Set connection timeout
                .readTimeout(600, TimeUnit.SECONDS) // Set read timeout
                .writeTimeout(600, TimeUnit.SECONDS) // Set write timeout
                .addInterceptor(logging)
                // REMOVE BaseRetrofitInClusterInterceptor becoz its no login a valid. JWT forward for calling UAA
                .build();
        Retrofit build = new Retrofit.Builder()
                .baseUrl(endpoint)
                .client(httpClient)
                .addConverterFactory(getFactory())
                .build();
        return build.create(UaaApiClient.class);
    }


    @Bean
    public OnboardingApiClient onboardingApiClient(
            @Value("${ext.api.client.onboarding-svc.url}")
            String endpoint
    ) {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(600, TimeUnit.SECONDS) // Set connection timeout
                .readTimeout(600, TimeUnit.SECONDS) // Set read timeout
                .writeTimeout(600, TimeUnit.SECONDS) // Set write timeout
                .addInterceptor(logging)
                .addInterceptor(new BaseRetrofitInClusterInterceptor())
                .build();
        Retrofit build = new Retrofit.Builder()
                .baseUrl(endpoint)
                .client(httpClient)
                .addConverterFactory(getFactory())
                .build();
        return build.create(OnboardingApiClient.class);
    }

    @Bean
    public TenantApiClient tenantApiClient(
            @Value("${ext.api.client.tenant-svc.url}")
            String endpoint
    ) {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(600, TimeUnit.SECONDS) // Set connection timeout
                .readTimeout(600, TimeUnit.SECONDS) // Set read timeout
                .writeTimeout(600, TimeUnit.SECONDS) // Set write timeout
                .addInterceptor(logging)
                .addInterceptor(new BaseRetrofitInClusterInterceptor())
                .build();
        Retrofit build = new Retrofit.Builder()
                .baseUrl(endpoint)
                .client(httpClient)
                .addConverterFactory(getFactory())
                .build();
        return build.create(TenantApiClient.class);
    }

    @Bean
    public ProfileApiClient profileApiClient(
            @Value("${ext.api.client.profile-svc.url}")
            String endpoint,
            @Value("${ext.api.client.uaa-svc.url}")
            String uaaEndpoint,
            @Value("${ext.api.client.uaa-svc.client-id}")
            String clientId,
            @Value("${ext.api.client.uaa-svc.client-secret}")
            String clientSecret
    ) {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(600, TimeUnit.SECONDS) // Set connection timeout
                .readTimeout(600, TimeUnit.SECONDS) // Set read timeout
                .writeTimeout(600, TimeUnit.SECONDS) // Set write timeout
                .addInterceptor(logging)
                .addInterceptor(new BaseRetrofitInClusterInterceptor(true, uaaEndpoint, clientId, clientSecret, redisUtil))
                .build();
        Retrofit build = new Retrofit.Builder()
                .baseUrl(endpoint)
                .client(httpClient)
                .addConverterFactory(getFactory())
                .build();
        return build.create(ProfileApiClient.class);
    }

    @Bean
    public DiscordApiClient discordApiClient() {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();
        String endpoint = "https://discord.com/";
        Retrofit build = new Retrofit.Builder()
                .baseUrl(endpoint)
                .client(httpClient)
                .addConverterFactory(JacksonConverterFactory.create())
                .build();
        return build.create(DiscordApiClient.class);
    }

    @Bean
    public UtilApiClient utilApiClient(
            @Value("${ext.api.client.util-svc.url}")
            String endpoint
    ) {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(600, TimeUnit.SECONDS) // Set connection timeout
                .readTimeout(600, TimeUnit.SECONDS) // Set read timeout
                .writeTimeout(600, TimeUnit.SECONDS) // Set write timeout
                .addInterceptor(logging)
                .addInterceptor(new BaseRetrofitInClusterInterceptor())
                .build();
        Retrofit build = new Retrofit.Builder()
                .baseUrl(endpoint)
                .client(httpClient)
                .addConverterFactory(getFactory())
                .build();
        return build.create(UtilApiClient.class);
    }
}
