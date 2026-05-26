/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.api.command.admin.user;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class UpdateUserCmdTest {
    @InjectMocks
    private UpdateUserCmd cmd;

    @Test
    public void testGetApiResourceId() {
        Long userId = 99L;
        cmd.setId(userId);
        Assert.assertEquals(userId, cmd.getApiResourceId());
    }

    @Test
    public void testGetApiResourceType() {
        Assert.assertEquals(ApiCommandResourceType.User, cmd.getApiResourceType());
    }

    @Test
    public void testIsPasswordChangeRequired_True() {
        ReflectionTestUtils.setField(cmd, "passwordChangeRequired", Boolean.TRUE);
        Assert.assertTrue(cmd.isPasswordChangeRequired());
    }

    @Test
    public void testIsPasswordChangeRequired_False() {
        ReflectionTestUtils.setField(cmd, "passwordChangeRequired", Boolean.FALSE);
        Assert.assertFalse(cmd.isPasswordChangeRequired());
    }

    @Test
    public void testIsPasswordChangeRequired_Null() {
        ReflectionTestUtils.setField(cmd, "passwordChangeRequired", null);
        Assert.assertFalse(cmd.isPasswordChangeRequired());
    }
}
