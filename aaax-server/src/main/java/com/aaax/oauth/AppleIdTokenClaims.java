package com.aaax.oauth;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AppleIdTokenClaims {
    String sub;
    String email;
    Boolean emailVerified;
    Boolean isPrivateEmail;
}
