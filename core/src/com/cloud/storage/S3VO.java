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
package com.cloud.storage;

import com.cloud.agent.api.to.S3TO;
import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Entity
@Table(name = "s3")
public class S3VO implements S3 {

    public static final String ID_COLUMN_NAME = "id";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = ID_COLUMN_NAME)
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "access_key")
    private String accessKey;

    @Column(name = "secret_key")
    private String secretKey;

    @Column(name = "end_point")
    private String endPoint;

    @Column(name = "bucket")
    private String bucketName;

    @Column(name = "https")
    private Integer httpsFlag;

    @Column(name = "connection_timeout")
    private Integer connectionTimeout;

    @Column(name = "max_error_retry")
    private Integer maxErrorRetry;

    @Column(name = "socket_timeout")
    private Integer socketTimeout;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    public S3VO() {
        super();
    }

    public S3VO(final String uuid, final String accessKey,
            final String secretKey, final String endPoint,
            final String bucketName, final Boolean httpsFlag,
            final Integer connectionTimeout, final Integer maxErrorRetry,
            final Integer socketTimeout, final Date created) {

        super();

        this.uuid = uuid;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.endPoint = endPoint;
        this.bucketName = bucketName;

        Integer value = null;
        if (httpsFlag != null) {
            value = httpsFlag == false ? 0 : 1;
        }
        this.httpsFlag = value;

        this.connectionTimeout = connectionTimeout;
        this.maxErrorRetry = maxErrorRetry;
        this.socketTimeout = socketTimeout;
        this.created = created;

    }

    @Override
    public S3TO toS3TO() {

        Boolean httpsFlag = null;
        if (this.httpsFlag != null) {
            httpsFlag = this.httpsFlag == 0 ? false : true;
        }

        return new S3TO(this.id, this.uuid, this.accessKey, this.secretKey,
                this.endPoint, this.bucketName, httpsFlag,
                this.connectionTimeout, this.maxErrorRetry, this.socketTimeout,
                this.created);

    }

    public long getId() {
        return this.id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public String getUuid() {
        return this.uuid;
    }

    public void setUuid(final String uuid) {
        this.uuid = uuid;
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

    public String getBucketName() {
        return this.bucketName;
    }

    public void setBucketName(final String bucketName) {
        this.bucketName = bucketName;
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

    public void setConnectionTimeout(final int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Integer getMaxErrorRetry() {
        return this.maxErrorRetry;
    }

    public void setMaxErrorRetry(final int maxErrorRetry) {
        this.maxErrorRetry = maxErrorRetry;
    }

    public Integer getSocketTimeout() {
        return this.socketTimeout;
    }

    public void setSocketTimeout(final int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public Date getCreated() {
        return this.created;
    }

    public void setCreated(final Date created) {
        this.created = created;
    }

}
