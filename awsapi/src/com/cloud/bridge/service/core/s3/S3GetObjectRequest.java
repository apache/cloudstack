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

public class S3GetObjectRequest extends S3Request {
    private String bucketName;
    private String key;
    private String version;
    private boolean returnMetadata;
    private boolean returnData;
    private boolean inlineData;
    private long byteRangeStart = -1;
    private long byteRangeEnd = -1;
    private boolean returnCompleteObjectOnConditionFailure;
    private S3ConditionalHeaders conds;

    public S3GetObjectRequest() {
        super();
        version = null;
        returnCompleteObjectOnConditionFailure = false;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isReturnMetadata() {
        return returnMetadata;
    }

    public void setReturnMetadata(boolean returnMetadata) {
        this.returnMetadata = returnMetadata;
    }

    public boolean isReturnData() {
        return returnData;
    }

    public void setReturnData(boolean returnData) {
        this.returnData = returnData;
    }

    public boolean isInlineData() {
        return inlineData;
    }

    public void setInlineData(boolean inlineData) {
        this.inlineData = inlineData;
    }

    public long getByteRangeStart() {
        return byteRangeStart;
    }

    public void setByteRangeStart(long byteRangeStart) {
        this.byteRangeStart = byteRangeStart;
    }

    public long getByteRangeEnd() {
        return byteRangeEnd;
    }

    public void setByteRangeEnd(long byteRangeEnd) {
        this.byteRangeEnd = byteRangeEnd;
    }

    public boolean isReturnCompleteObjectOnConditionFailure() {
        return returnCompleteObjectOnConditionFailure;
    }

    public void setReturnCompleteObjectOnConditionFailure(boolean returnCompleteObjectOnConditionFailure) {
        this.returnCompleteObjectOnConditionFailure = returnCompleteObjectOnConditionFailure;
    }

    public void setConditions(S3ConditionalHeaders conds) {
        this.conds = conds;
    }

    public S3ConditionalHeaders getConditions() {
        return conds;
    }
}
