package com.aaax.server.entity.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Link an additional login method to the current user.
 * <ul>
 *   <li><b>Social:</b> {@code provider} + {@code idToken} (google / apple)</li>
 *   <li><b>Legacy password-style:</b> {@code username} + {@code credentials}
 *       (detects EMAIL / MOBILE / USERNAME)</li>
 * </ul>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddLinkedAuthenticationRequestDto {
    /** Social provider: {@code google} or {@code apple}. */
    private String provider;
    /** Google / Apple ID token from the client SDK. */
    private String idToken;

    /** Legacy: identifier for password-based link (email / mobile). */
    private String username;
    /** Legacy: password for password-based link. */
    private String credentials;
}
