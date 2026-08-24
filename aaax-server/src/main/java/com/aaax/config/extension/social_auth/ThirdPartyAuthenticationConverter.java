package com.aaax.config.extension.social_auth;

import com.aaax.config.extension.GrantTypeExtension;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class ThirdPartyAuthenticationConverter implements AuthenticationConverter {
    private static MultiValueMap<String, String> getParameters(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>(parameterMap.size());
        parameterMap.forEach((key, values) -> {
            for (String value : values) {
                parameters.add(key, value);
            }
        });
        return parameters;
    }

    @Nullable
    @Override
    public Authentication convert(HttpServletRequest request) {
        // grant_type (REQUIRED)
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!GrantTypeExtension.THIRD_PARTY_OAUTH_GRANT.getKey().equals(grantType)) {
            return null;
        }

        Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();

        MultiValueMap<String, String> parameters = getParameters(request);

        // check [username] [password] (REQUIRED)
        String idToken = parameters.getFirst("idToken");
        String provider = parameters.getFirst("provider");
        String deviceType = parameters.getFirst("deviceType");

        if (!StringUtils.hasText(idToken) ||
                !StringUtils.hasText(provider) ||
                !StringUtils.hasText(deviceType) ||
                parameters.get("idToken").size() != 1 ||
                parameters.get("provider").size() != 1 ||
                parameters.get("deviceType").size() != 1
        ) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST.concat(":").concat(GrantTypeExtension.THIRD_PARTY_OAUTH_GRANT.getKey()));
        }

        Map<String, Object> additionalParameters = new HashMap<>();
        parameters.forEach((key, value) -> {
            if (!key.equals(OAuth2ParameterNames.GRANT_TYPE) &&
                    !key.equals(OAuth2ParameterNames.CLIENT_ID) &&
                    !key.equals(OAuth2ParameterNames.CODE)) {
                additionalParameters.put(key, value.get(0));
            }
        });

        return new ThirdPartyAuthenticationToken(idToken, provider, deviceType, clientPrincipal, additionalParameters);
    }

}
