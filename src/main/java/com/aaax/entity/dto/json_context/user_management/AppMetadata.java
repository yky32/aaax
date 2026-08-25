package com.aaax.entity.dto.json_context.user_management;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class AppMetadata {
    private String lastIp;
    private String lastLoginDt; // Consider using LocalDateTime for better date handling
    private Integer loginsCount;
}
