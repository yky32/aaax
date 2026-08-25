package com.aaax.core.utils.jpa;

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
public class JpaSearchFieldMetadata {
    private String field;
    private String jsonPath;
    private Double ratio;
    private JpaSpecificationColumn column;
}