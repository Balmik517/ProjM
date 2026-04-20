package com.pma.spring.web.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.xml.bind.DatatypeConverter;

import org.apache.log4j.Logger;

/**
 * Legacy utility class for encoding/decoding passwords and tokens.
 * Originally used JDK internal APIs (sun.misc.BASE64Encoder/Decoder) - 
 * migrated to java.util.Base64 but still uses javax.xml.bind (removed in JDK 11).
 */
public class LegacyEncoder {

    private static final Logger logger = Logger.getLogger(LegacyEncoder.class);

    // Replaced sun.misc.BASE64Encoder/Decoder with java.util.Base64
    private static final Base64.Encoder encoder = Base64.getEncoder();
    private static final Base64.Decoder decoder = Base64.getDecoder();

    private static LegacyEncoder instance;

    private LegacyEncoder() {
        // Singleton pattern - old style
    }

    public static synchronized LegacyEncoder getInstance() {
        if (instance == null) {
            instance = new LegacyEncoder();
        }
        return instance;
    }

    /**
     * Encode password using Base64 (originally used sun.misc.BASE64Encoder).
     */
    public String encodePassword(String password) {
        try {
            byte[] bytes = password.getBytes("UTF-8");
            String encoded = encoder.encodeToString(bytes);
            logger.info("Password encoded successfully using Base64 encoder");
            return encoded;
        } catch (UnsupportedEncodingException e) {
            logger.error("Encoding failed", e);
            throw new RuntimeException("Failed to encode password", e);
        }
    }

    /**
     * Decode password using Base64 (originally used sun.misc.BASE64Decoder).
     */
    public String decodePassword(String encodedPassword) {
        try {
            byte[] bytes = decoder.decode(encodedPassword);
            return new String(bytes, "UTF-8");
        } catch (Exception e) {
            logger.error("Decoding failed", e);
            throw new RuntimeException("Failed to decode password", e);
        }
    }

    /**
     * Hash string using javax.xml.bind.DatatypeConverter (removed in JDK 11).
     */
    public String hashWithJaxb(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(input.getBytes());
            byte[] digest = md.digest();
            // Uses javax.xml.bind - removed from JDK 11
            String hexString = DatatypeConverter.printHexBinary(digest);
            logger.info("Generated MD5 hash using JAXB DatatypeConverter");
            return hexString;
        } catch (NoSuchAlgorithmException e) {
            logger.error("MD5 not available", e);
            return null;
        }
    }

    /**
     * Convert hex string using javax.xml.bind.DatatypeConverter.
     */
    public byte[] hexToBytes(String hex) {
        return DatatypeConverter.parseHexBinary(hex);
    }

    /**
     * Encode to hex using javax.xml.bind.DatatypeConverter.
     */
    public String bytesToHex(byte[] bytes) {
        return DatatypeConverter.printHexBinary(bytes);
    }
}
