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

import com.cloud.agent.api.to.DataStoreTO;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ListDataStoreObjectsCommandTest {

    @Test
    public void testStartIndex() {
        DataStoreTO dataStore = Mockito.mock(DataStoreTO.class);
        ListDataStoreObjectsCommand cmd = new ListDataStoreObjectsCommand(dataStore, "path", 40, 10);
        assertEquals(40, cmd.getStartIndex());
        assertEquals(10, cmd.getPageSize());
        assertEquals("path", cmd.getPath());
        assertEquals(dataStore, cmd.getStore());
        assertFalse(cmd.executeInSequence());
    }

}
