package com.aaax.config.extension;

import com.aaax.core.utils.RandomHashGenerator;
import com.aaax.config.security.jwt.JwtMetadata;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * This class is to include the logic of [JWT Token Generation]
 * @By Wayne.yu
 */
@RequiredArgsConstructor
public class CustomOAuth2TokenGenerator implements OAuth2TokenGenerator {

    private final JwtEncoder jwtEncoder;
    private final HttpServletRequest request;
    private OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer;

    @Override
    public OAuth2Token generate(OAuth2TokenContext context) {
        if (context.getTokenType() == null ||
                (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType()) &&
                        !OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue()))) {
            return null;
        }
        if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType()) &&
                !OAuth2TokenFormat.SELF_CONTAINED.equals(context.getRegisteredClient().getTokenSettings().getAccessTokenFormat())) {
            return null;
        }


        String issuer = null;
        if (context.getAuthorizationServerContext() != null) {
            issuer = context.getAuthorizationServerContext().getIssuer();
        }
        RegisteredClient registeredClient = context.getRegisteredClient();

        Instant issuedAt = Instant.now();
        Instant expiresAt;
        JwsAlgorithm jwsAlgorithm = SignatureAlgorithm.RS256;
        if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
            expiresAt = issuedAt.plus(registeredClient.getTokenSettings().getAccessTokenTimeToLive().getSeconds(), ChronoUnit.SECONDS);
            if (registeredClient.getTokenSettings().getIdTokenSignatureAlgorithm() != null) {
                jwsAlgorithm = registeredClient.getTokenSettings().getIdTokenSignatureAlgorithm();
            }
        } else {
            expiresAt = issuedAt.plus(registeredClient.getTokenSettings().getAccessTokenTimeToLive());
        }

        // @formatter:off
        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder();
        if (StringUtils.hasText(issuer)) {
            claimsBuilder.issuer(issuer);
        }

        // === common fields
        List<String> audience = new ArrayList<>(Collections.singleton(registeredClient.getClientId()));
        audience.add(requestHost());

        claimsBuilder
                .audience(audience)
                .issuedAt(issuedAt);

        if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(context.getAuthorizationGrantType())) {
            // bypass
            claimsBuilder.expiresAt(expiresAt);
            // add subject as client UUID
            claimsBuilder.subject(registeredClient.getId());
        } else {
            String userId = Objects.requireNonNull(context.get("userId")).toString();
            String identifier = Objects.requireNonNull(context.get("identifier")).toString();
            // REMINDER: this is to include JWT content___________________
            JwtMetadata jwtMetadata = JwtMetadata.builder()
                    .identifier(identifier)
                    .sessionId(sessionId())
                    .extReferenceMap(context.get("extReferenceMap"))
                    .build();

            claimsBuilder
                    .subject(userId)
                    .expiresAt(expiresAt)
                    .claim("metadata", jwtMetadata);
        }


        if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
            claimsBuilder.notBefore(issuedAt);
            if (!CollectionUtils.isEmpty(context.getAuthorizedScopes())) {
                claimsBuilder.claim(OAuth2ParameterNames.SCOPE, context.getAuthorizedScopes());
            }

        } else if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
            claimsBuilder.claim(IdTokenClaimNames.AZP, registeredClient.getClientId());
            if (AuthorizationGrantType.AUTHORIZATION_CODE.equals(context.getAuthorizationGrantType())) {
                OAuth2AuthorizationRequest authorizationRequest = context.getAuthorization().getAttribute(
                        OAuth2AuthorizationRequest.class.getName());
                String nonce = (String) authorizationRequest.getAdditionalParameters().get(OidcParameterNames.NONCE);
                if (StringUtils.hasText(nonce)) {
                    claimsBuilder.claim(IdTokenClaimNames.NONCE, nonce);
                }
            }
            SessionInformation sessionInformation = context.get(SessionInformation.class);
            if (sessionInformation != null) {
                claimsBuilder.claim("sid", sessionInformation.getSessionId());
                claimsBuilder.claim(IdTokenClaimNames.AUTH_TIME, sessionInformation.getLastRequest());
            }
        }
        // @formatter:on

        JwsHeader.Builder jwsHeaderBuilder = JwsHeader.with(jwsAlgorithm);

        if (this.jwtCustomizer != null) {
            // @formatter:off
            JwtEncodingContext.Builder jwtContextBuilder = JwtEncodingContext.with(jwsHeaderBuilder, claimsBuilder)
                    .registeredClient(context.getRegisteredClient())
                    .principal(context.getPrincipal())
                    .authorizationServerContext(context.getAuthorizationServerContext())
                    .authorizedScopes(context.getAuthorizedScopes())
                    .tokenType(context.getTokenType())
                    .authorizationGrantType(context.getAuthorizationGrantType());
            if (context.getAuthorization() != null) {
                jwtContextBuilder.authorization(context.getAuthorization());
            }
            if (context.getAuthorizationGrant() != null) {
                jwtContextBuilder.authorizationGrant(context.getAuthorizationGrant());
            }
            if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
                SessionInformation sessionInformation = context.get(SessionInformation.class);
                if (sessionInformation != null) {
                    jwtContextBuilder.put(SessionInformation.class, sessionInformation);
                }
            }
            // @formatter:on

            JwtEncodingContext jwtContext = jwtContextBuilder.build();
            this.jwtCustomizer.customize(jwtContext);
        }

        JwsHeader jwsHeader = jwsHeaderBuilder.build();
        JwtClaimsSet claims = claimsBuilder.build();

        Jwt jwt = this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims));

        return jwt;
    }

    private String requestHost() {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        return scheme + "://" + serverName;
    }

    private String sessionId() {
        return RandomHashGenerator.generateRandomHash(50);
    }
}
