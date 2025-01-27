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

import org.apache.cloudstack.storage.object.Bucket;

public final class BucketTO {

    private String name;

    private String accessKey;

    private String secretKey;

    public BucketTO(Bucket bucket) {
        this.name = bucket.getName();
        this.accessKey = bucket.getAccessKey();
        this.secretKey = bucket.getSecretKey();
    }

    public BucketTO(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public String getAccessKey() {
        return this.accessKey;
    }

    public String getSecretKey() {
        return this.secretKey;
    }
}
