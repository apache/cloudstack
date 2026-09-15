// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
// SPDX-License-Identifier: Apache-2.0
package org.apache.cloudstack.storage.datastore.util;

import org.apache.commons.lang3.StringUtils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 * Utility class for the SeaweedFS object storage provider.
 *
 * SeaweedFS exposes both an S3-compatible API and an AWS IAM-compatible API,
 * so this provider needs no proprietary admin client — only the AWS S3 and IAM
 * SDKs, the same pair Cloudian HyperStore already uses in this tree.
 */
public class SeaweedFSObjectStoreUtil {

    /** The name of our Object Store Provider */
    public static final String OBJECT_STORE_PROVIDER_NAME = "SeaweedFS";

    public static final String STORE_KEY_PROVIDER_NAME = "providerName";
    public static final String STORE_KEY_URL           = "url";
    public static final String STORE_KEY_NAME          = "name";
    public static final String STORE_KEY_SIZE          = "size";
    public static final String STORE_KEY_DETAILS       = "details";

    // Store Details Map key names - managed outside of plugin
    public static final String STORE_DETAILS_KEY_ACCESS_KEY = "accesskey";   // admin/root access key
    public static final String STORE_DETAILS_KEY_SECRET_KEY = "secretkey";   // admin/root secret key
    public static final String STORE_DETAILS_KEY_S3_URL     = "s3Url";        // S3 endpoint URL
    public static final String STORE_DETAILS_KEY_IAM_URL     = "iamUrl";       // IAM endpoint URL

    // Account Detail Map key names - credentials created per CloudStack account.
    // Namespaced by store ID so one account can use multiple SeaweedFS pools
    // without the second pool overwriting the first pool's credentials.
    public static final String KEY_ACCESS_KEY_PREFIX = "swfs_AccessKey_";
    public static final String KEY_SECRET_KEY_PREFIX = "swfs_SecretKey_";

    /**
     * Build the account-detail key for the IAM access key of a given store.
     */
    public static String keyAccessKey(long storeId) {
        return KEY_ACCESS_KEY_PREFIX + storeId;
    }

    /**
     * Build the account-detail key for the IAM secret key of a given store.
     */
    public static String keySecretKey(long storeId) {
        return KEY_SECRET_KEY_PREFIX + storeId;
    }

    /**
     * Connect timeout for the S3 extension HTTP client, in seconds.
     */
    public static final int S3_EXTENSION_CONNECT_TIMEOUT_SECONDS = 10;
    /**
     * Per-request timeout for the S3 extension HTTP request, in seconds.
     */
    public static final int S3_EXTENSION_REQUEST_TIMEOUT_SECONDS = 30;

    /**
     * IAM user policy applied to each per-account IAM user. Grants full S3
     * access except bucket creation/deletion, so CloudStack retains control of
     * bucket lifecycle while the account's IAM credentials can manage objects.
     */
    public static final String IAM_USER_POLICY = "{\n" +
        "  \"Version\": \"2012-10-17\",\n" +
        "  \"Statement\": [\n" +
        "    {\n" +
        "      \"Sid\": \"AllowFullS3Access\",\n" +
        "      \"Effect\": \"Allow\",\n" +
        "      \"Action\": [\n" +
        "        \"s3:*\"\n" +
        "      ],\n" +
        "      \"Resource\": \"*\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"Sid\": \"ExceptBucketCreationOrDeletion\",\n" +
        "      \"Effect\": \"Deny\",\n" +
        "      \"Action\": [\n" +
        "        \"s3:CreateBucket\",\n" +
        "        \"s3:DeleteBucket\"\n" +
        "      ],\n" +
        "      \"Resource\": \"*\"\n" +
        "    }\n" +
        "  ]\n" +
        "}\n";

