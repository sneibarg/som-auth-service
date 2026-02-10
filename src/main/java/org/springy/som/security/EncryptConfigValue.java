package org.springy.som.security;

import org.springy.som.util.CryptoUtil;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.Certificate;

public class EncryptConfigValue {
    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            System.err.println("Usage: <keystore.p12> <storePass> <alias> <plaintext> <keyPassIgnored>");
            System.exit(2);
        }

        String ksPath = args[0];
        String storePass = args[1];
        String alias = args[2];
        String plaintext = args[3];

        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(ksPath)) {
            ks.load(fis, storePass.toCharArray());
        }

        Certificate cert = ks.getCertificate(alias);
        PublicKey pub = cert.getPublicKey();

        System.out.println(CryptoUtil.encryptToEncString(plaintext, pub));
    }
}
