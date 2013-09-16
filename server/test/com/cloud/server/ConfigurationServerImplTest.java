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
package com.cloud.server;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class ConfigurationServerImplTest {
    final static String TEST = "the quick brown fox jumped over the lazy dog";

    @Test(expected = IOException.class)
    public void testGetBase64KeystoreNoSuchFile() throws IOException {
        ConfigurationServerImpl.getBase64Keystore("notexisting" + System.currentTimeMillis());
    }

    @Test(expected = IOException.class)
    public void testGetBase64KeystoreTooBigFile() throws IOException {
        File temp = File.createTempFile("keystore", "");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            builder.append("way too long...\n");
        }
        FileUtils.writeStringToFile(temp, builder.toString());
        try {
            ConfigurationServerImpl.getBase64Keystore(temp.getPath());
        } finally {
            temp.delete();
        }
    }

    @Test
    public void testGetBase64Keystore() throws IOException {
        File temp = File.createTempFile("keystore", "");
        try {
            FileUtils.writeStringToFile(temp, Base64.encodeBase64String(TEST.getBytes()));
            final String keystore = ConfigurationServerImpl.getBase64Keystore(temp.getPath());
            // let's decode it to make sure it makes sense
            Base64.decodeBase64(keystore);
        } finally {
            temp.delete();
        }
    }
}
