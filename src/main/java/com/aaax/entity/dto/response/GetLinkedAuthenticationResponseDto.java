package com.aaax.entity.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One linked sign-in method for the current user (settings / account security UI).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetLinkedAuthenticationResponseDto {
    /** {@code EMAIL}, {@code GOOGLE}, {@code APPLE}, … */
    private String loginType;
    /** Raw auth identifier (email, Apple {@code sub}, etc.). */
    private String identifier;
    /**
     * Best-effort email for UI (Google identifier, account email for Apple {@code sub}, etc.).
     */
    private String displayEmail;
    /** Always true for rows returned from GET linked list. */
    @JsonProperty("isLinked")
    private boolean isLinked;
    /** Whether the client may call DELETE for this method. */
    @JsonProperty("isAbleToUnlink")
    private boolean isAbleToUnlink;
}
