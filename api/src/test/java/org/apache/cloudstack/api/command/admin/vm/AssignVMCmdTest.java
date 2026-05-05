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

package org.apache.cloudstack.api.command.admin.vm;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class AssignVMCmdTest {

    @Test
    public void test_setSkipNetwork_default() {
        AssignVMCmd assignVMCmd = new AssignVMCmd();
        Object value = ReflectionTestUtils.getField(assignVMCmd, "skipNetwork");
        Assert.assertTrue(value instanceof Boolean);
        Assert.assertFalse((Boolean) value);
    }

    @Test
    public void test_setSkipNetwork_set() {
        AssignVMCmd assignVMCmd = new AssignVMCmd();
        assignVMCmd.setSkipNetwork(true);
        Object value = ReflectionTestUtils.getField(assignVMCmd, "skipNetwork");
        Assert.assertTrue(value instanceof Boolean);
        Assert.assertTrue((Boolean) value);
    }

    @Test
    public void test_isSkipNetwork_default() {
        AssignVMCmd assignVMCmd = new AssignVMCmd();
        Assert.assertFalse(assignVMCmd.isSkipNetwork());
    }

    @Test
    public void test_isSkipNetwork_set() {
        AssignVMCmd assignVMCmd = new AssignVMCmd();
        ReflectionTestUtils.setField(assignVMCmd, "skipNetwork", true);
        Assert.assertTrue(assignVMCmd.isSkipNetwork());
    }
}
