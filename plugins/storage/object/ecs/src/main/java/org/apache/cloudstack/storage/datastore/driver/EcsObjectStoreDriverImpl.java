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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDetailsDao;
import org.apache.cloudstack.storage.object.BaseObjectStoreDriverImpl;
import org.apache.cloudstack.storage.object.Bucket;
import org.apache.cloudstack.storage.object.BucketObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
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
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.exception.CloudRuntimeException;

public class EcsObjectStoreDriverImpl extends BaseObjectStoreDriverImpl {
    private static final Logger logger = LogManager.getLogger(EcsObjectStoreDriverImpl.class);

    private static final String MGMT_URL      = EcsConstants.MGMT_URL;
    private static final String SA_USER       = EcsConstants.SA_USER;
    private static final String SA_PASS       = EcsConstants.SA_PASS;
    private static final String NAMESPACE     = EcsConstants.NAMESPACE;
    private static final String INSECURE      = EcsConstants.INSECURE;
    private static final String S3_HOST       = EcsConstants.S3_HOST;
    private static final String AD_KEY_ACCESS = EcsConstants.AD_KEY_ACCESS;
    private static final String AD_KEY_SECRET = EcsConstants.AD_KEY_SECRET;

    // ---- ECS token caching ----
    private static final long DEFAULT_TOKEN_MAX_AGE_SEC = 300;
    private static final long EXPIRY_SKEW_SEC = 30;
    private static final ConcurrentHashMap<TokenKey, TokenEntry> TOKEN_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<TokenKey, Object> TOKEN_LOCKS = new ConcurrentHashMap<>();

    private static final class TokenKey {
        final String mgmtUrl;
        final String user;
        TokenKey(final String mgmtUrl, final String user) {
            this.mgmtUrl = mgmtUrl;
            this.user    = user;
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
            this.token       = token;
            this.expiresAtMs = expiresAtMs;
        }
        boolean validNow() { return token != null && !token.isBlank() && System.currentTimeMillis() < expiresAtMs; }
    }

    @FunctionalInterface
    private interface WithToken<T> { T run(String token) throws Exception; }

    @Inject private AccountDao accountDao;
    @Inject private AccountDetailsDao accountDetailsDao;
    @Inject private BucketDao bucketDao;
    @Inject private ObjectStoreDetailsDao storeDetailsDao;

    public EcsObjectStoreDriverImpl() { }

    @Override
    public DataStoreTO getStoreTO(final DataStore store) { return null; }

    // ---- helpers: config ----

    private EcsCfg ecsCfgFromDetails(final Map<String, String> ds, final long storeId) {
        final String mgmtUrl  = EcsUtils.trimTail(ds.get(MGMT_URL));
        final String saUser   = ds.get(SA_USER);
        final String saPass   = ds.get(SA_PASS);
        final String ns       = StringUtils.defaultIfBlank(ds.get(NAMESPACE), "default");
        final boolean insecure = "true".equalsIgnoreCase(ds.getOrDefault(INSECURE, "false"));

        if (EcsUtils.isBlank(mgmtUrl) || EcsUtils.isBlank(saUser) || EcsUtils.isBlank(saPass)) {
            throw new CloudRuntimeException("ECS: missing mgmt_url/sa_user/sa_password for store id=" + storeId);
        }
        return new EcsCfg(mgmtUrl, saUser, saPass, ns, insecure);
    }

    private EcsCfg ecsCfgFromStore(final long storeId) {
        return ecsCfgFromDetails(storeDetailsDao.getDetails(storeId), storeId);
    }

    // ---- helpers: token ----

    private <T> T mgmtCallWithRetry401(final EcsCfg cfg, final WithToken<T> op) throws Exception {
        try {
            return op.run(getAuthToken(cfg.mgmtUrl, cfg.saUser, cfg.saPass, cfg.insecure));
        } catch (EcsUnauthorizedException u) {
            invalidateToken(cfg.mgmtUrl, cfg.saUser);
            return op.run(getAuthToken(cfg.mgmtUrl, cfg.saUser, cfg.saPass, cfg.insecure));
        }
    }

    private void invalidateToken(final String mgmtUrl, final String user) {
        TOKEN_CACHE.remove(new TokenKey(EcsUtils.trimTail(mgmtUrl), user));
    }

    private String getAuthToken(final String mgmtUrl, final String user, final String pass, final boolean insecure) {
        final String mu = EcsUtils.trimTail(mgmtUrl);
        final TokenKey key = new TokenKey(mu, user);
        final TokenEntry cached = TOKEN_CACHE.get(key);
        if (cached != null && cached.validNow()) return cached.token;

        final Object lock = TOKEN_LOCKS.computeIfAbsent(key, k -> new Object());
        synchronized (lock) {
            final TokenEntry cached2 = TOKEN_CACHE.get(key);
            if (cached2 != null && cached2.validNow()) return cached2.token;
            final TokenEntry fresh = loginAndGetTokenFresh(mu, user, pass, insecure);
            TOKEN_CACHE.put(key, fresh);
            return fresh.token;
        }
    }

