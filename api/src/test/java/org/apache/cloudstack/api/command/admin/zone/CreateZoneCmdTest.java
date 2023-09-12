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
package org.apache.cloudstack.api.command.admin.zone;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class CreateZoneCmdTest {

    @Test
    public void isEdge() {
        CreateZoneCmd createZoneCmd = new CreateZoneCmd();
        ReflectionTestUtils.setField(createZoneCmd, "isEdge", null);
        Assert.assertFalse("Null or no isedge param value for API should return false", createZoneCmd.isEdge());
        ReflectionTestUtils.setField(createZoneCmd, "isEdge", false);
        Assert.assertFalse("false value for isedge param value for API should return false", createZoneCmd.isEdge());
        ReflectionTestUtils.setField(createZoneCmd, "isEdge", true);
        Assert.assertTrue("true value for isedge param value for API should return true", createZoneCmd.isEdge());
    }
}
