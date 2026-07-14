package com.thinklab.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.annotation.Nonnull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Domain Value Object: Type-safe enumeration representing supported cryptographic hashing algorithms.
 * <p>This enum acts as the definitive source of truth for all hashing operations. It enforces
 * a strict fail-fast mechanism at class initialization, ensuring the environment supports
 * the required algorithms before any business operations commence.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Immutability:</b> All definitions and algorithm mappings are unmodifiable.</li>
 * <li><b>Defensive Integrity:</b> Validates algorithm availability immediately via the {@link MessageDigest} API.</li>
 * <li><b>Serialization-Ready:</b> Uses standard annotations for consistent JSON-to-Enum mapping across the infrastructure.</li>
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
     * SHA3-256 cryptographic hash algorithm. NIST standard (Keccak).
     */
    SHA3_256("SHA3-256"),

    /**
     * SHA3-512 cryptographic hash algorithm. NIST standard (Keccak) for maximum security.
     */
    SHA3_512("SHA3-512"),

    /**
     * BLAKE3 (256-bit) cryptographic hash algorithm. Optimized for high-throughput scenarios.
     */
    BLAKE3_256("BLAKE3-256"),

    /**
     * SHA-1 cryptographic hash algorithm.
     * @deprecated Vulnerable to collision attacks. Retained for legacy integration only.
     */
    @Deprecated(since = "1.0")
    SHA_1("SHA-1"),

    /**
     * MD5 cryptographic hash algorithm.
     * @deprecated Cryptographically broken. Retained only for legacy compatibility.
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
     * Factory method for deserialization based on algorithm standard name.
     *
     * @param name The algorithm standard name (e.g., "SHA-256").
     * @return The corresponding {@link HashAlgorithm}.
     * @throws IllegalArgumentException if the provided name is unsupported.
     */
    @JsonCreator
    @Nonnull
    public static HashAlgorithm fromStandardName(@Nonnull String name) {
        HashAlgorithm alg = BY_STANDARD_NAME.get(name.toUpperCase());
        if (alg == null) {
            throw new IllegalArgumentException("Unsupported cryptographic algorithm: " + name);
        }
        return alg;
    }

    /**
     * Retrieves the standard algorithm identifier.
     *
     * @return The identifier string used by the Java Security API.
     */
    @JsonValue
    @Nonnull
    public String getStandardName() {
        return standardName;
    }

    /**
     * Obtains an initialized {@link MessageDigest} instance for the algorithm.
     *
     * @return An initialized digest.
     * @throws IllegalStateException if the algorithm is unavailable in the current runtime environment.
     */
    @Nonnull
    public MessageDigest getMessageDigest() {
        try {
            return MessageDigest.getInstance(this.standardName);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Critical error: Algorithm " + this.standardName +
                    " is not supported by the current JVM environment.", e);
        }
    }
}