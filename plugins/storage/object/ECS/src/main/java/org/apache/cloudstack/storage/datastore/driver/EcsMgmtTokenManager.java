package org.apache.cloudstack.storage.datastore.driver;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import com.cloud.utils.exception.CloudRuntimeException;

public class EcsMgmtTokenManager {
    private static final long DEFAULT_TOKEN_MAX_AGE_SEC = 300;
    private static final long EXPIRY_SKEW_SEC = 30;

    private static final ConcurrentHashMap<TokenKey, TokenEntry> TOKEN_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<TokenKey, Object> TOKEN_LOCKS = new ConcurrentHashMap<>();

    static final class EcsUnauthorizedException extends RuntimeException {
        EcsUnauthorizedException(final String msg) { super(msg); }
    }

    @FunctionalInterface
    public interface WithToken<T> { T run(String token) throws Exception; }

    private static final class TokenKey {
        final String mgmtUrl;
        final String user;
        TokenKey(final String mgmtUrl, final String user) {
            this.mgmtUrl = mgmtUrl;
            this.user = user;
        }
        @Override public boolean equals(final Object o) {
            if (this == o) return true;
            if (!(o instanceof TokenKey)) return false;
            final TokenKey k = (TokenKey) o;
            return Objects.equals(mgmtUrl, k.mgmtUrl) && Objects.equals(user, k.user);
        }
        @Override public int hashCode() { return Objects.hash(mgmtUrl, user); }
    }

    private static final class TokenEntry {
        final String token;
        final long expiresAtMs;
        TokenEntry(final String token, final long expiresAtMs) {
            this.token = token;
            this.expiresAtMs = expiresAtMs;
        }
        boolean validNow() {
            return token != null && !token.isBlank() && System.currentTimeMillis() < expiresAtMs;
        }
    }

    public <T> T callWithRetry401(final EcsObjectStoreDriverImpl.EcsCfg cfg,
                                  final WithToken<T> op,
                                  final HttpClientFactory httpFactory) throws Exception {
        try {
            return op.run(getAuthToken(cfg, httpFactory));
        } catch (EcsUnauthorizedException u) {
            invalidate(cfg);
            return op.run(getAuthToken(cfg, httpFactory));
        }
    }

    public void invalidate(final EcsObjectStoreDriverImpl.EcsCfg cfg) {
        TOKEN_CACHE.remove(new TokenKey(trimTail(cfg.mgmtUrl), cfg.saUser));
    }

    public String getAuthToken(final EcsObjectStoreDriverImpl.EcsCfg cfg,
                               final HttpClientFactory httpFactory) {
        final String mu = trimTail(cfg.mgmtUrl);
        final TokenKey key = new TokenKey(mu, cfg.saUser);

        final TokenEntry cached = TOKEN_CACHE.get(key);
        if (cached != null && cached.validNow()) return cached.token;

        final Object lock = TOKEN_LOCKS.computeIfAbsent(key, k -> new Object());
        synchronized (lock) {
            final TokenEntry cached2 = TOKEN_CACHE.get(key);
            if (cached2 != null && cached2.validNow()) return cached2.token;

            final TokenEntry fresh = loginAndGetTokenFresh(mu, cfg.saUser, cfg.saPass, cfg.insecure, httpFactory);
            TOKEN_CACHE.put(key, fresh);
            return fresh.token;
        }
    }

    private TokenEntry loginAndGetTokenFresh(final String mgmtUrl,
                                             final String user,
                                             final String pass,
                                             final boolean insecure,
                                             final HttpClientFactory httpFactory) {
        try (CloseableHttpClient http = httpFactory.build(insecure)) {
            final HttpGet get = new HttpGet(mgmtUrl + "/login");
            UsernamePasswordCredentials creds = new UsernamePasswordCredentials(user, pass);
            get.addHeader(new BasicScheme().authenticate(creds, get, null));

            try (CloseableHttpResponse resp = http.execute(get)) {
                final int status = resp.getStatusLine().getStatusCode();
                if (status != 200 && status != 201) {
                    final String body = resp.getEntity() != null
                            ? EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8)
                            : "";
                    throw new CloudRuntimeException("ECS /login failed: HTTP " + status + " body=" + body);
                }
                if (resp.getFirstHeader("X-SDS-AUTH-TOKEN") == null) {
                    throw new CloudRuntimeException("ECS /login did not return X-SDS-AUTH-TOKEN header");
                }

                final String token = resp.getFirstHeader("X-SDS-AUTH-TOKEN").getValue();

                long maxAgeSec = DEFAULT_TOKEN_MAX_AGE_SEC;
                try {
                    if (resp.getFirstHeader("X-SDS-AUTH-MAX-AGE") != null) {
                        maxAgeSec = Long.parseLong(resp.getFirstHeader("X-SDS-AUTH-MAX-AGE").getValue().trim());
                    }
                } catch (Exception ignore) { }

                final long effectiveSec = Math.max(5, maxAgeSec - EXPIRY_SKEW_SEC);
                final long expiresAtMs = System.currentTimeMillis() + (effectiveSec * 1000L);
                return new TokenEntry(token, expiresAtMs);
            }
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to obtain ECS auth token: " + e.getMessage(), e);
        }
    }

    private static String trimTail(final String s) {
        if (s == null) return null;
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    /** Simple seam for testability; implemented by the driver using its existing buildHttpClient(). */
    @FunctionalInterface
    public interface HttpClientFactory {
        CloseableHttpClient build(boolean insecure);
    }
}
