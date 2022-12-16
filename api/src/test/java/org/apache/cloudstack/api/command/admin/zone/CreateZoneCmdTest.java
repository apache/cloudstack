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