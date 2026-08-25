package com.aaax.core.utils;

import com.aaax.core.api.UaaApiClient;
import com.aaax.core.exception.BizException;
import com.aaax.core.response.SystemResponse;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/** Plz take this util class for retrieve any JWT related information
 * - jwt user id
 * - jwt scope
 * - jwt expired Time etc.
 */
@Slf4j
public class JwtUtil {
    public static final String USER_ID = "userId";
    public static final String SCOPE = "scope";
    public static final String METADATA = "metadata";

    public final static Map<String, String> JWT_KEY_VALUE_MAPPING = Map.of(
            USER_ID, "sub",
            SCOPE, SCOPE,
            METADATA, METADATA
    );

    public static JwtAuthenticationToken mySecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        if (!(authentication instanceof JwtAuthenticationToken)) {
            return null;
        }

        return (JwtAuthenticationToken) authentication;
    }

    public static boolean isJwt(String token) {
        try {
            Jwts.parser().parseClaimsJwt(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public static Jwt myJwt() {
        if (mySecurityContext() != null) {
            log.info("-- JwtUtil.myJwt() has value {}", mySecurityContext().getToken());
            return mySecurityContext().getToken();
        }
        log.info("-- JwtUtil.myJwt() == null.");
        return null;
    }

    public static Object getFromJwt(String key) {
        Map<String, Object> claims = new HashMap<>();
        if (myJwt() != null) {
            claims = myJwt().getClaims();
        }
        Object value = null;
        if (!claims.isEmpty()) {
            value = claims.get(JWT_KEY_VALUE_MAPPING.get(key));
        }

        if (value == null) {
            return key.concat("_not_existed");
        }

        if (value instanceof String) {
            return value;
        } else if (value instanceof ArrayList) {
            return value;
        } else if (value instanceof Map) {
            return value;
        } else if (value instanceof Instant) {
            return value;
        } else {
            throw new BizException(SystemResponse.SYS9999, "myJwt().getClaims() got issue.");
        }
    }

    /**
     * OAuth2 {@code sub} for the current request JWT (same claim as {@link #USER_ID} in {@link #JWT_KEY_VALUE_MAPPING}).
     * Returns {@code "0"} when there is no JWT or {@code sub} is absent/blank.
     */
    public static String userId() {
        Jwt jwt = myJwt();
        if (jwt == null) {
            return "0";
        }
        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            return "0";
        }
        return sub;
    }

    public static com.aaax.core.api.Jwt login(UaaApiClient uaaApiClient, String authorization, String grantType, String username, String credentials) {
        com.aaax.core.api.Jwt jwt = RetrofitCallHandler.execute(uaaApiClient.oauth2Login(authorization, grantType, username, credentials));
        jwt.setBearerToken(jwt.getTokenType().concat(" ").concat(jwt.getAccessToken()));
        return jwt;
    }
}
