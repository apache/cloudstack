//
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
//

package org.apache.cloudstack.outofbandmanagement.driver.nestedcloudstack;

import com.cloud.utils.exception.CloudRuntimeException;
import com.google.common.collect.ImmutableMap;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagement;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

@RunWith(MockitoJUnitRunner.class)
public class NestedCloudStackOutOfBandManagementDriverTest {
    private NestedCloudStackOutOfBandManagementDriver driver = new NestedCloudStackOutOfBandManagementDriver();

    @Test
    public void testEnsureOptionExists() throws IOException {
        final ImmutableMap.Builder<OutOfBandManagement.Option, String> builder = ImmutableMap.builder();
        builder.put(OutOfBandManagement.Option.ADDRESS, "http://some.cloud/client/api");
        final ImmutableMap<OutOfBandManagement.Option, String> options = builder.build();
        driver.ensureOptionExists(options, OutOfBandManagement.Option.ADDRESS);

        boolean caughtException = false;
        try {
            driver.ensureOptionExists(options, OutOfBandManagement.Option.PORT);
        } catch (CloudRuntimeException e) {
            caughtException = true;
        }
        Assert.assertTrue(caughtException);
    }

    @Test
    public void testIsVMRunningTrue() throws IOException {
        String json = "{\"listvirtualmachinesresponse\":{\"count\":1,\"virtualmachine\":[{\"id\":\"38fa7380-9543-486a-b083-190ecf726ba4\",\"name\":\"test-vm\",\"displayname\":\"test-vm\",\"account\":\"admin\",\"userid\":\"78ed9ce8-f3ee-11e4-91ab-00012e4fde1c\",\"username\":\"admin\",\"domainid\":\"53601d4b-f3ee-11e4-91ab-00012e4fde1c\",\"domain\":\"ROOT\",\"created\":\"2017-04-04T19:50:56+0200\",\"state\":\"Running\"}]}}";
        Assert.assertEquals(driver.getNestedVMPowerState(json), OutOfBandManagement.PowerState.On);
    }

    @Test
    public void testIsVMRunningFalse() throws IOException {
        String json = "{\"listvirtualmachinesresponse\":{\"count\":1,\"virtualmachine\":[{\"id\":\"38fa7380-9543-486a-b083-190ecf726ba4\",\"name\":\"test-vm\",\"displayname\":\"test-vm\",\"account\":\"admin\",\"userid\":\"78ed9ce8-f3ee-11e4-91ab-00012e4fde1c\",\"username\":\"admin\",\"domainid\":\"53601d4b-f3ee-11e4-91ab-00012e4fde1c\",\"domain\":\"ROOT\",\"created\":\"2017-04-04T19:50:56+0200\",\"state\":\"Stopped\"}]}}";
        Assert.assertEquals(driver.getNestedVMPowerState(json), OutOfBandManagement.PowerState.Off);
    }

    @Test
    public void testIsVMRunningInvalidJson() throws IOException {
        String json = "{\"listvirtualmachinesresponse\":{\"count\":1,\"virtualmachine\"83-190ecf726ba4\",\"name";
        Assert.assertEquals(driver.getNestedVMPowerState(json), OutOfBandManagement.PowerState.Unknown);
    }

    @Test
    public void testIsVMRunningEmptyJson() throws IOException {
        String json = "{}";
        Assert.assertEquals(driver.getNestedVMPowerState(json), OutOfBandManagement.PowerState.Unknown);
    }
}
