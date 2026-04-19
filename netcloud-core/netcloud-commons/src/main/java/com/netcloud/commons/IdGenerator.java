package com.netcloud.commons;

import lombok.experimental.UtilityClass;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.Base64;

@UtilityClass
public class IdGenerator {
    public static String getShortId() {
        UUID uuid = UUID.randomUUID();
        // Convert UUID to a 16-byte array
        ByteBuffer bb = ByteBuffer.wrap(new byte[10]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());

        // Encode to Base64 and remove padding/special chars
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bb.array());
    }
}