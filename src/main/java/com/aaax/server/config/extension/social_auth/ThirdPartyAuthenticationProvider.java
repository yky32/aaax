package com.aaax.server.config.extension.social_auth;

import com.aaax.core.common.AppContext;
import com.aaax.core.common.AppContextHolder;
import com.aaax.core.constant.enu.LoginType;
import com.aaax.core.exception.BizException;
import com.aaax.core.utils.JSONUtil;
import com.aaax.server.config.extension.BaseAuthenticationProvider;
import com.aaax.server.config.extension.GrantTypeExtension;
import com.aaax.server.entity.po.user.User;
import com.aaax.server.exception.response.UaaErrorResponse;
import com.aaax.server.service.AuthenticationService;
import com.aaax.server.usecase.SocialAuthenticationUseCase;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.LazyInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.util.Assert;

import java.security.Principal;
import java.time.Instant;
import java.util.*;


@Slf4j
public class ThirdPartyAuthenticationProvider extends BaseAuthenticationProvider implements AuthenticationProvider {
    private static final String ERROR_URI = "https://datatracker.ietf.org/doc/html/rfc6749#section-5.2";
    private final OAuth2AuthorizationService authorizationService;
    private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;
    private final AuthenticationManager authenticationManager;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private SocialAuthenticationUseCase socialAuthenticationUseCase;

    @Autowired
    public ThirdPartyAuthenticationProvider(OAuth2AuthorizationService authorizationService,
                                            OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator,
                                            AuthenticationManager authenticationManager
    ) {
        Assert.notNull(authorizationService, "authorizationService cannot be null");
        Assert.notNull(tokenGenerator, "tokenGenerator cannot be null");
        this.authorizationService = authorizationService;
        this.tokenGenerator = tokenGenerator;
        this.authenticationManager = authenticationManager;
    }

    private static OAuth2ClientAuthenticationToken getAuthenticatedClientElseThrowInvalidClient(Authentication authentication) {
        OAuth2ClientAuthenticationToken clientPrincipal = null;
        if (OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication.getPrincipal().getClass())) {
            clientPrincipal = (OAuth2ClientAuthenticationToken) authentication.getPrincipal();
        }
        if (clientPrincipal != null && clientPrincipal.isAuthenticated()) {
            return clientPrincipal;
        }
        throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Instant startTrafficDt = Instant.now();
        ThirdPartyAuthenticationToken thirdPartyAuthentication = (ThirdPartyAuthenticationToken) authentication;

        // Ensure the client is authenticated
        OAuth2ClientAuthenticationToken clientPrincipal = getAuthenticatedClientElseThrowInvalidClient(thirdPartyAuthentication);
        RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();

