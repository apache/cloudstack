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
package com.cloud.bridge.io;

import java.util.Arrays;
import java.util.HashSet;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.activation.DataHandler;
import javax.activation.DataSource;

import org.apache.log4j.Logger;

import com.cloud.bridge.service.core.s3.S3BucketAdapter;
import com.cloud.bridge.service.core.s3.S3MultipartPart;
import com.cloud.bridge.service.exception.ConfigurationException;
import com.cloud.bridge.service.exception.FileNotExistException;
import com.cloud.bridge.service.exception.InternalErrorException;
import com.cloud.bridge.service.exception.OutOfStorageException;
import com.cloud.bridge.service.exception.UnsupportedException;
import com.cloud.bridge.util.StringHelper;
import com.cloud.bridge.util.OrderedPair;

import com.caringo.client.locate.Locator;
import com.caringo.client.locate.StaticLocator;
import com.caringo.client.locate.ZeroconfLocator;
import com.caringo.client.ResettableFileInputStream;
import com.caringo.client.ScspClient;
import com.caringo.client.ScspExecutionException;
import com.caringo.client.ScspHeaders;
import com.caringo.client.ScspQueryArgs;
import com.caringo.client.ScspResponse;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;

/**
 * Creates an SCSP client to a CAStor cluster, configured in "storage.root",
 * and use CAStor as the back-end storage instead of a file system.
 */
public class S3CAStorBucketAdapter implements S3BucketAdapter {
    protected final static Logger s_logger = Logger.getLogger(S3CAStorBucketAdapter.class);
    private static final MultiThreadedHttpConnectionManager s_httpClientManager = new MultiThreadedHttpConnectionManager();

    private static final int HTTP_OK = 200;
    private static final int HTTP_CREATED = 201;
    private static final int HTTP_UNSUCCESSFUL = 300;
    private static final int HTTP_PRECONDITION_FAILED = 412;

    // For ScspClient
    private static final int DEFAULT_SCSP_PORT = 80;
    private static final int DEFAULT_MAX_POOL_SIZE = 50;
    private static final int DEFAULT_MAX_RETRIES = 5;
    private static final int CONNECTION_TIMEOUT = 60 * 1000; // Request activity timeout - 1 minute
    private static final int CM_IDLE_TIMEOUT = 60 * 1000; // HttpConnectionManager idle timeout - 1 minute
    private static final int LOCATOR_RETRY_TIMEOUT = 0; // StaticLocator pool retry timeout

    private ScspClient _scspClient;  // talks to CAStor cluster
    private Locator _locator;         // maintains list of CAStor nodes
    private String _domain;          // domain where all CloudStack streams will live

    private synchronized ScspClient myClient(String mountedRoot) {
        if (_scspClient!=null) {
            return _scspClient;
        }
        // The castor cluster is specified either by listing the ip addresses of some nodes, or
        // by specifying "zeroconf=" and the cluster's mdns name -- this is "cluster" in castor's node.cfg.
        // The "domain" to store streams can be specified. If not specified, streams will be written
        // without a "domain" query arg, so they will go into the castor default domain.
        // The port is optional and must be at the end of the config string, defaults to 80.
        // Examples: "castor 172.16.78.130 172.16.78.131 80", "castor 172.16.78.130 domain=mycluster.example.com", 
        // "castor zeroconf=mycluster.example.com domain=mycluster.example.com 80"
        String[] cfg = mountedRoot.split(" ");
        int numIPs = cfg.length-1;
        String possiblePort = cfg[cfg.length-1];
        int castorPort = DEFAULT_SCSP_PORT;
        try {
            castorPort = Integer.parseInt(possiblePort);
            --numIPs;
        } catch (NumberFormatException nfe) {
            // okay, it's an ip address, not a port number
        }
        if (numIPs <= 0) {
            throw new ConfigurationException("No CAStor nodes specified in '" + mountedRoot + "'");
        }
        HashSet<String> ips = new HashSet<String>();
        String clusterName = null;
        for ( int i = 0; i < numIPs; ++i ) {
            String option = cfg[i+1]; // ip address or zeroconf=mycluster.example.com or domain=mydomain.example.com
            if (option.toLowerCase().startsWith("zeroconf=")) {
                String[] confStr = option.split("=");
                if (confStr.length != 2) {
                    throw new ConfigurationException("Could not parse cluster name from '" + option + "'");
                }
                clusterName = confStr[1];
            } else if (option.toLowerCase().startsWith("domain=")) {
                String[] confStr = option.split("=");
                if (confStr.length != 2) {
                    throw new ConfigurationException("Could not parse domain name from '" + option + "'");
                }
                _domain = confStr[1];
            } else {
                ips.add(option);
            }
        }
        if (clusterName == null && ips.isEmpty()) {
            throw new ConfigurationException("No CAStor nodes specified in '" + mountedRoot + "'");
        }
        String[] castorNodes = ips.toArray(new String[0]);  // list of configured nodes
        if (clusterName == null) {
            try {
                _locator = new StaticLocator(castorNodes, castorPort, LOCATOR_RETRY_TIMEOUT);
                _locator.start();
            } catch (IOException e) {
                throw new ConfigurationException("Could not create CAStor static locator for '" + 
                                                 Arrays.toString(castorNodes) + "'");
            }
        } else {
            try {
                clusterName = clusterName.replace(".", "_"); // workaround needed for CAStorSDK 1.3.1
                _locator = new ZeroconfLocator(clusterName);
                _locator.start();
            } catch (IOException e) {
                throw new ConfigurationException("Could not create CAStor zeroconf locator for '" + clusterName + "'");
            }
        }
        try {
            s_logger.info("CAStor client starting: " + (_domain==null ? "default domain" : "domain " + _domain) + " " + (clusterName==null ? Arrays.toString(castorNodes) : clusterName) + " :" + castorPort);
            _scspClient = new ScspClient(_locator, castorPort, DEFAULT_MAX_POOL_SIZE, DEFAULT_MAX_RETRIES, CONNECTION_TIMEOUT, CM_IDLE_TIMEOUT);
            _scspClient.start();
        } catch (Exception e) {
            s_logger.error("Unable to create CAStor client for '" + mountedRoot + "': " + e.getMessage(), e);
            throw new ConfigurationException("Unable to create CAStor client for '" + mountedRoot + "': " + e);
        }
        return _scspClient;
    }

