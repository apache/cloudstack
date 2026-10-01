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

public interface ObjectStore extends Identity, InternalIdentity {

    /**
     * Prefix of the account details that hold the credentials CloudStack keeps for an account on
     * an object store, one set per store. They are CloudStack's own credentials for the backend
     * rather than the account's, so they are never returned in API responses.
     */
    String ACCOUNT_DETAIL_PREFIX = "objectstore-";

    /** Prefix shared by the account details of one object store. */
    static String accountDetailPrefix(long storeId) {
        return ACCOUNT_DETAIL_PREFIX + storeId + "-";
    }

    /** Key of an account detail holding an object store credential for one store. */
    static String accountDetailKey(long storeId, String name) {
        return accountDetailPrefix(storeId) + name;
    }

    /** Whether an account detail key holds an object store credential CloudStack keeps internally. */
    static boolean isInternalAccountDetail(String key) {
        return key != null && key.startsWith(ACCOUNT_DETAIL_PREFIX);
    }

    /**
     * @return name of the object store.
     */
    String getName();

    /**
     * @return object store provider name
     */
    String getProviderName();

    /**
     *
     * @return uri
     */
    String getUrl();

}
