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

package org.apache.cloudstack.storage.command.browser;


import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ListDataStoreObjectsAnswerTest {

    @Test
    public void testGetters() {
        ListDataStoreObjectsAnswer answer = new ListDataStoreObjectsAnswer(true, 2,
                Arrays.asList("file1", "file2"), Arrays.asList("path1", "path2"),
                Arrays.asList("/mnt/datastore/path1", "/mnt/datastore/path2"), Arrays.asList(false, false),
                Arrays.asList(1024L, 2048L), Arrays.asList(123456789L, 987654321L));

        assertTrue(answer.isPathExists());
        assertEquals(2, answer.getCount());
        assertEquals(Arrays.asList("file1", "file2"), answer.getNames());
        assertEquals(Arrays.asList("path1", "path2"), answer.getPaths());
        assertEquals(Arrays.asList("/mnt/datastore/path1", "/mnt/datastore/path2"), answer.getAbsPaths());
        assertEquals(Arrays.asList(false, false), answer.getIsDirs());
        assertEquals(Arrays.asList(1024L, 2048L), answer.getSizes());
        assertEquals(Arrays.asList(123456789L, 987654321L), answer.getLastModified());
    }

    @Test
    public void testEmptyLists() {
        ListDataStoreObjectsAnswer answer = new ListDataStoreObjectsAnswer(true, 0, null, null, null, null, null, null);

        assertTrue(answer.isPathExists());
        assertEquals(0, answer.getCount());
        assertEquals(Collections.emptyList(), answer.getNames());
        assertEquals(Collections.emptyList(), answer.getPaths());
        assertEquals(Collections.emptyList(), answer.getAbsPaths());
        assertEquals(Collections.emptyList(), answer.getIsDirs());
        assertEquals(Collections.emptyList(), answer.getSizes());
        assertEquals(Collections.emptyList(), answer.getLastModified());
    }
}