    private String castorURL(String mountedRoot, String bucket, String fileName) {
        // TODO: Replace this method with access to ScspClient's Locator,
        // or add read method that returns the body as an unread
        // InputStream for use by loadObject() and loadObjectRange().

        myClient(mountedRoot); // make sure castorNodes and castorPort initialized
        InetSocketAddress nodeAddr = _locator.locate();
        if (nodeAddr == null) {
            throw new ConfigurationException("Unable to locate CAStor node with locator " + _locator);
        }
        InetAddress nodeInetAddr = nodeAddr.getAddress();
        if (nodeInetAddr == null) {
            _locator.foundDead(nodeAddr);
            throw new ConfigurationException("Unable to resolve CAStor node name '" + nodeAddr.getHostName() +
                                             "' to IP address");
        }
        return "http://" + nodeInetAddr.getHostAddress() + ":" + nodeAddr.getPort() + "/" + bucket + "/" + fileName +
            (_domain==null ? "" : "?domain=" + _domain);
    }

    private ScspQueryArgs domainQueryArg() {
        ScspQueryArgs qa = new ScspQueryArgs();
        if (this._domain != null)
            qa.setValue("domain", this._domain);
        return qa;
    }

    public S3CAStorBucketAdapter() {
        // TODO: is there any way to initialize CAStor client here, can it
        // get to config?
    }

    @Override
    public void createContainer(String mountedRoot, String bucket) {
        try {
            ScspResponse bwResponse = myClient(mountedRoot).write(bucket, new ByteArrayInputStream("".getBytes()), 0, domainQueryArg(), new ScspHeaders());
            if (bwResponse.getHttpStatusCode() != HTTP_CREATED) {
                if (bwResponse.getHttpStatusCode() == HTTP_PRECONDITION_FAILED)
                    s_logger.error("CAStor unable to create bucket " + bucket + " because domain " +
                                   (this._domain==null ? "(default)" : this._domain) + " does not exist");
                else
                    s_logger.error("CAStor unable to create bucket " + bucket + ": " + bwResponse.getHttpStatusCode());
                throw new OutOfStorageException("CAStor unable to create bucket " + bucket + ": " +
                                                bwResponse.getHttpStatusCode());
            }
        } catch (ScspExecutionException e) {
            s_logger.error("CAStor unable to create bucket " + bucket, e);
            throw new OutOfStorageException("CAStor unable to create bucket " + bucket + ": " + e.getMessage());
        }
    }

    @Override
    public void deleteContainer(String mountedRoot, String bucket) {
        try {
            ScspResponse bwResponse = myClient(mountedRoot).delete("", bucket, domainQueryArg(), new ScspHeaders());
            if (bwResponse.getHttpStatusCode() >= HTTP_UNSUCCESSFUL) {
                s_logger.error("CAStor unable to delete bucket " + bucket + ": " + bwResponse.getHttpStatusCode());
                throw new OutOfStorageException("CAStor unable to delete bucket " + bucket + ": " +
                                                bwResponse.getHttpStatusCode());
            }
        } catch (ScspExecutionException e) {
            s_logger.error("CAStor unable to delete bucket " + bucket, e);
            throw new OutOfStorageException("CAStor unable to delete bucket " + bucket + ": " + e.getMessage());
        }
    }

