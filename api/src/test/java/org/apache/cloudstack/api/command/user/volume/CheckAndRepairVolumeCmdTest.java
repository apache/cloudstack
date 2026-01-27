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

package org.apache.cloudstack.api.command.user.volume;

import com.cloud.exception.InvalidParameterValueException;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class CheckAndRepairVolumeCmdTest extends TestCase {
    private CheckAndRepairVolumeCmd checkAndRepairVolumeCmd;
    private AutoCloseable closeable;

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        checkAndRepairVolumeCmd = new CheckAndRepairVolumeCmd();
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testGetRepair() {
        ReflectionTestUtils.setField(checkAndRepairVolumeCmd, "repair", "all");
        assertEquals("all", checkAndRepairVolumeCmd.getRepair());

        ReflectionTestUtils.setField(checkAndRepairVolumeCmd, "repair", "LEAKS");
        assertEquals("leaks", checkAndRepairVolumeCmd.getRepair());

        ReflectionTestUtils.setField(checkAndRepairVolumeCmd, "repair", null);
        assertNull(checkAndRepairVolumeCmd.getRepair());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetRepairInvalid() {
        ReflectionTestUtils.setField(checkAndRepairVolumeCmd, "repair", "RANDOM STRING");
        checkAndRepairVolumeCmd.getRepair();
    }
}
