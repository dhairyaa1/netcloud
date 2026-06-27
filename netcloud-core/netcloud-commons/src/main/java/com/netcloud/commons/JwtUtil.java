package com.netcloud.commons;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${netcloud.secrets.secret}")
    private String secretKeyString;

    private SecretKey key;

    @PostConstruct
    public void init() throws NoSuchAlgorithmException {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(secretKeyString.getBytes(StandardCharsets.UTF_8));
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String tenantId, String tenantName) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(tenantId)
                .claim("tenantName", tenantName)
                .issuedAt(new Date(now))
                .expiration(new Date(now + 86400000L))
                .signWith(key)
                .compact();
    }

    public String extractTenantId(String token) {
        return getClaims(token).getSubject();
    }

    public String extractTenantName(String token) {
        return getClaims(token).get("tenantName", String.class);
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
