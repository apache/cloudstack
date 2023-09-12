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
package org.apache.cloudstack.api.command.user.snapshot;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class CreateSnapshotPolicyCmdTest {
    @Test
    public void testParsingTags() {
        final CreateSnapshotPolicyCmd createSnapshotPolicyCmd = new CreateSnapshotPolicyCmd();
        final Map<String, String> tag1 = new HashMap<>();
        tag1.put("key", "key1");
        tag1.put("value", "value1");
        final Map<String, String> tag2 = new HashMap<>();
        tag2.put("key", "key2");
        tag2.put("value", "value2");
        final Map<String, String> expectedTags = new HashMap<>();
        expectedTags.put("key1", "value1");
        expectedTags.put("key2", "value2");

        final Map<String, Map<String, String>> tagsParams = new HashMap<>();
        tagsParams.put("0", tag1);
        tagsParams.put("1", tag2);
        ReflectionTestUtils.setField(createSnapshotPolicyCmd, "tags", tagsParams);
        Assert.assertEquals(createSnapshotPolicyCmd.getTags(), expectedTags);
    }
}