    private TokenEntry loginAndGetTokenFresh(final String mgmtUrl, final String user, final String pass, final boolean insecure) {
        try (CloseableHttpClient http = EcsUtils.buildHttpClient(insecure)) {
            final HttpGet get = new HttpGet(mgmtUrl + "/login");
            get.addHeader(new BasicScheme().authenticate(new UsernamePasswordCredentials(user, pass), get, null));
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
                logger.debug("ECS token fetched for user='{}' (maxAge={}s, effective={}s)", user, maxAgeSec, effectiveSec);
                return new TokenEntry(token, System.currentTimeMillis() + (effectiveSec * 1000L));
            }
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to obtain ECS auth token: " + e.getMessage(), e);
        }
    }

    // ---- create bucket ----

    @Override
    public Bucket createBucket(final Bucket bucket, final boolean objectLock) {
        if (objectLock) {
            throw new CloudRuntimeException("Dell ECS doesn't support this feature: object locking");
        }

        final long storeId = bucket.getObjectStoreId();
        final String name  = bucket.getName();
        final Map<String, String> ds = storeDetailsDao.getDetails(storeId);
        final EcsCfg cfg   = ecsCfgFromDetails(ds, storeId);

        final BucketVO vo = bucketDao.findById(bucket.getId());
        final Account acct = accountDao.findById(vo.getAccountId());
        if (acct == null) {
            throw new CloudRuntimeException("ECS createBucket: account not found: id=" + vo.getAccountId());
        }
        final String ownerUser = getUserPrefix(ds) + acct.getUuid();

        ensureAccountUserAndSecret(vo.getAccountId(), ownerUser, cfg);

        Integer quotaGb = safeIntFromGetter(bucket, "getQuota");
        if (quotaGb == null) quotaGb = safeIntFromGetter(bucket, "getSize");
        final int blockSizeGb = (quotaGb != null && quotaGb > 0) ? quotaGb : 2;
        final int notifSizeGb = (quotaGb != null && quotaGb > 0) ? quotaGb : 1;

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
                "<is_encryption_enabled>" + encryptionEnabled + "</is_encryption_enabled>" +
                "</object_bucket_create>";

        logger.debug("ECS createBucket XML for '{}': {}", name, createBody);

        try {
            mgmtCallWithRetry401(cfg, token -> doCreateBucket(cfg, token, name, createBody));

            final String s3Host   = resolveS3HostForUI(ds);
            final String s3UrlForUI = "https://" + s3Host + "/" + name;

            logger.info("ECS bucket created: name='{}' owner='{}' ns='{}' quota={}GB enc={} (UI URL: {})",
                    name, ownerUser, cfg.ns, quotaGb != null ? quotaGb : blockSizeGb, encryptionEnabled, s3UrlForUI);

            final String accKey = EcsUtils.valueOrNull(accountDetailsDao.findDetail(vo.getAccountId(), AD_KEY_ACCESS));
            final String secKey = EcsUtils.valueOrNull(accountDetailsDao.findDetail(vo.getAccountId(), AD_KEY_SECRET));
            if (vo != null) {
                vo.setBucketURL(s3UrlForUI);
                if (!EcsUtils.isBlank(accKey)) vo.setAccessKey(accKey);
                if (!EcsUtils.isBlank(secKey)) vo.setSecretKey(secKey);
                bucketDao.update(vo.getId(), vo);
            }
            return bucket;
        } catch (CloudRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to create ECS bucket " + name + ": " + e.getMessage(), e);
        }
    }

    private Void doCreateBucket(final EcsCfg cfg, final String token, final String name, final String createBody) throws Exception {
        try (CloseableHttpClient http = EcsUtils.buildHttpClient(cfg.insecure)) {
            final HttpPost post = new HttpPost(cfg.mgmtUrl + "/object/bucket");
            post.setHeader("X-SDS-AUTH-TOKEN", token);
            post.setHeader("Content-Type", "application/xml");
            post.setEntity(new StringEntity(createBody, StandardCharsets.UTF_8));
            try (CloseableHttpResponse resp = http.execute(post)) {
                final int status = resp.getStatusLine().getStatusCode();
                final String body = resp.getEntity() != null
                        ? EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8) : "";
                if (status == 401) throw new EcsUnauthorizedException("ECS createBucket got 401");
                if (status != 200 && status != 201) {
                    String reason = "HTTP " + status;
                    if (status == 400) {
                        if (EcsXmlParser.looksLikeBucketAlreadyExists400(body)) {
                            reason = "HTTP 400 bucket name already exists";
                        }
                    }
                    logger.error("ECS create bucket failed: {} body={}", reason, body);
                    throw new CloudRuntimeException("Failed to create ECS bucket " + name + ": " + reason);
                }
            }
        }
        return null;
    }

    @Override
    public boolean createUser(final long accountId, final long storeId) {
        final Account acct = accountDao.findById(accountId);
        if (acct == null) throw new CloudRuntimeException("ECS createUser: account not found: id=" + accountId);
        final Map<String, String> ds = storeDetailsDao.getDetails(storeId);
        ensureAccountUserAndSecret(accountId, getUserPrefix(ds) + acct.getUuid(), ecsCfgFromDetails(ds, storeId));
        return true;
    }

    // ---- list buckets (S3 SigV2) ----

    @Override
    public List<Bucket> listBuckets(final long storeId) {
        final Map<String, String> ds = storeDetailsDao.getDetails(storeId);

        final CallContext ctx = CallContext.current();
        if (ctx == null || ctx.getCallingAccount() == null) {
            throw new CloudRuntimeException("ECS listBuckets: no calling account in context.");
        }
        final long accountId = ctx.getCallingAccount().getId();
        final String accessKey = EcsUtils.valueOrNull(accountDetailsDao.findDetail(accountId, AD_KEY_ACCESS));
        final String secretKey = EcsUtils.valueOrNull(accountDetailsDao.findDetail(accountId, AD_KEY_SECRET));
        if (EcsUtils.isBlank(accessKey) || EcsUtils.isBlank(secretKey)) {
            throw new CloudRuntimeException("ECS listBuckets: account has no stored S3 credentials");
        }

        final S3Endpoint ep = resolveS3Endpoint(ds);
        if (ep == null || EcsUtils.isBlank(ep.host)) {
            throw new CloudRuntimeException("ECS listBuckets: S3 endpoint not resolvable");
        }

        final String ns = StringUtils.defaultIfBlank(ds.get(NAMESPACE), "default");
        final List<Bucket> out = new ArrayList<>();
        try (CloseableHttpClient http = EcsUtils.buildHttpClient("true".equalsIgnoreCase(ds.getOrDefault(INSECURE, "false")))) {
            final String dateHdr = rfc1123Now();
            final String emcNsHeader = "x-emc-namespace:" + ns + "\n";
            final String sts = "GET\n\n\n" + dateHdr + "\n" + emcNsHeader + "/";
            final String signature = hmacSha1Base64(sts, secretKey);

            final HttpGet get = new HttpGet(ep.scheme + "://" + ep.host + "/");
            get.setHeader("Host", ep.host);
            get.setHeader("Date", dateHdr);
            get.setHeader("Authorization", "AWS " + accessKey + ":" + signature);
            get.setHeader("x-emc-namespace", ns);

            try (CloseableHttpResponse resp = http.execute(get)) {
                final int st = resp.getStatusLine().getStatusCode();
                final String body = resp.getEntity() != null
                        ? EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8) : "";
                if (st != 200) {
                    logger.error("ECS listBuckets failed: HTTP {} body={}", st, body);
                    throw new CloudRuntimeException("ECS listBuckets failed: HTTP " + st);
                }
                for (String n : extractAllTags(body, "Name")) {
                    if (EcsUtils.isBlank(n)) continue;
                    final Bucket b = new BucketObject();
                    b.setName(n.trim());
                    out.add(b);
                }
            }
        } catch (Exception e) {
            throw new CloudRuntimeException("ECS listBuckets failed: " + e.getMessage(), e);
        }
        return out;
    }

    // ---- delete bucket ----

    @Override
    public boolean deleteBucket(final BucketTO bucket, final long storeId) {
        final String bucketName = bucket.getName();
        final EcsCfg cfg = ecsCfgFromStore(storeId);
        final String url = cfg.mgmtUrl + "/object/bucket/" + bucketName + "/deactivate?namespace=" + cfg.ns;

        try {
            return mgmtCallWithRetry401(cfg, token -> doDeleteBucket(cfg, token, url, bucketName));
        } catch (CloudRuntimeException cre) {
            throw cre;
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to delete ECS bucket '" + bucketName + "': " + e.getMessage(), e);
        }
    }

    private boolean doDeleteBucket(final EcsCfg cfg, final String token, final String url, final String bucketName) throws Exception {
        try (CloseableHttpClient http = EcsUtils.buildHttpClient(cfg.insecure)) {
            final HttpPost post = new HttpPost(url);
            post.setHeader("X-SDS-AUTH-TOKEN", token);
            try (CloseableHttpResponse r = http.execute(post)) {
                final int st = r.getStatusLine().getStatusCode();
                final String body = r.getEntity() != null
                        ? EntityUtils.toString(r.getEntity(), StandardCharsets.UTF_8) : "";
                if (st == 401) throw new EcsUnauthorizedException("ECS deleteBucket got 401");
                if (st == 200 || st == 204) {
                    logger.info("ECS bucket deactivated (deleted): '{}'", bucketName);
                    return true;
                }
                if (st == 400 || st == 409) {
                    final String lb = body.toLowerCase(Locale.ROOT);
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
    }

    // ---- ACL (unsupported) ----

    @Override
    public AccessControlList getBucketAcl(final BucketTO bucket, final long storeId) {
        throw new UnsupportedOperationException("ECS: getBucketAcl is not supported");
    }

    @Override
    public void setBucketAcl(final BucketTO bucket, final AccessControlList acl, final long storeId) {
        throw new UnsupportedOperationException("ECS: setBucketAcl is not supported");
    }

    // ---- Policy ----

    @Override
    public void setBucketPolicy(final BucketTO bucket, final String policy, final long storeId) {
        final String b      = bucket.getName();
        final EcsCfg cfg    = ecsCfgFromStore(storeId);
        final String url    = buildPolicyUrl(cfg, b);
        final String req    = policy == null ? "" : policy.trim();
        final boolean wantPublic  = "public".equalsIgnoreCase(req) || "public-read".equalsIgnoreCase(req);
        final boolean wantPrivate = req.isEmpty() || "{}".equals(req) || "private".equalsIgnoreCase(req);

        if (!wantPublic && !wantPrivate && !req.startsWith("{")) {
            throw new CloudRuntimeException("ECS setBucketPolicy: unsupported policy value '" + policy +
                    "'. Use 'public', 'private', or raw JSON.");
        }
        try {
            mgmtCallWithRetry401(cfg, token -> doSetBucketPolicy(cfg, token, url, b, req, wantPublic, wantPrivate));
        } catch (CloudRuntimeException cre) {
            throw cre;
        } catch (Exception e) {
            throw new CloudRuntimeException("ECS setBucketPolicy error for bucket '" + b + "': " + e.getMessage(), e);
        }
    }

    private Void doSetBucketPolicy(final EcsCfg cfg, final String token, final String url,
            final String b, final String req, final boolean wantPublic, final boolean wantPrivate) throws Exception {
        final String current  = getBucketPolicyRaw(url, token, cfg.insecure);
        final boolean hasPolicy = !current.isBlank();

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
                "{\n  \"Version\":\"2012-10-17\",\n  \"Statement\":[{\n" +
                "    \"Sid\":\"PublicReadGetObject\",\n    \"Effect\":\"Allow\",\n" +
                "    \"Principal\":\"*\",\n    \"Action\":[\"s3:GetObject\"],\n" +
                "    \"Resource\":[\"arn:aws:s3:::" + b + "/*\"]\n  }]\n}";
        putBucketPolicy(url, token, policyJson, cfg.insecure);
        logger.info("ECS setBucketPolicy: applied policy (bucket='{}').", b);
        return null;
    }

    @Override
    public BucketPolicy getBucketPolicy(final BucketTO bucket, final long storeId) {
        final String bucketName = bucket.getName();
        final EcsCfg cfg = ecsCfgFromStore(storeId);
        final String url = buildPolicyUrl(cfg, bucketName);
        try {
            return mgmtCallWithRetry401(cfg, token -> doGetBucketPolicy(cfg, token, url));
        } catch (Exception e) {
            if (e instanceof CloudRuntimeException) throw (CloudRuntimeException) e;
            throw new CloudRuntimeException("ECS getBucketPolicy error: " + e.getMessage(), e);
        }
    }

    private BucketPolicy doGetBucketPolicy(final EcsCfg cfg, final String token, final String url) throws Exception {
        try (CloseableHttpClient http = EcsUtils.buildHttpClient(cfg.insecure)) {
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
    }

    @Override
    public void deleteBucketPolicy(final BucketTO bucket, final long storeId) {
        setBucketPolicy(bucket, "{}", storeId);
    }

    // ---- Encryption ----

    @Override
    public boolean setBucketEncryption(final BucketTO bucket, final long storeId) {
        // Encryption is set at bucket creation time in the createBucket XML body.
        // CloudStack calls this method as a post-creation step; treat it as a no-op.
        logger.debug("ECS setBucketEncryption('{}') called after creation — no-op (encryption is set at creation time).",
                bucket != null ? bucket.getName() : "<null>");
        return true;
    }

    @Override
    public boolean deleteBucketEncryption(final BucketTO bucket, final long storeId) {
        throw new UnsupportedOperationException(
                "ECS: bucket encryption cannot be disabled after creation");
    }

    // ---- Versioning (S3 SigV2) ----

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
        final S3Endpoint ep = resolveS3Endpoint(ds);
        if (ep == null || EcsUtils.isBlank(ep.host)) {
            throw new CloudRuntimeException("ECS: S3 endpoint is not configured; cannot " +
                    (enable ? "enable" : "disable") + " versioning for bucket '" + bucket.getName() + "'");
        }

        final boolean insecure  = "true".equalsIgnoreCase(ds.getOrDefault(INSECURE, "false"));
        final String bucketName = bucket.getName();
        final String desired    = enable ? "Enabled" : "Suspended";

        // Prefer the calling account; fall back to the bucket owner record.
        long accountId = -1L;
        final CallContext ctx = CallContext.current();
        if (ctx != null && ctx.getCallingAccount() != null) {
            accountId = ctx.getCallingAccount().getId();
        }
        if (accountId <= 0) {
            accountId = resolveBucketOwnerAccountId(bucket, storeId, bucketName);
        }
        if (accountId <= 0) {
            throw new CloudRuntimeException("ECS: cannot determine account for bucket '" +
                    bucketName + "'; unable to set versioning");
        }

        final String accessKey = EcsUtils.valueOrNull(accountDetailsDao.findDetail(accountId, AD_KEY_ACCESS));
        final String secretKey = EcsUtils.valueOrNull(accountDetailsDao.findDetail(accountId, AD_KEY_SECRET));
        if (EcsUtils.isBlank(accessKey) || EcsUtils.isBlank(secretKey)) {
            throw new CloudRuntimeException("ECS: no S3 credentials for account " + accountId +
                    "; cannot set versioning for bucket '" + bucketName + "'");
        }

        try (CloseableHttpClient http = EcsUtils.buildHttpClient(insecure)) {
            putBucketVersioningSigV2(http, ep.scheme, ep.host, bucketName, accessKey, secretKey, desired);
            logger.info("ECS: versioning {} applied for bucket='{}'.", desired, bucketName);
            return true;
        } catch (Exception e) {
            // Best-effort: log but do not fail bucket creation.
            // ECS may deny S3 versioning if the object user lacks namespace-level versioning rights.
            logger.warn("ECS: versioning {} failed for '{}': {} — bucket created without versioning.",
                    desired, bucketName, e.getMessage());
            return true;
        }
    }

    private void putBucketVersioningSigV2(final CloseableHttpClient http,
                                          final String scheme, final String host,
                                          final String bucketName,
                                          final String accessKey, final String secretKey,
                                          final String status) throws Exception {
        // Plain XML — no xmlns attribute (matches what ECS S3 endpoint expects)
        final String body = "<VersioningConfiguration><Status>" + status + "</Status></VersioningConfiguration>";
        final byte[] bodyBytes   = body.getBytes(StandardCharsets.UTF_8);
        final String contentType = "application/xml";
        final String contentMd5  = base64Md5(bodyBytes);
        final String dateHdr     = rfc1123Now();

        final String canonicalResource = "/" + bucketName + "?versioning";
        final String stringToSign = "PUT\n" + contentMd5 + "\n" + contentType + "\n" + dateHdr + "\n" + canonicalResource;
        final String signature = hmacSha1Base64(stringToSign, secretKey);

        final HttpPut put = new HttpPut(scheme + "://" + host + "/" + bucketName + "?versioning");
        put.setHeader("Date", dateHdr);
        put.setHeader("Content-Type", contentType);
        put.setHeader("Content-MD5", contentMd5);
        put.setHeader("Authorization", "AWS " + accessKey + ":" + signature);
        put.setEntity(new StringEntity(body, StandardCharsets.UTF_8));

        try (CloseableHttpResponse resp = http.execute(put)) {
            final int st = resp.getStatusLine().getStatusCode();
            if (st != 200 && st != 204) {
                final String rb = resp.getEntity() != null
                        ? EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8) : "";
                throw new CloudRuntimeException("S3 versioning " + status + " failed: HTTP " + st + " body=" + rb);
            }
        }
    }

    private long resolveBucketOwnerAccountId(final BucketTO bucket, final long storeId, final String bucketName) {
        // Always use the bucket record's owner, not the calling context.
        final BucketVO vo = resolveBucketVO(bucket, storeId);
        if (vo != null) {
            try { return vo.getAccountId(); } catch (Throwable ignore) { }
        }
        final BucketVO voByName = findBucketVOByName(bucketName, storeId);
        if (voByName != null) {
            try { return voByName.getAccountId(); } catch (Throwable ignore) { }
        }
        return -1L;
    }

    private void setS3BucketVersioning(final CloseableHttpClient http, final String scheme, final String host,
            final String bucketName, final String accessKey, final String secretKey,
            final String status, final String namespace) throws Exception {
        final String body        = "<VersioningConfiguration xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\"><Status>" + status + "</Status></VersioningConfiguration>";
        final byte[] bodyBytes   = body.getBytes(StandardCharsets.UTF_8);
        final String contentType = "application/xml";
        final String contentMd5  = base64Md5(bodyBytes);
        final String dateHdr     = rfc1123Now();
        final String canonicalResource = "/" + bucketName + "?versioning";
        final String emcNsHeader = EcsUtils.isBlank(namespace) ? "" : "x-emc-namespace:" + namespace + "\n";
        final String sts = "PUT\n" + contentMd5 + "\n" + contentType + "\n" + dateHdr + "\n" + emcNsHeader + canonicalResource;
        final String signature = hmacSha1Base64(sts, secretKey);

        final HttpPut put = new HttpPut(scheme + "://" + host + "/" + bucketName + "?versioning");
        put.setHeader("Host", host);
        put.setHeader("Date", dateHdr);
        put.setHeader("Authorization", "AWS " + accessKey + ":" + signature);
        put.setHeader("Content-Type", contentType);
        put.setHeader("Content-MD5", contentMd5);
        if (!EcsUtils.isBlank(namespace)) put.setHeader("x-emc-namespace", namespace);
        put.setEntity(new StringEntity(body, StandardCharsets.UTF_8));

        try (CloseableHttpResponse resp = http.execute(put)) {
            final int st = resp.getStatusLine().getStatusCode();
            if (st != 200 && st != 204) {
                final String rb = resp.getEntity() != null ? EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8) : "";
                throw new CloudRuntimeException("S3 versioning " + status + " failed: HTTP " + st + " body=" + rb);
            }
        }
    }

    // ---- Quota ----

    @Override
    public void setBucketQuota(final BucketTO bucket, final long storeId, final long size) {
        if (size <= 0) {
            logger.debug("ECS setBucketQuota ignored for {}: non-positive size {}", bucket.getName(), size);
            return;
        }
        final EcsCfg cfg = ecsCfgFromStore(storeId);
        final String bucketName = bucket.getName();
        try {
            mgmtCallWithRetry401(cfg, token -> doSetBucketQuota(cfg, token, bucketName, size));
        } catch (Exception e) {
            logger.warn("ECS setBucketQuota encountered error for {}: {} (ignored)", bucketName, e.getMessage());
        }
    }

    private Void doSetBucketQuota(final EcsCfg cfg, final String token,
                                  final String bucketName, final long size) throws Exception {
        try (CloseableHttpClient http = EcsUtils.buildHttpClient(cfg.insecure)) {
            final Integer currentGb = fetchCurrentQuota(http, cfg, token, bucketName);
            if (currentGb != null && size <= currentGb) {
                throw new CloudRuntimeException("ECS setBucketQuota: cannot reduce quota for '" + bucketName +
                        "' from " + currentGb + "GB to " + size + "GB (ECS only supports increasing quota)");
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

            try (CloseableHttpResponse r = http.execute(put)) {
                final int st = r.getStatusLine().getStatusCode();
                final String rb = r.getEntity() != null ? EntityUtils.toString(r.getEntity(), StandardCharsets.UTF_8) : "";
                if (st == 401) throw new EcsUnauthorizedException("ECS set quota got 401");
                if (st != 200 && st != 204) {
                    logger.warn("ECS set quota failed for {}: HTTP {} body={}. Ignoring.", bucketName, st, rb);
                }
            }
            logger.info("ECS quota set for bucket='{}' newQuota={}GB", bucketName, size);
            return null;
        }
    }

    private Integer fetchCurrentQuota(final CloseableHttpClient http, final EcsCfg cfg,
                                      final String token, final String bucketName) {
        try {
            final HttpGet get = new HttpGet(cfg.mgmtUrl + "/object/bucket/" + bucketName + "/quota");
            get.setHeader("X-SDS-AUTH-TOKEN", token);
            try (CloseableHttpResponse r = http.execute(get)) {
                final int st = r.getStatusLine().getStatusCode();
                if (st == 401) throw new EcsUnauthorizedException("ECS get quota got 401");
                if (st == 200) {
                    final String xml = r.getEntity() != null ? EntityUtils.toString(r.getEntity(), StandardCharsets.UTF_8) : "";
                    Integer gb = parseIntTag(xml, "blockSize");
                    return gb != null ? gb : parseIntTag(xml, "notificationSize");
                }
            }
        } catch (EcsUnauthorizedException u) {
            throw u;
        } catch (Exception e) {
            logger.debug("ECS get quota for {} failed (non-fatal): {}", bucketName, e.getMessage());
        }
        return null;
    }

    @Override
    public Map<String, Long> getAllBucketsUsage(final long storeId) {
        throw new UnsupportedOperationException("ECS: getAllBucketsUsage is not implemented in this plugin");
    }

    // ---- user / secret management ----

    private void ensureAccountUserAndSecret(final long accountId, final String username, final EcsCfg cfg) {
        final String haveAcc = EcsUtils.valueOrNull(accountDetailsDao.findDetail(accountId, AD_KEY_ACCESS));
        final String haveSec = EcsUtils.valueOrNull(accountDetailsDao.findDetail(accountId, AD_KEY_SECRET));

        // If the stored access key is stale (different prefix/username), clear it so we re-provision.
        final boolean stale = !EcsUtils.isBlank(haveAcc) && !username.equals(haveAcc);
        if (stale) {
            logger.info("ECS: stored access key '{}' does not match expected username '{}'; re-provisioning credentials.",
                    haveAcc, username);
            accountDetailsDao.deleteDetails(accountId);
        }
        final String effectiveHaveAcc = stale ? null : haveAcc;
        final String effectiveHaveSec = stale ? null : haveSec;

        try {
            mgmtCallWithRetry401(cfg, token -> {
                try (CloseableHttpClient http = EcsUtils.buildHttpClient(cfg.insecure)) {
                    createEcsUser(http, cfg, token, accountId, username);
                    reconcileOrCreateSecret(http, cfg, token, accountId, username, effectiveHaveAcc, effectiveHaveSec);
                }
                return null;
            });
        } catch (CloudRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to ensure/reconcile ECS user/secret: " + e.getMessage(), e);
        }
    }

    private void createEcsUser(final CloseableHttpClient http, final EcsCfg cfg, final String token,
                               final long accountId, final String username) throws Exception {
        final String xml =
                "<user_create_param>" +
                "<user>" + username + "</user>" +
                "<namespace>" + cfg.ns + "</namespace>" +
                "<tags></tags>" +
                "</user_create_param>";
        final HttpPost post = new HttpPost(cfg.mgmtUrl + "/object/users");
        post.setHeader("X-SDS-AUTH-TOKEN", token);
        post.setHeader("Content-Type", "application/xml");
        post.setEntity(new StringEntity(xml, StandardCharsets.UTF_8));
        try (CloseableHttpResponse r = http.execute(post)) {
            final int st = r.getStatusLine().getStatusCode();
            final String rb = r.getEntity() != null ? EntityUtils.toString(r.getEntity(), StandardCharsets.UTF_8) : "";
            if (st == 401) throw new EcsUnauthorizedException("ECS ensure user got 401");
            if (st == 200 || st == 201) {
                logger.info("ECS user ensured/created for accountId={} -> {}", accountId, username);
            } else if (st == 400 && rb.contains("already exists")) {
                logger.info("ECS user {} already exists (idempotent).", username);
            } else {
                logger.error("ECS user creation failed: status={} body={}", st, rb);
                throw new CloudRuntimeException("ECS user creation failed: HTTP " + st);
            }
        }
    }

    private void reconcileOrCreateSecret(final CloseableHttpClient http, final EcsCfg cfg, final String token,
                                         final long accountId, final String username,
                                         final String haveAcc, final String haveSec) throws Exception {
        if (!EcsUtils.isBlank(haveAcc) && !EcsUtils.isBlank(haveSec)) {
            reconcileExistingAcsSecret(http, cfg, token, accountId, username, haveSec);
            return;
        }
        if (adoptExistingEcsSecret(http, cfg, token, accountId, username, haveAcc, haveSec)) {
            return;
        }
        createNewSecret(http, cfg, token, accountId, username, haveAcc, haveSec);
    }

    private void reconcileExistingAcsSecret(final CloseableHttpClient http, final EcsCfg cfg, final String token,
                                            final long accountId, final String username, final String haveSec) throws Exception {
        logger.info("ECS single-key policy: accountId={} already has keys in ACS; skipping secret creation.", accountId);
        try {
            final List<String> ecsKeys = fetchEcsUserSecrets(http, cfg.mgmtUrl, token, username);
            if (ecsKeys.isEmpty()) {
                pushSecretToEcs(http, cfg, token, username, haveSec, accountId);
            }
        } catch (EcsUnauthorizedException u) {
            throw u;
        } catch (Exception e) {
            logger.debug("ECS secret reconcile check skipped for {}: {}", username, e.getMessage());
        }
    }

    private void pushSecretToEcs(final CloseableHttpClient http, final EcsCfg cfg, final String token,
                                  final String username, final String secret, final long accountId) throws Exception {
        final String xml =
                "<user_secret_key_create>" +
                "<namespace>" + cfg.ns + "</namespace>" +
                "<secretkey>" + secret + "</secretkey>" +
                "</user_secret_key_create>";
        final HttpPost post = new HttpPost(cfg.mgmtUrl + "/object/user-secret-keys/" + username);
        post.setHeader("X-SDS-AUTH-TOKEN", token);
        post.setHeader("Content-Type", "application/xml");
        post.setEntity(new StringEntity(xml, StandardCharsets.UTF_8));
        try (CloseableHttpResponse r = http.execute(post)) {
            final int st = r.getStatusLine().getStatusCode();
            final String rb = r.getEntity() != null ? EntityUtils.toString(r.getEntity(), StandardCharsets.UTF_8) : "";
            if (st == 401) throw new EcsUnauthorizedException("ECS reconcile secret got 401");
            if (st == 200 || st == 201) {
                logger.info("ECS secret reconciled for user {} (secret taken from ACS).", username);
            } else if (st == 400 && rb.contains("already has") && rb.contains("valid keys")) {
                logger.info("ECS user {} already has valid secret(s); reconciliation not needed.", username);
            } else {
                logger.warn("ECS secret reconcile for {} returned HTTP {} body={} (continuing).", username, st, rb);
            }
        }
    }

    private boolean adoptExistingEcsSecret(final CloseableHttpClient http, final EcsCfg cfg, final String token,
                                           final long accountId, final String username,
                                           final String haveAcc, final String haveSec) throws Exception {
        try {
            final List<String> ecsKeys = fetchEcsUserSecrets(http, cfg.mgmtUrl, token, username);
            if (!ecsKeys.isEmpty()) {
                final String adopt = ecsKeys.get(0);
                if (EcsUtils.isBlank(haveAcc)) accountDetailsDao.addDetail(accountId, AD_KEY_ACCESS, username, false);
                if (EcsUtils.isBlank(haveSec)) accountDetailsDao.addDetail(accountId, AD_KEY_SECRET, adopt, false);
                logger.info("Adopted existing ECS secret for user {} into ACS.", username);
                return true;
            }
        } catch (EcsUnauthorizedException u) {
            throw u;
        } catch (Exception e) {
            logger.debug("Failed to fetch existing ECS keys for {} (proceeding to create one): {}", username, e.getMessage());
        }
        return false;
    }

    private void createNewSecret(final CloseableHttpClient http, final EcsCfg cfg, final String token,
                                 final long accountId, final String username,
                                 final String haveAcc, final String haveSec) throws Exception {
        final String newSecret = UUID.randomUUID().toString().replace("-", "");
        final String xml =
                "<user_secret_key_create>" +
                "<namespace>" + cfg.ns + "</namespace>" +
                "<secretkey>" + newSecret + "</secretkey>" +
                "</user_secret_key_create>";
        final HttpPost post = new HttpPost(cfg.mgmtUrl + "/object/user-secret-keys/" + username);
        post.setHeader("X-SDS-AUTH-TOKEN", token);
        post.setHeader("Content-Type", "application/xml");
        post.setEntity(new StringEntity(xml, StandardCharsets.UTF_8));
        try (CloseableHttpResponse r = http.execute(post)) {
            final int st = r.getStatusLine().getStatusCode();
            final String rb = r.getEntity() != null ? EntityUtils.toString(r.getEntity(), StandardCharsets.UTF_8) : "";
            if (st == 401) throw new EcsUnauthorizedException("ECS create secret got 401");
            if (st != 200 && st != 201) {
                if (st == 400 && rb.contains("already has") && rb.contains("valid keys")) {
                    adoptExistingEcsSecret(http, cfg, token, accountId, username, haveAcc, haveSec);
                    return;
                }
                logger.error("ECS create secret-key failed for {}: status={} body={}", username, st, rb);
                throw new CloudRuntimeException("ECS secret-key creation failed: HTTP " + st);
            }
        }
        if (EcsUtils.isBlank(haveAcc)) accountDetailsDao.addDetail(accountId, AD_KEY_ACCESS, username, false);
        if (EcsUtils.isBlank(haveSec)) accountDetailsDao.addDetail(accountId, AD_KEY_SECRET, newSecret, false);
        logger.info("ECS secret key created and stored for user={} (accountId={})", username, accountId);
    }

    private List<String> fetchEcsUserSecrets(final CloseableHttpClient http,
                                             final String mgmtUrl, final String token,
                                             final String username) throws Exception {
        final HttpGet get = new HttpGet(mgmtUrl + "/object/user-secret-keys/" + username);
        get.setHeader("X-SDS-AUTH-TOKEN", token);
        try (CloseableHttpResponse r = http.execute(get)) {
            final int st = r.getStatusLine().getStatusCode();
            if (st == 401) throw new EcsUnauthorizedException("ECS fetch secrets got 401");
            if (st == 200) {
                final String xml = r.getEntity() != null ? EntityUtils.toString(r.getEntity(), StandardCharsets.UTF_8) : "";
                final List<String> out = new ArrayList<>();
                final String s1 = parseXmlTag(xml, "secret_key_1");
                final String s2 = parseXmlTag(xml, "secret_key_2");
                if ("true".equalsIgnoreCase(parseXmlTag(xml, "secret_key_1_exist")) && !EcsUtils.isBlank(s1)) out.add(s1.trim());
                if ("true".equalsIgnoreCase(parseXmlTag(xml, "secret_key_2_exist")) && !EcsUtils.isBlank(s2)) out.add(s2.trim());
                return out;
            }
            return new ArrayList<>();
        }
    }

    // ---- S3 endpoint resolution ----

    private static final class S3Endpoint {
        final String scheme;
        final String host;
        S3Endpoint(final String scheme, final String host) {
            this.scheme = scheme;
            this.host   = host;
        }
    }

    private S3Endpoint resolveS3Endpoint(final Map<String, String> ds) {
        String host = normalizeHostOnly(ds.get(S3_HOST));
        if (EcsUtils.isBlank(host)) host = normalizeHostOnly(ds.get("host"));
        return new S3Endpoint("https", host);
    }

    private String resolveS3HostForUI(final Map<String, String> ds) {
        String host = normalizeHostOnly(ds.get(S3_HOST));
        if (EcsUtils.isBlank(host)) host = normalizeHostOnly(ds.get("host"));
        return host;
    }

    // ---- HTTP helpers ----

    private String buildPolicyUrl(final EcsCfg cfg, final String bucketName) {
        try {
            return cfg.mgmtUrl + "/object/bucket/" + bucketName + "/policy?namespace=" +
                    URLEncoder.encode(cfg.ns, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            throw new CloudRuntimeException("ECS: failed to encode namespace", e);
        }
    }

    private String getBucketPolicyRaw(final String url, final String token, final boolean insecure) {
        try (CloseableHttpClient http = EcsUtils.buildHttpClient(insecure)) {
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
        } catch (CloudRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new CloudRuntimeException("ECS getBucketPolicy error: " + e.getMessage(), e);
        }
    }

    private void putBucketPolicy(final String url, final String token, final String policyJson, final boolean insecure) {
        try (CloseableHttpClient http = EcsUtils.buildHttpClient(insecure)) {
            final HttpPut put = new HttpPut(url);
            put.setHeader("X-SDS-AUTH-TOKEN", token);
            put.setHeader("Content-Type", "application/json");
            put.setEntity(new StringEntity(policyJson, StandardCharsets.UTF_8));
            try (CloseableHttpResponse resp = http.execute(put)) {
                final int st = resp.getStatusLine().getStatusCode();
                if (st == 401) throw new EcsUnauthorizedException("ECS putBucketPolicy got 401");
                if (st == 200 || st == 204) return;
                final String body = resp.getEntity() == null ? "" : EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8);
                throw new CloudRuntimeException("ECS setBucketPolicy failed: HTTP " + st + " body=" + body);
            }
        } catch (EcsUnauthorizedException u) {
            throw u;
        } catch (CloudRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new CloudRuntimeException("ECS setBucketPolicy error: " + e.getMessage(), e);
        }
    }

    private void deleteBucketPolicyHttp(final String url, final String token, final boolean insecure) {
        try (CloseableHttpClient http = EcsUtils.buildHttpClient(insecure)) {
            final HttpDelete del = new HttpDelete(url);
            del.setHeader("X-SDS-AUTH-TOKEN", token);
            try (CloseableHttpResponse resp = http.execute(del)) {
                final int st = resp.getStatusLine().getStatusCode();
                if (st == 401) throw new EcsUnauthorizedException("ECS deleteBucketPolicyHttp got 401");
                if (st == 200 || st == 204) return;
                final String body = resp.getEntity() == null ? "" : EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8);
                throw new CloudRuntimeException("ECS deleteBucketPolicy failed: HTTP " + st + " body=" + body);
            }
        } catch (EcsUnauthorizedException u) {
            throw u;
        } catch (CloudRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new CloudRuntimeException("ECS deleteBucketPolicy error: " + e.getMessage(), e);
        }
    }

    // ---- S3 SigV2 helpers ----

    private static String rfc1123Now() {
        return DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US)
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
    }

    private static String base64Md5(final byte[] data) throws Exception {
        return Base64.getEncoder().encodeToString(MessageDigest.getInstance("MD5").digest(data));
    }

    private static String hmacSha1Base64(final String data, final String key) throws Exception {
        final Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    // ---- VO / reflection helpers ----

    private BucketVO resolveBucketVO(final BucketTO bucket, final long storeId) {
        long id = getLongFromGetter(bucket, "getId", -1L);
        if (id <= 0) id = getLongFromGetter(bucket, "getBucketId", -1L);
        if (id > 0) {
            try { return bucketDao.findById(id); } catch (Throwable ignore) { }
        }
        String uuid = null;
        try { uuid = (String) bucket.getClass().getMethod("getUuid").invoke(bucket); } catch (Throwable ignore) { }
        if (!EcsUtils.isBlank(uuid)) {
            try {
                java.lang.reflect.Method m = bucketDao.getClass().getMethod("findByUuid", String.class);
                Object r = m.invoke(bucketDao, uuid);
                if (r instanceof BucketVO) return (BucketVO) r;
            } catch (Throwable ignore) { }
        }
        return findBucketVOByName(bucket.getName(), storeId);
    }

    private BucketVO findBucketVOByName(final String name, final long storeId) {
        try {
            final List<BucketVO> buckets = bucketDao.listByObjectStoreId(storeId);
            if (buckets != null) {
                for (BucketVO vo : buckets) {
                    if (name.equals(vo.getName())) return vo;
                }
            }
        } catch (Throwable t) {
            logger.debug("ECS: findBucketVOByName '{}' failed: {}", name, t.getMessage());
        }
        return null;
    }

    private static long getLongFromGetter(final Object o, final String getter, final long defVal) {
        if (o == null) return defVal;
        try {
            Object v = o.getClass().getMethod(getter).invoke(o);
            if (v instanceof Number) return ((Number) v).longValue();
            if (v instanceof String && !((String) v).isEmpty()) return Long.parseLong((String) v);
        } catch (Throwable ignore) { }
        return defVal;
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
        try { v = o.getClass().getMethod(getMethod).invoke(o); } catch (Exception ignored) { }
        if (v == null) {
            try { v = o.getClass().getMethod(isMethod).invoke(o); } catch (Exception ignored) { }
        }
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof Number)  return ((Number) v).intValue() != 0;
        if (v instanceof String) {
            final String s = ((String) v).trim();
            if ("true".equalsIgnoreCase(s) || "1".equals(s)) return true;
            if ("false".equalsIgnoreCase(s) || "0".equals(s)) return false;
        }
        return defVal;
    }

    private static String normalizeHostOnly(final String hostOrUrl) {
        if (hostOrUrl == null) return null;
        String h = hostOrUrl.trim();
        if (h.startsWith("https://")) h = h.substring(8);
        else if (h.startsWith("http://")) h = h.substring(7);
        while (h.endsWith("/")) h = h.substring(0, h.length() - 1);
        return h;
    }

    // ---- user prefix ----

    private String getUserPrefix(final Map<String, String> ds) {
        final String p = ds != null ? ds.get(EcsConstants.USER_PREFIX) : null;
        return StringUtils.isBlank(p) ? EcsConstants.DEFAULT_USER_PREFIX : p.trim();
    }

    // ---- XML helpers (delegated to EcsXmlParser) ----

    private static String extractTag(final String xml, final String tag) {
        return EcsXmlParser.extractTag(xml, tag);
    }

    private static List<String> extractAllTags(final String xml, final String tag) {
        return EcsXmlParser.extractAllTags(xml, tag);
    }

    private static String parseXmlTag(final String xml, final String tag) {
        return EcsXmlParser.extractTag(xml, tag);
    }

    private static Integer parseIntTag(final String xml, final String tag) {
        return EcsXmlParser.parseIntTag(xml, tag);
    }
}
