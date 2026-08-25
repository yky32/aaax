package com.aaax.server.config.security;

import com.aaax.core.response.R;
import com.aaax.core.response.SystemResponse;
import com.aaax.core.utils.JSONUtil;
import com.aaax.core.utils.RedisUtil;
import com.aaax.core.utils.handler.EndpointHandler;
import com.aaax.server.config.extension.CustomOAuth2RefreshTokenGenerator;
import com.aaax.server.config.extension.CustomOAuth2TokenGenerator;
import com.aaax.server.config.extension.GrantTypeExtension;
import com.aaax.server.config.extension.custom_password.CustomPasswordAuthenticationConverter;
import com.aaax.server.config.extension.custom_password.CustomPasswordAuthenticationProvider;
import com.aaax.server.config.extension.custom_password_e.CustomPasswordEncryptedAuthenticationConverter;
import com.aaax.server.config.extension.custom_password_e.CustomPasswordEncryptedAuthenticationProvider;
import com.aaax.server.config.extension.custom_refresh_token.CustomRefreshTokenAuthenticationConverter;
import com.aaax.server.config.extension.custom_refresh_token.CustomRefreshTokenAuthenticationProvider;
import com.aaax.server.config.extension.customcode.CustomCodeGrantAuthenticationConverter;
import com.aaax.server.config.extension.customcode.CustomCodeGrantAuthenticationProvider;
import com.aaax.server.config.extension.ext_password.ExtPasswordAuthenticationConverter;
import com.aaax.server.config.extension.ext_password.ExtPasswordAuthenticationProvider;
import com.aaax.server.config.extension.social_auth.ThirdPartyAuthenticationConverter;
import com.aaax.server.config.extension.social_auth.ThirdPartyAuthenticationProvider;
import com.aaax.server.config.security.jwt.ClientCredentialsJwt;
import com.aaax.server.config.security.jwt.Jwt;
import com.aaax.server.exception.GlobalExceptionHandler;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServerConfig {

    public static final String KEY_ALIAS = "bael-oauth-jwt";
    public static final String KEY_STORE_FILE = "jwk/bael-jwt.jks";
    public static final String KEY_STORE_PASSWORD = "bael-pass";
    private final static String[] byPassUris = {
            "/actuator/**",
            "/v3/**",
            "/swagger-ui/**",
            "/keys/public-keys",
            "/ws/**"
    };
    private final HttpServletRequest request;
    private final GlobalExceptionHandler globalExceptionHandler;
    @Value("${spring.security.issuer}")
    private String issuerUrl;
    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;
    @Value("${spring.security.server.expiry-time}")
    private Integer serverTokenExpiryTime;
    @Autowired
    private RedisUtil redisUtil;

    /**
     * This Filter is to focus on `/oauth2/token`
     *
     * @param http
     * @return
     * @throws Exception
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerFilterChain(HttpSecurity http) throws Exception {
        // default setting, bypass oauth/token csrf
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        AuthenticationManager authenticationManager = http.getSharedObject(AuthenticationManager.class);
        // GRANT_TYPE converter 和 GRANT_TYPE provider

        // ===== CUSTOM CODE
        CustomCodeGrantAuthenticationConverter customCodeGrantAuthenticationConverter = new CustomCodeGrantAuthenticationConverter();
        CustomCodeGrantAuthenticationProvider customCodeGrantAuthenticationProvider = new CustomCodeGrantAuthenticationProvider(
                authorizationService(), tokenGenerator(), authenticationManager
        );

        // ===== CUSTOM PASSWORD
        CustomPasswordAuthenticationConverter customPasswordAuthenticationConverter = new CustomPasswordAuthenticationConverter();
        CustomPasswordAuthenticationProvider customPasswordAuthenticationProvider = new CustomPasswordAuthenticationProvider(
                authorizationService(), tokenGenerator(), authenticationManager
        );

        // ===== CUSTOM PASSWORD E
        CustomPasswordEncryptedAuthenticationConverter customPasswordEncryptedAuthenticationConverter = new CustomPasswordEncryptedAuthenticationConverter();
        CustomPasswordEncryptedAuthenticationProvider customPasswordEncryptedAuthenticationProvider = new CustomPasswordEncryptedAuthenticationProvider(
                authorizationService(), tokenGenerator(), authenticationManager
        );

        // ===== CUSTOM REFRESH TOKEN
        CustomRefreshTokenAuthenticationConverter customRefreshTokenAuthenticationConverter = new CustomRefreshTokenAuthenticationConverter();
        CustomRefreshTokenAuthenticationProvider customRefreshTokenAuthenticationProvider = new CustomRefreshTokenAuthenticationProvider(
                authorizationService(), tokenGenerator(), authenticationManager
        );

        // ===== CUSTOM EXT PASSWORD
        ExtPasswordAuthenticationConverter extPasswordAuthenticationConverter = new ExtPasswordAuthenticationConverter();
        ExtPasswordAuthenticationProvider extPasswordAuthenticationProvider = new ExtPasswordAuthenticationProvider(
                authorizationService(), tokenGenerator(), authenticationManager
        );

        // ===== SOCIAL AUTH
        ThirdPartyAuthenticationConverter thirdPartyAuthenticationConverter = new ThirdPartyAuthenticationConverter();
        ThirdPartyAuthenticationProvider thirdPartyAuthenticationProvider = new ThirdPartyAuthenticationProvider(
                authorizationService(), tokenGenerator(), authenticationManager
        );


        AuthenticationSuccessHandler myAuthenticationSuccessHandler = (request, response, authentication) -> {
            OAuth2AccessTokenAuthenticationToken token = (OAuth2AccessTokenAuthenticationToken) authentication;
            long expires_in = Objects.requireNonNull(token.getAccessToken().getExpiresAt()).getEpochSecond() - (System.currentTimeMillis() / 1000);
            if (request.getParameter("grant_type").equals(AuthorizationGrantType.CLIENT_CREDENTIALS.getValue())) {
                ClientCredentialsJwt jwt = ClientCredentialsJwt.builder()
                        .accessToken(token.getAccessToken().getTokenValue())
                        .build();
                EndpointHandler.out(response, response.getStatus(), R.success(jwt));
            } else {
                Jwt jwt = Jwt.builder()
                        .accessToken(token.getAccessToken().getTokenValue())
                        .refreshToken(token.getRefreshToken() != null ? token.getRefreshToken().getTokenValue() : "--NA")
                        .expiresIn(expires_in)
                        .tokenType(OAuth2AccessToken.TokenType.BEARER.getValue())
                        .build();
                EndpointHandler.out(response, response.getStatus(), R.success(jwt));
            }
        };

        // customize the return
        AuthenticationFailureHandler authenticationFailureHandler = (
                request,
                response,
                exception
        ) -> {
            OAuth2Error oAuth2Error = ((OAuth2AuthenticationException) exception).getError();
            Map errorResponse = JSONUtil.convertFromObject(oAuth2Error, Map.class);
            try {
                errorResponse.put("detail", exception.getMessage());
            } catch (Exception e) {
            }
            EndpointHandler.out(response, HttpStatus.UNAUTHORIZED.value(), R.fail(SystemResponse.SAU0400, errorResponse));
        };

        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                // set authorization server customized grantTypeCode in metadata
                .authorizationServerMetadataEndpoint(
                        metadata -> metadata.authorizationServerMetadataCustomizer(
                                customizer -> customizer
                                        .grantType(GrantTypeExtension.CUSTOM_PASSWORD_GRANT.getKey())
                                        .grantType(GrantTypeExtension.CUSTOM_PASSWORD_GRANT_ENCRYPTED.getKey())
                                        .grantType(GrantTypeExtension.CUSTOM_CODE_GRANT.getKey())
                                        .grantType(GrantTypeExtension.CUSTOM_REFRESH_TOKEN.getKey())
                                        .grantType(GrantTypeExtension.THIRD_PARTY_OAUTH_GRANT.getKey())
                        ))
                // add custom grant_type here
                .tokenEndpoint(tokenEndpoint -> tokenEndpoint
                        .accessTokenRequestConverter(customPasswordAuthenticationConverter)
                        .authenticationProvider(customPasswordAuthenticationProvider)
                        .accessTokenRequestConverter(customPasswordEncryptedAuthenticationConverter)
                        .authenticationProvider(customPasswordEncryptedAuthenticationProvider)
                        .accessTokenRequestConverter(customRefreshTokenAuthenticationConverter)
                        .authenticationProvider(customRefreshTokenAuthenticationProvider)
                        .accessTokenRequestConverter(customCodeGrantAuthenticationConverter)
                        .authenticationProvider(customCodeGrantAuthenticationProvider)
                        .accessTokenRequestConverter(extPasswordAuthenticationConverter)
                        .authenticationProvider(extPasswordAuthenticationProvider)
                        .accessTokenRequestConverter(thirdPartyAuthenticationConverter)
                        .authenticationProvider(thirdPartyAuthenticationProvider)
                        .accessTokenResponseHandler(myAuthenticationSuccessHandler) // final return to client side
                        .errorResponseHandler(authenticationFailureHandler) // final custom to client side
                )
        ;

        http.exceptionHandling(c ->
                // __ Redirect to the login page when not authenticated from the
                // __ authorization endpoint
                c.defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"),
                        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                )

        ).oauth2ResourceServer(c ->
                c.jwt(x -> x.jwkSetUri(jwkSetUri))
        );
        return http.build();
    }


    @Bean
    CorsFilter corsFilter() {
        var config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("*")); // try
        config.setAllowCredentials(true);
        config.addAllowedHeader("*");
        config.addExposedHeader("*");
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    @Bean
    @Order(1)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults());
        http.formLogin(Customizer.withDefaults());
        http.csrf(AbstractHttpConfigurer::disable); // __ Prevent 403

        http.authorizeHttpRequests(az -> az
                        .requestMatchers(byPassUris).permitAll()
                        .requestMatchers(HttpMethod.POST, "/ext/users").permitAll() // # for register-user external no-otp

                        // # for forgot-password
                        .requestMatchers(HttpMethod.POST, "/users/credentials/reset", "/users/credentials/reset/validations").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/users/credentials/reset/one-time-passwords").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/users/credentials").permitAll()
                        // # for forgot-password END

                        // # for register-user
                        .requestMatchers(HttpMethod.POST, "/users/registrations", "/users", "/users/verifications").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/users/registrations/one-time-passwords").permitAll()
                        // # for register-user END

                        .requestMatchers(HttpMethod.POST, "/authentications/one-time-passwords/**").permitAll() // # for gen otp
                        .requestMatchers(HttpMethod.PUT, "/authentications/one-time-passwords/**").permitAll() // # for re-gen otp
                        .requestMatchers(HttpMethod.GET, "/ws/devices/**").permitAll() // # for device ws
                        .requestMatchers(HttpMethod.POST, "/webhooks", "/webhooks/**").permitAll() // # temporarily bypass only
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> {
                    exception.authenticationEntryPoint(this.globalExceptionHandler::authenticationDenied);
                    exception.accessDeniedHandler(this.globalExceptionHandler::accessDenied);
                })
                .oauth2ResourceServer(oauth2 -> {
                    oauth2.jwt(x -> x.jwkSetUri(jwkSetUri));
                    oauth2.authenticationEntryPoint(this.globalExceptionHandler::authenticationDenied);
                    oauth2.accessDeniedHandler(this.globalExceptionHandler::accessDenied);
                });

        return http.build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        List<RegisteredClient> clients = new ArrayList<>();
        // REMINDER: for generating the insert SQL of registered client via programmatically
//        RegisteredClient registeredClient = RegisteredClient
//                .withId("760cc5ca-b513-4ce9-9e89-185ccbe1a403") // FIXED ID
//                .clientId("client")
//                .clientSecret(passwordEncoder().encode("secret")) // # https://www.base64encode.org/ [client:secret]
//                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
//                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
//                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.CUSTOM_CODE_GRANT.getKey()))
//                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.CUSTOM_PASSWORD_GRANT.getKey()))
//                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.CUSTOM_REFRESH_TOKEN.getKey()))
//                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.EXT_PASSWORD_GRANT.getKey()))
//                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.THIRD_PARTY_OAUTH_GRANT.getKey()))
//                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
//                .redirectUri("http://insomina")
//                .scope(OidcScopes.OPENID)
//                .tokenSettings(TokenSettings.builder()
//                        .accessTokenTimeToLive(Duration.ofSeconds(tokenExpiryTime))
//                        .refreshTokenTimeToLive(Duration.ofSeconds(refreshTokenExpiryTime))
//                        .reuseRefreshTokens(false)
//                        .build()
//                )
//                .build();
//        clients.add(registeredClient);
//
//        RegisteredClient thirdPartyClient = RegisteredClient
//                .withId("94314e9c-c71e-46fd-9ac0-b93c4d636c15") // FIXED ID
//                .clientId("third-party")
//                .clientSecret(passwordEncoder().encode("secret")) // # https://www.base64encode.org/ [client:secret]
//                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
//                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
//                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.CUSTOM_CODE_GRANT.getKey()))
//                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.CUSTOM_PASSWORD_GRANT.getKey()))
//                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.CUSTOM_REFRESH_TOKEN.getKey()))
//                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.EXT_PASSWORD_GRANT.getKey()))
//                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.THIRD_PARTY_OAUTH_GRANT.getKey()))
//                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
//                .redirectUri("http://insomina") // change later
//                .scope(OidcScopes.OPENID)
//                .tokenSettings(TokenSettings.builder()
//                        .accessTokenTimeToLive(Duration.ofSeconds(tokenExpiryTime))
//                        .refreshTokenTimeToLive(Duration.ofSeconds(refreshTokenExpiryTime))
//                        .reuseRefreshTokens(false)
//                        .build()
//                )
//                .build();
//        clients.add(thirdPartyClient);
//
//        RegisteredClient mobileClient = RegisteredClient
//                .withId("1110c4c6-4a6b-429f-b04d-ce035db75123") // FIXED ID
//                .clientId("mobile-client")
//                .clientSecret(passwordEncoder().encode("secret")) // # https://www.base64encode.org/ [mobile-client:secret]
//                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
//                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
//                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.CUSTOM_CODE_GRANT.getKey()))
//                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.CUSTOM_PASSWORD_GRANT.getKey()))
//                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.CUSTOM_REFRESH_TOKEN.getKey()))
//                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.EXT_PASSWORD_GRANT.getKey()))
//                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
//                .scope(OidcScopes.OPENID)
//                .redirectUri("http://insomina") // change later
//                .tokenSettings(TokenSettings.builder()
//                        .accessTokenTimeToLive(Duration.ofSeconds(tokenExpiryTime))
//                        .refreshTokenTimeToLive(Duration.ofSeconds(refreshTokenExpiryTime))
//                        .reuseRefreshTokens(false)
//                        .build()
//                )
//                .build();
//        clients.add(mobileClient);
//
//        RegisteredClient serverClient = RegisteredClient
//                .withId("7f5295c6-5ac2-400a-b215-cde166237c8c") // FIXED ID
//                .clientId("server")
//                .clientSecret(passwordEncoder().encode("secret")) // # https://www.base64encode.org/ [server:secret]
//                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
//                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
//                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
//                .tokenSettings(TokenSettings.builder()
//                        .accessTokenTimeToLive(Duration.ofSeconds(serverTokenExpiryTime))     //TODO
//                        .build()
//                )
//                .build();
//        clients.add(serverClient);

        // return new RedisRegisteredClientRepository(redisUtil, registeredClient);  // JDBC
        JdbcRegisteredClientRepository jdbcRegisteredClientRepository = new JdbcRegisteredClientRepository(jdbcTemplate);
        for (RegisteredClient client : clients) {
            jdbcRegisteredClientRepository.save(client);
        }
        return jdbcRegisteredClientRepository;
    }

    @Bean
    public OAuth2AuthorizationService authorizationService() {
        return new RedisOAuth2AuthorizationService();
    }

    @SneakyThrows
    @Bean
    public OAuth2TokenGenerator<?> tokenGenerator() {
        CustomOAuth2TokenGenerator accessTokenGenerator = new CustomOAuth2TokenGenerator(new NimbusJwtEncoder(jwkSource()), request);
        CustomOAuth2RefreshTokenGenerator customOAuth2RefreshTokenGenerator = new CustomOAuth2RefreshTokenGenerator();
        return new DelegatingOAuth2TokenGenerator(accessTokenGenerator, customOAuth2RefreshTokenGenerator);
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.authenticationProvider(authenticationProvider());
        return authenticationManagerBuilder.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(passwordEncoder());
        provider.setUserDetailsService(userDetailsService());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("--------------- its configured bean passwordEncoder");
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new JwtUserDetailsService();
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        // __ Default Configuration of Authorization Server Settings
        // __ it provides a way to configure various settings related to the Authorization Server.
        return AuthorizationServerSettings.builder()
                .issuer(issuerUrl)
                .build();
    }

    public KeyPair keyPair() {
        // TODO: can be migrated to AWS.kms , Azure.keyVault
        ClassPathResource ksFile = new ClassPathResource(KEY_STORE_FILE);
        KeyStoreKeyFactory ksFactory = new KeyStoreKeyFactory(ksFile, KEY_STORE_PASSWORD.toCharArray());
        return ksFactory.getKeyPair(KEY_ALIAS);
    }

    private RSAKey generateRsa(KeyPair keyPair) {
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID("aaax")
                .keyUse(KeyUse.SIGNATURE)
                .build();
    }

    @Bean
    @SuppressWarnings("unused")
    public JWKSource<SecurityContext> jwkSource() {
        JWKSet jwkSet = new JWKSet(generateRsa(keyPair()));
        return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
    }
}
