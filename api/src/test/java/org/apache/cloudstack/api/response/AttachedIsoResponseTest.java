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
package org.apache.cloudstack.api.response;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public final class AttachedIsoResponseTest {

    @Test
    public void testFullConstructorPopulatesAllFields() {
        AttachedIsoResponse response = new AttachedIsoResponse("uuid-1", "alpine-iso", "Alpine boot", 3);
        Assert.assertEquals("uuid-1", response.getId());
        Assert.assertEquals("alpine-iso", response.getName());
        Assert.assertEquals("Alpine boot", response.getDisplayText());
        Assert.assertEquals(Integer.valueOf(3), response.getDeviceSeq());
    }

    @Test
    public void testNoArgConstructorLeavesFieldsNull() {
        AttachedIsoResponse response = new AttachedIsoResponse();
        Assert.assertNull(response.getId());
        Assert.assertNull(response.getName());
        Assert.assertNull(response.getDisplayText());
        Assert.assertNull(response.getDeviceSeq());
    }
}
