package com.aaax.config.extension;

import com.aaax.core.utils.KafkaUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaseAuthenticationProviderTest {

    @Mock
    private KafkaUtil kafkaUtil;

    private BaseAuthenticationProvider provider;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        provider = new BaseAuthenticationProvider() {};
        ReflectionTestUtils.setField(provider, "kafkaUtil", kafkaUtil);
    }

    @Test
    @DisplayName("removeSensitiveInformation should strip credentials fields")
    void removeSensitiveInformation_shouldStrip() {
        Map<String, Object> map = new HashMap<>();
        map.put("username", "u");
        map.put("credentials", "secret");
        map.put("additionalParameters", Map.of("x", 1));

        Map result = ReflectionTestUtils.invokeMethod(provider, "removeSensitiveInformation", map);

        assertFalse(result.containsKey("credentials"));
        assertFalse(result.containsKey("additionalParameters"));
        assertEquals("u", result.get("username"));
    }

    @Test
    @DisplayName("post_login_event should publish kafka event")
    void postLoginEvent_shouldPublish() {
        Map<String, Object> map = new HashMap<>();
        map.put("username", "u");
        map.put("credentials", "secret");

        ReflectionTestUtils.invokeMethod(provider, "post_login_event",
                1L, "password", Instant.now(), map, Map.of("accessToken", "t"));

        verify(kafkaUtil).send(anyString(), any());
        assertFalse(map.containsKey("credentials"));
    }
}
