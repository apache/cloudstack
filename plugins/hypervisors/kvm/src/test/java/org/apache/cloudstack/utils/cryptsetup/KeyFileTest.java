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
package org.apache.cloudstack.utils.cryptsetup;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RunWith(MockitoJUnitRunner.class)
public class KeyFileTest {

    @Test
    public void keyFileTest() throws IOException {
        byte[] contents = "the quick brown fox".getBytes();
        KeyFile keyFile = new KeyFile(contents);
        System.out.printf("New test KeyFile at %s%n", keyFile);
        Path path = keyFile.getPath();

        Assert.assertTrue(keyFile.isSet());

        // check contents
        byte[] fileContents = Files.readAllBytes(path);
        Assert.assertArrayEquals(contents, fileContents);

        // delete file on close
        keyFile.close();

        Assert.assertFalse("key file was not cleaned up", Files.exists(path));
        Assert.assertFalse("key file is still set", keyFile.isSet());
    }
}
