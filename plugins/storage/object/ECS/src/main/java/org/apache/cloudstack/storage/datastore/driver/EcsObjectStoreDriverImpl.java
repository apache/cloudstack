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
import javax.inject.Inject;
import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDetailsDao;
import org.apache.cloudstack.storage.object.BaseObjectStoreDriverImpl;
import org.apache.cloudstack.storage.object.Bucket;
import org.apache.cloudstack.storage.object.BucketObject;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;

import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.BucketPolicy;
import com.cloud.agent.api.to.BucketTO;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.storage.BucketVO;
import com.cloud.storage.dao.BucketDao;
import com.cloud.user.Account;
import com.cloud.user.AccountDetailVO;
import com.cloud.user.AccountDetailsDao;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.exception.CloudRuntimeException;


public class EcsObjectStoreDriverImpl extends BaseObjectStoreDriverImpl {

    // ---- Injected dependencies ----
    @Inject
    private AccountDao accountDao;
    @Inject
    private AccountDetailsDao accountDetailsDao;
    @Inject
    private BucketDao bucketDao;
    @Inject
    private ObjectStoreDetailsDao storeDetailsDao;

    private final EcsMgmtTokenManager tokenManager = new EcsMgmtTokenManager();
    private final EcsXmlParser xml = new EcsXmlParser();

    // Versioning retry (ECS can be eventually consistent)
    private static final int VERSIONING_MAX_TRIES = 10;
    private static final long VERSIONING_RETRY_SLEEP_MS = 1000L;

    public EcsObjectStoreDriverImpl() {
    }

    @Override
    public DataStoreTO getStoreTO(final DataStore store) {
        return null;
    }

    // ---------------- create bucket ----------------

