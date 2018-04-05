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
package com.cloud.agent.api.to;

import java.util.Date;

import com.cloud.agent.api.LogLevel;
import com.cloud.agent.api.LogLevel.Log4jLevel;
import com.cloud.storage.DataStoreRole;
import com.cloud.utils.storage.S3.ClientOptions;

public final class S3TO implements ClientOptions, DataStoreTO {

    private Long id;
    private String uuid;
    @LogLevel(Log4jLevel.Off)
    private String accessKey;
    @LogLevel(Log4jLevel.Off)
    private String secretKey;
    private String endPoint;
    private String bucketName;
    private String signer;
    private Boolean httpsFlag;
    private Boolean useTCPKeepAlive;
    private Integer connectionTimeout;
    private Integer maxErrorRetry;
    private Integer socketTimeout;
    private Integer connectionTtl;
    private Date created;
    private boolean enableRRS;
    private long maxSingleUploadSizeInBytes;
    private static final String pathSeparator = "/";

    public S3TO(final Long id, final String uuid, final String accessKey, final String secretKey, final String endPoint, final String bucketName,
            final String signer, final Boolean httpsFlag, final Integer connectionTimeout, final Integer maxErrorRetry, final Integer socketTimeout,
            final Date created, final boolean enableRRS, final long maxUploadSize, final Integer connectionTtl, final Boolean useTCPKeepAlive) {

        this.id = id;
        this.uuid = uuid;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.endPoint = endPoint;
        this.bucketName = bucketName;
        this.signer = signer;
        this.httpsFlag = httpsFlag;
        this.connectionTimeout = connectionTimeout;
        this.maxErrorRetry = maxErrorRetry;
        this.socketTimeout = socketTimeout;
        this.created = created;
        this.enableRRS = enableRRS;
        this.maxSingleUploadSizeInBytes = maxUploadSize;
        this.connectionTtl = connectionTtl;
        this.useTCPKeepAlive = useTCPKeepAlive;

    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @Override
    public String getUuid() {
        return this.uuid;
    }

    @Override
    public String getUrl() {
        return null;
    }

    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getAccessKey() {
        return this.accessKey;
    }

    public void setAccessKey(final String accessKey) {
        this.accessKey = accessKey;
    }

    @Override
    public String getSecretKey() {
        return this.secretKey;
    }

    public void setSecretKey(final String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public String getEndPoint() {
        return this.endPoint;
    }

    public void setEndPoint(final String endPoint) {
        this.endPoint = endPoint;
    }

    public String getBucketName() {
        return this.bucketName;
    }

    public void setBucketName(final String bucketName) {
        this.bucketName = bucketName;
    }

    @Override
    public String getSigner() {
        return this.signer;
    }

    public void setSigner(final String signer) {
        this.signer = signer;
    }

    @Override
    public Boolean isHttps() {
        return this.httpsFlag;
    }

    public void setHttps(final Boolean httpsFlag) {
        this.httpsFlag = httpsFlag;
    }

    @Override
    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(final Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    @Override
    public Integer getMaxErrorRetry() {
        return maxErrorRetry;
    }

    public void setMaxErrorRetry(final Integer maxErrorRetry) {
        this.maxErrorRetry = maxErrorRetry;
    }

    @Override
    public Integer getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(final Integer socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    @Override
    public Integer getConnectionTtl() {
        return this.connectionTtl;
    }

    public void setConnectionTtl(final Integer connectionTtl) {
        this.connectionTtl = connectionTtl;
    }

    @Override
    public Boolean getUseTCPKeepAlive() {
        return this.useTCPKeepAlive;
    }

    public void setUseTCPKeepAlive(final Boolean useTCPKeepAlive) {
        this.useTCPKeepAlive = useTCPKeepAlive;
    }

    public Date getCreated() {
        return this.created;
    }

    public void setCreated(final Date created) {
        this.created = created;
    }

    @Override
    public DataStoreRole getRole() {
        return DataStoreRole.Image;
    }

    public boolean getEnableRRS() {
        return enableRRS;
    }

    public void setEnableRRS(boolean enableRRS) {
        this.enableRRS = enableRRS;
    }

    public long getMaxSingleUploadSizeInBytes() {
        return maxSingleUploadSizeInBytes;
    }

    public void setMaxSingleUploadSizeInBytes(long maxSingleUploadSizeInBytes) {
        this.maxSingleUploadSizeInBytes = maxSingleUploadSizeInBytes;
    }

    public boolean getSingleUpload(long objSize) {
        if (maxSingleUploadSizeInBytes < 0) {
            // always use single part upload
            return true;
        } else if (maxSingleUploadSizeInBytes == 0) {
            // always use multi part upload
            return false;
        } else {
            // check object size to set flag
            if (objSize < maxSingleUploadSizeInBytes) {
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public String getPathSeparator() {
        return pathSeparator;
    }

    @Override
    public boolean equals(final Object thatObject) {

        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        final S3TO thatS3TO = (S3TO)thatObject;

        if (httpsFlag != null ? !httpsFlag.equals(thatS3TO.httpsFlag) : thatS3TO.httpsFlag != null) {
            return false;
        }

        if (accessKey != null ? !accessKey.equals(thatS3TO.accessKey) : thatS3TO.accessKey != null) {
            return false;
        }

        if (connectionTimeout != null ? !connectionTimeout.equals(thatS3TO.connectionTimeout) : thatS3TO.connectionTimeout != null) {
            return false;
        }

        if (endPoint != null ? !endPoint.equals(thatS3TO.endPoint) : thatS3TO.endPoint != null) {
            return false;
        }

        if (id != null ? !id.equals(thatS3TO.id) : thatS3TO.id != null) {
            return false;
        }

        if (uuid != null ? !uuid.equals(thatS3TO.uuid) : thatS3TO.uuid != null) {
            return false;
        }

        if (maxErrorRetry != null ? !maxErrorRetry.equals(thatS3TO.maxErrorRetry) : thatS3TO.maxErrorRetry != null) {
            return false;
        }

        if (secretKey != null ? !secretKey.equals(thatS3TO.secretKey) : thatS3TO.secretKey != null) {
            return false;
        }

        if (socketTimeout != null ? !socketTimeout.equals(thatS3TO.socketTimeout) : thatS3TO.socketTimeout != null) {
            return false;
        }

        if (connectionTtl != null ? !connectionTtl.equals(thatS3TO.connectionTtl) : thatS3TO.connectionTtl != null) {
            return false;
        }

        if (useTCPKeepAlive != null ? !useTCPKeepAlive.equals(thatS3TO.useTCPKeepAlive) : thatS3TO.useTCPKeepAlive != null) {
            return false;
        }

        if (bucketName != null ? !bucketName.equals(thatS3TO.bucketName) : thatS3TO.bucketName != null) {
            return false;
        }

        if (signer != null ? !signer.equals(thatS3TO.signer) : thatS3TO.signer != null) {
            return false;
        }

        if (created != null ? !created.equals(thatS3TO.created) : thatS3TO.created != null) {
            return false;
        }

        if (enableRRS != thatS3TO.enableRRS) {
            return false;
        }

        return true;

    }

    @Override
    public int hashCode() {

        int result = id != null ? id.hashCode() : 0;

        result = 31 * result + (accessKey != null ? accessKey.hashCode() : 0);
        result = 31 * result + (secretKey != null ? secretKey.hashCode() : 0);
        result = 31 * result + (endPoint != null ? endPoint.hashCode() : 0);
        result = 31 * result + (bucketName != null ? bucketName.hashCode() : 0);
        result = 31 * result + (signer != null ? signer.hashCode() : 0);
        result = 31 * result + (httpsFlag ? 1 : 0);
        result = 31 * result + (connectionTimeout != null ? connectionTimeout.hashCode() : 0);
        result = 31 * result + (maxErrorRetry != null ? maxErrorRetry.hashCode() : 0);
        result = 31 * result + (socketTimeout != null ? socketTimeout.hashCode() : 0);
        result = 31 * result + (connectionTtl != null ? connectionTtl.hashCode() : 0);
        result = 31 * result + (useTCPKeepAlive ? 1 : 0);

        return result;
    }
}
