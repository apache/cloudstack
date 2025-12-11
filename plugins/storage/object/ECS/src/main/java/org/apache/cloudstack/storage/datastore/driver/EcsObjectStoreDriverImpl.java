/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.datastore.driver;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.net.ssl.SSLContext;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDetailsDao;
import org.apache.cloudstack.storage.object.BaseObjectStoreDriverImpl;
import org.apache.cloudstack.storage.object.Bucket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.BucketPolicy;
import com.cloud.agent.api.to.BucketTO;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.storage.BucketVO;
import com.cloud.storage.dao.BucketDao;
import com.cloud.user.Account;
import com.cloud.user.AccountDetailsDao;
import com.cloud.user.AccountDetailVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.exception.CloudRuntimeException;

public class EcsObjectStoreDriverImpl extends BaseObjectStoreDriverImpl {
    private static final Logger logger = LogManager.getLogger(EcsObjectStoreDriverImpl.class);

    // Object store details keys
    private static final String MGMT_URL   = "mgmt_url";      // e.g. https://ecs-api.example.com
    private static final String SA_USER    = "sa_user";       // service account user
    private static final String SA_PASS    = "sa_password";   // service account password
    private static final String NAMESPACE  = "namespace";     // e.g. cloudstack
    private static final String INSECURE   = "insecure";      // "true" to ignore TLS cert/host
    private static final String S3_HOST    = "s3_host";       // S3 endpoint host (or URL if UI provides it)

    // Per-account keys
    private static final String AD_KEY_ACCESS = "ecs.accesskey";
    private static final String AD_KEY_SECRET = "ecs.secretkey";

    // ---- ECS token caching ----
    private static final long DEFAULT_TOKEN_MAX_AGE_SEC = 300; // fallback if header missing
    private static final long EXPIRY_SKEW_SEC = 30;           // refresh early
    private static final ConcurrentHashMap<TokenKey, TokenEntry> TOKEN_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<TokenKey, Object> TOKEN_LOCKS = new ConcurrentHashMap<>();

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

    private static final class EcsUnauthorizedException extends RuntimeException {
        EcsUnauthorizedException(final String msg) { super(msg); }
    }

    @FunctionalInterface
    private interface WithToken<T> { T run(String token) throws Exception; }

    @Inject private AccountDao accountDao;
    @Inject private AccountDetailsDao accountDetailsDao;
    @Inject private BucketDao bucketDao;
    @Inject private ObjectStoreDetailsDao storeDetailsDao;

    public EcsObjectStoreDriverImpl() { }

    @Override
    public DataStoreTO getStoreTO(final DataStore store) {
        return null;
    }

    // ---------------- create bucket ----------------

