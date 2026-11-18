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
package org.apache.cloudstack.api.command.user.template;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.storage.Storage;

@RunWith(MockitoJUnitRunner.class)
public class UpdateTemplateCmdTest {

    @Test
    public void testGetTemplateType() {
        UpdateTemplateCmd cmd = new UpdateTemplateCmd();
        ReflectionTestUtils.setField(cmd, "templateType", null);
        Assert.assertNull(cmd.getTemplateType());
        String type = Storage.TemplateType.ROUTING.toString();
        ReflectionTestUtils.setField(cmd, "templateTag", type);
        Assert.assertEquals(type, cmd.getTemplateTag());
    }

    @Test
    public void testGetTemplateTag() {
        UpdateTemplateCmd cmd = new UpdateTemplateCmd();
        ReflectionTestUtils.setField(cmd, "templateTag", null);
        Assert.assertNull(cmd.getTemplateTag());
        String tag = "ABC";
        ReflectionTestUtils.setField(cmd, "templateTag", tag);
        Assert.assertEquals(tag, cmd.getTemplateTag());
    }
}
