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
package com.cloud.bridge.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "multipart_uploads")
public class MultiPartUploadsVO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "AccessKey")
    private String accessKey;

    @Column(name = "BucketName")
    private String bucketName;

    @Column(name = "NameKey")
    private String nameKey;

    @Column(name = "x_amz_acl")
    private String amzAcl;

    @Column(name = "CreateTime")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date createTime;

    public MultiPartUploadsVO() {
    }

    public MultiPartUploadsVO(String accessKey, String bucketName, String key, String cannedAccess, Date tod) {
        this.accessKey = accessKey;
        this.bucketName = bucketName;
        this.nameKey = key;
        this.amzAcl = cannedAccess;
        this.createTime = tod;
    }

    public Long getId() {
        return id;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getNameKey() {
        return nameKey;
    }

    public void setNameKey(String nameKey) {
        this.nameKey = nameKey;
    }

    public String getAmzAcl() {
        return amzAcl;
    }

    public void setAmzAcl(String amzAcl) {
        this.amzAcl = amzAcl;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

}