package com.aaax.server.config;

import com.aaax.core.api.Jackson3ConverterFactory;
import com.aaax.core.utils.RedisUtil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RetrofitConfigTest {

    @Mock
    private RedisUtil redisUtil;

    @InjectMocks
    private RetrofitConfig retrofitConfig;

    @Test
    @DisplayName("getFactory should create jackson converter")
    void getFactory_shouldCreateConverter() {
        Jackson3ConverterFactory factory = retrofitConfig.getFactory();
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
    @DisplayName("utilApiClient blank url uses disabled placeholder")
    void utilApiClient_blank_shouldBuild() {
        assertNotNull(retrofitConfig.utilApiClient(""));
    }

    @Test
    @DisplayName("aaaxApiClient should build retrofit client")
    void aaaxApiClient_shouldBuild() {
        assertNotNull(retrofitConfig.aaaxApiClient("https://aaax.test/"));
    }
}
