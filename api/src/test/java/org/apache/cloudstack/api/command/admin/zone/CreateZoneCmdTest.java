package org.apache.cloudstack.api.command.admin.zone;

import java.lang.reflect.Field;

import org.junit.Assert;
import org.junit.Test;

public class CreateZoneCmdTest {

    @Test
    public void isEdge() {
        CreateZoneCmd createZoneCmd = new CreateZoneCmd();
        try {
            Field f = createZoneCmd.getClass().getDeclaredField("isEdge");
            f.setAccessible(true);
            f.set(createZoneCmd, null);
            Assert.assertFalse("Null or no isedge param value for API should return false", createZoneCmd.isEdge());
            f.set(createZoneCmd, false);
            Assert.assertFalse("false value for isedge param value for API should return false", createZoneCmd.isEdge());
            f.set(createZoneCmd, true);
            Assert.assertTrue("true value for isedge param value for API should return true", createZoneCmd.isEdge());
        } catch (NoSuchFieldException | SecurityException | IllegalAccessException e) {
            Assert.fail();
        }
    }
}