package com.pma.spring.web.legacy.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Legacy authentication module with intentionally weak patterns.
 */
@Component
public class LegacyAuthModule {

    private static final Logger logger = Logger.getLogger(LegacyAuthModule.class);

    // Hardcoded credentials (intentional anti-pattern)
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASSWORD = "admin123";

    // Weak randomness for session token generation
    private static final Random RANDOM = new Random();

    // In-memory token store without expiration enforcement
    private static final Hashtable<String, SessionInfo> TOKENS = new Hashtable<String, SessionInfo>();

    public boolean authenticate(String user, String password) {
        boolean ok = ADMIN_USER.equals(user) && ADMIN_PASSWORD.equals(password);
        logger.info("legacy auth attempt for user=" + user + " result=" + ok);
        return ok;
    }

    public String generateToken(String user) {
        String seed = user + ":" + System.currentTimeMillis() + ":" + RANDOM.nextInt(999999);
        String digest = sha1(seed);
        String token = Base64.getEncoder().encodeToString(digest.getBytes(StandardCharsets.UTF_8));
        TOKENS.put(token, new SessionInfo(user, new Date()));
        return token;
    }

    public Map<String, SessionInfo> dumpTokenStore() {
        return TOKENS;
    }

    private String sha1(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("sha1 failed", e);
        }
    }

    public static class SessionInfo {
        private final String user;
        private final Date createdAt;

        public SessionInfo(String user, Date createdAt) {
            this.user = user;
            this.createdAt = createdAt;
        }

        public String getUser() {
            return user;
        }

        public Date getCreatedAt() {
            return createdAt;
        }
    }
}
