package dev.willram.ramcore.resourcepack;

import org.jetbrains.annotations.NotNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-1 helpers for resource-pack hashing. Minecraft distributes packs with a SHA-1 the client
 * verifies, so the builder hashes the finished zip.
 */
public final class Sha1 {

    private Sha1() {
    }

    /**
     * The SHA-1 digest of the given bytes.
     *
     * @param bytes the input
     * @return the 20-byte digest
     */
    public static byte @NotNull [] digest(byte @NotNull [] bytes) {
        return newDigest().digest(bytes);
    }

    /**
     * The lower-case hex of a digest.
     *
     * @param digest the digest bytes
     * @return the hex string
     */
    @NotNull
    public static String hex(byte @NotNull [] digest) {
        return HexFormat.of().formatHex(digest);
    }

    /** A fresh SHA-1 {@link MessageDigest}. */
    @NotNull
    public static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-1 not available", impossible);
        }
    }
}
