package com.aaax.core.entity.dto.util.response;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetPreSignedCdnResponseDto extends GetCdnResponseDto {

    private String key;

    private String signedUrl;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", locale = "en_US", timezone = "UTC")
    private Instant expiredAt;

}

