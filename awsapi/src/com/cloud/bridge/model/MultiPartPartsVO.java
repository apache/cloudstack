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
@Table(name="multipart_parts")
public class MultiPartPartsVO {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="ID")
    private Long id;
    
    @Column(name="UploadID")
    private Long uploadid;
    
    @Column(name="partNumber")
    private int partNumber;
    
    @Column(name="MD5")
    private String md5;
    
    @Column(name="StoredPath")
    private String storedPath;
    
    @Column(name="StoredSize")
    private Long storedSize;
    
    @Column(name="CreateTime")
    @Temporal(value=TemporalType.TIMESTAMP)
    private Date createTime;

    public MultiPartPartsVO() { }
    
    public MultiPartPartsVO(int uploadId, int partNumber, String md5,
            String storedPath, int size, Date date) {
        this.uploadid = new Long(uploadId);
        this.partNumber = partNumber;
        this.md5 = md5;
        this.storedPath = storedPath;
        this.storedSize = new Long(size);
        this.createTime = date;
    }

    public Long getUploadid() {
        return uploadid;
    }

    public void setUploadid(Long uploadid) {
        this.uploadid = uploadid;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(int partNumber) {
        this.partNumber = partNumber;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getStoredPath() {
        return storedPath;
    }

    public void setStoredPath(String storedPath) {
        this.storedPath = storedPath;
    }

    public Long getStoredSize() {
        return storedSize;
    }

    public void setStoredSize(Long storedSize) {
        this.storedSize = storedSize;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Long getId() {
        return id;
    }
    
    

}
