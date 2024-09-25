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

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import org.apache.cloudstack.cloudian.client.CloudianClient;
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

public class CloudianHyperStoreUtil {

    /** The name of our Object Store Provider */
    public static final String OBJECT_STORE_PROVIDER_NAME = "Cloudian HyperStore";

    public static final String STORE_KEY_PROVIDER_NAME = "providerName";
    public static final String STORE_KEY_URL           = "url";
    public static final String STORE_KEY_NAME          = "name";
    public static final String STORE_KEY_DETAILS       = "details";

    // GUI Object Store Details map key names
    public static final String GUI_DETAILS_KEY_ACCESSKEY    = "accesskey";
    public static final String GUI_DETAILS_KEY_SECRETKEY    = "secretkey";
    public static final String GUI_DETAILS_KEY_VALIDATE_SSL = "validateSSL";
    public static final String GUI_DETAILS_KEY_S3_URL       = "s3Url";
    public static final String GUI_DETAILS_KEY_IAM_URL      = "iamUrl";

    // detail map key names
    public static final String KEY_ADMIN_USER         = "hs_AdminUser";
    public static final String KEY_ADMIN_PASS         = "hs_AdminPass";
    public static final String KEY_ADMIN_VALIDATE_SSL = "hs_AdminValidateSSL";
    public static final String KEY_S3_ENDPOINT_URL    = "hs_S3EndpointURL";
    public static final String KEY_ROOT_ACCESS_KEY    = "hs_AccessKey";
    public static final String KEY_ROOT_SECRET_KEY    = "hs_SecretKey";
    public static final String KEY_IAM_ENDPOINT_URL   = "hs_IAMEndpointURL";
    public static final String KEY_IAM_ACCESS_KEY     = "hs_IAMAccessKey";
    public static final String KEY_IAM_SECRET_KEY     = "hs_IAMSecretKey";

    public static final int DEFAULT_ADMIN_PORT = 19443;
    public static final int DEFAULT_ADMIN_TIMEOUT = 10;

    public static final String IAM_USER_USERNAME = "CloudStack";
    public static final String IAM_USER_POLICY_NAME = "CloudStackPolicy";
    public static final String IAM_USER_POLICY;
    static {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"Version\": \"2012-10-17\",\n");
        sb.append("  \"Statement\": [\n");
        sb.append("    {\n");
        sb.append("      \"Sid\": \"AllowFullS3Access\",\n");
        sb.append("      \"Effect\": \"Allow\",\n");
        sb.append("      \"Action\": [\n");
        sb.append("        \"s3:*\"\n");
        sb.append("      ],\n");
        sb.append("      \"Resource\": \"*\"\n");
        sb.append("    },\n");
        sb.append("    {\n");
        sb.append("      \"Sid\": \"ExceptBucketCreationOrDeletion\",\n");
        sb.append("      \"Effect\": \"Deny\",\n");
        sb.append("      \"Action\": [\n");
        sb.append("        \"s3:createBucket\",\n");
        sb.append("        \"s3:deleteBucket\"\n");
        sb.append("      ],\n");
        sb.append("      \"Resource\": \"*\"\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n");
        IAM_USER_POLICY = sb.toString();
    }

    /**
     * Get a connection to the Cloudian HyperStore ADMIN API Service.
     * @param url the url of the ADMIN API service
     * @param user the admin username to connect as
     * @param pass the matching admin password
     * @param validateSSL validate the SSL Certificate (when using https://)
     * @return a connection object (never null)
     * @throws CloudRuntimeException if the connection fails for any reason
     */
    public static CloudianClient getCloudianClient(String url, String user, String pass, boolean validateSSL) {
        try {
            URL parsedURL = new URL(url);
            String scheme = parsedURL.getProtocol();
            String host = parsedURL.getHost();
            int port = parsedURL.getPort();
            if (port == -1) {
                port = DEFAULT_ADMIN_PORT;
            }
            return new CloudianClient(host, port, scheme, user, pass, validateSSL, DEFAULT_ADMIN_PORT);
        } catch (MalformedURLException e) {
            throw new CloudRuntimeException(e);
        } catch (KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
            throw new CloudRuntimeException(e);
        }
    }

    /**
     * Returns an S3 connection for the given endpoint and credentials.
     * NOTE: https connections must use a trusted certificate.
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
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(url, "auto"))
                .build();
        if (client == null) {
            throw new CloudRuntimeException("Error while creating Cloudian S3 client");
        }
        return client;
    }

    /**
     * Returns an IAM connection for the given endpoint and credentials.
     * NOTE: https connections must use a trusted certificate.
     * NOTE: HyperStore IAM service is usually found on ports 16080/16443.
     *
     * @param url the url which should include the HyperStore IAM port if not 80/443.
     * @param accessKey the credentials to use for the iam connection.
     * @param secretKey the matching secret key.
     * @return an IAM connection (never null)
     * @throws CloudRuntimeException on failure.
     */
    public static AmazonIdentityManagement getIAMClient(String url, String accessKey, String secretKey) {
        AmazonIdentityManagement iamClient = AmazonIdentityManagementClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(url, "auto"))
            .build();
        if (iamClient == null) {
            throw new CloudRuntimeException("Error while creating Cloudian IAM client");
        }
        return iamClient;
    }

    /**
     * Test the S3Url to confirm it behaves like an S3 Service.
     *
     * The method uses bad credentials and looks for the particular error from S3
     * that says InvalidAccessKeyId was used. The method quietly returns if
     * we connect and get the expected error back.
     *
     * @param s3Url the url to check
     *
     * @throws RuntimeException if there is any issue.
     */
    public static void validateS3Url(String s3Url) {
        try {
            AmazonS3 s3Client = CloudianHyperStoreUtil.getS3Client(s3Url, "unknown", "unknown");
            s3Client.listBuckets();
        } catch (AmazonServiceException e) {
            // Check if the ErrorCode says that the access key (we used "unknown" was invalid
            if (StringUtils.compareIgnoreCase(e.getErrorCode(), "InvalidAccessKeyId") != 0) {
                throw new CloudRuntimeException("Unexpected response from S3 Endpoint.", e);
            }
        }
    }

    /**
     * Test the IAMUrl to confirm it behaves like an IAM Service.
     *
     * The method uses bad credentials and looks for the particular error from IAM
     * that says InvalidAccessKeyId was used. The method quietly returns if
     * we connect and get the expected error back.
     *
     * @param iamUrl the url to check
     *
     * @throws RuntimeException if there is any issue.
     */
    public static void validateIAMUrl(String iamUrl) {
        try {
            AmazonIdentityManagement iamClient = CloudianHyperStoreUtil.getIAMClient(iamUrl, "unknown", "unknown");
            iamClient.listAccessKeys();
        } catch (AmazonServiceException e) {
            if (StringUtils.compareIgnoreCase(e.getErrorCode(), "InvalidAccessKeyId") != 0) {
                throw new CloudRuntimeException("Unexpected response from IAM Endpoint.", e);
            }
        }
    }
}
