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

package com.cloud.utils;

import static com.amazonaws.Protocol.HTTP;
import static com.amazonaws.Protocol.HTTPS;
import static com.cloud.utils.StringUtils.join;
import static java.io.File.createTempFile;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static org.apache.commons.lang.ArrayUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.cloud.utils.exception.CloudRuntimeException;

public final class S3Utils {

    private static final Logger LOGGER = Logger.getLogger(S3Utils.class);

    public static final String SEPARATOR = "/";

    private static final int MIN_BUCKET_NAME_LENGTH = 3;
    private static final int MAX_BUCKET_NAME_LENGTH = 63;

    private S3Utils() {
        super();
    }

    public static AmazonS3 acquireClient(final ClientOptions clientOptions) {

        final AWSCredentials credentials = new BasicAWSCredentials(clientOptions.getAccessKey(), clientOptions.getSecretKey());

        final ClientConfiguration configuration = new ClientConfiguration();

        if (clientOptions.isHttps() != null) {
            configuration.setProtocol(clientOptions.isHttps() == true ? HTTPS : HTTP);
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
            //configuration.setUseTcpKeepAlive(clientOptions.getUseTCPKeepAlive());
            LOGGER.debug("useTCPKeepAlive not supported by old AWS SDK");
        }

        if (clientOptions.getConnectionTtl() != null) {
            //configuration.setConnectionTTL(clientOptions.getConnectionTtl());
            LOGGER.debug("connectionTtl not supported by old AWS SDK");
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(format("Creating S3 client with configuration: [protocol: %1$s, connectionTimeOut: " + "%2$s, maxErrorRetry: %3$s, socketTimeout: %4$s, useTCPKeepAlive: %5$s, connectionTtl: %6$s]",
                configuration.getProtocol(), configuration.getConnectionTimeout(), configuration.getMaxErrorRetry(), configuration.getSocketTimeout(),
                -1, -1));
        }

        final AmazonS3Client client = new AmazonS3Client(credentials, configuration);

        if (isNotBlank(clientOptions.getEndPoint())) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(format("Setting the end point for S3 client %1$s to %2$s.", client, clientOptions.getEndPoint()));
            }
            client.setEndpoint(clientOptions.getEndPoint());
        }

        return client;

    }

    public static void putFile(final ClientOptions clientOptions, final File sourceFile, final String bucketName, final String key) {

        assert clientOptions != null;
        assert sourceFile != null;
        assert !isBlank(bucketName);
        assert !isBlank(key);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(format("Sending file %1$s as S3 object %2$s in " + "bucket %3$s", sourceFile.getName(), key, bucketName));
        }

        acquireClient(clientOptions).putObject(bucketName, key, sourceFile);

    }

    public static void putObject(final ClientOptions clientOptions, final InputStream sourceStream, final String bucketName, final String key) {

        assert clientOptions != null;
        assert sourceStream != null;
        assert !isBlank(bucketName);
        assert !isBlank(key);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(format("Sending stream as S3 object %1$s in " + "bucket %2$s", key, bucketName));
        }

        acquireClient(clientOptions).putObject(bucketName, key, sourceStream, null);

    }

    public static void putObject(final ClientOptions clientOptions, final PutObjectRequest req) {

        assert clientOptions != null;
        assert req != null;

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(format("Sending stream as S3 object using PutObjectRequest"));
        }

        acquireClient(clientOptions).putObject(req);

    }

    // multi-part upload file
    public static void mputFile(final ClientOptions clientOptions, final File sourceFile, final String bucketName, final String key) throws InterruptedException {

        assert clientOptions != null;
        assert sourceFile != null;
        assert !isBlank(bucketName);
        assert !isBlank(key);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(format("Multipart sending file %1$s as S3 object %2$s in " + "bucket %3$s", sourceFile.getName(), key, bucketName));
        }
        TransferManager tm = new TransferManager(S3Utils.acquireClient(clientOptions));
        Upload upload = tm.upload(bucketName, key, sourceFile);
        upload.waitForCompletion();
    }

    // multi-part upload object
    public static void mputObject(final ClientOptions clientOptions, final InputStream sourceStream, final String bucketName, final String key)
        throws InterruptedException {

        assert clientOptions != null;
        assert sourceStream != null;
        assert !isBlank(bucketName);
        assert !isBlank(key);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(format("Multipart sending stream as S3 object %1$s in " + "bucket %2$s", key, bucketName));
        }
        TransferManager tm = new TransferManager(S3Utils.acquireClient(clientOptions));
        Upload upload = tm.upload(bucketName, key, sourceStream, null);
        upload.waitForCompletion();
    }

    // multi-part upload object
    public static void mputObject(final ClientOptions clientOptions, final PutObjectRequest req) throws InterruptedException {

        assert clientOptions != null;
        assert req != null;

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Multipart sending object to S3 using PutObjectRequest");
        }
        TransferManager tm = new TransferManager(S3Utils.acquireClient(clientOptions));
        Upload upload = tm.upload(req);
        upload.waitForCompletion();

    }

    public static void setObjectAcl(final ClientOptions clientOptions, final String bucketName, final String key, final CannedAccessControlList acl) {

        assert clientOptions != null;
        assert acl != null;

        acquireClient(clientOptions).setObjectAcl(bucketName, key, acl);

    }

    public static URL generatePresignedUrl(final ClientOptions clientOptions, final String bucketName, final String key, final Date expiration) {

        assert clientOptions != null;
        assert !isBlank(bucketName);
        assert !isBlank(key);

        return acquireClient(clientOptions).generatePresignedUrl(bucketName, key, expiration, HttpMethod.GET);

    }

    // Note that whenever S3Object is returned, client code needs to close the internal stream to avoid resource leak.
    public static S3Object getObject(final ClientOptions clientOptions, final String bucketName, final String key) {

        assert clientOptions != null;
        assert !isBlank(bucketName);
        assert !isBlank(key);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(format("Get S3 object %1$s in " + "bucket %2$s", key, bucketName));
        }

        return acquireClient(clientOptions).getObject(bucketName, key);

    }

    @SuppressWarnings("unchecked")
    public static File getFile(final ClientOptions clientOptions, final String bucketName, final String key, final File targetDirectory,
        final FileNamingStrategy namingStrategy) {

        assert clientOptions != null;
        assert isNotBlank(bucketName);
        assert isNotBlank(key);
        assert targetDirectory != null && targetDirectory.isDirectory();
        assert namingStrategy != null;

        final AmazonS3 connection = acquireClient(clientOptions);

        File tempFile = null;
        try {

            tempFile = createTempFile(join("-", targetDirectory.getName(), currentTimeMillis(), "part"), "tmp", targetDirectory);
            tempFile.deleteOnExit();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(format("Downloading object %1$s from bucket %2$s to temp file %3$s", key, bucketName, tempFile.getName()));
            }

            try {
                connection.getObject(new GetObjectRequest(bucketName, key), tempFile);
            } catch (AmazonClientException ex) {
                // hack to handle different ETAG format generated from RiakCS for multi-part uploaded object
                String msg = ex.getMessage();
                if (!msg.contains("verify integrity")) {
                    throw ex;
                }
            }

            final File targetFile = new File(targetDirectory, namingStrategy.determineFileName(key));
            tempFile.renameTo(targetFile);

            return targetFile;

        } catch (FileNotFoundException e) {

            throw new CloudRuntimeException(format("Failed open file %1$s in order to get object %2$s from bucket %3$s.", targetDirectory.getAbsoluteFile(), bucketName,
                key), e);

        } catch (IOException e) {

            throw new CloudRuntimeException(format("Unable to allocate temporary file in directory %1$s to download %2$s:%3$s from S3",
                targetDirectory.getAbsolutePath(), bucketName, key), e);

        } finally {

            if (tempFile != null) {
                tempFile.delete();
            }

        }

    }

    public static List<File> getDirectory(final ClientOptions clientOptions, final String bucketName, final String sourcePath, final File targetDirectory,
        final FileNamingStrategy namingStrategy) {

        assert clientOptions != null;
        assert isNotBlank(bucketName);
        assert isNotBlank(sourcePath);
        assert targetDirectory != null;

        final AmazonS3 connection = acquireClient(clientOptions);

        // List the objects in the source directory on S3
        final List<S3ObjectSummary> objectSummaries = listDirectory(bucketName, sourcePath, connection);
        final List<File> files = new ArrayList<File>();

        for (final S3ObjectSummary objectSummary : objectSummaries) {

            files.add(getFile(clientOptions, bucketName, objectSummary.getKey(), targetDirectory, namingStrategy));

        }

        return unmodifiableList(files);

    }

    public static List<S3ObjectSummary> getDirectory(final ClientOptions clientOptions, final String bucketName, final String sourcePath) {
        assert clientOptions != null;
        assert isNotBlank(bucketName);
        assert isNotBlank(sourcePath);

        final AmazonS3 connection = acquireClient(clientOptions);

        // List the objects in the source directory on S3
        return listDirectory(bucketName, sourcePath, connection);
    }

    private static List<S3ObjectSummary> listDirectory(final String bucketName, final String directory, final AmazonS3 client) {

        List<S3ObjectSummary> objects = new ArrayList<S3ObjectSummary>();
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(bucketName).withPrefix(directory + SEPARATOR);

        ObjectListing ol = client.listObjects(listObjectsRequest);
        if(ol.isTruncated()) {
            do {
                objects.addAll(ol.getObjectSummaries());
                listObjectsRequest.setMarker(ol.getNextMarker());
                ol = client.listObjects(listObjectsRequest);
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

    public static void putDirectory(final ClientOptions clientOptions, final String bucketName, final File directory, final FilenameFilter fileNameFilter,
        final ObjectNamingStrategy namingStrategy) {

        assert clientOptions != null;
        assert isNotBlank(bucketName);
        assert directory != null && directory.isDirectory();
        assert fileNameFilter != null;
        assert namingStrategy != null;

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(format("Putting directory %1$s in S3 bucket %2$s.", directory.getAbsolutePath(), bucketName));
        }

        // Determine the list of files to be sent using the passed filter ...
        final File[] files = directory.listFiles(fileNameFilter);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(format("Putting files (%1$s) in S3 bucket %2$s.", ArrayUtils.toString(files, "no files found"), bucketName));
        }

        // Skip spinning up an S3 connection when no files will be sent ...
        if (isEmpty(files)) {
            return;
        }

        final AmazonS3 client = acquireClient(clientOptions);

        // Send the files to S3 using the passed ObjectNaming strategy to
        // determine the key ...
        for (final File file : files) {
            final String key = namingStrategy.determineKey(file);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(format("Putting file %1$s into bucket %2$s with key %3$s.", file.getAbsolutePath(), bucketName, key));
            }
            client.putObject(bucketName, key, file);
        }

    }

    public static void deleteObject(final ClientOptions clientOptions, final String bucketName, final String key) {

        assert clientOptions != null;
        assert isNotBlank(bucketName);
        assert isNotBlank(key);

        final AmazonS3 client = acquireClient(clientOptions);

        client.deleteObject(bucketName, key);

    }

    public static void deleteDirectory(final ClientOptions clientOptions, final String bucketName, final String directoryName) {

        assert clientOptions != null;
        assert isNotBlank(bucketName);
        assert isNotBlank(directoryName);

        final AmazonS3 client = acquireClient(clientOptions);

        final List<S3ObjectSummary> objects = listDirectory(bucketName, directoryName, client);

        for (final S3ObjectSummary object : objects) {

            client.deleteObject(bucketName, object.getKey());

        }

        client.deleteObject(bucketName, directoryName);

    }

    public static boolean canConnect(final ClientOptions clientOptions) {

        try {

            acquireClient(clientOptions);
            return true;

        } catch (AmazonClientException e) {

            LOGGER.warn("Ignored Exception while checking connection options", e);
            return false;

        }

    }

    public static boolean doesBucketExist(final ClientOptions clientOptions, final String bucketName) {

        assert clientOptions != null;
        assert !isBlank(bucketName);

        try {

            final List<Bucket> buckets = acquireClient(clientOptions).listBuckets();

            for (Bucket bucket : buckets) {
                if (bucket.getName().equals(bucketName)) {
                    return true;
                }
            }

            return false;

        } catch (AmazonClientException e) {

            LOGGER.warn("Ignored Exception while checking bucket existence", e);
            return false;

        }

    }

    public static boolean canReadWriteBucket(final ClientOptions clientOptions, final String bucketName) {

        assert clientOptions != null;
        assert isNotBlank(bucketName);

        try {

            final AmazonS3 client = acquireClient(clientOptions);

            final String fileContent = "testing put and delete";
            final InputStream inputStream = new ByteArrayInputStream(fileContent.getBytes());
            final String key = UUID.randomUUID().toString() + ".txt";

            final ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(fileContent.length());

            client.putObject(bucketName, key, inputStream, metadata);
            client.deleteObject(bucketName, key);

            return true;

        } catch (AmazonClientException e) {

            return false;

        }

    }

    public static List<String> checkClientOptions(ClientOptions clientOptions) {

        assert clientOptions != null;

        List<String> errorMessages = new ArrayList<String>();

        errorMessages.addAll(checkRequiredField("access key", clientOptions.getAccessKey()));
        errorMessages.addAll(checkRequiredField("secret key", clientOptions.getSecretKey()));

        errorMessages.addAll(checkOptionalField("connection timeout", clientOptions.getConnectionTimeout()));
        errorMessages.addAll(checkOptionalField("socket timeout", clientOptions.getSocketTimeout()));
        errorMessages.addAll(checkOptionalField("max error retries", clientOptions.getMaxErrorRetry()));
        errorMessages.addAll(checkOptionalField("connection ttl", clientOptions.getConnectionTtl()));

        return unmodifiableList(errorMessages);

    }

    public static List<String> checkBucketName(final String bucketLabel, final String bucket) {

        assert isNotBlank(bucketLabel);
        assert isNotBlank(bucket);

        final List<String> errorMessages = new ArrayList<String>();

        if (bucket.length() < MIN_BUCKET_NAME_LENGTH) {
            errorMessages.add(format("The length of %1$s " + "for the %2$s must have a length of at least %3$s " + "characters", bucket, bucketLabel,
                MIN_BUCKET_NAME_LENGTH));
        }

        if (bucket.length() > MAX_BUCKET_NAME_LENGTH) {
            errorMessages.add(format("The length of %1$s " + "for the %2$s must not have a length of at greater" + " than %3$s characters", bucket, bucketLabel,
                MAX_BUCKET_NAME_LENGTH));
        }

        return unmodifiableList(errorMessages);

    }

    private static List<String> checkOptionalField(final String fieldName, final Integer fieldValue) {
        if (fieldValue != null && fieldValue < 0) {
            return singletonList(format("The value of %1$s must " + "be greater than zero.", fieldName));
        }
        return emptyList();
    }

    private static List<String> checkRequiredField(String fieldName, String fieldValue) {
        if (isBlank(fieldValue)) {
            return singletonList(format("A %1$s must be specified.", fieldName));
        }
        return emptyList();
    }

    public interface ClientOptions {

        String getAccessKey();

        String getSecretKey();

        String getEndPoint();

        Boolean isHttps();

        Integer getConnectionTimeout();

        Integer getMaxErrorRetry();

        Integer getSocketTimeout();

        Boolean getUseTCPKeepAlive();

        Integer getConnectionTtl();
    }

    public interface ObjectNamingStrategy {

        String determineKey(File file);

    }

    public interface FileNamingStrategy {

        String determineFileName(String key);

    }

}
