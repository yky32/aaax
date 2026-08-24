package com.aaax.oauth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.aaax.core.exception.BizException;
import com.aaax.exception.response.UaaErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URL;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
@Slf4j
public class AppleIdTokenVerifier {

    private static final String APPLE_JWKS_URL = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    private final List<String> audiences;

    public AppleIdTokenVerifier(
            @Value("${oauth-provider.apple.ios.client-id:}") String iosClientId,
            @Value("${oauth-provider.apple.web.client-id:}") String webClientId
    ) {
        this.audiences = Stream.of(iosClientId, webClientId)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    public AppleIdTokenClaims verify(String idToken) {
        if (!StringUtils.hasText(idToken)) {
            throw new BizException(UaaErrorResponse.UAA0401, Map.of("provider", "Apple", "error", "idToken is blank"));
        }
        if (audiences.isEmpty()) {
            throw new BizException(UaaErrorResponse.UAA0401, Map.of("provider", "Apple", "error", "No Apple client-id configured"));
        }

        try {
            JWKSource<SecurityContext> keySource = new RemoteJWKSet<>(new URL(APPLE_JWKS_URL));
            JWSKeySelector<SecurityContext> keySelector =
                    new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource);
            ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
            jwtProcessor.setJWSKeySelector(keySelector);

            JWTClaimsSet claims = jwtProcessor.process(idToken, null);
            validateIssuer(claims);
            validateAudience(claims);

            String sub = claims.getSubject();
            if (!StringUtils.hasText(sub)) {
                throw new BizException(UaaErrorResponse.UAA0401, Map.of("provider", "Apple", "error", "sub is missing"));
            }

            Map<String, Object> appleIdTokenClaimsJson = new LinkedHashMap<>();
            appleIdTokenClaimsJson.put("iss", claims.getIssuer());
            appleIdTokenClaimsJson.put("aud", claims.getAudience());
            appleIdTokenClaimsJson.put("sub", claims.getSubject());
            appleIdTokenClaimsJson.put("exp", claims.getExpirationTime());
            appleIdTokenClaimsJson.put("iat", claims.getIssueTime());
            appleIdTokenClaimsJson.put("auth_time", claims.getClaim("auth_time"));
            appleIdTokenClaimsJson.put("email", claims.getStringClaim("email"));
            appleIdTokenClaimsJson.put("email_verified", claims.getBooleanClaim("email_verified"));
            appleIdTokenClaimsJson.put("is_private_email", claims.getBooleanClaim("is_private_email"));
            appleIdTokenClaimsJson.put("nonce_supported", claims.getBooleanClaim("nonce_supported"));
            log.info("Apple id_token claims verified: {}", appleIdTokenClaimsJson);

            return AppleIdTokenClaims.builder()
                    .sub(sub)
                    .email(claims.getStringClaim("email"))
                    .emailVerified(claims.getBooleanClaim("email_verified"))
                    .isPrivateEmail(claims.getBooleanClaim("is_private_email"))
                    .build();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Apple id_token verification failed", e);
            throw new BizException(UaaErrorResponse.UAA0401, Map.of("provider", "Apple", "error", e.getMessage()));
        }
    }

    private void validateIssuer(JWTClaimsSet claims) throws ParseException {
        if (!APPLE_ISSUER.equals(claims.getIssuer())) {
            throw new BizException(UaaErrorResponse.UAA0401, Map.of(
                    "provider", "Apple",
                    "error", "invalid issuer: " + claims.getIssuer()
            ));
        }
    }

    private void validateAudience(JWTClaimsSet claims) {
        List<String> tokenAudiences = claims.getAudience();
        if (tokenAudiences == null || tokenAudiences.isEmpty()) {
            throw new BizException(UaaErrorResponse.UAA0401, Map.of("provider", "Apple", "error", "aud is missing"));
        }
        boolean matched = tokenAudiences.stream().anyMatch(audiences::contains);
        if (!matched) {
            throw new BizException(UaaErrorResponse.UAA0401, Map.of(
                    "provider", "Apple",
                    "error", "aud does not match configured Apple client-id"
            ));
        }
    }
}