    @Override
    public Bucket createBucket(final Bucket bucket, final boolean objectLock) {
        final long storeId = bucket.getObjectStoreId();
        final String name = bucket.getName();

        if (objectLock) {
            throw new InvalidParameterValueException("Dell ECS doesn't support this feature: object locking");
        }

        final Map<String, String> ds = storeDetailsDao.getDetails(storeId);
        final EcsCfg cfg = ecsCfgFromDetails(ds, storeId);

        final BucketVO vo = bucketDao.findById(bucket.getId());
        if (vo == null) {
            throw new CloudRuntimeException("ECS createBucket: bucket record not found: id=" + bucket.getId());
        }

        final long accountId = vo.getAccountId();
        final Account acct = accountDao.findById(accountId);
        if (acct == null) {
            throw new CloudRuntimeException("ECS createBucket: account not found: id=" + accountId);
        }

        final String ownerUser = getUserPrefix(ds) + acct.getUuid();

        // Ensure per-account credentials exist (single-key policy with adopt-if-exists)
        ensureAccountUserAndSecret(accountId, ownerUser, cfg.mgmtUrl, cfg.saUser, cfg.saPass, cfg.ns, cfg.insecure);

        // Quota from UI (INT GB). Bucket.getQuota may be Integer; Bucket.getSize may be Long.
        Integer quotaGb = null;
        try {
            quotaGb = bucket.getQuota();
        } catch (Throwable ignored) {
        }

        if (quotaGb == null) {
            try {
                final Long sz = bucket.getSize();
                if (sz != null) {
                    quotaGb = sz.intValue();
                }
            } catch (Throwable ignored) {
            }
        }

        final int blockSizeGb;
        final int notifSizeGb;
        if (quotaGb != null && quotaGb > 0) {
            blockSizeGb = quotaGb;
            notifSizeGb = quotaGb;
        } else {
            blockSizeGb = 2;
            notifSizeGb = 1;
        }

        // Encryption flag from request (Bucket has isEncryption()).
        boolean encryptionEnabled = bucket.isEncryption();

        // Fallback to persisted value if request did not explicitly enable it.
        if (!encryptionEnabled) {
            try {
                encryptionEnabled = vo.isEncryption();
            } catch (Throwable ignored) {
            }
        }

        logger.info("ECS createBucket flags for '{}': encryptionEnabled={}", name, encryptionEnabled);

        final String createBody =
                "<object_bucket_create>"
                        + "<blockSize>" + blockSizeGb + "</blockSize>"
                        + "<notificationSize>" + notifSizeGb + "</notificationSize>"
                        + "<name>" + name + "</name>"
                        + "<head_type>s3</head_type>"
                        + "<namespace>" + cfg.ns + "</namespace>"
                        + "<owner>" + ownerUser + "</owner>"
                        + "<is_encryption_enabled>" + (encryptionEnabled ? "true" : "false") + "</is_encryption_enabled>"
                        + "</object_bucket_create>";

        if (logger.isDebugEnabled()) {
            logger.debug("ECS createBucket XML for '{}': {}", name, createBody);
        }

        try {
            tokenManager.callWithRetry401(cfg, token -> {
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
                            throw new EcsMgmtTokenManager.EcsUnauthorizedException("ECS createBucket got 401");
                        }

                        if (status != 200 && status != 201) {
                            String reason = "HTTP " + status;
                            if (status == 400 && xml.looksLikeBucketAlreadyExists400(respBody)) {
                                reason = "HTTP 400 bucket name already exists";
                            }
                            logger.error("ECS create bucket failed: {} body={}", reason, respBody);
                            throw new CloudRuntimeException("Failed to create ECS bucket " + name + ": " + reason);
                        }
                    }
                }
                return null;
            }, this::buildHttpClient);

            // UI URL should show S3 endpoint
            final String s3Host = resolveS3HostForUI(storeId, ds);
            final String s3UrlForUI = "https://" + s3Host + "/" + name;

            logger.info("ECS bucket created: name='{}' owner='{}' ns='{}' quota={}GB enc={} (UI URL: {})",
                    name, ownerUser, cfg.ns, (quotaGb != null ? quotaGb : blockSizeGb), encryptionEnabled, s3UrlForUI);

            // Persist UI-visible details on the bucket record
            final String accKey = valueOrNull(accountDetailsDao.findDetail(accountId, EcsConstants.AD_KEY_ACCESS));
            final String secKey = valueOrNull(accountDetailsDao.findDetail(accountId, EcsConstants.AD_KEY_SECRET));

            vo.setBucketURL(s3UrlForUI);

            if (!StringUtils.isBlank(accKey)) {
                vo.setAccessKey(accKey);
            }

            if (!StringUtils.isBlank(secKey)) {
                vo.setSecretKey(secKey);
            }

            bucketDao.update(vo.getId(), vo);

            // NOTE: Do NOT attempt to enable versioning here unless you have a reliable signal in your CloudStack
            // version. The provided code previously referenced missing methods and broke compilation.

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
        if (acct == null) {
            throw new CloudRuntimeException("ECS createUser: account not found: id=" + accountId);
        }

        final Map<String, String> ds = storeDetailsDao.getDetails(storeId);
        final EcsCfg cfg = ecsCfgFromDetails(ds, storeId);

        final String username = getUserPrefix(ds) + acct.getUuid();
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
        final String accessKey = valueOrNull(accountDetailsDao.findDetail(accountId, EcsConstants.AD_KEY_ACCESS));
        final String secretKey = valueOrNull(accountDetailsDao.findDetail(accountId, EcsConstants.AD_KEY_SECRET));

        if (StringUtils.isBlank(accessKey) || StringUtils.isBlank(secretKey)) {
            throw new CloudRuntimeException("ECS listBuckets: account has no stored S3 credentials");
        }

        final S3Endpoint ep = resolveS3Endpoint(ds, storeId);
        if (ep == null || StringUtils.isBlank(ep.host)) {
            throw new CloudRuntimeException("ECS listBuckets: S3 endpoint not resolvable");
        }

        final boolean insecure = "true".equalsIgnoreCase(ds.getOrDefault(EcsConstants.INSECURE, "false"));
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

                final List<String> names = xml.extractAllTags(body, "Name");
                for (String n : names) {
                    if (StringUtils.isBlank(n)) {
                        continue;
                    }
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

    // ---------------- S3: list objects in a bucket (SigV2, path-style) ----------------
    public List<String> listBucketObjects(final String bucketName, final long storeId) {
        final Map<String, String> ds = storeDetailsDao.getDetails(storeId);

        final CallContext ctx = CallContext.current();
        if (ctx == null || ctx.getCallingAccount() == null) {
            throw new CloudRuntimeException("ECS listBucketObjects: no calling account in context");
        }

        final long accountId = ctx.getCallingAccount().getId();
        final String accessKey = valueOrNull(accountDetailsDao.findDetail(accountId, EcsConstants.AD_KEY_ACCESS));
        final String secretKey = valueOrNull(accountDetailsDao.findDetail(accountId, EcsConstants.AD_KEY_SECRET));

        if (StringUtils.isBlank(accessKey) || StringUtils.isBlank(secretKey)) {
            throw new CloudRuntimeException("ECS listBucketObjects: account has no stored S3 credentials");
        }

        final S3Endpoint ep = resolveS3Endpoint(ds, storeId);
        if (ep == null || StringUtils.isBlank(ep.host)) {
            throw new CloudRuntimeException("ECS listBucketObjects: S3 endpoint not resolvable");
        }

        final boolean insecure = "true".equalsIgnoreCase(ds.getOrDefault(EcsConstants.INSECURE, "false"));
        final List<String> keys = new java.util.ArrayList<>();

        String marker = null;
        try (CloseableHttpClient http = buildHttpClient(insecure)) {
            while (true) {
                final String dateHdr = rfc1123Now();
                final String canonicalResource = "/" + bucketName + "/";
                final String sts = "GET\n\n\n" + dateHdr + "\n" + canonicalResource;
                final String signature = hmacSha1Base64(sts, secretKey);

                final StringBuilder qs = new StringBuilder("max-keys=1000");
                if (!StringUtils.isBlank(marker)) {
                    qs.append("&marker=").append(java.net.URLEncoder.encode(
                            marker, java.nio.charset.StandardCharsets.UTF_8.name()));
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

                    xml.extractKeysFromListBucketXml(body, keys);

                    final boolean truncated = "true".equalsIgnoreCase(xml.extractTag(body, "IsTruncated"));
                    if (!truncated) {
                        break;
                    }

                    String next = xml.extractTag(body, "NextMarker");
                    if (StringUtils.isBlank(next) && !keys.isEmpty()) {
                        next = keys.get(keys.size() - 1);
                    }
                    if (StringUtils.isBlank(next)) {
                        break;
                    }

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
            return tokenManager.callWithRetry401(cfg, token -> {
                try (CloseableHttpClient http = buildHttpClient(cfg.insecure)) {
                    final HttpPost post = new HttpPost(url);
                    post.setHeader("X-SDS-AUTH-TOKEN", token);

                    try (CloseableHttpResponse r = http.execute(post)) {
                        final int st = r.getStatusLine().getStatusCode();
                        final String body = r.getEntity() != null
                                ? EntityUtils.toString(r.getEntity(), StandardCharsets.UTF_8)
                                : "";

                        if (st == 401) {
                            throw new EcsMgmtTokenManager.EcsUnauthorizedException("ECS deleteBucket got 401");
                        }

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
            }, this::buildHttpClient);
        } catch (CloudRuntimeException cre) {
            throw cre;
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to delete ECS bucket '" + bucketName + "': " + e.getMessage(), e);
        }
    }

    @Override
    public AccessControlList getBucketAcl(final BucketTO bucket, final long storeId) {
        return null;
    }

    @Override
    public void setBucketAcl(final BucketTO bucket, final AccessControlList acl, final long storeId) {
        // not supported
    }

    // ---------------- Policy ----------------
    @Override
    public void setBucketPolicy(final BucketTO bucket, final String policy, final long storeId) {
        final String b = bucket.getName();

        final Map<String, String> ds = storeDetailsDao.getDetails(storeId);
        final EcsCfg cfg = ecsCfgFromDetails(ds, storeId);

        final String url;
        try {
            url = cfg.mgmtUrl + "/object/bucket/" + b + "/policy?namespace="
                    + java.net.URLEncoder.encode(cfg.ns, java.nio.charset.StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            throw new CloudRuntimeException("ECS setBucketPolicy: failed to encode namespace", e);
        }

        final String req = policy == null ? "" : policy.trim();
        final boolean wantPublic = "public".equalsIgnoreCase(req) || "public-read".equalsIgnoreCase(req);
        final boolean wantPrivate = req.isEmpty() || "{}".equals(req) || "private".equalsIgnoreCase(req);

        if (!wantPublic && !wantPrivate && !req.startsWith("{")) {
            throw new CloudRuntimeException("ECS setBucketPolicy: unsupported policy value '" + policy
                    + "'. Use 'public', 'private', or raw JSON.");
        }

        try {
            tokenManager.callWithRetry401(cfg, token -> {
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

                final String policyJson;
                if (req.startsWith("{")) {
                    policyJson = req;
                } else {
                    policyJson = "{\n"
                            + "  \"Version\":\"2012-10-17\",\n"
                            + "  \"Statement\":[{\n"
                            + "    \"Sid\":\"PublicReadGetObject\",\n"
                            + "    \"Effect\":\"Allow\",\n"
                            + "    \"Principal\":\"*\",\n"
                            + "    \"Action\":[\"s3:GetObject\"],\n"
                            + "    \"Resource\":[\"arn:aws:s3:::" + b + "/*\"]\n"
                            + "  }]\n"
                            + "}";
                }

                putBucketPolicy(url, token, policyJson, cfg.insecure);
                logger.info("ECS setBucketPolicy: applied policy (bucket='{}').", b);
                return null;
            }, this::buildHttpClient);
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
            url = cfg.mgmtUrl + "/object/bucket/" + bucketName + "/policy?namespace="
                    + java.net.URLEncoder.encode(cfg.ns, java.nio.charset.StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            throw new CloudRuntimeException("ECS getBucketPolicy: failed to encode namespace", e);
        }

        try {
            return tokenManager.callWithRetry401(cfg, token -> {
                try (CloseableHttpClient http = buildHttpClient(cfg.insecure)) {
                    final HttpGet get = new HttpGet(url);
                    get.setHeader("X-SDS-AUTH-TOKEN", token);

                    try (CloseableHttpResponse resp = http.execute(get)) {
                        final int st = resp.getStatusLine().getStatusCode();
                        if (st == 401) {
                            throw new EcsMgmtTokenManager.EcsUnauthorizedException("ECS getBucketPolicy got 401");
                        }

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
            }, this::buildHttpClient);
        } catch (Exception e) {
            if (e instanceof CloudRuntimeException) {
                throw (CloudRuntimeException) e;
            }
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
                "Dell ECS bucket encryption can only be chosen at bucket creation; "
                        + "it cannot be disabled afterwards (bucket=" + bucketName + ")";
        logger.error("ECS deleteBucketEncryption('{}') requested but {}", bucketName, msg);
        throw new CloudRuntimeException(msg);
    }

    // ---------------- Versioning ----------------

    @Override
    public boolean setBucketVersioning(final BucketTO bucket, final long storeId) {
        return setOrSuspendVersioning(bucket, storeId, true);
    }

    @Override
    public boolean deleteBucketVersioning(final BucketTO bucket, final long storeId) {
        return setOrSuspendVersioning(bucket, storeId, false);
    }

    private boolean setOrSuspendVersioning(final BucketTO bucket,
                                        final long storeId,
                                        final boolean enable) {
        final Map<String, String> ds = storeDetailsDao.getDetails(storeId);
        final S3Endpoint ep = resolveS3Endpoint(ds, storeId);
        final boolean insecure = "true".equalsIgnoreCase(ds.getOrDefault(EcsConstants.INSECURE, "false"));

        if (ep == null || StringUtils.isBlank(ep.host)) {
            logger.warn("ECS: S3 endpoint not resolvable; skipping bucket versioning.");
            return true; // best-effort
        }

        final String bucketName = bucket.getName();
        final String desired = enable ? "Enabled" : "Suspended";

        // Resolve accountId
        long accountId = -1L;
        final CallContext ctx = CallContext.current();
        if (ctx != null && ctx.getCallingAccount() != null) {
            accountId = ctx.getCallingAccount().getId();
        }
        if (accountId <= 0) {
            final BucketVO vo = resolveBucketVO(bucket);
            if (vo != null) {
                accountId = vo.getAccountId();
            }
        }

        if (accountId <= 0) {
            logger.warn("ECS: cannot resolve accountId for bucket='{}'; skipping versioning.", bucketName);
            return true;
        }

        String accessKey = valueOrNull(accountDetailsDao.findDetail(accountId, EcsConstants.AD_KEY_ACCESS));
        String secretKey = valueOrNull(accountDetailsDao.findDetail(accountId, EcsConstants.AD_KEY_SECRET));

        if (StringUtils.isBlank(accessKey) || StringUtils.isBlank(secretKey)) {
            logger.warn("ECS: missing S3 credentials for accountId={}; skipping versioning.", accountId);
            return true;
        }

        try (CloseableHttpClient http = buildHttpClient(insecure)) {
            putBucketVersioningSigV2(
                    http,
                    ep.scheme,
                    ep.host,
                    bucketName,
                    accessKey,
                    secretKey,
                    desired
            );
            logger.info("ECS: bucket versioning {} succeeded for '{}'", desired, bucketName);
            return true;
        } catch (Exception e) {
            logger.warn("ECS: bucket versioning {} failed for '{}': {}",
                    desired, bucketName, e.getMessage());
            return true; // best-effort (do NOT break createBucket)
        }
    }

    // ----- S3 Versioning (SigV2, EXACTLY matches bash script) -----

    private void putBucketVersioningSigV2(final CloseableHttpClient http,
                                        final String scheme,
                                        final String host,
                                        final String bucketName,
                                        final String accessKey,
                                        final String secretKey,
                                        final String status) throws Exception {

        // EXACT XML (no namespace, matches bash)
        final String body =
                "<VersioningConfiguration>"
                        + "<Status>" + status + "</Status>"
                        + "</VersioningConfiguration>";

        final byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        final String contentType = "application/xml";
        final String contentMd5 = base64Md5(bodyBytes);
        final String dateHdr = rfc1123Now();

        // IMPORTANT: NO trailing slash before ?versioning
        final String canonicalResource = "/" + bucketName + "?versioning";

        final String stringToSign =
                "PUT\n"
                        + contentMd5 + "\n"
                        + contentType + "\n"
                        + dateHdr + "\n"
                        + canonicalResource;

        final String signature = hmacSha1Base64(stringToSign, secretKey);

        final String url = scheme + "://" + host + "/" + bucketName + "?versioning";

        final HttpPut put = new HttpPut(url);
        put.setHeader("Date", dateHdr);
        put.setHeader("Content-Type", contentType);
        put.setHeader("Content-MD5", contentMd5);
        put.setHeader("Authorization", "AWS " + accessKey + ":" + signature);
        put.setEntity(new StringEntity(body, StandardCharsets.UTF_8));

        try (CloseableHttpResponse resp = http.execute(put)) {
            final int statusCode = resp.getStatusLine().getStatusCode();
            final String respBody = resp.getEntity() != null
                    ? EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8)
                    : "";

            if (statusCode != 200 && statusCode != 204) {
                throw new CloudRuntimeException(
                        "S3 versioning failed: HTTP " + statusCode + " body=" + respBody
                );
            }
        }
    }

    // ---------------- Quota ----------------

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
            tokenManager.callWithRetry401(cfg, token -> {
                try (CloseableHttpClient http = buildHttpClient(cfg.insecure)) {
                    Integer currentGb = null;

                    try {
                        final HttpGet get = new HttpGet(cfg.mgmtUrl + "/object/bucket/" + bucketName + "/quota");
                        get.setHeader("X-SDS-AUTH-TOKEN", token);

                        try (CloseableHttpResponse r = http.execute(get)) {
                            final int st = r.getStatusLine().getStatusCode();
                            if (st == 401) {
                                throw new EcsMgmtTokenManager.EcsUnauthorizedException("ECS get quota got 401");
                            }
                            if (st == 200) {
                                final String xmlBody = r.getEntity() != null
                                        ? EntityUtils.toString(r.getEntity(), StandardCharsets.UTF_8)
                                        : "";
                                currentGb = xml.parseIntTag(xmlBody, "blockSize");
                                if (currentGb == null) {
                                    currentGb = xml.parseIntTag(xmlBody, "notificationSize");
                                }
                            }
                        }
                    } catch (EcsMgmtTokenManager.EcsUnauthorizedException u) {
                        throw u;
                    } catch (Exception e) {
                        logger.debug("ECS get quota for {} failed (non-fatal): {}", bucketName, e.getMessage());
                    }

                    if (currentGb != null && size <= currentGb) {
                        logger.info("ECS setBucketQuota noop for '{}': requested {}GB <= current {}GB", bucketName, size, currentGb);
                        return null;
                    }

                    final String quotaBody =
                            "<bucket_quota_param>"
                                    + "<blockSize>" + size + "</blockSize>"
                                    + "<notificationSize>" + size + "</notificationSize>"
                                    + "<namespace>" + cfg.ns + "</namespace>"
                                    + "</bucket_quota_param>";

                    final HttpPut put = new HttpPut(cfg.mgmtUrl + "/object/bucket/" + bucketName + "/quota");
                    put.setHeader("X-SDS-AUTH-TOKEN", token);
                    put.setHeader("Content-Type", "application/xml");
                    put.setEntity(new StringEntity(quotaBody, StandardCharsets.UTF_8));

                    try (CloseableHttpResponse r2 = http.execute(put)) {
                        final int st2 = r2.getStatusLine().getStatusCode();
                        final String rb2 = r2.getEntity() != null
                                ? EntityUtils.toString(r2.getEntity(), StandardCharsets.UTF_8)
                                : "";

                        if (st2 == 401) {
                            throw new EcsMgmtTokenManager.EcsUnauthorizedException("ECS set quota got 401");
                        }
                        if (st2 != 200 && st2 != 204) {
                            logger.warn("ECS set quota failed for {}: HTTP {} body={}. Ignoring.", bucketName, st2, rb2);
                            return null;
                        }
                    }

                    logger.info("ECS quota set for bucket='{}' newQuota={}GB", bucketName, size);
                    return null;
                }
            }, this::buildHttpClient);
        } catch (Exception e) {
            logger.warn("ECS setBucketQuota encountered error for {}: {} (ignored)", bucketName, e.getMessage());
        }
    }

    @Override
    public Map<String, Long> getAllBucketsUsage(final long storeId) {
        throw new CloudRuntimeException("Bucket usage aggregation is not implemented via Mgmt API in this plugin.");
    }

    // ---------------- helpers ----------------

    static final class EcsCfg {
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
        final String mgmtUrl = trimTail(ds.get(EcsConstants.MGMT_URL));
        final String saUser = ds.get(EcsConstants.SA_USER);
        final String saPass = ds.get(EcsConstants.SA_PASS);
        final String ns = StringUtils.isBlank(ds.get(EcsConstants.NAMESPACE)) ? "default" : ds.get(EcsConstants.NAMESPACE);
        final boolean insecure = "true".equalsIgnoreCase(ds.getOrDefault(EcsConstants.INSECURE, "false"));

        if (StringUtils.isBlank(mgmtUrl) || StringUtils.isBlank(saUser) || StringUtils.isBlank(saPass)) {
            throw new CloudRuntimeException("ECS: missing mgmt_url/sa_user/sa_password for store id=" + storeId);
        }
        return new EcsCfg(mgmtUrl, saUser, saPass, ns, insecure);
    }

    private String getUserPrefix(final Map<String, String> ds) {
        String p = null;
        if (ds != null) {
            p = ds.get(EcsConstants.USER_PREFIX);
        }
        if (StringUtils.isBlank(p)) {
            return EcsConstants.DEFAULT_USER_PREFIX;
        }
        return p.trim();
    }

    private static String valueOrNull(final AccountDetailVO d) {
        if (d == null) {
            return null;
        }
        return d.getValue();
    }

    private static String trimTail(final String s) {
        if (s == null) {
            return null;
        }
        if (s.endsWith("/")) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }

    private static String normalizeHostOnly(final String hostOrUrl) {
        if (hostOrUrl == null) {
            return null;
        }

        String h = hostOrUrl.trim();
        if (h.startsWith("http://")) {
            h = h.substring("http://".length());
        }
        if (h.startsWith("https://")) {
            h = h.substring("https://".length());
        }
        while (h.endsWith("/")) {
            h = h.substring(0, h.length() - 1);
        }
        return h;
    }

    private CloseableHttpClient buildHttpClient(final boolean insecure) {
        if (!insecure) {
            return HttpClients.createDefault();
        }
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

    // GET /object/user-secret-keys/{username} and parse any existing secrets.
    private List<String> fetchEcsUserSecrets(final CloseableHttpClient http,
                                            final String mgmtUrl,
                                            final String token,
                                            final String username) throws Exception {
        final HttpGet get = new HttpGet(mgmtUrl + "/object/user-secret-keys/" + username);
        get.setHeader("X-SDS-AUTH-TOKEN", token);

        try (CloseableHttpResponse r = http.execute(get)) {
            final int st = r.getStatusLine().getStatusCode();
            if (st == 401) {
                throw new EcsMgmtTokenManager.EcsUnauthorizedException("ECS fetch secrets got 401");
            }

            if (st == 200) {
                final String xmlBody = r.getEntity() != null
                        ? EntityUtils.toString(r.getEntity(), StandardCharsets.UTF_8)
                        : "";
                final java.util.ArrayList<String> out = new java.util.ArrayList<>();

                final String s1 = xml.extractTag(xmlBody, "secret_key_1");
                final String s2 = xml.extractTag(xmlBody, "secret_key_2");
                final String e1 = xml.extractTag(xmlBody, "secret_key_1_exist");
                final String e2 = xml.extractTag(xmlBody, "secret_key_2_exist");

                if ("true".equalsIgnoreCase(e1) && !StringUtils.isBlank(s1)) {
                    out.add(s1.trim());
                }
                if ("true".equalsIgnoreCase(e2) && !StringUtils.isBlank(s2)) {
                    out.add(s2.trim());
                }

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
        final String haveAcc = valueOrNull(accountDetailsDao.findDetail(accountId, EcsConstants.AD_KEY_ACCESS));
        final String haveSec = valueOrNull(accountDetailsDao.findDetail(accountId, EcsConstants.AD_KEY_SECRET));

        final EcsCfg cfg = new EcsCfg(trimTail(mgmtUrl), saUser, saPass, ns, insecure);

        try {
            tokenManager.callWithRetry401(cfg, token -> {
                try (CloseableHttpClient http = buildHttpClient(insecure)) {
                    // Ensure/CREATE user (idempotent)
                    final String createUserXml =
                            "<user_create_param>"
                                    + "<user>" + username + "</user>"
                                    + "<namespace>" + ns + "</namespace>"
                                    + "<tags></tags>"
                                    + "</user_create_param>";

                    final HttpPost postUser = new HttpPost(mgmtUrl + "/object/users");
                    postUser.setHeader("X-SDS-AUTH-TOKEN", token);
                    postUser.setHeader("Content-Type", "application/xml");
                    postUser.setEntity(new StringEntity(createUserXml, StandardCharsets.UTF_8));

                    try (CloseableHttpResponse r = http.execute(postUser)) {
                        final int st = r.getStatusLine().getStatusCode();
                        final String rb = r.getEntity() != null
                                ? EntityUtils.toString(r.getEntity(), StandardCharsets.UTF_8)
                                : "";

                        if (st == 401) {
                            throw new EcsMgmtTokenManager.EcsUnauthorizedException("ECS ensure user got 401");
                        }

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
                    if (!StringUtils.isBlank(haveAcc) && !StringUtils.isBlank(haveSec)) {
                        logger.info("ECS single-key policy: accountId={} already has keys stored in ACS; skipping secret creation.", accountId);

                        // Optional reconciliation: if ECS has no secret, push ACS secret
                        try {
                            final List<String> ecsKeys = fetchEcsUserSecrets(http, mgmtUrl, token, username);
                            if (ecsKeys.isEmpty()) {
                                final String skXml =
                                        "<user_secret_key_create>"
                                                + "<namespace>" + ns + "</namespace>"
                                                + "<secretkey>" + haveSec + "</secretkey>"
                                                + "</user_secret_key_create>";

                                final HttpPost postKey = new HttpPost(mgmtUrl + "/object/user-secret-keys/" + username);
                                postKey.setHeader("X-SDS-AUTH-TOKEN", token);
                                postKey.setHeader("Content-Type", "application/xml");
                                postKey.setEntity(new StringEntity(skXml, StandardCharsets.UTF_8));

                                try (CloseableHttpResponse kr = http.execute(postKey)) {
                                    final int st = kr.getStatusLine().getStatusCode();
                                    final String rb = kr.getEntity() != null
                                            ? EntityUtils.toString(kr.getEntity(), StandardCharsets.UTF_8)
                                            : "";

                                    if (st == 401) {
                                        throw new EcsMgmtTokenManager.EcsUnauthorizedException("ECS reconcile secret got 401");
                                    }

                                    if (st == 200 || st == 201) {
                                        logger.info("ECS secret reconciled for user {} (secret taken from ACS).", username);
                                    } else if (st == 400 && rb != null && rb.contains("already has") && rb.contains("valid keys")) {
                                        logger.info("ECS user {} already has valid secret(s); reconciliation not needed.", username);
                                    } else {
                                        logger.warn("ECS secret reconcile for {} returned HTTP {} body={} (continuing).", username, st, rb);
                                    }
                                }
                            }
                        } catch (EcsMgmtTokenManager.EcsUnauthorizedException u) {
                            throw u;
                        } catch (Exception e) {
                            logger.debug("ECS secret reconcile check skipped for {}: {}", username, e.getMessage());
                        }

                        return null;
                    }

                    // ACS does NOT have key -> try to ADOPT existing ECS key first
                    try {
                        final List<String> ecsKeys = fetchEcsUserSecrets(http, mgmtUrl, token, username);
                        if (!ecsKeys.isEmpty()) {
                            final String adopt = ecsKeys.get(0);

                            if (StringUtils.isBlank(haveAcc)) {
                                accountDetailsDao.addDetail(accountId, EcsConstants.AD_KEY_ACCESS, username, false);
                            }
                            if (StringUtils.isBlank(haveSec)) {
                                accountDetailsDao.addDetail(accountId, EcsConstants.AD_KEY_SECRET, adopt, false);
                            }

                            logger.info("Adopted existing ECS secret for user {} into ACS (no new key created).", username);
                            return null;
                        }
                    } catch (EcsMgmtTokenManager.EcsUnauthorizedException u) {
                        throw u;
                    } catch (Exception e) {
                        logger.debug("Failed to fetch existing ECS keys for {} (proceeding to create one): {}", username, e.getMessage());
                    }

                    // No ECS key either -> create exactly ONE new secret and store in ACS
                    final String newSecret = java.util.UUID.randomUUID().toString().replace("-", "");
                    final String skXmlCreate =
                            "<user_secret_key_create>"
                                    + "<namespace>" + ns + "</namespace>"
                                    + "<secretkey>" + newSecret + "</secretkey>"
                                    + "</user_secret_key_create>";

                    final HttpPost postKey2 = new HttpPost(mgmtUrl + "/object/user-secret-keys/" + username);
                    postKey2.setHeader("X-SDS-AUTH-TOKEN", token);
                    postKey2.setHeader("Content-Type", "application/xml");
                    postKey2.setEntity(new StringEntity(skXmlCreate, StandardCharsets.UTF_8));

                    try (CloseableHttpResponse kr2 = http.execute(postKey2)) {
                        final int st = kr2.getStatusLine().getStatusCode();
                        final String rb = kr2.getEntity() != null
                                ? EntityUtils.toString(kr2.getEntity(), StandardCharsets.UTF_8)
                                : "";

                        if (st == 401) {
                            throw new EcsMgmtTokenManager.EcsUnauthorizedException("ECS create secret got 401");
                        }

                        if (st != 200 && st != 201) {
                            if (st == 400 && rb != null && rb.contains("already has") && rb.contains("valid keys")) {
                                final List<String> ecsKeys = fetchEcsUserSecrets(http, mgmtUrl, token, username);
                                if (!ecsKeys.isEmpty()) {
                                    final String adopt = ecsKeys.get(0);

                                    if (StringUtils.isBlank(haveAcc)) {
                                        accountDetailsDao.addDetail(accountId, EcsConstants.AD_KEY_ACCESS, username, false);
                                    }
                                    if (StringUtils.isBlank(haveSec)) {
                                        accountDetailsDao.addDetail(accountId, EcsConstants.AD_KEY_SECRET, adopt, false);
                                    }

                                    logger.info("Race: ECS already has key(s). Adopted existing secret for {} into ACS.", username);
                                    return null;
                                }
                            }

                            logger.error("ECS create secret-key failed for {}: status={} body={}", username, st, rb);
                            throw new CloudRuntimeException("ECS secret-key creation failed: HTTP " + st);
                        }
                    }

                    if (StringUtils.isBlank(haveAcc)) {
                        accountDetailsDao.addDetail(accountId, EcsConstants.AD_KEY_ACCESS, username, false);
                    }
                    if (StringUtils.isBlank(haveSec)) {
                        accountDetailsDao.addDetail(accountId, EcsConstants.AD_KEY_SECRET, newSecret, false);
                    }

                    logger.info("ECS secret key created and stored for user={} (accountId={})", username, accountId);
                    return null;
                }
            }, this::buildHttpClient);
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

        S3Endpoint(final String scheme, final String host) {
            this.scheme = scheme;
            this.host = host;
        }
    }

    private S3Endpoint resolveS3Endpoint(final Map<String, String> ds, final long storeId) {
        String host = normalizeHostOnly(ds.get(EcsConstants.S3_HOST)); // accept host or URL from UI
        final String scheme = "https";

        if (StringUtils.isBlank(host)) {
            host = normalizeHostOnly(ds.get("host"));
        }

        return new S3Endpoint(scheme, host);
    }

    private String resolveS3HostForUI(final long storeId, final Map<String, String> ds) {
        String host = normalizeHostOnly(ds.get(EcsConstants.S3_HOST));

        if (StringUtils.isBlank(host)) {
            host = normalizeHostOnly(ds.get("host"));
        }

        return host;
    }

    // ---------- Mgmt owner → accountId fallback ----------
    private Long resolveAccountIdViaMgmt(final String bucketName, final Map<String, String> ds, final boolean insecure) {
        final String mgmtUrl = trimTail(ds.get(EcsConstants.MGMT_URL));
        final String saUser = ds.get(EcsConstants.SA_USER);
        final String saPass = ds.get(EcsConstants.SA_PASS);

        if (StringUtils.isBlank(mgmtUrl) || StringUtils.isBlank(saUser) || StringUtils.isBlank(saPass)) {
            return null;
        }

        final EcsCfg cfg = new EcsCfg(
                mgmtUrl,
                saUser,
                saPass,
                StringUtils.isBlank(ds.get(EcsConstants.NAMESPACE)) ? "default" : ds.get(EcsConstants.NAMESPACE),
                insecure);

        final String prefix = getUserPrefix(ds);

        try {
            return tokenManager.callWithRetry401(cfg, token -> {
                try (CloseableHttpClient http = buildHttpClient(insecure)) {
                    final String owner = fetchBucketOwnerViaMgmt(http, mgmtUrl, token, bucketName);

                    if (!StringUtils.isBlank(owner) && !StringUtils.isBlank(prefix) && owner.startsWith(prefix) && owner.length() > prefix.length()) {
                        final String uuid = owner.substring(prefix.length());
                        try {
                            final Account acct = accountDao.findByUuid(uuid);
                            if (acct != null) {
                                return acct.getId();
                            }
                        } catch (Throwable ignore) {
                        }
                    }

                    return null;
                }
            }, this::buildHttpClient);
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

            if (st == 401) {
                throw new EcsMgmtTokenManager.EcsUnauthorizedException("ECS fetch bucket owner got 401");
            }

            if (st == 200) {
                final String xmlBody = r.getEntity() != null
                        ? EntityUtils.toString(r.getEntity(), StandardCharsets.UTF_8)
                        : "";
                final String owner = xml.extractTag(xmlBody, "owner");
                if (!StringUtils.isBlank(owner)) {
                    return owner.trim();
                }
            }

            return null;
        }
    }

    // ---------- Reflection helper (only where needed) ----------
    private static long getLongFromGetter(final Object o, final String getter, final long defVal) {
        if (o == null) {
            return defVal;
        }
        try {
            final Object v = o.getClass().getMethod(getter).invoke(o);
            if (v instanceof Number) {
                return ((Number) v).longValue();
            }
            if (v instanceof String && !((String) v).isEmpty()) {
                return Long.parseLong((String) v);
            }
        } catch (Throwable ignore) {
        }
        return defVal;
    }

    private BucketVO resolveBucketVO(final BucketTO bucket) {
        if (bucket == null) {
            return null;
        }

        final long id = getLongFromGetter(bucket, "getId", -1L);
        if (id > 0) {
            return bucketDao.findById(id);
        }
        return null;
    }

    private static String base64Md5(final byte[] data) throws Exception {
        final java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
        final byte[] digest = md.digest(data);
        return Base64.getEncoder().encodeToString(digest);
    }

    private static String hmacSha1Base64(final String data, final String key) throws Exception {
        final javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
        final javax.crypto.spec.SecretKeySpec sk =
                new javax.crypto.spec.SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
        mac.init(sk);
        final byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(raw);
    }

    private static String rfc1123Now() {
        final java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter
                .ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US)
                .withZone(java.time.ZoneOffset.UTC);
        return fmt.format(java.time.Instant.now());
    }

    // GET /policy raw body; returns "" if none (200 with empty/{} or 204/404).
    private String getBucketPolicyRaw(final String url, final String token, final boolean insecure) {
        try (CloseableHttpClient http = buildHttpClient(insecure)) {
            final HttpGet get = new HttpGet(url);
            get.setHeader("X-SDS-AUTH-TOKEN", token);

            try (CloseableHttpResponse resp = http.execute(get)) {
                final int st = resp.getStatusLine().getStatusCode();
                if (st == 401) {
                    throw new EcsMgmtTokenManager.EcsUnauthorizedException("ECS getBucketPolicyRaw got 401");
                }

                final String body = resp.getEntity() == null ? ""
                        : EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8).trim();

                if (st == 200) {
                    return "{}".equals(body) ? "" : body;
                }
                if (st == 204 || st == 404 || ((st / 100) == 2 && body.isEmpty())) {
                    return "";
                }

                throw new CloudRuntimeException("ECS getBucketPolicy failed: HTTP " + st + " body=" + body);
            }
        } catch (EcsMgmtTokenManager.EcsUnauthorizedException u) {
            throw u;
        } catch (Exception e) {
            throw new CloudRuntimeException("ECS getBucketPolicy error: " + e.getMessage(), e);
        }
    }

    // PUT /policy with JSON.
    private void putBucketPolicy(final String url, final String token, final String policyJson, final boolean insecure) {
        try (CloseableHttpClient http = buildHttpClient(insecure)) {
            final HttpPut put = new HttpPut(url);
            put.setHeader("X-SDS-AUTH-TOKEN", token);
            put.setHeader("Content-Type", "application/json");
            put.setEntity(new StringEntity(policyJson, StandardCharsets.UTF_8));

            try (CloseableHttpResponse resp = http.execute(put)) {
                final int st = resp.getStatusLine().getStatusCode();
                if (st == 401) {
                    throw new EcsMgmtTokenManager.EcsUnauthorizedException("ECS putBucketPolicy got 401");
                }
                if (st == 200 || st == 204) {
                    return;
                }

                final String body = resp.getEntity() == null ? ""
                        : EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8);

                throw new CloudRuntimeException("ECS setBucketPolicy failed: HTTP " + st + " body=" + body);
            }
        } catch (EcsMgmtTokenManager.EcsUnauthorizedException u) {
            throw u;
        } catch (Exception e) {
            throw new CloudRuntimeException("ECS setBucketPolicy error: " + e.getMessage(), e);
        }
    }

    // DELETE /policy to make bucket private.
    private void deleteBucketPolicyHttp(final String url, final String token, final boolean insecure) {
        try (CloseableHttpClient http = buildHttpClient(insecure)) {
            final HttpDelete del = new HttpDelete(url);
            del.setHeader("X-SDS-AUTH-TOKEN", token);

            try (CloseableHttpResponse resp = http.execute(del)) {
                final int st = resp.getStatusLine().getStatusCode();
                if (st == 401) {
                    throw new EcsMgmtTokenManager.EcsUnauthorizedException("ECS deleteBucketPolicyHttp got 401");
                }
                if (st == 200 || st == 204) {
                    return;
                }

                final String body = resp.getEntity() == null ? ""
                        : EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8);

                throw new CloudRuntimeException("ECS deleteBucketPolicy failed: HTTP " + st + " body=" + body);
            }
        } catch (EcsMgmtTokenManager.EcsUnauthorizedException u) {
            throw u;
        } catch (Exception e) {
            throw new CloudRuntimeException("ECS deleteBucketPolicy error: " + e.getMessage(), e);
        }
    }

    // Check if a bucket exists on ECS via Mgmt API /object/bucket/{name}/info?namespace=...
    private boolean ecsBucketExists(final String bucketName, final Map<String, String> ds) {
        final String mgmtUrl = trimTail(ds.get(EcsConstants.MGMT_URL));
        final String saUser = ds.get(EcsConstants.SA_USER);
        final String saPass = ds.get(EcsConstants.SA_PASS);
        final String ns = StringUtils.isBlank(ds.get(EcsConstants.NAMESPACE)) ? "default" : ds.get(EcsConstants.NAMESPACE);
        final boolean insecure = "true".equalsIgnoreCase(ds.getOrDefault(EcsConstants.INSECURE, "false"));

        if (StringUtils.isBlank(bucketName)) {
            logger.warn("ecsBucketExists: bucket name is blank; treating as non-existent.");
            return false;
        }

        if (StringUtils.isBlank(mgmtUrl) || StringUtils.isBlank(saUser) || StringUtils.isBlank(saPass)) {
            logger.warn("ecsBucketExists('{}'): missing mgmt_url/sa_user/sa_password; assuming bucket exists.", bucketName);
            return true;
        }

        final EcsCfg cfg = new EcsCfg(mgmtUrl, saUser, saPass, ns, insecure);

        try {
            return tokenManager.callWithRetry401(cfg, token -> {
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

                        if (st == 401) {
                            throw new EcsMgmtTokenManager.EcsUnauthorizedException("ecsBucketExists got 401");
                        }

                        if (st == 200) {
                            return true;
                        }
                        if (st == 404) {
                            return false;
                        }

                        if (st == 400) {
                            final String errCode = xml.extractTag(body, "code");
                            final String errDetail = xml.extractTag(body, "details");
                            final String errDesc = xml.extractTag(body, "description");

                            final String lowerBody = body == null ? "" : body.toLowerCase(Locale.ROOT);
                            final String lowerDetail = errDetail == null ? "" : errDetail.toLowerCase(Locale.ROOT);
                            final String lowerDesc = errDesc == null ? "" : errDesc.toLowerCase(Locale.ROOT);

                            final boolean notFoundByCode = "1004".equals(errCode);
                            final boolean notFoundByText =
                                    lowerBody.contains("unable to find entity with the given id")
                                            || lowerDetail.contains("unable to find entity with the given id")
                                            || lowerDesc.contains("unable to find entity with the given id")
                                            || lowerDesc.contains("request parameter cannot be found");

                            if (notFoundByCode || notFoundByText) {
                                return false;
                            }
                        }

                        logger.warn("ecsBucketExists('{}'): unexpected HTTP {} body={}; treating as EXISTS.", bucketName, st, body);
                        return true;
                    }
                }
            }, this::buildHttpClient);
        } catch (Exception e) {
            logger.warn("ecsBucketExists('{}') failed: {}. Conservatively treating as EXISTS.", bucketName, e.getMessage());
            return true;
        }
    }
}
