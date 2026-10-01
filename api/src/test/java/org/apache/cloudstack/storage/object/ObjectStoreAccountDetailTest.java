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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ObjectStoreAccountDetailTest {

    @Test
    public void testAccountDetailKeysAreScopedPerStore() {
        assertEquals("objectstore-7-root-accesskey", ObjectStore.accountDetailKey(7L, "root-accesskey"));
        assertEquals("objectstore-7-", ObjectStore.accountDetailPrefix(7L));
        assertFalse(ObjectStore.accountDetailKey(7L, "root-accesskey").equals(ObjectStore.accountDetailKey(8L, "root-accesskey")));
    }

    @Test
    public void testInternalDetailsAreRecognised() {
        assertTrue(ObjectStore.isInternalAccountDetail(ObjectStore.accountDetailKey(1L, "root-secretkey")));
        assertTrue(ObjectStore.isInternalAccountDetail(ObjectStore.accountDetailKey(1L, "account-id")));
    }

    @Test
    public void testUnrelatedDetailsAreLeftAlone() {
        // pre-existing account details, including the provider's own legacy rows, are not ours to hide
        assertFalse(ObjectStore.isInternalAccountDetail("ceph-rgw-accesskey"));
        assertFalse(ObjectStore.isInternalAccountDetail("ceph-rgw-secretkey"));
        assertFalse(ObjectStore.isInternalAccountDetail("some.account.setting"));
        assertFalse(ObjectStore.isInternalAccountDetail(null));
    }
}
