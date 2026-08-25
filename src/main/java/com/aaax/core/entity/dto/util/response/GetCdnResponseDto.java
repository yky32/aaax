package com.aaax.core.entity.dto.util.response;

import com.aaax.core.entity.dto.ImageLink;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetCdnResponseDto {
    private String id;
    private ImageLink link;
    /** true = PublicRead object ACL; false = Private. */
    private Boolean isPublic;
}