    @Override
    public String saveObject(InputStream is, String mountedRoot, String bucket, String fileName)
    {
        // TODO: Currently this writes the object to a temporary file,
        // so that the MD5 can be computed and so that we have the
        // stream length needed by this version of CAStor SDK. Will
        // change to calculate MD5 while streaming to CAStor and to
        // either pass Content-length to this method or use newer SDK
        // that doesn't require it.

        FileOutputStream fos = null;
        MessageDigest md5 = null;

        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            s_logger.error("Unexpected exception " + e.getMessage(), e);
            throw new InternalErrorException("Unable to get MD5 MessageDigest", e);
        }

        File spoolFile = null;
        try {
            spoolFile = File.createTempFile("castor", null);
        } catch (IOException e) {
            s_logger.error("Unexpected exception creating temporary CAStor spool file: " + e.getMessage(), e);
            throw new InternalErrorException("Unable to create temporary CAStor spool file", e);
        }
        try {
            String retVal;
            int streamLen = 0;
            try {
                fos = new FileOutputStream(spoolFile);
                byte[] buffer = new byte[4096];
                int len = 0;
                while( (len = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                    streamLen = streamLen + len;
                    md5.update(buffer, 0, len);

                }
                //Convert MD5 digest to (lowercase) hex String
                retVal = StringHelper.toHexString(md5.digest());

            } catch(IOException e) {
                s_logger.error("Unexpected exception " + e.getMessage(), e);
                throw new OutOfStorageException(e);
            } finally {
                try {
                    if (null != fos)
                        fos.close();
                } catch( Exception e ) {
                    s_logger.error("Can't close CAStor spool file " +
                                   spoolFile.getAbsolutePath() + ": " + e.getMessage(), e);
                    throw new OutOfStorageException("Unable to close CAStor spool file: " + e.getMessage(), e);
                }
            }

            try {
                ScspResponse bwResponse =
                    myClient(mountedRoot).write(bucket + "/" + fileName,
                                                new ResettableFileInputStream(spoolFile), streamLen,
                                                domainQueryArg(), new ScspHeaders());
                if (bwResponse.getHttpStatusCode() >= HTTP_UNSUCCESSFUL) {
                    s_logger.error("CAStor write responded with error " + bwResponse.getHttpStatusCode());
                    throw new OutOfStorageException("Unable to write object to CAStor " +
                                                    bucket + "/" + fileName + ": " + bwResponse.getHttpStatusCode());
                }
            } catch (ScspExecutionException e) {
                s_logger.error("Unable to write object to CAStor " + bucket + "/" + fileName, e);
                throw new OutOfStorageException("Unable to write object to CAStor " + bucket + "/" + fileName + ": " +
                                                e.getMessage());
            } catch (IOException ie) {
                s_logger.error("Unable to write object to CAStor " + bucket + "/" + fileName, ie);
                throw new OutOfStorageException("Unable to write object to CAStor " + bucket + "/" + fileName + ": " +
                                                ie.getMessage());
            }
            return retVal;
        } finally {
            try {
                if (!spoolFile.delete()) {
                    s_logger.error("Failed to delete CAStor spool file " + spoolFile.getAbsolutePath());
                }
            } catch (SecurityException e) {
                s_logger.error("Unable to delete CAStor spool file " + spoolFile.getAbsolutePath(), e);
            }
        }
    }

    /**
     * From a list of files (each being one part of the multipart upload), concatentate all files into a single
     * object that can be accessed by normal S3 calls.    This function could take a long time since a multipart is
     * allowed to have upto 10,000 parts (each 5 gib long).      Amazon defines that while this operation is in progress
     * whitespace is sent back to the client inorder to keep the HTTP connection alive.
     *
     * @param mountedRoot - where both the source and dest buckets are located
     * @param destBucket - resulting location of the concatenated objects
     * @param fileName - resulting file name of the concatenated objects
     * @param sourceBucket - special bucket used to save uploaded file parts
     * @param parts - an array of file names in the sourceBucket
     * @param client - if not null, then keep the servlet connection alive while this potentially long concatentation takes place
     * @return OrderedPair with the first value the MD5 of the final object, and the second value the length of the final object
     */
    @Override
    public OrderedPair<String,Long> concatentateObjects(String mountedRoot, String destBucket, String fileName, String sourceBucket, S3MultipartPart[] parts, OutputStream client)
    {
        // TODO
        throw new UnsupportedException("Multipart upload support not yet implemented in CAStor plugin");

        /*
        MessageDigest md5;
        long totalLength = 0;

        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            s_logger.error("Unexpected exception " + e.getMessage(), e);
            throw new InternalErrorException("Unable to get MD5 MessageDigest", e);
        }

        File file = new File(getBucketFolderDir(mountedRoot, destBucket) + File.separatorChar + fileName);
        try {
            // -> when versioning is off we need to rewrite the file contents
            file.delete();
            file.createNewFile();

            final FileOutputStream fos = new FileOutputStream(file);
            byte[] buffer = new byte[4096];

            // -> get the input stream for the next file part
            for( int i=0; i < parts.length; i++ )
            {
               DataHandler nextPart = loadObject( mountedRoot, sourceBucket, parts[i].getPath());
               InputStream is = nextPart.getInputStream();

               int len = 0;
               while( (len = is.read(buffer)) > 0) {
                   fos.write(buffer, 0, len);
                   md5.update(buffer, 0, len);
                   totalLength += len;
               }
               is.close();

               // -> after each file write tell the client we are still here to keep connection alive
               if (null != client) {
                   client.write( new String(" ").getBytes());
                   client.flush();
               }
            }
            fos.close();
            return new OrderedPair<String, Long>(StringHelper.toHexString(md5.digest()), new Long(totalLength));
            //Create an ordered pair whose first element is the MD4 digest as a (lowercase) hex String
        }
        catch(IOException e) {
            s_logger.error("concatentateObjects unexpected exception " + e.getMessage(), e);
            throw new OutOfStorageException(e);
        }
        */
    }

