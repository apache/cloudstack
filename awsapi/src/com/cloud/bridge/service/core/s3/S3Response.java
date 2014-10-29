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

public class S3Response {
    protected int resultCode;
    protected String version;
    protected String resultDescription;

    public S3Response() {
    }

    public S3Response(int code, String description) {
        resultCode = code;
        resultDescription = description;
    }

    public int getResultCode() {
        return resultCode;
    }

    public void setResultCode(int code) {
        resultCode = code;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getResultDescription() {
        return resultDescription;
    }

    public void setResultDescription(String description) {
        resultDescription = description;
    }
}
