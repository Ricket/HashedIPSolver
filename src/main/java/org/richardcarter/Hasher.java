package org.richardcarter;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hasher {

    private static final String SALT = "SALT GOES HERE";

    private static final ThreadLocal<MessageDigest> md = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    });

    public static byte[] calculateHash(IpAddress ip) {
        return md.get().digest(ip.toBytesPlusSalt(SALT));
    }
}
