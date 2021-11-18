//
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
//

package com.cloud.utils.storage.S3;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.amazonaws.Protocol.HTTP;
import static com.amazonaws.Protocol.HTTPS;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;


public final class S3Utils {

    private static final Logger LOGGER = Logger.getLogger(S3Utils.class);

    public static final String SEPARATOR = "/";

    private static final Map<String, TransferManager> TRANSFERMANAGER_ACCESSKEY_MAP = new HashMap<>();

    private S3Utils() {}

    public static TransferManager getTransferManager(final ClientOptions clientOptions) {

        if(TRANSFERMANAGER_ACCESSKEY_MAP.containsKey(clientOptions.getAccessKey())) {
            return TRANSFERMANAGER_ACCESSKEY_MAP.get(clientOptions.getAccessKey());
        }

        final AWSCredentials basicAWSCredentials = new BasicAWSCredentials(clientOptions.getAccessKey(), clientOptions.getSecretKey());

        final ClientConfiguration configuration = new ClientConfiguration();

        if (clientOptions.isHttps() != null) {
            configuration.setProtocol(clientOptions.isHttps() ? HTTPS : HTTP);
        }

        if (clientOptions.getConnectionTimeout() != null) {
            configuration.setConnectionTimeout(clientOptions.getConnectionTimeout());
        }

        if (clientOptions.getMaxErrorRetry() != null) {
            configuration.setMaxErrorRetry(clientOptions.getMaxErrorRetry());
        }

        if (clientOptions.getSocketTimeout() != null) {
            configuration.setSocketTimeout(clientOptions.getSocketTimeout());
        }

        if (clientOptions.getUseTCPKeepAlive() != null) {
            configuration.setUseTcpKeepAlive(clientOptions.getUseTCPKeepAlive());
        }

        if (clientOptions.getConnectionTtl() != null) {
            configuration.setConnectionTTL(clientOptions.getConnectionTtl());
        }

        if (clientOptions.getSigner() != null) {

            configuration.setSignerOverride(clientOptions.getSigner());
        }

        LOGGER.debug(format("Creating S3 client with configuration: [protocol: %1$s, signer: %2$s, connectionTimeOut: %3$s, maxErrorRetry: %4$s, socketTimeout: %5$s, useTCPKeepAlive: %6$s, connectionTtl: %7$s]",
                configuration.getProtocol(), configuration.getSignerOverride(), configuration.getConnectionTimeout(), configuration.getMaxErrorRetry(), configuration.getSocketTimeout(),
                clientOptions.getUseTCPKeepAlive(), clientOptions.getConnectionTtl()));

        final AmazonS3Client client = new AmazonS3Client(basicAWSCredentials, configuration);

        if (StringUtils.isNotBlank(clientOptions.getEndPoint())) {
            LOGGER.debug(format("Setting the end point for S3 client with access key %1$s to %2$s.", clientOptions.getAccessKey(), clientOptions.getEndPoint()));

            client.setEndpoint(clientOptions.getEndPoint());
        }

        TRANSFERMANAGER_ACCESSKEY_MAP.put(clientOptions.getAccessKey(), new TransferManager(client));

        return TRANSFERMANAGER_ACCESSKEY_MAP.get(clientOptions.getAccessKey());
    }

    public static AmazonS3 getAmazonS3Client(final ClientOptions clientOptions) {

        return getTransferManager(clientOptions).getAmazonS3Client();
    }

    public static Upload putFile(final ClientOptions clientOptions, final File sourceFile, final String bucketName, final String key) {
        LOGGER.debug(format("Sending file %1$s as S3 object %2$s in bucket %3$s", sourceFile.getName(), key, bucketName));

        return getTransferManager(clientOptions).upload(bucketName, key, sourceFile);
    }

