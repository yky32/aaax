package com.aaax.server.config.security;

import com.aaax.core.utils.KeystoreKeyPairs;
import com.aaax.core.utils.RedisUtil;
import com.aaax.server.config.extension.CustomOAuth2RefreshTokenGenerator;
import com.aaax.server.config.extension.CustomOAuth2TokenGenerator;
import com.aaax.server.config.extension.GrantTypeExtension;
import com.aaax.server.config.extension.RfcOAuth2TokenHttp;
import com.aaax.server.config.extension.custom_password.CustomPasswordAuthenticationConverter;
import com.aaax.server.config.extension.custom_password.CustomPasswordAuthenticationProvider;
import com.aaax.server.config.extension.custom_password_e.CustomPasswordEncryptedAuthenticationConverter;
import com.aaax.server.config.extension.custom_password_e.CustomPasswordEncryptedAuthenticationProvider;
import com.aaax.server.config.extension.custom_refresh_token.CustomRefreshTokenAuthenticationConverter;
import com.aaax.server.config.extension.custom_refresh_token.CustomRefreshTokenAuthenticationProvider;
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
import org.springframework.http.HttpMethod;
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
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
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

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServerConfig {

    @Value("${aaax.jwk.keystore:}")
    private String jwkKeystorePath;
    @Value("${aaax.jwk.keystore-password:}")
    private String jwkKeystorePassword;
    @Value("${aaax.jwk.keystore-alias:}")
    private String jwkKeystoreAlias;
    private final static String[] byPassUris = {
            "/actuator/**",
            "/v3/**",
            "/swagger-ui/**",
            "/keys/public-keys",
            "/ws/**",
            "/.well-known/**",
            "/oauth2/jwks"
    };
    private final HttpServletRequest request;
    private final GlobalExceptionHandler globalExceptionHandler;
    @Value("${aaax.security.issuer}")
    private String issuerUrl;
    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;
    @Value("${aaax.security.server.expiry-time}")
    private Integer serverTokenExpiryTime;
    @Value("${aaax.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}")
    private String corsOriginPatterns;
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
        OAuth2AuthorizationServerConfigurer authorizationServer = new OAuth2AuthorizationServerConfigurer();
        http.securityMatcher(authorizationServer.getEndpointsMatcher())
                .with(authorizationServer, as -> as.oidc(Customizer.withDefaults()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/.well-known/**", "/oauth2/jwks").permitAll()
                        .anyRequest().authenticated());

        AuthenticationManager authenticationManager = http.getSharedObject(AuthenticationManager.class);

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

        AuthenticationSuccessHandler myAuthenticationSuccessHandler = (request, response, authentication) ->
                RfcOAuth2TokenHttp.writeSuccess(response, (OAuth2AccessTokenAuthenticationToken) authentication);

        AuthenticationFailureHandler authenticationFailureHandler = (request, response, exception) ->
                RfcOAuth2TokenHttp.writeError(response, exception);

        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                // set authorization server customized grantTypeCode in metadata
                .authorizationServerMetadataEndpoint(
                        metadata -> metadata.authorizationServerMetadataCustomizer(
                                customizer -> customizer
                                        .grantType(GrantTypeExtension.CUSTOM_PASSWORD_GRANT.getKey())
                                        .grantType(GrantTypeExtension.CUSTOM_PASSWORD_GRANT_ENCRYPTED.getKey())
                                        .grantType(GrantTypeExtension.CUSTOM_REFRESH_TOKEN.getKey())
                        ))
                // add custom grant_type here
                .tokenEndpoint(tokenEndpoint -> tokenEndpoint
                        .accessTokenRequestConverter(customPasswordAuthenticationConverter)
                        .authenticationProvider(customPasswordAuthenticationProvider)
                        .accessTokenRequestConverter(customPasswordEncryptedAuthenticationConverter)
                        .authenticationProvider(customPasswordEncryptedAuthenticationProvider)
                        .accessTokenRequestConverter(customRefreshTokenAuthenticationConverter)
                        .authenticationProvider(customRefreshTokenAuthenticationProvider)
                        .accessTokenResponseHandler(myAuthenticationSuccessHandler) // final return to client side
                        .errorResponseHandler(authenticationFailureHandler) // final custom to client side
                )
        ;

        http.exceptionHandling(c ->
                c.defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"),
                        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                )
        );
        return http.build();
    }


    @Bean
    CorsFilter corsFilter() {
        var config = new CorsConfiguration();
        List<String> origins = List.of(corsOriginPatterns.split(",")).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        config.setAllowedOriginPatterns(origins.isEmpty() ? List.of("http://localhost:*") : origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        boolean wildcard = origins.stream().anyMatch("*"::equals);
        config.setAllowCredentials(!wildcard);
        config.addAllowedHeader("*");
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    /**
     * Hosted browser login for {@code /oauth2/authorize}. APIs stay on {@link #filterChain}.
     */
    @Bean
    @Order(0)
    public SecurityFilterChain hostedLoginFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/login", "/logout", "/authorized")
                .authorizeHttpRequests(az -> az.anyRequest().permitAll())
                .formLogin(Customizer.withDefaults())
                .logout(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults());
        http.csrf(AbstractHttpConfigurer::disable); // API-only. Login CSRF is on hostedLoginFilterChain.

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
    public AuthenticationManager authenticationManager(HttpSecurity http, DaoAuthenticationProvider authenticationProvider) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.authenticationProvider(authenticationProvider);
        return authenticationManagerBuilder.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(JwtUserDetailsService jwtUserDetailsService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(jwtUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("--------------- its configured bean passwordEncoder");
        return new BCryptPasswordEncoder();
    }

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
        if (jwkKeystorePath != null && !jwkKeystorePath.isBlank()) {
            if (jwkKeystorePassword == null || jwkKeystorePassword.isBlank()
                    || jwkKeystoreAlias == null || jwkKeystoreAlias.isBlank()) {
                throw new IllegalStateException(
                        "AAAX_JWK_KEYSTORE is set; also set AAAX_JWK_KEYSTORE_PASSWORD and AAAX_JWK_KEYSTORE_ALIAS");
            }
            return KeystoreKeyPairs.load(jwkKeystorePath, jwkKeystorePassword, jwkKeystoreAlias);
        }
        try {
            log.warn("AAAX_JWK_KEYSTORE unset — generating ephemeral RSA (tokens invalid after restart)");
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to generate ephemeral RSA key pair", e);
        }
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
