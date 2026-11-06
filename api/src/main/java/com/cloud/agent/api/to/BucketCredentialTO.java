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

import java.util.List;

/**
 * A dedicated backend identity provisioned for a single bucket by an object
 * store provider, together with the key pairs it currently holds.
 */
public final class BucketCredentialTO {

    private final String providerCredentialId;

    private final List<BucketKeyTO> keys;

    public BucketCredentialTO(String providerCredentialId, List<BucketKeyTO> keys) {
        this.providerCredentialId = providerCredentialId;
        this.keys = keys;
    }

    public String getProviderCredentialId() {
        return providerCredentialId;
    }

    public List<BucketKeyTO> getKeys() {
        return keys;
    }
}
