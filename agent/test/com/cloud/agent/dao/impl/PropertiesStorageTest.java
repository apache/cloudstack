package com.cloud.agent.dao.impl;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class PropertiesStorageTest {
    @Test
    public void configureWithNotExistingFile() {
        String fileName = "target/notyetexistingfile"
                + System.currentTimeMillis();
        File file = new File(fileName);

        PropertiesStorage storage = new PropertiesStorage();
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("path", fileName);
        Assert.assertTrue(storage.configure("test", params));
        Assert.assertTrue(file.exists());
        storage.persist("foo", "bar");
        Assert.assertEquals("bar", storage.get("foo"));

        storage.stop();
        file.delete();
    }

    @Test
    public void configureWithExistingFile() throws IOException {
        String fileName = "target/existingfile"
                + System.currentTimeMillis();
        File file = new File(fileName);

        FileUtils.writeStringToFile(file, "a=b\n\n");

        PropertiesStorage storage = new PropertiesStorage();
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("path", fileName);
        Assert.assertTrue(storage.configure("test", params));
        Assert.assertEquals("b", storage.get("a"));
        Assert.assertTrue(file.exists());
        storage.persist("foo", "bar");
        Assert.assertEquals("bar", storage.get("foo"));

        storage.stop();
        file.delete();
    }
}
