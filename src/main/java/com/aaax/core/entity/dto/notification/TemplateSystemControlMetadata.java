package com.aaax.core.entity.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TemplateSystemControlMetadata {
    private String name;
    private Boolean isEnabled;
}