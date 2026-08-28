package com.aaax.core.entity.dto.aaax.response;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GetUserResponseDtoTest {

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    @Test
    void deserializesIsActiveField() {
        String json = """
                {
                  "id": "u_1",
                  "username": "user@example.com",
                  "isActive": true
                }
                """;

        GetUserResponseDto dto = objectMapper.readValue(json, GetUserResponseDto.class);

        assertEquals("u_1", dto.getId());
        assertEquals("user@example.com", dto.getUsername());
        assertEquals(true, dto.getIsActive());
    }

    @Test
    void ignoresUnknownFields() {
        String json = """
                {
                  "id": "u_2",
                  "username": "other@example.com",
                  "futureField": "ignored"
                }
                """;

        GetUserResponseDto dto = objectMapper.readValue(json, GetUserResponseDto.class);

        assertNotNull(dto);
        assertEquals("u_2", dto.getId());
    }
}
