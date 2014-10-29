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
package com.cloud.bridge.service.core.s3;

public class S3CreateBucketRequest extends S3Request {
    protected String bucketName;
    protected S3CreateBucketConfiguration config;
    protected String cannedAccessPolicy;    // -> REST only sets an acl with a simple keyword
    protected S3AccessControlList acl;

    public S3CreateBucketRequest() {
        super();
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public S3CreateBucketConfiguration getConfig() {
        return config;
    }

    public void setConfig(S3CreateBucketConfiguration config) {
        this.config = config;
    }

    public String getCannedAccess() {
        return cannedAccessPolicy;
    }

    public void setCannedAccess(String cannedAccessPolicy) {
        this.cannedAccessPolicy = cannedAccessPolicy;
    }

    public S3AccessControlList getAcl() {
        return acl;
    }

    public void setAcl(S3AccessControlList acl) {
        this.acl = acl;
    }
}
