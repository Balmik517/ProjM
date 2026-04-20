package com.pma.spring.web.legacy.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Legacy crypto module with weak ciphers and hash algorithms.
 */
@Component
public class InsecureCryptoModule {

    private static final Logger logger = Logger.getLogger(InsecureCryptoModule.class);

    private static final byte[] DES_KEY = "12345678".getBytes(StandardCharsets.UTF_8);

    public String md5(String input) {
        return digest("MD5", input);
    }

    public String sha1(String input) {
        return digest("SHA-1", input);
    }

    public String desEncryptEcb(String plainText) {
        try {
            SecretKeySpec key = new SecretKeySpec(DES_KEY, "DES");
            Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            logger.error("DES encryption failed", e);
            throw new RuntimeException(e);
        }
    }

    public String desDecryptEcb(String encryptedText) {
        try {
            SecretKeySpec key = new SecretKeySpec(DES_KEY, "DES");
            Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("DES decryption failed", e);
            throw new RuntimeException(e);
        }
    }

    private String digest(String algorithm, String input) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("digest failed", e);
        }
    }
}
