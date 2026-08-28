package com.aaax.core.utils;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.List;

/** Load an RSA (or other) key pair from a JKS/PKCS12 file. Replaces oauth2 2.5 KeyStoreKeyFactory. */
public final class KeystoreKeyPairs {

    private KeystoreKeyPairs() {
    }

    public static KeyPair load(String path, String password, String alias) {
        return load(new FileSystemResource(path), password, alias);
    }

    public static KeyPair load(Resource resource, String password, String alias) {
        char[] chars = password.toCharArray();
        Exception last = null;
        for (String type : List.of("PKCS12", "JKS")) {
            try (InputStream in = resource.getInputStream()) {
                KeyStore ks = KeyStore.getInstance(type);
                ks.load(in, chars);
                PrivateKey privateKey = (PrivateKey) ks.getKey(alias, chars);
                Certificate cert = ks.getCertificate(alias);
                if (privateKey == null || cert == null) {
                    throw new IllegalStateException("Keystore alias not found: " + alias);
                }
                return new KeyPair(cert.getPublicKey(), privateKey);
            } catch (Exception e) {
                last = e;
            }
        }
        throw new IllegalStateException("Failed to load keystore " + resource, last);
    }
}
