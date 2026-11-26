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
package org.apache.cloudstack.api.command.admin.account;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class UpdateAccountCmdTest {

    @Test
    public void testGetDetailsEmpty() {
        final UpdateAccountCmd cmd = new UpdateAccountCmd();
        Assert.assertNull(cmd.getDetails());
        ReflectionTestUtils.setField(cmd, "details", new HashMap<>());
        Assert.assertNull(cmd.getDetails());
    }

    @Test
    public void testGetDetailsReturnsFirstEntry() {
        final UpdateAccountCmd cmd = new UpdateAccountCmd();
        Map<String, String> entry = Map.of("somekey", "somevalue");
        Map<Integer, Map<String, String>> details = new HashMap<>();
        details.put(0, entry);
        ReflectionTestUtils.setField(cmd, "details", details);
        Assert.assertEquals(entry, cmd.getDetails());
    }
}
