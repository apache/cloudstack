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
package org.apache.cloudstack.api.response;

import static org.apache.cloudstack.api.ApiConstants.ID;
import static org.apache.cloudstack.api.ApiConstants.S3_ACCESS_KEY;
import static org.apache.cloudstack.api.ApiConstants.S3_BUCKET_NAME;
import static org.apache.cloudstack.api.ApiConstants.S3_CONNECTION_TIMEOUT;
import static org.apache.cloudstack.api.ApiConstants.S3_END_POINT;
import static org.apache.cloudstack.api.ApiConstants.S3_HTTPS_FLAG;
import static org.apache.cloudstack.api.ApiConstants.S3_MAX_ERROR_RETRY;
import static org.apache.cloudstack.api.ApiConstants.S3_SECRET_KEY;
import static org.apache.cloudstack.api.ApiConstants.S3_SOCKET_TIMEOUT;

import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class S3Response extends BaseResponse {

    @SerializedName(ID)
    @Param(description = "The ID of the S3 configuration")
    private String id;

    @SerializedName(S3_ACCESS_KEY)
    @Param(description = "The S3 access key")
    private String accessKey;

    @SerializedName(S3_SECRET_KEY)
    @Param(description = "The S3 secret key")
    private String secretKey;

    @SerializedName(S3_END_POINT)
    @Param(description = "The S3 end point")
    private String endPoint;

    @SerializedName(S3_BUCKET_NAME)
    @Param(description = "The name of the template storage bucket")
    private String bucketName;

    @SerializedName(S3_HTTPS_FLAG)
    @Param(description = "Connect to S3 using HTTPS?")
    private Integer httpsFlag;

    @SerializedName(S3_CONNECTION_TIMEOUT)
    @Param(description = "The connection timeout (milliseconds)")
    private Integer connectionTimeout;

    @SerializedName(S3_MAX_ERROR_RETRY)
    @Param(description = "The maximum number of time to retry a connection on error.")
    private Integer maxErrorRetry;

    @SerializedName(S3_SOCKET_TIMEOUT)
    @Param(description = "The connection socket (milliseconds)")
    private Integer socketTimeout;

    @Override
    public boolean equals(final Object thatObject) {

        if (this == thatObject) {
            return true;
        }

        if (thatObject == null || this.getClass() != thatObject.getClass()) {
            return false;
        }

        final S3Response thatS3Response = (S3Response) thatObject;

        if (this.httpsFlag != null ? !this.httpsFlag.equals(thatS3Response.httpsFlag) : thatS3Response.httpsFlag != null) {
            return false;
        }

        if (this.accessKey != null ? !this.accessKey.equals(thatS3Response.accessKey) : thatS3Response.accessKey != null) {
            return false;
        }

        if (this.connectionTimeout != null ? !this.connectionTimeout.equals(thatS3Response.connectionTimeout) : thatS3Response.connectionTimeout != null) {
            return false;
        }

        if (this.endPoint != null ? !this.endPoint.equals(thatS3Response.endPoint) : thatS3Response.endPoint != null) {
            return false;
        }

        if (this.id != null ? !this.id.equals(thatS3Response.id) : thatS3Response.id != null) {
            return false;
        }

        if (this.maxErrorRetry != null ? !this.maxErrorRetry.equals(thatS3Response.maxErrorRetry) : thatS3Response.maxErrorRetry != null) {
            return false;
        }

        if (this.secretKey != null ? !this.secretKey.equals(thatS3Response.secretKey) : thatS3Response.secretKey != null) {
            return false;
        }

        if (this.socketTimeout != null ? !this.socketTimeout.equals(thatS3Response.socketTimeout) : thatS3Response.socketTimeout != null) {
            return false;
        }

        if (this.bucketName != null ? !this.bucketName.equals(thatS3Response.bucketName) : thatS3Response.bucketName != null) {
            return false;
        }

        return true;

    }

    @Override
    public int hashCode() {

        int result = this.id != null ? this.id.hashCode() : 0;
        result = 31 * result + (this.accessKey != null ? this.accessKey.hashCode() : 0);
        result = 31 * result + (this.secretKey != null ? this.secretKey.hashCode() : 0);
        result = 31 * result + (this.endPoint != null ? this.endPoint.hashCode() : 0);
        result = 31 * result + (this.bucketName != null ? this.bucketName.hashCode() : 0);
        result = 31 * result + (this.httpsFlag != null ? this.httpsFlag : 0);
        result = 31 * result + (this.connectionTimeout != null ? this.connectionTimeout.hashCode() : 0);
        result = 31 * result + (this.maxErrorRetry != null ? this.maxErrorRetry.hashCode() : 0);
        result = 31 * result + (this.socketTimeout != null ? this.socketTimeout.hashCode() : 0);

        return result;

    }

    @Override
    public String getObjectId() {
        return this.id;
    }

    public void setObjectId(String id) {
        this.id = id;
    }

    public String getAccessKey() {
        return this.accessKey;
    }

    public void setAccessKey(final String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return this.secretKey;
    }

    public void setSecretKey(final String secretKey) {
        this.secretKey = secretKey;
    }

    public String getEndPoint() {
        return this.endPoint;
    }

    public void setEndPoint(final String endPoint) {
        this.endPoint = endPoint;
    }


    public String getTemplateBucketName() {
        return this.bucketName;
    }

    public void setTemplateBucketName(final String templateBucketName) {
        this.bucketName = templateBucketName;
    }

    public Integer getHttpsFlag() {
        return this.httpsFlag;
    }

    public void setHttpsFlag(final Integer httpsFlag) {
        this.httpsFlag = httpsFlag;
    }

    public Integer getConnectionTimeout() {
        return this.connectionTimeout;
    }

    public void setConnectionTimeout(final Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Integer getMaxErrorRetry() {
        return this.maxErrorRetry;
    }

    public void setMaxErrorRetry(final Integer maxErrorRetry) {
        this.maxErrorRetry = maxErrorRetry;
    }

    public Integer getSocketTimeout() {
        return this.socketTimeout;
    }

    public void setSocketTimeout(final Integer socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

}
