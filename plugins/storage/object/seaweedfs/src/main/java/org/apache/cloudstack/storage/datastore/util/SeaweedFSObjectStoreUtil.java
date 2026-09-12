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
    public static final String STORE_KEY_DETAILS       = "details";

    // Store Details Map key names - managed outside of plugin
    public static final String STORE_DETAILS_KEY_ACCESS_KEY = "accesskey";   // admin/root access key
    public static final String STORE_DETAILS_KEY_SECRET_KEY = "secretkey";   // admin/root secret key
    public static final String STORE_DETAILS_KEY_S3_URL     = "s3Url";        // S3 endpoint URL
    public static final String STORE_DETAILS_KEY_IAM_URL     = "iamUrl";       // IAM endpoint URL

    // Account Detail Map key names - credentials created per CloudStack account
    public static final String KEY_ACCESS_KEY = "swfs_AccessKey";
    public static final String KEY_SECRET_KEY = "swfs_SecretKey";

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

    /**
     * IAM policy applied to the CloudStack service credential (the access/secret
     * key configured on the object store). Grants only the SeaweedFS-specific
     * quota management permissions, so the service credential cannot delete
     * buckets, manage users, or change cluster topology. Bucket lifecycle
     * operations (create/delete bucket) are performed by the per-account IAM
     * users, not the service credential.
     */
    public static final String SERVICE_CREDENTIAL_POLICY = "{\n" +
        "  \"Version\": \"2012-10-17\",\n" +
        "  \"Statement\": [\n" +
        "    {\n" +
        "      \"Sid\": \"AllowBucketQuotaManagement\",\n" +
        "      \"Effect\": \"Allow\",\n" +
        "      \"Action\": [\n" +
        "        \"s3:PutBucketQuota\",\n" +
        "        \"s3:GetBucketQuota\"\n" +
        "      ],\n" +
        "      \"Resource\": \"*\"\n" +
        "    }\n" +
        "  ]\n" +
        "}\n";

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
        String body;
        if (sizeGiB <= 0) {
            body = "{\"quota_size\":0,\"quota_unit\":\"B\",\"quota_enabled\":false}";
        } else {
            body = String.format("{\"quota_size\":%d,\"quota_unit\":\"GB\",\"quota_enabled\":true}", sizeGiB);
        }
        executeSignedS3Request("PUT", s3Url, "/" + bucketName + "?seaweedfs-quota", accessKey, secretKey, body);
    }

    /**
     * Execute a custom S3 request with SigV4 signing.
     *
     * Uses the AWS SDK v1 Aws4Signer to sign the request, then sends it via
     * java.net.http.HttpClient. This allows calling SeaweedFS-specific S3
     * extensions (like ?seaweedfs-quota) that the AWS SDK doesn't natively
     * support.
     *
     * @param method     HTTP method (PUT, GET, etc.)
     * @param s3Url      the S3 endpoint base URL
     * @param resourcePath the path + query string (e.g. /bucket?seaweedfs-quota)
     * @param accessKey  S3 access key
     * @param secretKey  S3 secret key
     * @param body       the request body (null for GET)
     * @return the response body as a string
     * @throws CloudRuntimeException on any failure
     */
    private static String executeSignedS3Request(String method, String s3Url, String resourcePath,
                                                  String accessKey, String secretKey, String body) {
        try {
            java.net.URI endpointUri = java.net.URI.create(s3Url);
            java.net.URL endpointUrl = endpointUri.toURL();

            // Build AWS SDK v1 Request for SigV4 signing
            com.amazonaws.DefaultRequest<?> request = new com.amazonaws.DefaultRequest<>("s3");
            request.setEndpoint(endpointUri);
            request.setHttpMethod(com.amazonaws.http.HttpMethodName.valueOf(method));
            request.setResourcePath(resourcePath);
            if (body != null) {
                byte[] bodyBytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                request.setContent(new java.io.ByteArrayInputStream(bodyBytes));
                request.getHeaders().put("Content-Length", String.valueOf(bodyBytes.length));
                request.getHeaders().put("Content-Type", "application/json");
            }

            // Sign with SigV4
            com.amazonaws.auth.AWSCredentials credentials = new com.amazonaws.auth.BasicAWSCredentials(accessKey, secretKey);
            com.amazonaws.services.s3.internal.S3Signer signer = new com.amazonaws.services.s3.internal.S3Signer();
            signer.sign(request, credentials);

            // Build and send the HTTP request with signed headers
            java.net.URI fullUri = endpointUri.resolve(resourcePath);
            java.net.http.HttpRequest.Builder reqBuilder = java.net.http.HttpRequest.newBuilder()
                    .uri(fullUri);
            for (java.util.Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    reqBuilder.header(entry.getKey(), entry.getValue());
                }
            }
            if (body != null) {
                reqBuilder.method(method, java.net.http.HttpRequest.BodyPublishers.ofString(body));
            } else {
                reqBuilder.method(method, java.net.http.HttpRequest.BodyPublishers.noBody());
            }

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpResponse<String> response = client.send(reqBuilder.build(),
                    java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new CloudRuntimeException(String.format(
                        "S3 extension request %s %s failed with status %d: %s",
                        method, fullUri, response.statusCode(), response.body()));
            }
            return response.body();
        } catch (CloudRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new CloudRuntimeException("S3 extension request failed: " + method + " " + resourcePath, e);
        }
    }
}
