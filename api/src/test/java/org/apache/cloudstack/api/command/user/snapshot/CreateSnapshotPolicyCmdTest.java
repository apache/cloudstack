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