    @Override
    public Bucket createBucket(final Bucket bucket, final boolean objectLock) {
        final long storeId = bucket.getObjectStoreId();
        final String name  = bucket.getName();

        if (objectLock) {
            throw new CloudRuntimeException("Dell ECS doesn't support this feature: object locking");
        }

        final Map<String, String> ds = storeDetailsDao.getDetails(storeId);
        final EcsCfg cfg = ecsCfgFromDetails(ds, storeId);

        // Resolve owner username for this bucket
        final BucketVO vo = bucketDao.findById(bucket.getId());
        final long accountId = vo.getAccountId();
        final Account acct = accountDao.findById(accountId);
        if (acct == null) {
            throw new CloudRuntimeException("ECS createBucket: account not found: id=" + accountId);
        }
        final String ownerUser = "cs-" + acct.getUuid();

        // Ensure per-account credentials exist (single-key policy with adopt-if-exists)
        ensureAccountUserAndSecret(accountId, ownerUser, cfg.mgmtUrl, cfg.saUser, cfg.saPass, cfg.ns, cfg.insecure);

        // Quota from UI (INT GB)
        Integer quotaGb = null;
        try {
            quotaGb = safeIntFromGetter(bucket, "getQuota");
            if (quotaGb == null) quotaGb = safeIntFromGetter(bucket, "getSize");
        } catch (Throwable ignored) { }

        final int blockSizeGb = quotaGb != null && quotaGb > 0 ? quotaGb : 2;
        final int notifSizeGb = quotaGb != null && quotaGb > 0 ? quotaGb : 1;

        // Encryption flag from request/VO best-effort
        boolean encryptionEnabled =
            getBooleanFlagLoose(bucket, "getEncryption", "isEncryption", false) ||
            getBooleanFlagLoose(bucket, "getEncryptionEnabled", "isEncryptionEnabled", false);

        if (!encryptionEnabled && vo != null) {
            encryptionEnabled =
                getBooleanFlagLoose(vo, "getEncryption", "isEncryption", false) ||
                getBooleanFlagLoose(vo, "getEncryptionEnabled", "isEncryptionEnabled", false);
        }

        logger.info("ECS createBucket flags for '{}': encryptionEnabled={}", name, encryptionEnabled);

        final String createBody =
            "<object_bucket_create>" +
            "<blockSize>" + blockSizeGb + "</blockSize>" +
            "<notificationSize>" + notifSizeGb + "</notificationSize>" +
            "<name>" + name + "</name>" +
            "<head_type>s3</head_type>" +
            "<namespace>" + cfg.ns + "</namespace>" +
            "<owner>" + ownerUser + "</owner>" +
            "<is_encryption_enabled>" + (encryptionEnabled ? "true" : "false") + "</is_encryption_enabled>" +
            "</object_bucket_create>";

        if (logger.isDebugEnabled()) {
            logger.debug("ECS createBucket XML for '{}': {}", name, createBody);
        }

        try {
            // Execute mgmt call with cached token (+ refresh on 401, once)
            mgmtCallWithRetry401(cfg, token -> {
                try (CloseableHttpClient http = buildHttpClient(cfg.insecure)) {
                    final HttpPost post = new HttpPost(cfg.mgmtUrl + "/object/bucket");
                    post.setHeader("X-SDS-AUTH-TOKEN", token);
                    post.setHeader("Content-Type", "application/xml");
                    post.setEntity(new StringEntity(createBody, StandardCharsets.UTF_8));

                    try (CloseableHttpResponse resp = http.execute(post)) {
                        final int status = resp.getStatusLine().getStatusCode();
                        final String respBody = resp.getEntity() != null
                                ? EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8)
                                : "";

                        if (status == 401) {
                            throw new EcsUnauthorizedException("ECS createBucket got 401");
                        }

                        if (status != 200 && status != 201) {
                            String reason = "HTTP " + status;
                            if (status == 400) {
                                final String lb = respBody == null ? "" : respBody.toLowerCase(Locale.ROOT);
                                if (lb.contains("already exist")
                                        || lb.contains("already_exists")
                                        || lb.contains("already-exists")
                                        || lb.contains("name already in use")
                                        || lb.contains("bucket exists")
                                        || lb.contains("duplicate")) {
                                    reason = "HTTP 400 bucket name already exists";
                                }
                            }
                            logger.error("ECS create bucket failed: {} body={}", reason, respBody);
                            throw new CloudRuntimeException("Failed to create ECS bucket " + name + ": " + reason);
                        }
                    }
                }
                return null;
            });

            // UI URL should show S3 endpoint
            final String s3Host = resolveS3HostForUI(storeId, ds);
            final String s3UrlForUI = "https://" + s3Host + "/" + name;

            logger.info("ECS bucket created: name='{}' owner='{}' ns='{}' quota={}GB enc={} (UI URL: {})",
                    name, ownerUser, cfg.ns, quotaGb != null ? quotaGb : blockSizeGb, encryptionEnabled, s3UrlForUI);

            // Persist UI-visible details on the bucket record
            final String accKey = valueOrNull(accountDetailsDao.findDetail(accountId, AD_KEY_ACCESS));
            final String secKey = valueOrNull(accountDetailsDao.findDetail(accountId, AD_KEY_SECRET));
            if (vo != null) {
                vo.setBucketURL(s3UrlForUI);
                if (!isBlank(accKey)) vo.setAccessKey(accKey);
                if (!isBlank(secKey)) vo.setSecretKey(secKey);
                bucketDao.update(vo.getId(), vo);
            }

            return bucket;
        } catch (CloudRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to create ECS bucket " + name + ": " + e.getMessage(), e);
        }
    }

    @Override
    public boolean createUser(final long accountId, final long storeId) {
        final Account acct = accountDao.findById(accountId);
        if (acct == null) throw new CloudRuntimeException("ECS createUser: account not found: id=" + accountId);

        final Map<String, String> ds = storeDetailsDao.getDetails(storeId);
        final EcsCfg cfg = ecsCfgFromDetails(ds, storeId);

        final String username = "cs-" + acct.getUuid();
        ensureAccountUserAndSecret(accountId, username, cfg.mgmtUrl, cfg.saUser, cfg.saPass, cfg.ns, cfg.insecure);
        return true;
    }

    // ---------------- S3: list buckets (SigV2, path-style GET /) ----------------
    @Override
    public List<Bucket> listBuckets(final long storeId) {
        final Map<String, String> ds = storeDetailsDao.getDetails(storeId);

        final CallContext ctx = CallContext.current();
        if (ctx == null || ctx.getCallingAccount() == null) {
            throw new CloudRuntimeException("ECS listBuckets: no calling account in context.");
        }
        final long accountId = ctx.getCallingAccount().getId();
        final String accessKey = valueOrNull(accountDetailsDao.findDetail(accountId, AD_KEY_ACCESS));
        final String secretKey = valueOrNull(accountDetailsDao.findDetail(accountId, AD_KEY_SECRET));
        if (isBlank(accessKey) || isBlank(secretKey)) {
            throw new CloudRuntimeException("ECS listBuckets: account has no stored S3 credentials");
        }

        final S3Endpoint ep = resolveS3Endpoint(ds, storeId);
        if (ep == null || isBlank(ep.host)) {
            throw new CloudRuntimeException("ECS listBuckets: S3 endpoint not resolvable");
        }

        final boolean insecure = "true".equalsIgnoreCase(ds.getOrDefault(INSECURE, "false"));
        final java.util.List<Bucket> out = new java.util.ArrayList<>();

        try (CloseableHttpClient http = buildHttpClient(insecure)) {
            final String dateHdr = rfc1123Now();
            final String canonicalResource = "/"; // ListAllMyBuckets
            final String sts = "GET\n\n\n" + dateHdr + "\n" + canonicalResource;
            final String signature = hmacSha1Base64(sts, secretKey);

            final String url = ep.scheme + "://" + ep.host + "/";
            final HttpGet get = new HttpGet(url);
            get.setHeader("Host", ep.host);
            get.setHeader("Date", dateHdr);
            get.setHeader("Authorization", "AWS " + accessKey + ":" + signature);

            try (CloseableHttpResponse resp = http.execute(get)) {
                final int st = resp.getStatusLine().getStatusCode();
                final String body = resp.getEntity() != null
                        ? EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8)
                        : "";
                if (st != 200) {
                    logger.error("ECS listBuckets failed: HTTP {} body={}", st, body);
                    throw new CloudRuntimeException("ECS listBuckets failed: HTTP " + st);
                }

                final List<String> names = extractAllTags(body, "Name");
                for (String n : names) {
                    if (isBlank(n)) continue;
                    final org.apache.cloudstack.storage.object.Bucket b =
                            new org.apache.cloudstack.storage.object.BucketObject();
                    b.setName(n.trim());
                    out.add(b);
                }
            }
        } catch (Exception e) {
            throw new CloudRuntimeException("ECS listBuckets failed: " + e.getMessage(), e);
        }

        return out;
    }

    public List<String> listBucketObjects(final String bucketName, final long storeId) {
        final Map<String, String> ds = storeDetailsDao.getDetails(storeId);

        final CallContext ctx = CallContext.current();
        if (ctx == null || ctx.getCallingAccount() == null) {
            throw new CloudRuntimeException("ECS listBucketObjects: no calling account in context");
        }
        final long accountId = ctx.getCallingAccount().getId();
        final String accessKey = valueOrNull(accountDetailsDao.findDetail(accountId, AD_KEY_ACCESS));
        final String secretKey = valueOrNull(accountDetailsDao.findDetail(accountId, AD_KEY_SECRET));
        if (isBlank(accessKey) || isBlank(secretKey)) {
            throw new CloudRuntimeException("ECS listBucketObjects: account has no stored S3 credentials");
        }

        final S3Endpoint ep = resolveS3Endpoint(ds, storeId);
        if (ep == null || isBlank(ep.host)) {
            throw new CloudRuntimeException("ECS listBucketObjects: S3 endpoint not resolvable");
        }
        final boolean insecure = "true".equalsIgnoreCase(ds.getOrDefault(INSECURE, "false"));

        final List<String> keys = new java.util.ArrayList<>();
        String marker = null;
        try (CloseableHttpClient http = buildHttpClient(insecure)) {
            while (true) {
                final String dateHdr = rfc1123Now();
                final String canonicalResource = "/" + bucketName + "/";
                final String sts = "GET\n\n\n" + dateHdr + "\n" + canonicalResource;
                final String signature = hmacSha1Base64(sts, secretKey);

                final StringBuilder qs = new StringBuilder("max-keys=1000");
                if (!isBlank(marker)) {
                    qs.append("&marker=").append(java.net.URLEncoder
                            .encode(marker, java.nio.charset.StandardCharsets.UTF_8.name()));
                }
                final String url = ep.scheme + "://" + ep.host + "/" + bucketName + "/?" + qs;

                final HttpGet get = new HttpGet(url);
                get.setHeader("Host", ep.host);
                get.setHeader("Date", dateHdr);
                get.setHeader("Authorization", "AWS " + accessKey + ":" + signature);

                try (CloseableHttpResponse resp = http.execute(get)) {
                    final int st = resp.getStatusLine().getStatusCode();
                    final String body = resp.getEntity() != null
                            ? EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8)
                            : "";
                    if (st != 200) {
                        logger.error("ECS listBucketObjects '{}' failed: HTTP {} body={}", bucketName, st, body);
                        throw new CloudRuntimeException("ECS listBucketObjects failed: HTTP " + st);
                    }

                    extractKeysFromListBucketXml(body, keys);

                    final boolean truncated = "true".equalsIgnoreCase(extractTag(body, "IsTruncated"));
                    if (!truncated) break;

                    String next = extractTag(body, "NextMarker");
                    if (isBlank(next) && !keys.isEmpty()) next = keys.get(keys.size() - 1);
                    if (isBlank(next)) break;
                    marker = next;
                }
            }
        } catch (Exception e) {
            throw new CloudRuntimeException("ECS listBucketObjects failed: " + e.getMessage(), e);
        }
        return keys;
    }

    // ---------------- delete bucket (Mgmt API) ----------------
    @Override
    public boolean deleteBucket(final BucketTO bucket, final long storeId) {
        final String bucketName = bucket.getName();

        final Map<String, String> ds = storeDetailsDao.getDetails(storeId);
        final EcsCfg cfg = ecsCfgFromDetails(ds, storeId);

        final String url = cfg.mgmtUrl + "/object/bucket/" + bucketName + "/deactivate?namespace=" + cfg.ns;

        try {
            return mgmtCallWithRetry401(cfg, token -> {
                try (CloseableHttpClient http = buildHttpClient(cfg.insecure)) {
                    final HttpPost post = new HttpPost(url);
                    post.setHeader("X-SDS-AUTH-TOKEN", token);

                    try (CloseableHttpResponse r = http.execute(post)) {
                        final int st = r.getStatusLine().getStatusCode();
                        final String body = r.getEntity() != null
                                ? EntityUtils.toString(r.getEntity(), StandardCharsets.UTF_8)
                                : "";

                        if (st == 401) throw new EcsUnauthorizedException("ECS deleteBucket got 401");

                        if (st == 200 || st == 204) {
                            logger.info("ECS bucket deactivated (deleted): '{}'", bucketName);
                            return true;
                        }

                        final String lb = body.toLowerCase(Locale.ROOT);
                        if (st == 400 || st == 409) {
                            if (lb.contains("not empty") || lb.contains("keypool not empty") || lb.contains("60019")) {
                                throw new CloudRuntimeException("Cannot delete bucket '" + bucketName + "': bucket is not empty");
                            }
                        }

                        if (st == 404) {
                            logger.info("ECS deleteBucket: '{}' not found; treating as already deleted.", bucketName);
                            return true;
                        }

                        logger.error("ECS delete bucket '{}' failed: HTTP {} body={}", bucketName, st, body);
                        throw new CloudRuntimeException("Failed to delete ECS bucket '" + bucketName + "': HTTP " + st);
                    }
                }
            });
        } catch (CloudRuntimeException cre) {
            throw cre;
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to delete ECS bucket '" + bucketName + "': " + e.getMessage(), e);
        }
    }

    @Override public AccessControlList getBucketAcl(final BucketTO bucket, final long storeId) { return null; }
    @Override public void setBucketAcl(final BucketTO bucket, final AccessControlList acl, final long storeId) { /* not supported */ }

    // ---------------- Policy ----------------
    @Override
    public void setBucketPolicy(final BucketTO bucket, final String policy, final long storeId) {
        final String b = bucket.getName();

        final Map<String, String> ds = storeDetailsDao.getDetails(storeId);
        final EcsCfg cfg = ecsCfgFromDetails(ds, storeId);

        final String url;
        try {
            url = cfg.mgmtUrl + "/object/bucket/" + b + "/policy?namespace=" +
                    java.net.URLEncoder.encode(cfg.ns, java.nio.charset.StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            throw new CloudRuntimeException("ECS setBucketPolicy: failed to encode namespace", e);
        }

        final String req = policy == null ? "" : policy.trim();
        final boolean wantPublic =
                "public".equalsIgnoreCase(req) || "public-read".equalsIgnoreCase(req);
        final boolean wantPrivate =
                req.isEmpty() || "{}".equals(req) || "private".equalsIgnoreCase(req);

        if (!wantPublic && !wantPrivate && !req.startsWith("{")) {
            throw new CloudRuntimeException("ECS setBucketPolicy: unsupported policy value '" + policy +
                    "'. Use 'public', 'private', or raw JSON.");
        }

        try {
            mgmtCallWithRetry401(cfg, token -> {
                final String current = getBucketPolicyRaw(url, token, cfg.insecure); // "" if none
                final boolean hasPolicy = current != null && !current.isBlank();

                if (wantPrivate) {
                    if (!hasPolicy) {
                        logger.info("ECS setBucketPolicy: already private (no policy). bucket='{}'", b);
                        return null;
                    }
                    deleteBucketPolicyHttp(url, token, cfg.insecure);
                    logger.info("ECS setBucketPolicy: removed policy via DELETE. bucket='{}'", b);
                    return null;
                }

                if (wantPublic && hasPolicy) {
                    logger.info("ECS setBucketPolicy: policy already present; leaving as-is. bucket='{}'", b);
                    return null;
                }

                final String policyJson = req.startsWith("{") ? req :
                        ("{\n" +
                        "  \"Version\":\"2012-10-17\",\n" +
                        "  \"Statement\":[{\n" +
                        "    \"Sid\":\"PublicReadGetObject\",\n" +
                        "    \"Effect\":\"Allow\",\n" +
                        "    \"Principal\":\"*\",\n" +
                        "    \"Action\":[\"s3:GetObject\"],\n" +
                        "    \"Resource\":[\"arn:aws:s3:::" + b + "/*\"]\n" +
                        "  }]\n" +
                        "}");

                putBucketPolicy(url, token, policyJson, cfg.insecure);
                logger.info("ECS setBucketPolicy: applied policy (bucket='{}').", b);
                return null;
            });
        } catch (CloudRuntimeException cre) {
            throw cre;
        } catch (Exception e) {
            throw new CloudRuntimeException("ECS setBucketPolicy error for bucket '" + b + "': " + e.getMessage(), e);
        }
    }

    @Override
    public BucketPolicy getBucketPolicy(final BucketTO bucket, final long storeId) {
        final String bucketName = bucket.getName();

        final Map<String, String> ds = storeDetailsDao.getDetails(storeId);
        final EcsCfg cfg = ecsCfgFromDetails(ds, storeId);

        final String url;
        try {
            url = cfg.mgmtUrl + "/object/bucket/" + bucketName + "/policy?namespace=" +
                    java.net.URLEncoder.encode(cfg.ns, java.nio.charset.StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            throw new CloudRuntimeException("ECS getBucketPolicy: failed to encode namespace", e);
        }

        try {
            return mgmtCallWithRetry401(cfg, token -> {
                try (CloseableHttpClient http = buildHttpClient(cfg.insecure)) {
                    final HttpGet get = new HttpGet(url);
                    get.setHeader("X-SDS-AUTH-TOKEN", token);

                    try (CloseableHttpResponse resp = http.execute(get)) {
                        final int st = resp.getStatusLine().getStatusCode();
                        if (st == 401) throw new EcsUnauthorizedException("ECS getBucketPolicy got 401");

                        final String body = resp.getEntity() == null ? "" :
                                EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8).trim();

                        final BucketPolicy bp = new BucketPolicy();
                        if (st == 200) {
                            bp.setPolicyText((body.isEmpty() || "{}".equals(body)) ? "{}" : body);
                            return bp;
                        }
                        if (st == 204 || st == 404 || ((st / 100) == 2 && body.isEmpty())) {
                            bp.setPolicyText("{}");
                            return bp;
                        }

                        throw new CloudRuntimeException("ECS getBucketPolicy failed: HTTP " + st + " body=" + body);
                    }
                }
            });
        } catch (Exception e) {
            if (e instanceof CloudRuntimeException) throw (CloudRuntimeException) e;
            throw new CloudRuntimeException("ECS getBucketPolicy error: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteBucketPolicy(final BucketTO bucket, final long storeId) {
        setBucketPolicy(bucket, "{}", storeId);
    }

    // --- Encryption (post-create): ECS cannot change it after creation ---
    @Override
    public boolean setBucketEncryption(final BucketTO bucket, final long storeId) {
        final String bucketName = bucket != null ? bucket.getName() : "<null>";
        logger.info("ECS setBucketEncryption('{}'): ECS only supports encryption at bucket creation. Treating as no-op.", bucketName);
        return true;
    }

    @Override
    public boolean deleteBucketEncryption(final BucketTO bucket, final long storeId) {
        final String bucketName = bucket != null ? bucket.getName() : "<null>";
        final String msg =
                "Dell ECS bucket encryption can only be chosen at bucket creation; " +
                "it cannot be disabled afterwards (bucket=" + bucketName + ")";
        logger.error("ECS deleteBucketEncryption('{}') requested but {}", bucketName, msg);
        throw new CloudRuntimeException(msg);
    }

    @Override
    public boolean setBucketVersioning(final BucketTO bucket, final long storeId) {
        return setOrSuspendVersioning(bucket, storeId, true);
    }

    @Override
    public boolean deleteBucketVersioning(final BucketTO bucket, final long storeId) {
        return setOrSuspendVersioning(bucket, storeId, false);
    }

    private boolean setOrSuspendVersioning(final BucketTO bucket, final long storeId, final boolean enable) {
        final Map<String, String> ds = storeDetailsDao.getDetails(storeId);
        final S3Endpoint ep = resolveS3Endpoint(ds, storeId);
        final boolean insecure = "true".equalsIgnoreCase(ds.getOrDefault(INSECURE, "false"));

        if (ep == null || isBlank(ep.host)) {
            logger.warn("ECS: {}BucketVersioning requested but S3 endpoint is not resolvable; skipping.",
                    enable ? "set" : "delete");
            return true;
        }

        final String bucketName = bucket.getName();
        final String desired = enable ? "Enabled" : "Suspended";

        final int maxTries = 45;
        for (int attempt = 1; attempt <= maxTries; attempt++) {
            BucketVO vo = resolveBucketVO(bucket, storeId);
            if (vo == null) {
                vo = findBucketVOByStoreAndName(storeId, bucketName);
                if (vo == null) {
                    vo = findBucketVOAnyByName(bucketName);
                }
            }

            String accessKey = vo != null ? safeString(vo, "getAccessKey") : null;
            String secretKey = vo != null ? safeString(vo, "getSecretKey") : null;

            if (!isBlank(accessKey) && !isBlank(secretKey)) {
                try (CloseableHttpClient http = buildHttpClient(insecure)) {
                    setS3BucketVersioning(http, ep.scheme, ep.host, bucketName, accessKey, secretKey, desired);
                    logger.info("ECS: S3 versioning {} for bucket='{}' using bucket-scoped keys (attempt {}/{}).",
                            desired, bucketName, attempt, maxTries);
                    return true;
                } catch (Exception e) {
                    logger.warn("ECS: versioning {} for '{}' (bucket keys) failed on attempt {}/{}: {}",
                            desired, bucketName, attempt, maxTries, e.getMessage());
                }
            }

            long accountId = -1L;
            if (vo != null) {
                try { accountId = vo.getAccountId(); } catch (Throwable ignore) { }
            }
            if (accountId <= 0) {
                accountId = getLongFromGetter(bucket, "getAccountId", -1L);
            }
            if (accountId <= 0) {
                Long aid = resolveAccountIdViaMgmt(bucketName, ds, insecure);
                if (aid != null && aid > 0) accountId = aid;
            }

            if (accountId > 0) {
                String accKey = valueOrNull(accountDetailsDao.findDetail(accountId, AD_KEY_ACCESS));
                String secKey = valueOrNull(accountDetailsDao.findDetail(accountId, AD_KEY_SECRET));

                if (isBlank(accKey) || isBlank(secKey)) {
                    final EcsCfg cfg = ecsCfgFromDetails(ds, storeId);
                    final Account acct = accountDao.findById(accountId);
                    if (acct != null) {
                        final String ownerUser = "cs-" + acct.getUuid();
                        try {
                            ensureAccountUserAndSecret(accountId, ownerUser, cfg.mgmtUrl, cfg.saUser, cfg.saPass, cfg.ns, cfg.insecure);
                        } catch (Exception e) {
                            logger.debug("ECS: ensureAccountUserAndSecret failed (attempt {}): {}", attempt, e.getMessage());
                        }
                        accKey = valueOrNull(accountDetailsDao.findDetail(accountId, AD_KEY_ACCESS));
                        secKey = valueOrNull(accountDetailsDao.findDetail(accountId, AD_KEY_SECRET));
                    }
                }

                if (!isBlank(accKey) && !isBlank(secKey)) {
                    try (CloseableHttpClient http = buildHttpClient(insecure)) {
                        setS3BucketVersioning(http, ep.scheme, ep.host, bucketName, accKey, secKey, desired);
                        logger.info("ECS: S3 versioning {} for bucket='{}' using account-scoped keys (attempt {}/{}).",
                                desired, bucketName, attempt, maxTries);
                        return true;
                    } catch (Exception e) {
                        logger.warn("ECS: versioning {} for '{}' (account keys) failed on attempt {}/{}: {}",
                                desired, bucketName, attempt, maxTries, e.getMessage());
                    }
                }
            }

            if (attempt < maxTries) {
                try { Thread.sleep(1000L); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }

        logger.warn("ECS: versioning {} for '{}' gave up after {} attempts; leaving as-is.", desired, bucket.getName(), 45);
        return true;
    }

    // ----- S3 Versioning (SigV2 path-style) -----
    private void setS3BucketVersioning(final CloseableHttpClient http,
                                       final String scheme,
                                       final String host,
                                       final String bucketName,
                                       final String accessKey,
                                       final String secretKey,
                                       final String status) throws Exception {
        final String body = "<VersioningConfiguration xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\"><Status>" + status + "</Status></VersioningConfiguration>";
        final byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        final String contentType = "application/xml";
        final String contentMd5  = base64Md5(bodyBytes);
        final String dateHdr     = rfc1123Now();

        final String canonicalResource = "/" + bucketName + "?versioning";
        final String sts = "PUT\n" + contentMd5 + "\n" + contentType + "\n" + dateHdr + "\n" + canonicalResource;
        final String signature = hmacSha1Base64(sts, secretKey);

        final String url = scheme + "://" + host + "/" + bucketName + "?versioning";
        final HttpPut put = new HttpPut(url);
        put.setHeader("Host", host);
        put.setHeader("Date", dateHdr);
        put.setHeader("Authorization", "AWS " + accessKey + ":" + signature);
        put.setHeader("Content-Type", contentType);
        put.setHeader("Content-MD5", contentMd5);
        put.setEntity(new StringEntity(body, StandardCharsets.UTF_8));

        try (CloseableHttpResponse resp = http.execute(put)) {
            final int st = resp.getStatusLine().getStatusCode();
            final String rb = resp.getEntity() != null ? EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8) : "";
            if (st != 200 && st != 204) {
                throw new CloudRuntimeException("S3 versioning " + status + " failed: HTTP " + st + " body=" + rb);
            }
        }
    }

    /**
     * Post-create quota changes (best-effort, never throws).
     */
    @Override
    public void setBucketQuota(final BucketTO bucket, final long storeId, final long size) {
        if (size <= 0) {
            logger.debug("ECS setBucketQuota ignored for {}: non-positive size {}", bucket.getName(), size);
            return;
        }

        final Map<String, String> ds = storeDetailsDao.getDetails(storeId);
        final EcsCfg cfg = ecsCfgFromDetails(ds, storeId);
        final String bucketName = bucket.getName();

        try {
            mgmtCallWithRetry401(cfg, token -> {
                try (CloseableHttpClient http = buildHttpClient(cfg.insecure)) {
                    Integer currentGb = null;
                    try {
                        final HttpGet get = new HttpGet(cfg.mgmtUrl + "/object/bucket/" + bucketName + "/quota");
                        get.setHeader("X-SDS-AUTH-TOKEN", token);
                        try (CloseableHttpResponse r = http.execute(get)) {
                            final int st = r.getStatusLine().getStatusCode();
                            if (st == 401) throw new EcsUnauthorizedException("ECS get quota got 401");
                            if (st == 200) {
                                final String xml = r.getEntity() != null ? EntityUtils.toString(r.getEntity(), StandardCharsets.UTF_8) : "";
                                currentGb = parseIntTag(xml, "blockSize");
                                if (currentGb == null) currentGb = parseIntTag(xml, "notificationSize");
                            }
                        }
                    } catch (EcsUnauthorizedException u) {
                        throw u;
                    } catch (Exception e) {
                        logger.debug("ECS get quota for {} failed (non-fatal): {}", bucketName, e.getMessage());
                    }

                    if (currentGb != null && size <= currentGb) {
                        logger.info("ECS setBucketQuota noop for '{}': requested {}GB <= current {}GB", bucketName, size, currentGb);
                        return null;
                    }

                    final String quotaBody =
                            "<bucket_quota_param>" +
                            "<blockSize>" + size + "</blockSize>" +
                            "<notificationSize>" + size + "</notificationSize>" +
                            "<namespace>" + cfg.ns + "</namespace>" +
                            "</bucket_quota_param>";

                    final HttpPut put = new HttpPut(cfg.mgmtUrl + "/object/bucket/" + bucketName + "/quota");
                    put.setHeader("X-SDS-AUTH-TOKEN", token);
                    put.setHeader("Content-Type", "application/xml");
                    put.setEntity(new StringEntity(quotaBody, StandardCharsets.UTF_8));

                    try (CloseableHttpResponse r2 = http.execute(put)) {
                        final int st2 = r2.getStatusLine().getStatusCode();
                        final String rb2 = r2.getEntity() != null ? EntityUtils.toString(r2.getEntity(), StandardCharsets.UTF_8) : "";
                        if (st2 == 401) throw new EcsUnauthorizedException("ECS set quota got 401");
                        if (st2 != 200 && st2 != 204) {
                            logger.warn("ECS set quota failed for {}: HTTP {} body={}. Ignoring.", bucketName, st2, rb2);
                            return null;
                        }
                    }

                    logger.info("ECS quota set for bucket='{}' newQuota={}GB", bucketName, size);
                    return null;
                }
            });
        } catch (Exception e) {
            logger.warn("ECS setBucketQuota encountered error for {}: {} (ignored)", bucketName, e.getMessage());
        }
    }

    @Override
    public Map<String, Long> getAllBucketsUsage(final long storeId) {
        throw new CloudRuntimeException("Bucket usage aggregation is not implemented via Mgmt API in this plugin.");
    }

    // ---------------- helpers ----------------

    private static final class EcsCfg {
        final String mgmtUrl;
        final String saUser;
        final String saPass;
        final String ns;
        final boolean insecure;

        EcsCfg(final String mgmtUrl, final String saUser, final String saPass, final String ns, final boolean insecure) {
            this.mgmtUrl = mgmtUrl;
            this.saUser = saUser;
            this.saPass = saPass;
            this.ns = ns;
            this.insecure = insecure;
        }
    }

    private EcsCfg ecsCfgFromDetails(final Map<String, String> ds, final long storeId) {
        final String mgmtUrl   = trimTail(ds.get(MGMT_URL));
        final String saUser    = ds.get(SA_USER);
        final String saPass    = ds.get(SA_PASS);
        final String ns        = org.apache.commons.lang3.StringUtils.isBlank(ds.get(NAMESPACE)) ? "default" : ds.get(NAMESPACE);
        final boolean insecure = "true".equalsIgnoreCase(ds.getOrDefault(INSECURE, "false"));

        if (isBlank(mgmtUrl) || isBlank(saUser) || isBlank(saPass)) {
            throw new CloudRuntimeException("ECS: missing mgmt_url/sa_user/sa_password for store id=" + storeId);
        }
        return new EcsCfg(mgmtUrl, saUser, saPass, ns, insecure);
    }

    private <T> T mgmtCallWithRetry401(final EcsCfg cfg, final WithToken<T> op) throws Exception {
        try {
            return op.run(getAuthToken(cfg.mgmtUrl, cfg.saUser, cfg.saPass, cfg.insecure));
        } catch (EcsUnauthorizedException u) {
            invalidateToken(cfg.mgmtUrl, cfg.saUser);
            return op.run(getAuthToken(cfg.mgmtUrl, cfg.saUser, cfg.saPass, cfg.insecure));
        }
    }

    private void invalidateToken(final String mgmtUrl, final String user) {
        final TokenKey key = new TokenKey(trimTail(mgmtUrl), user);
        TOKEN_CACHE.remove(key);
    }

    private String getAuthToken(final String mgmtUrl, final String user, final String pass, final boolean insecure) {
        final String mu = trimTail(mgmtUrl);
        final TokenKey key = new TokenKey(mu, user);

        final TokenEntry cached = TOKEN_CACHE.get(key);
        if (cached != null && cached.validNow()) {
            return cached.token;
        }

        final Object lock = TOKEN_LOCKS.computeIfAbsent(key, k -> new Object());
        synchronized (lock) {
            final TokenEntry cached2 = TOKEN_CACHE.get(key);
            if (cached2 != null && cached2.validNow()) {
                return cached2.token;
            }
            final TokenEntry fresh = loginAndGetTokenFresh(mu, user, pass, insecure);
            TOKEN_CACHE.put(key, fresh);
            return fresh.token;
        }
    }

    private TokenEntry loginAndGetTokenFresh(final String mgmtUrl, final String user, final String pass, final boolean insecure) {
        try (CloseableHttpClient http = buildHttpClient(insecure)) {
            final HttpGet get = new HttpGet(mgmtUrl + "/login");
            UsernamePasswordCredentials creds = new UsernamePasswordCredentials(user, pass);
            get.addHeader(new BasicScheme().authenticate(creds, get, null));

            try (CloseableHttpResponse resp = http.execute(get)) {
                final int status = resp.getStatusLine().getStatusCode();
                if (status != 200 && status != 201) {
                    throw new CloudRuntimeException("ECS /login failed: HTTP " + status);
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

                if (logger.isDebugEnabled()) {
                    logger.debug("ECS token fetched for user='{}' (maxAge={}s, effective={}s)", user, maxAgeSec, effectiveSec);
                }
                return new TokenEntry(token, expiresAtMs);
            }
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to obtain ECS auth token: " + e.getMessage(), e);
        }
    }

    private static String valueOrNull(final AccountDetailVO d) {
        return d == null ? null : d.getValue();
    }

    private static Integer safeIntFromGetter(final Object o, final String getter) {
        try {
            Object v = o.getClass().getMethod(getter).invoke(o);
            if (v instanceof Number) return ((Number) v).intValue();
            if (v instanceof String && !((String) v).isEmpty()) return Integer.parseInt((String) v);
        } catch (Exception ignore) { }
        return null;
    }

    private static boolean getBooleanFlagLoose(final Object o, final String getMethod, final String isMethod, final boolean defVal) {
        if (o == null) return defVal;
        Object v = null;
        try { v = o.getClass().getMethod(getMethod).invoke(o); } catch (NoSuchMethodException ignored) { } catch (Exception ignored) { }
        if (v == null) {
            try { v = o.getClass().getMethod(isMethod).invoke(o); } catch (NoSuchMethodException ignored) { } catch (Exception ignored) { }
        }
        if (v == null) return defVal;

        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof Number)  return ((Number) v).intValue() != 0;
        if (v instanceof String) {
            String s = ((String) v).trim();
            if ("true".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s) || "on".equalsIgnoreCase(s) || "1".equals(s)) return true;
            if ("false".equalsIgnoreCase(s) || "no".equalsIgnoreCase(s) || "off".equalsIgnoreCase(s) || "0".equals(s)) return false;
        }
        return defVal;
    }

    private static Integer parseIntTag(final String xml, final String tag) {
        if (xml == null) return null;
        final String open = "<" + tag + ">";
        final String close = "</" + tag + ">";
        final int i = xml.indexOf(open);
        final int j = xml.indexOf(close);
        if (i >= 0 && j > i) {
            final String val = xml.substring(i + open.length(), j).trim();
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException ignore) { }
        }
        return null;
    }

    private static String parseXmlTag(final String xml, final String tag) {
        if (xml == null) return null;
        final String open = "<" + tag + ">";
        final String close = "</" + tag + ">";
        final int i = xml.indexOf(open);
        final int j = xml.indexOf(close);
        if (i >= 0 && j > i) {
            return xml.substring(i + open.length(), j);
        }
        return null;
    }

    private static boolean isBlank(final String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String trimTail(final String s) {
        if (s == null) return null;
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static String normalizeHostOnly(final String hostOrUrl) {
        if (hostOrUrl == null) return null;
        String h = hostOrUrl.trim();
        if (h.startsWith("http://")) h = h.substring("http://".length());
        if (h.startsWith("https://")) h = h.substring("https://".length());
        while (h.endsWith("/")) h = h.substring(0, h.length() - 1);
        return h;
    }

    private CloseableHttpClient buildHttpClient(final boolean insecure) {
        if (!insecure) return HttpClients.createDefault();
        try {
            final TrustStrategy trustAll = (chain, authType) -> true;
            final SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial(null, trustAll)
                    .build();
            return HttpClients.custom()
                    .setSSLContext(sslContext)
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to build insecure HttpClient", e);
        }
    }

    /** GET /object/user-secret-keys/{username} and parse any existing secrets. */
    private List<String> fetchEcsUserSecrets(final CloseableHttpClient http,
                                            final String mgmtUrl, final String token, final String username) throws Exception {
        final HttpGet get = new HttpGet(mgmtUrl + "/object/user-secret-keys/" + username);
        get.setHeader("X-SDS-AUTH-TOKEN", token);
        try (CloseableHttpResponse r = http.execute(get)) {
            final int st = r.getStatusLine().getStatusCode();
            if (st == 401) throw new EcsUnauthorizedException("ECS fetch secrets got 401");
            if (st == 200) {
                final String xml = r.getEntity() != null ? EntityUtils.toString(r.getEntity(), StandardCharsets.UTF_8) : "";
                final java.util.ArrayList<String> out = new java.util.ArrayList<>();
                final String s1 = parseXmlTag(xml, "secret_key_1");
                final String s2 = parseXmlTag(xml, "secret_key_2");
                final String e1 = parseXmlTag(xml, "secret_key_1_exist");
                final String e2 = parseXmlTag(xml, "secret_key_2_exist");
                if ("true".equalsIgnoreCase(e1) && !isBlank(s1)) out.add(s1.trim());
                if ("true".equalsIgnoreCase(e2) && !isBlank(s2)) out.add(s2.trim());
                return out;
            }
            return java.util.Collections.emptyList();
        }
    }

    private void ensureAccountUserAndSecret(final long accountId,
                                           final String username,
                                           final String mgmtUrl,
                                           final String saUser,
                                           final String saPass,
                                           final String ns,
                                           final boolean insecure) {
        final String haveAcc = valueOrNull(accountDetailsDao.findDetail(accountId, AD_KEY_ACCESS));
        final String haveSec = valueOrNull(accountDetailsDao.findDetail(accountId, AD_KEY_SECRET));

        final EcsCfg cfg = new EcsCfg(trimTail(mgmtUrl), saUser, saPass, ns, insecure);

        try {
            mgmtCallWithRetry401(cfg, token -> {
                try (CloseableHttpClient http = buildHttpClient(insecure)) {
                    // Ensure/CREATE user (idempotent)
                    final String createUserXml =
                            "<user_create_param>" +
                            "<user>" + username + "</user>" +
                            "<namespace>" + ns + "</namespace>" +
                            "<tags></tags>" +
                            "</user_create_param>";

                    final HttpPost postUser = new HttpPost(mgmtUrl + "/object/users");
                    postUser.setHeader("X-SDS-AUTH-TOKEN", token);
                    postUser.setHeader("Content-Type", "application/xml");
                    postUser.setEntity(new StringEntity(createUserXml, StandardCharsets.UTF_8));
                    try (CloseableHttpResponse r = http.execute(postUser)) {
                        final int st = r.getStatusLine().getStatusCode();
                        final String rb = r.getEntity() != null ? EntityUtils.toString(r.getEntity(), StandardCharsets.UTF_8) : "";
                        if (st == 401) throw new EcsUnauthorizedException("ECS ensure user got 401");
                        if (st == 200 || st == 201) {
                            logger.info("ECS user ensured/created for accountId={} -> {}", accountId, username);
                        } else if (st == 400 && rb != null && rb.contains("already exists")) {
                            logger.info("ECS user {} already exists (idempotent).", username);
                        } else {
                            logger.error("ECS user creation failed: status={} body={}", st, rb);
                            throw new CloudRuntimeException("ECS user creation failed: HTTP " + st);
                        }
                    }

                    // If ACS already has key -> do NOT create another.
                    if (!isBlank(haveAcc) && !isBlank(haveSec)) {
                        logger.info("ECS single-key policy: accountId={} already has keys stored in ACS; skipping secret creation.", accountId);

                        // Optional reconciliation: if ECS has no secret, push ACS secret
                        try {
                            List<String> ecsKeys = fetchEcsUserSecrets(http, mgmtUrl, token, username);
                            if (ecsKeys.isEmpty()) {
                                final String skXml =
                                        "<user_secret_key_create>" +
                                        "<namespace>" + ns + "</namespace>" +
                                        "<secretkey>" + haveSec + "</secretkey>" +
                                        "</user_secret_key_create>";

                                final HttpPost postKey = new HttpPost(mgmtUrl + "/object/user-secret-keys/" + username);
                                postKey.setHeader("X-SDS-AUTH-TOKEN", token);
                                postKey.setHeader("Content-Type", "application/xml");
                                postKey.setEntity(new StringEntity(skXml, StandardCharsets.UTF_8));
                                try (CloseableHttpResponse kr = http.execute(postKey)) {
                                    final int st = kr.getStatusLine().getStatusCode();
                                    final String rb = kr.getEntity() != null ? EntityUtils.toString(kr.getEntity(), StandardCharsets.UTF_8) : "";
                                    if (st == 401) throw new EcsUnauthorizedException("ECS reconcile secret got 401");
                                    if (st == 200 || st == 201) {
                                        logger.info("ECS secret reconciled for user {} (secret taken from ACS).", username);
                                    } else if (st == 400 && rb != null && rb.contains("already has") && rb.contains("valid keys")) {
                                        logger.info("ECS user {} already has valid secret(s); reconciliation not needed.", username);
                                    } else {
                                        logger.warn("ECS secret reconcile for {} returned HTTP {} body={} (continuing).", username, st, rb);
                                    }
                                }
                            }
                        } catch (EcsUnauthorizedException u) {
                            throw u;
                        } catch (Exception e) {
                            logger.debug("ECS secret reconcile check skipped for {}: {}", username, e.getMessage());
                        }
                        return null;
                    }

                    // ACS does NOT have key -> try to ADOPT existing ECS key first
                    try {
                        List<String> ecsKeys = fetchEcsUserSecrets(http, mgmtUrl, token, username);
                        if (!ecsKeys.isEmpty()) {
                            final String adopt = ecsKeys.get(0);
                            if (isBlank(haveAcc)) accountDetailsDao.addDetail(accountId, AD_KEY_ACCESS, username, false);
                            if (isBlank(haveSec)) accountDetailsDao.addDetail(accountId, AD_KEY_SECRET, adopt,   false);
                            logger.info("Adopted existing ECS secret for user {} into ACS (no new key created).", username);
                            return null;
                        }
                    } catch (EcsUnauthorizedException u) {
                        throw u;
                    } catch (Exception e) {
                        logger.debug("Failed to fetch existing ECS keys for {} (proceeding to create one): {}", username, e.getMessage());
                    }

                    // No ECS key either -> create exactly ONE new secret and store in ACS
                    final String newSecret = java.util.UUID.randomUUID().toString().replace("-", "");
                    final String skXmlCreate =
                            "<user_secret_key_create>" +
                            "<namespace>" + ns + "</namespace>" +
                            "<secretkey>" + newSecret + "</secretkey>" +
                            "</user_secret_key_create>";

                    final HttpPost postKey2 = new HttpPost(mgmtUrl + "/object/user-secret-keys/" + username);
                    postKey2.setHeader("X-SDS-AUTH-TOKEN", token);
                    postKey2.setHeader("Content-Type", "application/xml");
                    postKey2.setEntity(new StringEntity(skXmlCreate, StandardCharsets.UTF_8));
                    try (CloseableHttpResponse kr2 = http.execute(postKey2)) {
                        final int st = kr2.getStatusLine().getStatusCode();
                        final String rb = kr2.getEntity() != null ? EntityUtils.toString(kr2.getEntity(), StandardCharsets.UTF_8) : "";
                        if (st == 401) throw new EcsUnauthorizedException("ECS create secret got 401");
                        if (st != 200 && st != 201) {
                            if (st == 400 && rb != null && rb.contains("already has") && rb.contains("valid keys")) {
                                List<String> ecsKeys = fetchEcsUserSecrets(http, mgmtUrl, token, username);
                                if (!ecsKeys.isEmpty()) {
                                    final String adopt = ecsKeys.get(0);
                                    if (isBlank(haveAcc)) accountDetailsDao.addDetail(accountId, AD_KEY_ACCESS, username, false);
                                    if (isBlank(haveSec)) accountDetailsDao.addDetail(accountId, AD_KEY_SECRET, adopt,   false);
                                    logger.info("Race: ECS already has key(s). Adopted existing secret for {} into ACS.", username);
                                    return null;
                                }
                            }
                            logger.error("ECS create secret-key failed for {}: status={} body={}", username, st, rb);
                            throw new CloudRuntimeException("ECS secret-key creation failed: HTTP " + st);
                        }
                    }

                    if (isBlank(haveAcc)) accountDetailsDao.addDetail(accountId, AD_KEY_ACCESS, username, false);
                    if (isBlank(haveSec)) accountDetailsDao.addDetail(accountId, AD_KEY_SECRET, newSecret, false);
                    logger.info("ECS secret key created and stored for user={} (accountId={})", username, accountId);
                    return null;
                }
            });
        } catch (CloudRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to ensure/reconcile ECS user/secret: " + e.getMessage(), e);
        }
    }

    // ---------- Endpoint resolving for S3 (from s3_host) ----------
    private static final class S3Endpoint {
        final String scheme; // "http" or "https"
        final String host;   // hostname only
        S3Endpoint(final String scheme, final String host) { this.scheme = scheme; this.host = host; }
    }

    private S3Endpoint resolveS3Endpoint(final Map<String, String> ds, final long storeId) {
        String host = normalizeHostOnly(ds.get(S3_HOST)); // accept host or URL from UI
        final String scheme = "https";
        if (isBlank(host)) {
            // last-resort (but prefer failing loudly earlier)
            host = normalizeHostOnly(ds.get("host"));
        }
        return new S3Endpoint(scheme, host);
    }

    private String resolveS3HostForUI(final long storeId, final Map<String, String> ds) {
        String host = normalizeHostOnly(ds.get(S3_HOST));
        if (isBlank(host)) host = normalizeHostOnly(ds.get("host"));
        return host;
    }

    // ---------- Mgmt owner → accountId fallback ----------
    private Long resolveAccountIdViaMgmt(final String bucketName, final Map<String,String> ds, final boolean insecure) {
        final String mgmtUrl = trimTail(ds.get(MGMT_URL));
        final String saUser  = ds.get(SA_USER);
        final String saPass  = ds.get(SA_PASS);
        if (isBlank(mgmtUrl) || isBlank(saUser) || isBlank(saPass)) return null;

        final EcsCfg cfg = new EcsCfg(mgmtUrl, saUser, saPass,
                org.apache.commons.lang3.StringUtils.isBlank(ds.get(NAMESPACE)) ? "default" : ds.get(NAMESPACE),
                insecure);

        try {
            return mgmtCallWithRetry401(cfg, token -> {
                try (CloseableHttpClient http = buildHttpClient(insecure)) {
                    final String owner = fetchBucketOwnerViaMgmt(http, mgmtUrl, token, bucketName);
                    if (!isBlank(owner) && owner.startsWith("cs-") && owner.length() > 3) {
                        final String uuid = owner.substring(3);
                        try {
                            Account acct = accountDao.findByUuid(uuid);
                            if (acct != null) return acct.getId();
                        } catch (Throwable ignore) { }
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            logger.debug("ECS: resolveAccountIdViaMgmt '{}' failed: {}", bucketName, e.getMessage());
            return null;
        }
    }

    private String fetchBucketOwnerViaMgmt(final CloseableHttpClient http, final String mgmtUrl, final String token, final String bucketName) throws Exception {
        final HttpGet get = new HttpGet(mgmtUrl + "/object/bucket/" + bucketName);
        get.setHeader("X-SDS-AUTH-TOKEN", token);
        try (CloseableHttpResponse r = http.execute(get)) {
            final int st = r.getStatusLine().getStatusCode();
            if (st == 401) throw new EcsUnauthorizedException("ECS fetch bucket owner got 401");
            if (st == 200) {
                final String xml = r.getEntity() != null ? EntityUtils.toString(r.getEntity(), StandardCharsets.UTF_8) : "";
                final String owner = parseXmlTag(xml, "owner");
                if (!isBlank(owner)) return owner.trim();
            }
            return null;
        }
    }

    // ---------- Reflection helpers / VO lookups ----------
    private static long getLongFromGetter(final Object o, final String getter, final long defVal) {
        if (o == null) return defVal;
        try {
            Object v = o.getClass().getMethod(getter).invoke(o);
            if (v instanceof Number) return ((Number) v).longValue();
            if (v instanceof String && !((String) v).isEmpty()) return Long.parseLong((String) v);
        } catch (Throwable ignore) { }
        return defVal;
    }

    private static String safeString(final Object o, final String getter) {
        if (o == null) return null;
        try {
            Object v = o.getClass().getMethod(getter).invoke(o);
            return v != null ? v.toString() : null;
        } catch (Throwable ignore) { }
        return null;
    }

    private BucketVO resolveBucketVO(final BucketTO bucket, final long storeId) {
        long id = getLongFromGetter(bucket, "getId", -1L);
        if (id <= 0) id = getLongFromGetter(bucket, "getBucketId", -1L);
        if (id > 0) {
            try { return bucketDao.findById(id); } catch (Throwable ignore) { }
        }

        String uuid = null;
        try { uuid = (String) bucket.getClass().getMethod("getUuid").invoke(bucket); } catch (Throwable ignore) { }
        if (isBlank(uuid)) {
            try { uuid = (String) bucket.getClass().getMethod("getId").invoke(bucket); } catch (Throwable ignore) { }
        }
        if (!isBlank(uuid)) {
            try {
                java.lang.reflect.Method m = bucketDao.getClass().getMethod("findByUuid", String.class);
                Object r = m.invoke(bucketDao, uuid);
                if (r instanceof BucketVO) return (BucketVO) r;
            } catch (NoSuchMethodException ignored) { } catch (Throwable ignore) { }
        }

        final String name = bucket.getName();
        try {
            try {
                java.lang.reflect.Method m1 = bucketDao.getClass().getMethod("findByName", String.class);
                Object r1 = m1.invoke(bucketDao, name);
                if (r1 instanceof BucketVO) return (BucketVO) r1;
            } catch (NoSuchMethodException ignored) { }
            try {
                java.lang.reflect.Method m2 = bucketDao.getClass().getMethod("findByName", String.class, long.class);
                Object r2 = m2.invoke(bucketDao, name, storeId);
                if (r2 instanceof BucketVO) return (BucketVO) r2;
            } catch (NoSuchMethodException ignored) { }
            try {
                java.lang.reflect.Method m3 = bucketDao.getClass().getMethod("findByName", long.class, String.class);
                Object r3 = m3.invoke(bucketDao, storeId, name);
                if (r3 instanceof BucketVO) return (BucketVO) r3;
            } catch (NoSuchMethodException ignored) { }
        } catch (Throwable t) {
            logger.debug("ECS: resolveBucketVO by name '{}' failed: {}", name, t.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private BucketVO findBucketVOByStoreAndName(final long storeId, final String name) {
        try {
            java.lang.reflect.Method m = bucketDao.getClass().getMethod("listByStoreId", long.class);
            Object res = m.invoke(bucketDao, storeId);
            if (res instanceof List<?>) {
                for (Object o : (List<?>) res) {
                    if (o instanceof BucketVO) {
                        BucketVO vo = (BucketVO) o;
                        try { if (name.equals(vo.getName())) return vo; } catch (Throwable ignore) { }
                    }
                }
            }
        } catch (NoSuchMethodException ignored) {
        } catch (Throwable t) {
            logger.debug("ECS: listByStoreId fallback failed: {}", t.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private BucketVO findBucketVOAnyByName(final String name) {
        try {
            java.lang.reflect.Method m = bucketDao.getClass().getMethod("listAll");
            Object res = m.invoke(bucketDao);
            if (res instanceof List<?>) {
                for (Object o : (List<?>) res) {
                    if (o instanceof BucketVO) {
                        BucketVO vo = (BucketVO) o;
                        try { if (name.equals(vo.getName())) return vo; } catch (Throwable ignore) { }
                    }
                }
            }
        } catch (NoSuchMethodException ignored) {
        } catch (Throwable t) {
            logger.debug("ECS: listAll scan failed: {}", t.getMessage());
        }
        return null;
    }

    /** First occurrence of <tag>value</tag>, no namespaces. Returns null if not found. */
    private static String extractTag(final String xml, final String tag) {
        if (xml == null) return null;
        final String open = "<" + tag + ">";
        final String close = "</" + tag + ">";
        int i = xml.indexOf(open);
        if (i < 0) return null;
        int j = xml.indexOf(close, i + open.length());
        if (j < 0) return null;
        return xml.substring(i + open.length(), j).trim();
    }

    /** All occurrences of <tag>value</tag>, no namespaces. */
    private static List<String> extractAllTags(final String xml, final String tag) {
        final List<String> out = new java.util.ArrayList<>();
        if (xml == null) return out;
        final String open = "<" + tag + ">";
        final String close = "</" + tag + ">";
        int from = 0;
        while (true) {
            int i = xml.indexOf(open, from);
            if (i < 0) break;
            int j = xml.indexOf(close, i + open.length());
            if (j < 0) break;
            out.add(xml.substring(i + open.length(), j).trim());
            from = j + close.length();
        }
        return out;
    }

    /** Pulls every <Contents>...<Key>...</Key>...</Contents> key into 'keys'. */
    private static void extractKeysFromListBucketXml(final String xml, final List<String> keys) {
        if (xml == null) return;
        final String contentsOpen = "<Contents>";
        final String contentsClose = "</Contents>";
        int from = 0;
        while (true) {
            int i = xml.indexOf(contentsOpen, from);
            if (i < 0) break;
            int j = xml.indexOf(contentsClose, i + contentsOpen.length());
            if (j < 0) break;
            String block = xml.substring(i, j + contentsClose.length());
            String key = extractTag(block, "Key");
            if (key != null && !key.isEmpty()) keys.add(key.trim());
            from = j + contentsClose.length();
        }
    }

    private static String base64Md5(final byte[] data) throws Exception {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(data);
        return Base64.getEncoder().encodeToString(digest);
    }

    private static String hmacSha1Base64(final String data, final String key) throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
        javax.crypto.spec.SecretKeySpec sk = new javax.crypto.spec.SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
        mac.init(sk);
        byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(raw);
    }

    private static String rfc1123Now() {
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter
                .ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US)
                .withZone(java.time.ZoneOffset.UTC);
        return fmt.format(java.time.Instant.now());
    }

    /** GET /policy raw body; returns "" if none (200 with empty/{} or 204/404). */
    private String getBucketPolicyRaw(final String url, final String token, final boolean insecure) {
        try (CloseableHttpClient http = buildHttpClient(insecure)) {
            final HttpGet get = new HttpGet(url);
            get.setHeader("X-SDS-AUTH-TOKEN", token);
            try (CloseableHttpResponse resp = http.execute(get)) {
                final int st = resp.getStatusLine().getStatusCode();
                if (st == 401) throw new EcsUnauthorizedException("ECS getBucketPolicyRaw got 401");
                final String body = resp.getEntity() == null ? "" :
                        EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8).trim();
                if (st == 200) return "{}".equals(body) ? "" : body;
                if (st == 204 || st == 404 || ((st / 100) == 2 && body.isEmpty())) return "";
                throw new CloudRuntimeException("ECS getBucketPolicy failed: HTTP " + st + " body=" + body);
            }
        } catch (EcsUnauthorizedException u) {
            throw u;
        } catch (Exception e) {
            throw new CloudRuntimeException("ECS getBucketPolicy error: " + e.getMessage(), e);
        }
    }

    /** PUT /policy with JSON. */
    private void putBucketPolicy(final String url, final String token, final String policyJson, final boolean insecure) {
        try (CloseableHttpClient http = buildHttpClient(insecure)) {
            final HttpPut put = new HttpPut(url);
            put.setHeader("X-SDS-AUTH-TOKEN", token);
            put.setHeader("Content-Type", "application/json");
            put.setEntity(new StringEntity(policyJson, StandardCharsets.UTF_8));
            try (CloseableHttpResponse resp = http.execute(put)) {
                final int st = resp.getStatusLine().getStatusCode();
                if (st == 401) throw new EcsUnauthorizedException("ECS putBucketPolicy got 401");
                if (st == 200 || st == 204) return;
                final String body = resp.getEntity() == null ? "" :
                        EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8);
                throw new CloudRuntimeException("ECS setBucketPolicy failed: HTTP " + st + " body=" + body);
            }
        } catch (EcsUnauthorizedException u) {
            throw u;
        } catch (Exception e) {
            throw new CloudRuntimeException("ECS setBucketPolicy error: " + e.getMessage(), e);
        }
    }

    /** DELETE /policy to make bucket private. */
    private void deleteBucketPolicyHttp(final String url, final String token, final boolean insecure) {
        try (CloseableHttpClient http = buildHttpClient(insecure)) {
            final HttpDelete del = new HttpDelete(url);
            del.setHeader("X-SDS-AUTH-TOKEN", token);
            try (CloseableHttpResponse resp = http.execute(del)) {
                final int st = resp.getStatusLine().getStatusCode();
                if (st == 401) throw new EcsUnauthorizedException("ECS deleteBucketPolicyHttp got 401");
                if (st == 200 || st == 204) return;
                final String body = resp.getEntity() == null ? "" :
                        EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8);
                throw new CloudRuntimeException("ECS deleteBucketPolicy failed: HTTP " + st + " body=" + body);
            }
        } catch (EcsUnauthorizedException u) {
            throw u;
        } catch (Exception e) {
            throw new CloudRuntimeException("ECS deleteBucketPolicy error: " + e.getMessage(), e);
        }
    }

    /** Check if a bucket exists on ECS via Mgmt API /object/bucket/{name}/info?namespace=... */
    private boolean ecsBucketExists(final String bucketName, final Map<String, String> ds) {
        final String mgmtUrl = trimTail(ds.get(MGMT_URL));
        final String saUser  = ds.get(SA_USER);
        final String saPass  = ds.get(SA_PASS);
        final String ns      = org.apache.commons.lang3.StringUtils.isBlank(ds.get(NAMESPACE)) ? "default" : ds.get(NAMESPACE);
        final boolean insecure = "true".equalsIgnoreCase(ds.getOrDefault(INSECURE, "false"));

        if (isBlank(bucketName)) {
            logger.warn("ecsBucketExists: bucket name is blank; treating as non-existent.");
            return false;
        }

        if (isBlank(mgmtUrl) || isBlank(saUser) || isBlank(saPass)) {
            logger.warn("ecsBucketExists('{}'): missing mgmt_url/sa_user/sa_password; assuming bucket exists.", bucketName);
            return true;
        }

        final EcsCfg cfg = new EcsCfg(mgmtUrl, saUser, saPass, ns, insecure);

        try {
            return mgmtCallWithRetry401(cfg, token -> {
                try (CloseableHttpClient http = buildHttpClient(insecure)) {
                    final String url = mgmtUrl
                            + "/object/bucket/"
                            + java.net.URLEncoder.encode(bucketName, java.nio.charset.StandardCharsets.UTF_8.name())
                            + "/info?namespace="
                            + java.net.URLEncoder.encode(ns, java.nio.charset.StandardCharsets.UTF_8.name());

                    logger.debug("ecsBucketExists('{}'): GET {}", bucketName, url);

                    final HttpGet get = new HttpGet(url);
                    get.setHeader("X-SDS-AUTH-TOKEN", token);

                    try (CloseableHttpResponse resp = http.execute(get)) {
                        final int st = resp.getStatusLine().getStatusCode();
                        final String body = resp.getEntity() != null
                                ? EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8)
                                : "";

                        if (st == 401) throw new EcsUnauthorizedException("ecsBucketExists got 401");

                        if (st == 200) return true;
                        if (st == 404) return false;

                        if (st == 400) {
                            final String errCode   = parseXmlTag(body, "code");
                            final String errDetail = parseXmlTag(body, "details");
                            final String errDesc   = parseXmlTag(body, "description");

                            final String lowerBody   = body == null ? "" : body.toLowerCase(Locale.ROOT);
                            final String lowerDetail = errDetail == null ? "" : errDetail.toLowerCase(Locale.ROOT);
                            final String lowerDesc   = errDesc == null ? "" : errDesc.toLowerCase(Locale.ROOT);

                            final boolean notFoundByCode = "1004".equals(errCode);
                            final boolean notFoundByText =
                                    lowerBody.contains("unable to find entity with the given id")
                                            || lowerDetail.contains("unable to find entity with the given id")
                                            || lowerDesc.contains("unable to find entity with the given id")
                                            || lowerDesc.contains("request parameter cannot be found");

                            if (notFoundByCode || notFoundByText) return false;
                        }

                        logger.warn("ecsBucketExists('{}'): unexpected HTTP {} body={}; treating as EXISTS.", bucketName, st, body);
                        return true;
                    }
                }
            });
        } catch (Exception e) {
            logger.warn("ecsBucketExists('{}') failed: {}. Conservatively treating as EXISTS.", bucketName, e.getMessage());
            return true;
        }
    }
}
