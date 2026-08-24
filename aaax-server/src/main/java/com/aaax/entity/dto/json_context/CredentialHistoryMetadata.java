package com.aaax.entity.dto.json_context;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class CredentialHistoryMetadata {
    private String credentials;
    private String createDt;
    private Instant _createDt;
}
