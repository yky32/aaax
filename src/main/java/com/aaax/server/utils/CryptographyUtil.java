package com.aaax.server.utils;

import com.aaax.server.exception.response.AaaxErrorResponse;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;

import javax.crypto.Cipher;
import java.security.PrivateKey;
import java.util.Base64;

public class CryptographyUtil {

    // Method to decrypt the Base64 encoded string
    public static String decrypt(String base64EncryptedData, PrivateKey privateKey) {
        try {
            // Decode the Base64 encoded string
            byte[] encryptedData = Base64.getDecoder().decode(base64EncryptedData);

            // Decrypt the data
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedBytes = cipher.doFinal(encryptedData);

            // Convert decrypted bytes to String
            return new String(decryptedBytes);
        } catch (Exception exception) {
            OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, AaaxErrorResponse.AAAX0002.getMessage(), "password, username or status decryption.");
            throw new OAuth2AuthenticationException(error);
        }
    }
}
