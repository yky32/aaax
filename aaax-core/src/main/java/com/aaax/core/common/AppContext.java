package com.aaax.core.common;

import com.aaax.core.common.jsonfield.LogContextMetadata;
import com.aaax.core.common.jsonfield.RequestContextMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppContext {
    // request metadata
    private RequestContextMetadata requestContext;
    private LogContextMetadata logContext;
    // app related data
    private String userId;
}
