package com.aaax.entity.dto.json_context;

import lombok.*;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class RegisterOtpMetadata extends OtpMetadata{
    private Boolean isVerified;
}
