package com.dmvlab.swapserver.net;

import com.google.gson.Gson;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class TransferPayload {
    private static final Gson gson = new Gson();

    public static String createPayload(Map<String, Object> payloadMap, String secret) throws Exception {
        String json = gson.toJson(payloadMap);
        byte[] hmac = computeHmac(json.getBytes(StandardCharsets.UTF_8), secret.getBytes(StandardCharsets.UTF_8));
        String bundled = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8)) + ":" + Base64.getEncoder().encodeToString(hmac);
        return bundled;
    }

    public static boolean verifyPayload(String bundled, String secret) {
        try {
            String[] parts = bundled.split(":");
            if (parts.length != 2) return false;
            byte[] json = Base64.getDecoder().decode(parts[0]);
            byte[] sig = Base64.getDecoder().decode(parts[1]);
            byte[] expected = computeHmac(json, secret.getBytes(StandardCharsets.UTF_8));
            if (expected.length != sig.length) return false;
            for (int i = 0; i < expected.length; i++) if (expected[i] != sig[i]) return false;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] computeHmac(byte[] data, byte[] key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }
}
