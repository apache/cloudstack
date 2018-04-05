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
        String fileName = "target/notyetexistingfile" + System.currentTimeMillis();
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
        String fileName = "target/existingfile" + System.currentTimeMillis();
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
