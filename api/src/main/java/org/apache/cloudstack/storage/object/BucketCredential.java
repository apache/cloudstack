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
package org.apache.cloudstack.storage.object;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import java.util.Date;

/**
 * A dedicated backend identity owned by a single bucket. A bucket without a
 * BucketCredential uses the legacy account-scoped credential of its owner.
 */
public interface BucketCredential extends Identity, InternalIdentity {

    /** Credential scope of a bucket that has its own credential. */
    String SCOPE_BUCKET = "bucket";
    /** Credential scope of a bucket still served by the account's credential. */
    String SCOPE_ACCOUNT = "account";

    long getBucketId();

    String getProviderCredentialId();

    State getState();

    Date getCreated();

    enum State {
        Active, Removed;
        @Override
        public String toString() {
            return this.name();
        }
    }
}
