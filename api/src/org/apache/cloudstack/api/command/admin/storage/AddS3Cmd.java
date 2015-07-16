/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.api.command.admin.storage;

import static com.cloud.user.Account.ACCOUNT_ID_SYSTEM;
import static org.apache.cloudstack.api.ApiConstants.S3_ACCESS_KEY;
import static org.apache.cloudstack.api.ApiConstants.S3_BUCKET_NAME;
import static org.apache.cloudstack.api.ApiConstants.S3_CONNECTION_TIMEOUT;
import static org.apache.cloudstack.api.ApiConstants.S3_CONNECTION_TTL;
import static org.apache.cloudstack.api.ApiConstants.S3_END_POINT;
import static org.apache.cloudstack.api.ApiConstants.S3_HTTPS_FLAG;
import static org.apache.cloudstack.api.ApiConstants.S3_MAX_ERROR_RETRY;
import static org.apache.cloudstack.api.ApiConstants.S3_SECRET_KEY;
import static org.apache.cloudstack.api.ApiConstants.S3_SOCKET_TIMEOUT;
import static org.apache.cloudstack.api.ApiConstants.S3_USE_TCP_KEEPALIVE;
import static org.apache.cloudstack.api.BaseCmd.CommandType.BOOLEAN;
import static org.apache.cloudstack.api.BaseCmd.CommandType.INTEGER;
import static org.apache.cloudstack.api.BaseCmd.CommandType.STRING;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ImageStoreResponse;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.DiscoveryException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.storage.ImageStore;

@APICommand(name = "addS3", description = "Adds S3", responseObject = ImageStoreResponse.class, since = "4.0.0",
        requestHasSensitiveInfo = true, responseHasSensitiveInfo = false)
