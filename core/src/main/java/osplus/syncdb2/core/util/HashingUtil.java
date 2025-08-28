package osplus.syncdb2.core.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Zweck
 * -----
 * Stellt Hash-Funktionen zur Bildung deterministischer Kennungen bereit.
 * Insbesondere: SHA-256 als Hex-String für die `messageId` von Envelopes.
 * <p>
 * Hinweise
 * --------
 * - Eingaben müssen zuvor **kanonisch** serialisiert werden (stabile JSON-Reihenfolge).
 * - Diese Klasse kapselt nur die kryptografische Hash-Bildung.
 */
public final class HashingUtil {

    private HashingUtil() {
    }

    /**
     * Bildet SHA-256 über den gegebenen Text (UTF-8) und liefert Hex-String.
     */
    public static String sha256Hex(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 nicht verfügbar", e);
        }
    }
}