    @Override
    public DataHandler loadObject(String mountedRoot, String bucket, String fileName) {
        try {
            return new DataHandler(new URL(castorURL(mountedRoot, bucket, fileName)));
        } catch (MalformedURLException e) {
            s_logger.error("Failed to loadObject from CAStor", e);
            throw new FileNotExistException("Unable to load object from CAStor: " + e.getMessage());
        }
    }

    @Override
    public void deleteObject(String mountedRoot, String bucket, String fileName) {
        String filePath = bucket + "/" + fileName;
        try {
            ScspResponse bwResponse = myClient(mountedRoot).delete("", filePath, domainQueryArg(), new ScspHeaders());
            if (bwResponse.getHttpStatusCode() != HTTP_OK) {
                s_logger.error("CAStor delete object responded with error " + bwResponse.getHttpStatusCode());
                throw new OutOfStorageException("CAStor unable to delete object " + filePath + ": " +
                                                bwResponse.getHttpStatusCode());
            }
        } catch (ScspExecutionException e) {
            s_logger.error("CAStor unable to delete object " + filePath, e);
            throw new OutOfStorageException("CAStor unable to delete object " + filePath + ": " + e.getMessage());
        }
    }

    public class ScspDataSource implements DataSource {
        String content_type = null;
        byte content[] = null;
        public ScspDataSource(GetMethod method) {
            Header h = method.getResponseHeader("Content-type");
            if (h != null) {
                content_type = h.getValue();
            }
            try{
                content = method.getResponseBody();
            }catch(IOException e){
                s_logger.error("CAStor loadObjectRange getInputStream error", e);
            }
        }
        @Override
        public String getContentType() {
            return content_type;
        }
        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }
        @Override
        public String getName() {
            assert(false);
            return null;
        }
        @Override
        public OutputStream getOutputStream() throws IOException {
            assert(false);
            return null;
        }
    }

    @Override
    public DataHandler loadObjectRange(String mountedRoot, String bucket, String fileName, long startPos, long endPos) {
        HttpClient httpClient = new HttpClient(s_httpClientManager);
        // Create a method instance.
        GetMethod method = new GetMethod(castorURL(mountedRoot, bucket, fileName));
        method.addRequestHeader("Range", "bytes=" + startPos + "-" + endPos);
        int statusCode;
        try {
            statusCode = httpClient.executeMethod(method);
        } catch (HttpException e) {
            s_logger.error("CAStor loadObjectRange failure", e);
            throw new FileNotExistException("CAStor loadObjectRange failure: " + e);
        } catch (IOException e) {
            s_logger.error("CAStor loadObjectRange failure", e);
            throw new FileNotExistException("CAStor loadObjectRange failure: " + e);
        }
        if (statusCode < HTTP_OK || statusCode >= HTTP_UNSUCCESSFUL) {
            s_logger.error("CAStor loadObjectRange response: "+  statusCode);
            throw new FileNotExistException("CAStor loadObjectRange response: " + statusCode);
        }
        DataHandler ret = new DataHandler(new ScspDataSource(method));
        method.releaseConnection();
        return ret;
    }

    @Override
    public String getBucketFolderDir(String mountedRoot, String bucket) {
        // This method shouldn't be needed and doesn't need to use
        // mountedRoot (which is CAStor config values here), right?
        String bucketFolder = getBucketFolderName(bucket);
        return bucketFolder;
    }

    private String getBucketFolderName(String bucket) {
        // temporary
        String name = bucket.replace(' ', '_');
        name = bucket.replace('\\', '-');
        name = bucket.replace('/', '-');

        return name;
    }
}
