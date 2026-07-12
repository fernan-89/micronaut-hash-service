package com.thinklab.domain.valueobject;

import jakarta.annotation.Nonnull;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Value Object: Type-Safe Enumeration representing supported cryptographic hashing algorithms.
 * This Enum acts as the source of truth for all hashing operations within the system.
 * It implements a strict fail-fast mechanism during class initialization: if the JVM
 * does not support a defined algorithm, an error is thrown immediately.
 *
 * <p><b>Supported Algorithms:</b></p>
 * <ul>
 *     <li>SHA-256/512: Current industry standards for secure hashing.</li>
 *     <li>SHA3-256/512: Latest NIST standards (Keccak), highly resilient.</li>
 *     <li>BLAKE3-256: High-performance algorithm for high-throughput serial/MFA generation.</li>
 *     <li>SHA-1/MD5: Legacy support only (Deprecated).</li>
 * </ul>
 */
public enum HashAlgorithm {

    /**
     * SHA-256 cryptographic hash algorithm. Standard for general secure hashing.
     */
    SHA_256("SHA-256"),

    /**
     * SHA-512 cryptographic hash algorithm. Used for high-security payloads.
     */
    SHA_512("SHA-512"),

    /**
     * SHA3-256 cryptographic hash algorithm. The latest Keccak-based NIST standard.
     */
    SHA3_256("SHA3-256"),

    /**
     * SHA3-512 cryptographic hash algorithm. The latest Keccak-based NIST standard
     * for maximum security requirements.
     */
    SHA3_512("SHA3-512"),

    /**
     * BLAKE3 (256-bit) cryptographic hash algorithm.
     * Extremely fast. Ideal for high-throughput scenarios like Windows-style serial keys.
     * <b>Note:</b> Requires Bouncy Castle provider to be registered in the JVM.
     */
    BLAKE3_256("BLAKE3-256"),

    /**
     * SHA-1 cryptographic hash algorithm.
     * @deprecated Vulnerable to collision attacks. STRICTLY for legacy integrations.
     */
    @Deprecated(since = "1.0")
    SHA_1("SHA-1"),

    /**
     * MD5 cryptographic hash algorithm.
     * @deprecated Broken and highly vulnerable. Use ONLY for backwards compatibility.
     */
    @Deprecated(since = "1.0")
    MD5("MD5");

    private static final Map<String, HashAlgorithm> BY_STANDARD_NAME;
    private final String standardName;

    static {
        Map<String, HashAlgorithm> map = new HashMap<>();
        for (HashAlgorithm alg : values()) {
            map.put(alg.standardName.toUpperCase(), alg);
        }
        BY_STANDARD_NAME = Collections.unmodifiableMap(map);
    }

    HashAlgorithm(String standardName) {
        this.standardName = standardName;
    }

    /**
     * Returns the algorithm associated with the provided standard name.
     * @param name The standard name (e.g., "SHA-256").
     * @return The corresponding HashAlgorithm.
     * @throws IllegalArgumentException if the name is null or unsupported.
     */
    @Nonnull
    public static HashAlgorithm fromStandardName(@Nonnull String name) {
        HashAlgorithm alg = BY_STANDARD_NAME.get(name.toUpperCase());
        if (alg == null) {
            throw new IllegalArgumentException("Unsupported cryptographic algorithm: " + name);
        }
        return alg;
    }

    /**
     * Obtains a ready-to-use MessageDigest instance for this algorithm.
     * @return An initialized MessageDigest.
     * @throws IllegalStateException if the algorithm becomes unavailable at runtime.
     */
    @Nonnull
    public MessageDigest getMessageDigest() {
        try {
            return MessageDigest.getInstance(this.standardName);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Critical error: Algorithm " + this.standardName +
                    " is not supported by the current JVM environment. Check if external providers are registered.", e);
        }
    }

    public String getStandardName() {
        return standardName;
    }
}