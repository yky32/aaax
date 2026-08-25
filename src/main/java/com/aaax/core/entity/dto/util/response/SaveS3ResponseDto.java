package com.aaax.core.entity.dto.util.response;

import com.aaax.core.entity.dto.ImageLink;
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
public class SaveS3ResponseDto {
    private String id;
    private ImageLink link;
}