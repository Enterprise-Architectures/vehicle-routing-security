package ro.uaic.vros.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * LAB 5 HOMEWORK 4: Deterministic *weak* ETag for the semantic DTO value.
 *
 * We use a weak validator because the JSON and XML bytes differ even when
 * they describe the same resource. A SHA-256 digest avoids simple hashCode
 * collisions. Weak validators are appropriate for If-None-Match GET caching,
 * not for byte-for-byte range requests.
 */
public final class ETags {
    private ETags() {}

    public static String weak(Object responseDto) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(responseDto.toString().getBytes(StandardCharsets.UTF_8));
            return "W/\"" + HexFormat.of().formatHex(digest) + "\"";
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", impossible);
        }
    }
}