        // Ensure the client is configured to use this authorization grant type
        if (!registeredClient.getAuthorizationGrantTypes().contains(thirdPartyAuthentication.getGrantType())) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT);
        }

        // __ verify the scopes here
        Set<String> authorizedScopes = new HashSet<>(); // ==== logic for [scopes]
        authorizedScopes.add("read:guest");
        authorizedScopes.add(OidcScopes.OPENID);

        // __ Validate the username & credentials parameter, verify the token
        String idToken = ((ThirdPartyAuthenticationToken) authentication).getIdToken();
        String provider = ((ThirdPartyAuthenticationToken) authentication).getProvider();
        String deviceType = ((ThirdPartyAuthenticationToken) authentication).getDeviceType();

        // ============= series of checking for granting token to [user].
        // === 1. check username password
        // === 2. check over [session id] vs [user id]
        User user;
        LoginType _provider = LoginType.get(provider);
        switch (_provider) {
            case GOOGLE:
                user = socialAuthenticationUseCase.loginByGoogleOauth(idToken, deviceType);
                break;
            case APPLE:
                user = socialAuthenticationUseCase.loginByAppleOauth(idToken, deviceType);
                break;
            default:
                throw new BizException(UaaErrorResponse.UAA0401, Map.of("reason", "Unsupported Provider", "provider", thirdPartyAuthentication.getProvider()));
        }


        Long userId = user.getId();
        // ___ We would like to proceed [upsert concept]
        // ___ Check over the [token is existed] first.
        OAuth2Authorization existedOAuth2Authorization = this.authorizationService.findById(String.valueOf(userId));
        if (existedOAuth2Authorization != null) {
            OAuth2AccessTokenAuthenticationToken token = new OAuth2AccessTokenAuthenticationToken(
                    registeredClient,
                    clientPrincipal,
                    existedOAuth2Authorization.getAccessToken().getToken(),
                    Objects.requireNonNull(existedOAuth2Authorization.getRefreshToken()).getToken(),
                    Map.of()
            );
            super.post_login_event(userId, GrantTypeExtension.THIRD_PARTY_OAUTH_GRANT.getKey(), startTrafficDt,
                    JSONUtil.convertFromObject(authentication, Map.class), token);
            return token;
        }

        Authentication authenticate = new ThirdPartyAuthenticationToken(idToken, provider, deviceType, authentication, null);
        // __ take the refer from OAuth2AuthorizationCodeAuthenticationProvider
        DefaultOAuth2TokenContext.Builder tokenContextBuilder = DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal(authenticate)
                .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                .authorizedScopes(authorizedScopes)
                .authorizationGrantType(thirdPartyAuthentication.getGrantType())
                .authorizationGrant(thirdPartyAuthentication)
                .put("userId", userId)
                .put("identifier", user.getUsername())
                .put("extReferenceMap", getUserMetaData(user));

        // Initialize the OAuth2Authorization
        OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.withRegisteredClient(registeredClient)
                .authorizedScopes(authorizedScopes) // put down the scopes
                .principalName(user.getUsername())// the current authorized username
                .attribute(Principal.class.getName(), authenticate) // set current authorized user info
                .authorizationGrantType(thirdPartyAuthentication.getGrantType());

        // ----- Access token -----
        OAuth2TokenContext tokenContext = tokenContextBuilder.tokenType(OAuth2TokenType.ACCESS_TOKEN).build(); // ** key
        OAuth2Token generatedAccessToken = this.tokenGenerator.generate(tokenContext);
        if (generatedAccessToken == null) {
            OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR, "The token generator failed to generate the access token.", ERROR_URI);
            throw new OAuth2AuthenticationException(error);
        }
        if (log.isTraceEnabled()) {
            log.trace("Generated access token");
        }
        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, generatedAccessToken.getTokenValue(), generatedAccessToken.getIssuedAt(), generatedAccessToken.getExpiresAt(), tokenContext.getAuthorizedScopes());
        if (generatedAccessToken instanceof ClaimAccessor) {
            authorizationBuilder.token(accessToken, (metadata) ->
                    metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, ((ClaimAccessor) generatedAccessToken).getClaims()));
        } else {
            authorizationBuilder.accessToken(accessToken);
        }

        // ----- Refresh token -----
        OAuth2RefreshToken refreshToken = null;
        if (GrantTypeExtension.allowsRefresh(registeredClient) &&
                // Do not issue refresh token to public client
                !clientPrincipal.getClientAuthenticationMethod().equals(ClientAuthenticationMethod.NONE)) {

            tokenContext = tokenContextBuilder.tokenType(OAuth2TokenType.REFRESH_TOKEN).build(); // ** key
            OAuth2Token generatedRefreshToken = this.tokenGenerator.generate(tokenContext);
            if (!(generatedRefreshToken instanceof OAuth2RefreshToken)) {
                OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR, "The token generator failed to generate the refresh token.", ERROR_URI);
                throw new OAuth2AuthenticationException(error);
            }
            if (log.isTraceEnabled()) {
                log.trace("Generated refresh token");
            }
            refreshToken = (OAuth2RefreshToken) generatedRefreshToken;
            authorizationBuilder.refreshToken(refreshToken);
        }

        // ----- ID token -----
        OidcIdToken oidcIdToken;
        if (authorizedScopes.contains(OidcScopes.OPENID)) {
            tokenContext = tokenContextBuilder.tokenType(new OAuth2TokenType(OidcParameterNames.ID_TOKEN))
                    // ID token customizer may need access to the access token and/or refresh token
                    .authorization(authorizationBuilder.build()).build();
            OAuth2Token generatedIdToken = this.tokenGenerator.generate(tokenContext);
            if (!(generatedIdToken instanceof Jwt)) {
                OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR, "The token generator failed to generate the ID token.", ERROR_URI);
                throw new OAuth2AuthenticationException(error);
            }
            if (log.isTraceEnabled()) {
                log.trace("Generated userId token");
            }
            oidcIdToken = new OidcIdToken(generatedIdToken.getTokenValue(), generatedIdToken.getIssuedAt(), generatedIdToken.getExpiresAt(), ((Jwt) generatedIdToken).getClaims());
            authorizationBuilder.token(oidcIdToken, (metadata) ->
                    metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, oidcIdToken.getClaims()));
        } else {
            oidcIdToken = null;
        }


        Map<String, Object> additionalParameters = new HashMap<>(1);
        if (oidcIdToken != null) {
            // put in idToken
            additionalParameters.put(OidcParameterNames.ID_TOKEN, oidcIdToken.getTokenValue());
        }


        // ___ Save the OAuth2Authorization => new generate [TOKEN]
        OAuth2Authorization authorization = authorizationBuilder.build();
        this.authorizationService.save(authorization);
        OAuth2AccessTokenAuthenticationToken token = new OAuth2AccessTokenAuthenticationToken(
                registeredClient, clientPrincipal, accessToken, refreshToken, additionalParameters);
        super.post_login_event(userId, GrantTypeExtension.THIRD_PARTY_OAUTH_GRANT.getKey(), startTrafficDt,
                JSONUtil.convertFromObject(authentication, Map.class), token);
        return token;
    }

    private void setupAppContext() {
        AppContext appContext = AppContext.builder()
                .userId("9527")
                .build();
        AppContextHolder.CONTEXT.set(appContext);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ThirdPartyAuthenticationToken.class.isAssignableFrom(authentication);
    }


    // ==== util method
    private void sessionCheck() {

    }

    private Map<String, Object> getUserMetaData(User user) {
        try {
            if (user.getMetadata() != null) {
                if (user.getMetadata().getExtReferenceMap() != null) {
                    return user.getMetadata().getExtReferenceMap();
                }
            }
        } catch (LazyInitializationException e) {
            log.info("--- Fail to get user metadata. Reason: {}", e.getMessage());
        }
        return new HashMap();
    }
}
