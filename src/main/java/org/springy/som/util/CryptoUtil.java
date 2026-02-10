package org.springy.som.util;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

public final class CryptoUtil {
    private CryptoUtil() {}

    private static final String PREFIX = "ENC(";
    private static final String SUFFIX = ")";

    public static String decryptIfEncrypted(String maybeEnc, PrivateKey privateKey) {
        if (maybeEnc == null) return null;
        String trimmed = maybeEnc.trim();
        if (!trimmed.startsWith(PREFIX) || !trimmed.endsWith(SUFFIX)) return maybeEnc;

        String b64 = trimmed.substring(PREFIX.length(), trimmed.length() - SUFFIX.length());
        byte[] ciphertext = Base64.getDecoder().decode(b64);
        return decrypt(ciphertext, privateKey);
    }

    public static String encryptToEncString(String plaintext, PublicKey publicKey) {
        byte[] ct = encrypt(plaintext, publicKey);
        return "ENC(" + Base64.getEncoder().encodeToString(ct) + ")";
    }

    public static byte[] encrypt(String plaintext, PublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("RSA encrypt failed", e);
        }
    }

    public static String decrypt(byte[] ciphertext, PrivateKey privateKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] pt = cipher.doFinal(ciphertext);
            return new String(pt, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("RSA decrypt failed", e);
        }
    }
}
