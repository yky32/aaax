package com.aaax.server.entity.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserAuthenticationCheckRequestDto {
    private String username;
    private String credentials;
    private boolean isEncrypted = false;
}
