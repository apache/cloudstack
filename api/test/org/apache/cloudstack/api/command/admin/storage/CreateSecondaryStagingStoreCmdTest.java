package org.apache.cloudstack.api.command.admin.storage;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.junit.Test;

public class CreateSecondaryStagingStoreCmdTest {

    static void set(BaseCmd cmd, String fieldName, Object value)
            throws IllegalArgumentException, IllegalAccessException {
        for (Field field : cmd.getClass().getDeclaredFields()) {
            Parameter parameter = field.getAnnotation(Parameter.class);
            if (parameter != null && fieldName.equals(parameter.name())) {
                field.setAccessible(true);
                field.set(cmd, value);
            }
        }
    }

    @Test
    public void getDetails() throws IllegalArgumentException,
            IllegalAccessException {
        CreateSecondaryStagingStoreCmd cmd = new CreateSecondaryStagingStoreCmd();
        HashMap<String, Map<String, String>> details = new HashMap<String, Map<String, String>>();
        HashMap<String, String> kv = new HashMap<String, String>();
        kv.put("key", "TEST-KEY");
        kv.put("value", "TEST-VALUE");
        details.put("does not matter", kv);
        set(cmd, "details", details);
        Map<String, String> detailsMap = cmd.getDetails();
        Assert.assertNotNull(detailsMap);
        Assert.assertEquals(1, detailsMap.size());
        Assert.assertTrue(detailsMap.containsKey("TEST-KEY"));
        Assert.assertEquals("TEST-VALUE", detailsMap.get("TEST-KEY"));
    }

    @Test
    public void getDetailsEmpty() throws IllegalArgumentException,
            IllegalAccessException {
        CreateSecondaryStagingStoreCmd cmd = new CreateSecondaryStagingStoreCmd();
        set(cmd, "details", new HashMap<String, Map<String, String>>());
        Assert.assertNull(cmd.getDetails());
    }

    @Test
    public void getDetailsNull() throws IllegalArgumentException,
            IllegalAccessException {
        CreateSecondaryStagingStoreCmd cmd = new CreateSecondaryStagingStoreCmd();
        set(cmd, "details", null);
        Assert.assertNull(cmd.getDetails());
    }

}