    public static Upload putObject(final ClientOptions clientOptions, final InputStream sourceStream, final String bucketName, final String key) {
        LOGGER.debug(format("Sending stream as S3 object %1$s in bucket %2$s", key, bucketName));

        return getTransferManager(clientOptions).upload(bucketName, key, sourceStream, null);
    }

    public static Upload putObject(final ClientOptions clientOptions, final PutObjectRequest req) {
        LOGGER.debug(format("Sending stream as S3 object %1$s in bucket %2$s using PutObjectRequest", req.getKey(), req.getBucketName()));

        return getTransferManager(clientOptions).upload(req);
    }

    public static Download getFile(final ClientOptions clientOptions, final String bucketName, final String key, final File file) {
        LOGGER.debug(format("Receiving object %1$s as file %2$s from bucket %3$s", key, file.getAbsolutePath(), bucketName));

        return getTransferManager(clientOptions).download(bucketName, key, file);
    }

    public static Download getFile(final ClientOptions clientOptions, final GetObjectRequest getObjectRequest, final File file) {
        LOGGER.debug(format("Receiving object %1$s as file %2$s from bucket %3$s using GetObjectRequest", getObjectRequest.getKey(), file.getAbsolutePath(), getObjectRequest.getBucketName()));

        return getTransferManager(clientOptions).download(getObjectRequest, file);
    }

    public static URL generatePresignedUrl(final ClientOptions clientOptions, final String bucketName, final String key, final Date expiration) {
        LOGGER.debug(format("Generating presigned url for key %1s in bucket %2s with expiration date %3s", key, bucketName, expiration.toString()));

        return getTransferManager(clientOptions).getAmazonS3Client().generatePresignedUrl(bucketName, key, expiration, HttpMethod.GET);
    }

    // Note that whenever S3ObjectInputStream is returned, client code needs to close the internal stream to avoid resource leak.
    public static S3ObjectInputStream getObjectStream(final ClientOptions clientOptions, final String bucketName, final String key) {
        LOGGER.debug(format("Get S3ObjectInputStream from S3 Object %1$s in bucket %2$s", key, bucketName));

        return getTransferManager(clientOptions).getAmazonS3Client().getObject(bucketName, key).getObjectContent();
    }

    public static List<S3ObjectSummary> listDirectory(final ClientOptions clientOptions, final String bucketName, final String directory) {
        LOGGER.debug(format("Listing S3 directory %1$s in bucket %2$s", directory, bucketName));

        List<S3ObjectSummary> objects = new ArrayList<>();
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest();

        listObjectsRequest.withBucketName(bucketName);
        listObjectsRequest.withPrefix(directory);

        ObjectListing ol = getAmazonS3Client(clientOptions).listObjects(listObjectsRequest);
        if(ol.isTruncated()) {
            do {
                objects.addAll(ol.getObjectSummaries());
                listObjectsRequest.setMarker(ol.getNextMarker());
                ol = getAmazonS3Client(clientOptions).listObjects(listObjectsRequest);
            } while (ol.isTruncated());
        }
        else {
            objects.addAll(ol.getObjectSummaries());
        }

        if (objects.isEmpty()) {
            return emptyList();
        }

        return unmodifiableList(objects);
    }

    public static void deleteObject(final ClientOptions clientOptions, final String bucketName, final String key) {
        LOGGER.debug(format("Deleting S3 Object %1$s in bucket %2$s", key, bucketName));

        getAmazonS3Client(clientOptions).deleteObject(bucketName,key);
    }

    public static void deleteDirectory(final ClientOptions clientOptions, final String bucketName, final String directoryName) {
        LOGGER.debug(format("Deleting S3 Directory %1$s in bucket %2$s", directoryName, bucketName));

        final List<S3ObjectSummary> objects = listDirectory(clientOptions, bucketName, directoryName);

        for (final S3ObjectSummary object : objects) {

            deleteObject(clientOptions, bucketName, object.getKey());
        }

        deleteObject(clientOptions, bucketName, directoryName);
    }
}
