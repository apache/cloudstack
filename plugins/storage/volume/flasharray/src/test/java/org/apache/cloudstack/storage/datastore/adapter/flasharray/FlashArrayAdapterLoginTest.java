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
package org.apache.cloudstack.storage.datastore.adapter.flasharray;

import static org.mockito.Mockito.mock;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class FlashArrayAdapterLoginTest {

    private final FlashArrayAdapter adapter = mock(FlashArrayAdapter.class, Mockito.CALLS_REAL_METHODS);

    @Test
    public void wrongPasswordSaysToCheckCredentials() {
        String msg = adapter.legacyLoginFailureMessage(400, "{\"msg\": \"invalid credentials\"}");

        Assert.assertTrue(msg.contains("check the username and password"));
        Assert.assertTrue(msg.contains("HTTP 400"));
        Assert.assertTrue(msg.contains("invalid credentials"));
    }

    @Test
    public void missingArrayMessageStillReadsCleanly() {
        String msg = adapter.legacyLoginFailureMessage(401, null);

        Assert.assertTrue(msg.contains("check the username and password"));
        Assert.assertTrue(msg.endsWith("(HTTP 401)"));
    }
}
