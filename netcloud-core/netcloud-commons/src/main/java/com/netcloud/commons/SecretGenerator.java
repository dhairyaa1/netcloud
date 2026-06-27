package com.netcloud.commons;
import com.netcloud.commons.dto.Secret;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * This service handles manual AES encryption and decryption for the tenantsecret.
 * This is used to pack tenantId and interval into a single encrypted string for URLs.
 */
@Slf4j
@Service
public class SecretGenerator {

    @Value("${netcloud.secrets.secret}")
    private String secretKeyString;
    private SecretKeySpec key;

    @PostConstruct
    public void init() throws NoSuchAlgorithmException {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(secretKeyString.getBytes(StandardCharsets.UTF_8));
        this.key = new SecretKeySpec(keyBytes, "AES");
    }
    public String generateSecret(String tenantId, long interval, String lbId) {
        try {

            String dataToEncrypt = tenantId + "|" + interval + "|" + lbId;
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encryptedBytes = cipher.doFinal(dataToEncrypt.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error during encryption: " + e.getMessage());
        }
    }

    public Secret getClaimsFromSecret(String encryptedSecret) {
        try {
            log.info("secret is {}", encryptedSecret);
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decodedBytes = Base64.getUrlDecoder().decode(encryptedSecret);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);

            String decryptedData = new String(decryptedBytes, StandardCharsets.UTF_8);
            String[] parts = decryptedData.split("\\|");
            return new Secret(parts[0], Long.parseLong(parts[1]), parts[2]);
        } catch (Exception e) {
            throw new RuntimeException("Invalid or tampered secret provided.");
        }
    }
}