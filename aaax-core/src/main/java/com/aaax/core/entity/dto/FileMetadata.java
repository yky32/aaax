package com.aaax.core.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileMetadata {
    private String gid; // util CDN file id (GetCdnResponseDto.id, e.g. cdn_{snowflake})
    private String fileName;
    /** Durable public CDN URL when access is PUBLIC; null for private course media. */
    private String url;
    /** Short-lived playable URL for private objects (Spaces origin). Prefer over {@link #url}. */
    private String signedUrl;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", locale = "en_US", timezone = "UTC")
    private Instant expiredAt;
    private String size;
    private String lastUpdateDt;
    private Map metadata;
    private String mimeType;
}