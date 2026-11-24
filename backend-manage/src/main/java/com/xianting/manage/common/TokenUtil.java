package com.xianting.manage.common;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Compact HMAC token: base64url(payload).base64url(hmac(payload)),
 * payload = adminId:expiresAtMillis.
 *
 * 注意：管理端 app.jwt.secret 必须与客户端 backend 不同。
 * payload 里只有一个 Long，若两端共用密钥，客户端用户的 token
 * 能通过管理端验签并被当成 adminId（身份串号漏洞）。
 */
public final class TokenUtil {

    private TokenUtil() {
    }

    //  生成ToKen
    public static String issue(Long adminId, String secret, long expireHours) {
        long exp = System.currentTimeMillis() + expireHours * 3600_000L;
        String payload = adminId + ":" + exp;
        String p = b64(payload.getBytes(StandardCharsets.UTF_8));
        return p + "." + b64(hmac(payload, secret));  //  b64(签名)
    }

    /** Returns the adminId, or null when the token is invalid/expired. */
    public static Long verify(String token, String secret) {
        if (token == null) return null;
        String[] parts = token.split("\\.");
        if (parts.length != 2) return null;
        try {
            //  验签解码
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            byte[] sig = Base64.getUrlDecoder().decode(parts[1]);
            if (!java.security.MessageDigest.isEqual(sig, hmac(payload, secret))) return null;
            String[] fields = payload.split(":");
            if (fields.length != 2) return null;
            long exp = Long.parseLong(fields[1]);
            if (exp < System.currentTimeMillis()) return null;
            return Long.parseLong(fields[0]);
        } catch (Exception e) {
            return null;
        }
    }

    //  签名算法本体
    private static byte[] hmac(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");  //  选"HmacSHA256"算法
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String b64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