    // The CloudStack service credential (the accesskey/secretkey configured on
    // the object store) is the admin credential used for ALL driver operations:
    //   - AmazonS3 client: bucket CRUD, policy, versioning, encryption, listing
    //   - AmazonIdentityManagement client: per-account IAM user provisioning
    //   - setBucketQuotaViaS3Extension: PUT /{bucket}?seaweedfs-quota
    // It must therefore have broad S3 and IAM permissions. It is NOT scoped
    // down to only s3:PutBucketQuota/s3:GetBucketQuota — that was an earlier
    // design idea that does not match the implementation. The per-account IAM
    // users (created by createUser) are the ones with restricted permissions
    // (see IAM_USER_POLICY above).

    /**
     * Returns an S3 connection for the given endpoint and credentials.
     * Uses path-style access, which SeaweedFS requires.
     *
     * @param url the url of the S3 service
     * @param accessKey the credentials to use for the S3 connection.
     * @param secretKey the matching secret key.
     * @return an S3 connection (never null)
     * @throws CloudRuntimeException on failure.
     */
    public static AmazonS3 getS3Client(String url, String accessKey, String secretKey) {
        AmazonS3 client = AmazonS3ClientBuilder.standard()
                .enablePathStyleAccess()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(url, "us-east-1"))
                .build();
        if (client == null) {
            throw new CloudRuntimeException("Error while creating SeaweedFS S3 client");
        }
        return client;
    }

    /**
     * Returns an IAM connection for the given endpoint and credentials.
     *
     * @param url the url of the IAM service
     * @param accessKey the credentials to use for the iam connection.
     * @param secretKey the matching secret key.
     * @return an IAM connection (never null)
     * @throws CloudRuntimeException on failure.
     */
    public static AmazonIdentityManagement getIAMClient(String url, String accessKey, String secretKey) {
        AmazonIdentityManagement iamClient = AmazonIdentityManagementClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(url, "us-east-1"))
            .build();
        if (iamClient == null) {
            throw new CloudRuntimeException("Error while creating SeaweedFS IAM client");
        }
        return iamClient;
    }

    /**
     * Test the S3Url to confirm it behaves like an S3 Service.
     *
     * Uses bad credentials and looks for the particular error from S3 that says
     * InvalidAccessKeyId was used. Quietly returns if we connect and get the
     * expected error back.
     *
     * @param s3Url the url to check
     * @throws CloudRuntimeException if there is any unexpected issue.
     */
    public static void validateS3Url(String s3Url) {
        try {
            AmazonS3 s3Client = SeaweedFSObjectStoreUtil.getS3Client(s3Url, "unknown", "unknown");
            s3Client.listBuckets();
        } catch (AmazonServiceException e) {
            if (StringUtils.compareIgnoreCase(e.getErrorCode(), "InvalidAccessKeyId") != 0
                    && StringUtils.compareIgnoreCase(e.getErrorCode(), "SignatureDoesNotMatch") != 0) {
                throw new CloudRuntimeException("Unexpected response from S3 Endpoint.", e);
            }
        }
    }

    /**
     * Test the IAMUrl to confirm it behaves like an IAM Service.
     *
     * Uses bad credentials and looks for the particular error from IAM that says
     * InvalidAccessKeyId or InvalidClientTokenId was used. Quietly returns if we
     * connect and get the expected error back.
     *
     * @param iamUrl the url to check
     * @throws CloudRuntimeException if there is any unexpected issue.
     */
    public static void validateIAMUrl(String iamUrl) {
        try {
            AmazonIdentityManagement iamClient = SeaweedFSObjectStoreUtil.getIAMClient(iamUrl, "unknown", "unknown");
            iamClient.listAccessKeys();
        } catch (AmazonServiceException e) {
            if (! StringUtils.equalsAnyIgnoreCase(e.getErrorCode(), "InvalidAccessKeyId", "InvalidClientTokenId", "SignatureDoesNotMatch")) {
                throw new CloudRuntimeException("Unexpected response from IAM Endpoint.", e);
            }
        }
    }

    /**
     * Set bucket quota via the SeaweedFS S3 extension endpoint.
     *
     * SeaweedFS exposes a custom S3 subresource at
     *   PUT /{bucket}?seaweedfs-quota
     * authenticated via standard S3 SigV4 and authorized via the
     * s3:PutBucketQuota IAM permission. This avoids the need for a
     * separate admin API credential.
     *
     * The request body is JSON:
     *   {"quota_size": <n>, "quota_unit": "GB", "quota_enabled": true}
     *
     * @param s3Url     the S3 endpoint URL (e.g. http://host:8333)
     * @param accessKey the S3 access key (must have s3:PutBucketQuota permission)
     * @param secretKey the S3 secret key
     * @param bucketName the bucket name
     * @param sizeGiB    the quota size in GiB (0 to disable quota)
     * @throws CloudRuntimeException on any failure
     */
    public static void setBucketQuotaViaS3Extension(String s3Url, String accessKey, String secretKey, String bucketName, long sizeGiB) {
        setBucketQuotaViaS3Extension(s3Url, accessKey, secretKey, bucketName, sizeGiB, newS3ExtensionHttpClient());
    }

    /**
     * Build a bounded HTTP client for SeaweedFS S3 extension requests with a
     * connect timeout so a stalled endpoint cannot block the management-server
     * API thread indefinitely.
     */
    public static java.net.http.HttpClient newS3ExtensionHttpClient() {
        return java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(S3_EXTENSION_CONNECT_TIMEOUT_SECONDS))
                .build();
    }

    /**
     * Set bucket quota via the SeaweedFS S3 extension endpoint using the
     * supplied HTTP client. The client is injected so tests can assert the
     * signed request without hitting the network.
     */
    public static void setBucketQuotaViaS3Extension(String s3Url, String accessKey, String secretKey,
                                                     String bucketName, long sizeGiB, java.net.http.HttpClient httpClient) {
        if (sizeGiB < 0) {
            // Only zero disables a quota; a negative value would corrupt
            // resource accounting (BucketApiServiceImpl persists the requested
            // value and computes deltas from it), so reject it outright.
            throw new CloudRuntimeException("Bucket quota cannot be negative: " + sizeGiB);
        }
        String body;
        if (sizeGiB == 0) {
            body = "{\"quota_size\":0,\"quota_unit\":\"B\",\"quota_enabled\":false}";
        } else {
            body = String.format("{\"quota_size\":%d,\"quota_unit\":\"GB\",\"quota_enabled\":true}", sizeGiB);
        }
        executeSignedS3Request("PUT", s3Url, "/" + bucketName + "?seaweedfs-quota", accessKey, secretKey, body, httpClient);
    }

    /**
     * Execute a custom S3 request with SigV4 signing.
     *
     * Uses the AWS SDK v1 Aws4Signer to sign the request, then sends it via
     * java.net.http.HttpClient. This allows calling SeaweedFS-specific S3
     * extensions (like ?seaweedfs-quota) that the AWS SDK doesn't natively
     * support.
     *
     * The query string portion of {@code resourcePath} (e.g.
     * {@code /bucket?seaweedfs-quota}) is split off and added to the request
     * via {@code addParameter(...)} before signing, so the signer includes it
     * in the canonical query string. {@code DefaultRequest.setResourcePath}
     * does not parse an embedded query string, so passing it verbatim would
     * leave the subresource unsigned while the outgoing URI would still carry
     * it, causing a signature mismatch on the server.
     *
     * @param method     HTTP method (PUT, GET, etc.)
     * @param s3Url      the S3 endpoint base URL
     * @param resourcePath the path + optional query string (e.g. /bucket?seaweedfs-quota)
     * @param accessKey  S3 access key
     * @param secretKey  S3 secret key
     * @param body       the request body (null for GET)
     * @param httpClient the HTTP client used to send the request
     * @return the response body as a string
     * @throws CloudRuntimeException on any failure
     */
    protected static String executeSignedS3Request(String method, String s3Url, String resourcePath,
                                                   String accessKey, String secretKey, String body,
                                                   java.net.http.HttpClient httpClient) {
        try {
            java.net.URI endpointUri = java.net.URI.create(s3Url);

            // Split the resource path into a path and a query string so the
            // query parameters are signed as canonical query parameters.
            String path = resourcePath;
            String queryString = "";
            int q = resourcePath.indexOf('?');
            if (q >= 0) {
                path = resourcePath.substring(0, q);
                queryString = resourcePath.substring(q + 1);
            }

            // Build AWS SDK v1 Request for SigV4 signing
            com.amazonaws.DefaultRequest<?> request = new com.amazonaws.DefaultRequest<>("s3");
            request.setEndpoint(endpointUri);
            request.setHttpMethod(com.amazonaws.http.HttpMethodName.valueOf(method));
            request.setResourcePath(path);
            if (! queryString.isEmpty()) {
                for (String pair : queryString.split("&")) {
                    if (pair.isEmpty()) {
                        continue;
                    }
                    int eq = pair.indexOf('=');
                    if (eq >= 0) {
                        request.addParameter(pair.substring(0, eq), pair.substring(eq + 1));
                    } else {
                        request.addParameter(pair, "");
                    }
                }
            }
            if (body != null) {
                byte[] bodyBytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                request.setContent(new java.io.ByteArrayInputStream(bodyBytes));
                request.getHeaders().put("Content-Length", String.valueOf(bodyBytes.length));
                request.getHeaders().put("Content-Type", "application/json");
            }

            // Sign with SigV4 (AWSS3V4Signer, not the legacy S3Signer which is SigV2)
            com.amazonaws.auth.AWSCredentials credentials = new com.amazonaws.auth.BasicAWSCredentials(accessKey, secretKey);
            com.amazonaws.services.s3.internal.AWSS3V4Signer signer = new com.amazonaws.services.s3.internal.AWSS3V4Signer();
            signer.setServiceName("s3");
            signer.setRegionName("us-east-1");
            signer.sign(request, credentials);

            // Build and send the HTTP request with signed headers. The URI
            // carries the original query string; the signed headers (including
            // Authorization) are copied from the signed request. Restricted
            // headers (e.g. Content-Length, Host) are set by the HTTP client /
            // URI itself and cannot be added via HttpRequest.Builder.header(),
            // so they are skipped here.
            java.net.URI fullUri = endpointUri.resolve(path);
            if (! queryString.isEmpty()) {
                fullUri = java.net.URI.create(fullUri.toString() + "?" + queryString);
            }
            java.net.http.HttpRequest.Builder reqBuilder = java.net.http.HttpRequest.newBuilder()
                    .uri(fullUri)
                    .timeout(java.time.Duration.ofSeconds(S3_EXTENSION_REQUEST_TIMEOUT_SECONDS));
            for (java.util.Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
                String headerName = entry.getKey();
                if (headerName == null || entry.getValue() == null) {
                    continue;
                }
                if (isRestrictedHttpHeader(headerName)) {
                    continue;
                }
                reqBuilder.header(headerName, entry.getValue());
            }
            if (body != null) {
                reqBuilder.method(method, java.net.http.HttpRequest.BodyPublishers.ofString(body));
            } else {
                reqBuilder.method(method, java.net.http.HttpRequest.BodyPublishers.noBody());
            }

            java.net.http.HttpResponse<String> response = httpClient.send(reqBuilder.build(),
                    java.net.http.HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new CloudRuntimeException(String.format(
                        "S3 extension request %s %s failed with status %d: %s",
                        method, fullUri, statusCode, response.body()));
            }
            return response.body();
        } catch (CloudRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new CloudRuntimeException("S3 extension request failed: " + method + " " + resourcePath, e);
        }
    }

    /**
     * Headers that {@code java.net.http.HttpRequest.Builder.header()} rejects
     * because they are managed by the HTTP client itself (content length is
     * derived from the body publisher, host from the URI, etc.). They must be
     * skipped when copying the signed headers onto the outgoing request.
     */
    private static boolean isRestrictedHttpHeader(String headerName) {
        if (headerName == null) {
            return true;
        }
        switch (headerName.toLowerCase(java.util.Locale.ROOT)) {
            case "content-length":
            case "host":
            case "connection":
            case "expect":
            case "upgrade":
                return true;
            default:
                return false;
        }
    }
}