public final class AddS3Cmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(AddS3Cmd.class.getName());

    private static final String s_name = "adds3response";

    @Parameter(name = S3_ACCESS_KEY, type = STRING, required = true, description = "S3 access key")
    private String accessKey;

    @Parameter(name = S3_SECRET_KEY, type = STRING, required = true, description = "S3 secret key")
    private String secretKey;

    @Parameter(name = S3_END_POINT, type = STRING, required = false, description = "S3 host name")
    private String endPoint;

    @Parameter(name = S3_BUCKET_NAME, type = STRING, required = true, description = "name of the template storage bucket")
    private String bucketName;

    @Parameter(name = S3_HTTPS_FLAG, type = BOOLEAN, required = false, description = "connect to the S3 endpoint via HTTPS?")
    private Boolean httpsFlag;

    @Parameter(name = S3_CONNECTION_TIMEOUT, type = INTEGER, required = false, description = "connection timeout (milliseconds)")
    private Integer connectionTimeout;

    @Parameter(name = S3_MAX_ERROR_RETRY, type = INTEGER, required = false, description = "maximum number of times to retry on error")
    private Integer maxErrorRetry;

    @Parameter(name = S3_SOCKET_TIMEOUT, type = INTEGER, required = false, description = "socket timeout (milliseconds)")
    private Integer socketTimeout;

    @Parameter(name = S3_CONNECTION_TTL, type = INTEGER, required = false, description = "connection ttl (milliseconds)")
    private Integer connectionTtl;

    @Parameter(name = S3_USE_TCP_KEEPALIVE, type = BOOLEAN, required = false, description = "whether tcp keepalive is used")
    private Boolean useTCPKeepAlive;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
        ResourceAllocationException, NetworkRuleConflictException {

        Map<String, String> dm = new HashMap<String, String>();
        dm.put(ApiConstants.S3_ACCESS_KEY, getAccessKey());
        dm.put(ApiConstants.S3_SECRET_KEY, getSecretKey());
        dm.put(ApiConstants.S3_END_POINT, getEndPoint());
        dm.put(ApiConstants.S3_BUCKET_NAME, getBucketName());
        if (getHttpsFlag() != null) {
            dm.put(ApiConstants.S3_HTTPS_FLAG, getHttpsFlag().toString());
        }
        if (getConnectionTimeout() != null) {
            dm.put(ApiConstants.S3_CONNECTION_TIMEOUT, getConnectionTimeout().toString());
        }
        if (getMaxErrorRetry() != null) {
            dm.put(ApiConstants.S3_MAX_ERROR_RETRY, getMaxErrorRetry().toString());
        }
        if (getSocketTimeout() != null) {
            dm.put(ApiConstants.S3_SOCKET_TIMEOUT, getSocketTimeout().toString());
        }
        if (getConnectionTtl() != null) {
            dm.put(ApiConstants.S3_CONNECTION_TTL, getConnectionTtl().toString());
        }
        if (getUseTCPKeepAlive() != null) {
            dm.put(ApiConstants.S3_USE_TCP_KEEPALIVE, getUseTCPKeepAlive().toString());
        }


        try{
            ImageStore result = _storageService.discoverImageStore(null, null, "S3", null, dm);
            ImageStoreResponse storeResponse = null;
            if (result != null) {
                storeResponse = _responseGenerator.createImageStoreResponse(result);
                storeResponse.setResponseName(getCommandName());
                storeResponse.setObjectName("secondarystorage");
                setResponseObject(storeResponse);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add S3 secondary storage");
            }
        } catch (DiscoveryException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }

    @Override
    public boolean equals(final Object thatObject) {

        if (this == thatObject) {
            return true;
        }

        if (thatObject == null || this.getClass() != thatObject.getClass()) {
            return false;
        }

        final AddS3Cmd thatAddS3Cmd = (AddS3Cmd)thatObject;

        if (httpsFlag != null ? !httpsFlag.equals(thatAddS3Cmd.httpsFlag) : thatAddS3Cmd.httpsFlag != null) {
            return false;
        }

        if (accessKey != null ? !accessKey.equals(thatAddS3Cmd.accessKey) : thatAddS3Cmd.accessKey != null) {
            return false;
        }

        if (connectionTimeout != null ? !connectionTimeout.equals(thatAddS3Cmd.connectionTimeout) : thatAddS3Cmd.connectionTimeout != null) {
            return false;
        }

        if (endPoint != null ? !endPoint.equals(thatAddS3Cmd.endPoint) : thatAddS3Cmd.endPoint != null) {
            return false;
        }

        if (maxErrorRetry != null ? !maxErrorRetry.equals(thatAddS3Cmd.maxErrorRetry) : thatAddS3Cmd.maxErrorRetry != null) {
            return false;
        }

        if (secretKey != null ? !secretKey.equals(thatAddS3Cmd.secretKey) : thatAddS3Cmd.secretKey != null) {
            return false;
        }

        if (socketTimeout != null ? !socketTimeout.equals(thatAddS3Cmd.socketTimeout) : thatAddS3Cmd.socketTimeout != null) {
            return false;
        }

        if (bucketName != null ? !bucketName.equals(thatAddS3Cmd.bucketName) : thatAddS3Cmd.bucketName != null) {
            return false;
        }

        if (connectionTtl != null ? !connectionTtl.equals(thatAddS3Cmd.connectionTtl) : thatAddS3Cmd.connectionTtl != null) {
            return false;
        }

        if (useTCPKeepAlive != null ? !useTCPKeepAlive.equals(thatAddS3Cmd.useTCPKeepAlive) : thatAddS3Cmd.useTCPKeepAlive != null) {
            return false;
        }

        return true;

    }

    @Override
    public int hashCode() {

        int result = accessKey != null ? accessKey.hashCode() : 0;
        result = 31 * result + (secretKey != null ? secretKey.hashCode() : 0);
        result = 31 * result + (endPoint != null ? endPoint.hashCode() : 0);
        result = 31 * result + (bucketName != null ? bucketName.hashCode() : 0);
        result = 31 * result + (httpsFlag != null && httpsFlag == true ? 1 : 0);
        result = 31 * result + (connectionTimeout != null ? connectionTimeout.hashCode() : 0);
        result = 31 * result + (maxErrorRetry != null ? maxErrorRetry.hashCode() : 0);
        result = 31 * result + (socketTimeout != null ? socketTimeout.hashCode() : 0);
        result = 31 * result + (connectionTtl != null ? connectionTtl.hashCode() : 0);
        result = 31 * result + (useTCPKeepAlive != null && useTCPKeepAlive == true ? 1 : 0);

        return result;

    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return ACCOUNT_ID_SYSTEM;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public String getBucketName() {
        return bucketName;
    }

    public Boolean getHttpsFlag() {
        return httpsFlag;
    }

    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    public Integer getMaxErrorRetry() {
        return maxErrorRetry;
    }

    public Integer getSocketTimeout() {
        return socketTimeout;
    }

    public Integer getConnectionTtl() {
        return connectionTtl;
    }

    public Boolean getUseTCPKeepAlive() {
        return useTCPKeepAlive;
    }
}
