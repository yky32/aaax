package com.aaax.endpoint.uaa_config;

import com.aaax.core.response.R;
import com.aaax.core.response.Result;
import com.aaax.utils.CryptographyUtil;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;

@RestController
@RequestMapping("/keys")
@RequiredArgsConstructor
@Slf4j
public class KeysEndpoint {

    private final RSAKey rsaKey;

    /**
     * Globally re-use
     *
     * @return - for r
     */
    @SneakyThrows
    @GetMapping("/public-keys")
    public Result<Map<String, String>> getPublicKey() {
        RSAPublicKey publicKey = (RSAPublicKey) rsaKey.toPublicKey(); // Extract the public key
        return R.success(Map.of("publicKey", Base64.encode(publicKey.getEncoded()).toString())); // Encode the public key in Base64
    }

    @SneakyThrows
    @GetMapping("/private-keys") // temporarily just check isAuthenticated() user , later should check specific scope
    public Result<Map<String, String>> getPrivateKey() {
        RSAPrivateKey privateKey = (RSAPrivateKey) rsaKey.toPrivateKey(); // Cast to RSAPrivateKey
        return R.success(Map.of("privateKey", Base64.encode(privateKey.getEncoded()).toString())); // Encode the public key in Base64
    }


    @SneakyThrows
    @PostMapping("/decryption") // temporarily just check isAuthenticated() user , later should check specific scope
    public Result<Map<String, String>> decrypt(
            @RequestBody Map<String, Object> data
    ) {
        String decryptedData = CryptographyUtil.decrypt(String.valueOf(data.get("q")), rsaKey.toPrivateKey());
        return R.success(Map.of("decryptedData", decryptedData));
    }
}
