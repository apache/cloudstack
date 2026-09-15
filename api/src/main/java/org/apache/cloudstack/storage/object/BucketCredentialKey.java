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
 * One key slot of a {@link BucketCredential}. A credential holds up to two
 * slots that rotate independently, so consumers can migrate to a fresh key
 * in one slot while the other keeps serving.
 */
public interface BucketCredentialKey extends Identity, InternalIdentity {

    int KEY_SLOT_ONE = 1;
    int KEY_SLOT_TWO = 2;

    long getBucketCredentialId();

    int getKeySlot();

    String getAccessKey();

    String getSecretKey();

    State getState();

    Date getCreated();

    Date getLastUsed();

    enum State {
        Active, Revoked;
        @Override
        public String toString() {
            return this.name();
        }
    }
}
