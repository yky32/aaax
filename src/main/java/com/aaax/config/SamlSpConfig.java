package com.aaax.config;

import java.util.Map;

import com.aaax.account.Account;
import com.aaax.account.AccountUserDetailsService;
import com.aaax.account.application.AccountQueries;
import com.aaax.account.application.FederateAccountUseCase;
import com.aaax.auth.application.FinishAuthenticatedSession;
import com.aaax.events.IdentityEvent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.util.StringUtils;

/**
 * SAML 2.0 Service Provider — login via external IdP metadata.
 * Enable: {@code aaax.saml.enabled=true} + {@code aaax.saml.idp-metadata-uri}.
 * Session completion uses {@link FinishAuthenticatedSession} (same as password/social).
 */
@Configuration
@ConditionalOnProperty(name = "aaax.saml.enabled", havingValue = "true")
public class SamlSpConfig {

    @Bean
    RelyingPartyRegistrationRepository relyingPartyRegistrationRepository(
            @Value("${aaax.saml.idp-metadata-uri}") String metadataUri,
            @Value("${aaax.saml.registration-id:idp}") String registrationId,
            @Value("${aaax.saml.sp-entity-id:}") String spEntityId,
            @Value("${aaax.issuer:http://localhost:8081}") String issuer) {
        if (!StringUtils.hasText(metadataUri)) {
            throw new IllegalStateException("aaax.saml.idp-metadata-uri is required when SAML is enabled");
        }
        String entityId = StringUtils.hasText(spEntityId) ? spEntityId : issuer + "/saml2/metadata";
        RelyingPartyRegistration registration = RelyingPartyRegistrations
                .fromMetadataLocation(metadataUri)
                .registrationId(registrationId)
                .entityId(entityId)
                .assertionConsumerServiceLocation("{baseUrl}/login/saml2/sso/{registrationId}")
                .build();
        return new InMemoryRelyingPartyRegistrationRepository(registration);
    }

    @Bean
    OpenSaml5AuthenticationProvider openSamlAuthenticationProvider(
            FederateAccountUseCase federate, AccountUserDetailsService userDetailsService) {
        OpenSaml5AuthenticationProvider provider = new OpenSaml5AuthenticationProvider();
        provider.setResponseAuthenticationConverter(responseToken -> {
            var result = OpenSaml5AuthenticationProvider.createDefaultResponseAuthenticationConverter()
                    .convert(responseToken);
            if (result == null || !(result.getPrincipal() instanceof Saml2AuthenticatedPrincipal principal)) {
                return result;
            }
            String nameId = principal.getName();
            String email = firstAttr(principal, "email", "mail",
                    "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress");
            Account account = federate.linkOrCreateSaml(nameId, email, nameId);
            UserDetails details = userDetailsService.loadUserByUsername(account.getUsername());
            return new UsernamePasswordAuthenticationToken(
                    details, result.getCredentials(), details.getAuthorities());
        });
        return provider;
    }

    /**
     * After SAML assertion → same session finish path as password/social.
     * Bean name must match injection in {@link SecurityConfig}.
     */
    @Bean(name = "samlLoginSuccessHandler")
    AuthenticationSuccessHandler samlLoginSuccessHandler(
            AccountQueries queries, FinishAuthenticatedSession finishSession) {
        return (request, response, authentication) -> {
            String username = authentication.getName();
            Account account = queries.requireEntityByUsername(username);
            finishSession.execute(
                    account,
                    "saml",
                    request,
                    response,
                    IdentityEvent.Types.AUTH_LOGIN_SOCIAL,
                    Map.of("method", "saml", "provider", "saml"));
            String target = account.roleSet().contains("ADMIN") ? "/admin/" : "/user/";
            response.sendRedirect(target);
        };
    }

    private static String firstAttr(Saml2AuthenticatedPrincipal principal, String... keys) {
        for (String k : keys) {
            var vals = principal.getAttribute(k);
            if (vals != null && !vals.isEmpty() && vals.getFirst() != null) {
                return vals.getFirst().toString();
            }
        }
        return null;
    }
}
