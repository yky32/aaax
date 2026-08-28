package com.aaax.server.endpoint.aaax_config;

import com.aaax.core.response.R;
import com.aaax.core.response.Result;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.interfaces.RSAPublicKey;
import java.util.Map;

@RestController
@RequestMapping("/keys")
@RequiredArgsConstructor
@Slf4j
public class KeysEndpoint {

    private final RSAKey rsaKey;

    /**
     * Encryption public key for clients (encrypted password grant).
     */
    @SneakyThrows
    @GetMapping("/public-keys")
    public Result<Map<String, String>> getPublicKey() {
        RSAPublicKey publicKey = (RSAPublicKey) rsaKey.toPublicKey();
        return R.success(Map.of("publicKey", Base64.encode(publicKey.getEncoded()).toString()));
    }
}
