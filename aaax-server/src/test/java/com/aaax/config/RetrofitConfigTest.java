package com.aaax.config;

import com.aaax.core.utils.RedisUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import retrofit2.converter.jackson.JacksonConverterFactory;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RetrofitConfigTest {

    @Mock private RedisUtil redisUtil;

    @InjectMocks
    private RetrofitConfig retrofitConfig;

    @Test
    @DisplayName("getFactory should create jackson converter")
    void getFactory_shouldCreateConverter() {
        JacksonConverterFactory factory = retrofitConfig.getFactory();
        assertNotNull(factory);
    }

    @Test
    @DisplayName("discordApiClient should build retrofit client")
    void discordApiClient_shouldBuild() {
        assertNotNull(retrofitConfig.discordApiClient());
    }

    @Test
    @DisplayName("utilApiClient should build retrofit client")
    void utilApiClient_shouldBuild() {
        assertNotNull(retrofitConfig.utilApiClient("https://util.test/"));
    }

    @Test
    @DisplayName("idvApiClient should build retrofit client")
    void idvApiClient_shouldBuild() {
        assertNotNull(retrofitConfig.idvApiClient("https://idv.test/"));
    }

    @Test
    @DisplayName("uaaApiClient should build retrofit client")
    void uaaApiClient_shouldBuild() {
        assertNotNull(retrofitConfig.uaaApiClient("https://uaa.test/"));
    }

    @Test
    @DisplayName("onboardingApiClient should build retrofit client")
    void onboardingApiClient_shouldBuild() {
        assertNotNull(retrofitConfig.onboardingApiClient("https://onboarding.test/"));
    }

    @Test
    @DisplayName("tenantApiClient should build retrofit client")
    void tenantApiClient_shouldBuild() {
        assertNotNull(retrofitConfig.tenantApiClient("https://tenant.test/"));
    }

    @Test
    @DisplayName("profileApiClient should build retrofit client")
    void profileApiClient_shouldBuild() {
        assertNotNull(retrofitConfig.profileApiClient(
                "https://profile.test/", "https://uaa.test/", "client", "secret"));
    }
}
