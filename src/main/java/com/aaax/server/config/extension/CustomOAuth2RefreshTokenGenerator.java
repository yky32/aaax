package com.aaax.server.config.extension;

import com.aaax.core.utils.InstantUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.time.Instant;
import java.util.Base64;

@Slf4j
public class CustomOAuth2RefreshTokenGenerator implements OAuth2TokenGenerator<OAuth2RefreshToken> {

    private final StringKeyGenerator refreshTokenGenerator = new Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 96);

    /**
     *  1. gen new
     *  2. check is401() -> reuse
     * @param context the context containing the OAuth 2.0 Token attributes
     * @return OAuth2RefreshToken.class
     */
    @Nullable
    @Override
    public OAuth2RefreshToken generate(OAuth2TokenContext context) {
        if (!GrantTypeExtension.isRefreshGrant(context.getTokenType().getValue())) {
            return null;
        }


        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(context.getRegisteredClient().getTokenSettings().getRefreshTokenTimeToLive());
        log.info("""
                
                > Client Setting:
                > name => {}
                > id => {}
                > secret => {} 
               
                =======START======== CustomOAuth2RefreshTokenGenerator
                expired at @ [{}] UTC
                ========END  ======= CustomOAuth2RefreshTokenGenerator
                """,
                context.getRegisteredClient().getClientId(),
                context.getRegisteredClient().getClientName(),
                context.getRegisteredClient().getClientSecret(),
                InstantUtil.parse(expiresAt)
        );
        return new OAuth2RefreshToken(this.refreshTokenGenerator.generateKey(), issuedAt, expiresAt);
    }
}
