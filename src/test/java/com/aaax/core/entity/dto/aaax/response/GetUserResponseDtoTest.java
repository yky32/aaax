package com.aaax.core.entity.dto.aaax.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GetUserResponseDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesIsActiveField() throws Exception {
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
    void ignoresUnknownFields() throws Exception {
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
