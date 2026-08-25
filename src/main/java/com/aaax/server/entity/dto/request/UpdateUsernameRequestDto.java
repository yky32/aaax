package com.aaax.server.entity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateUsernameRequestDto {
    /** New login identifier (email, mobile, or username). */
    @NotBlank
    private String username;
}
