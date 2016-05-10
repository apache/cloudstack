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

package org.apache.cloudstack.outofbandmanagement.driver.ipmitool;

import com.cloud.utils.exception.CloudRuntimeException;
import com.google.common.collect.ImmutableMap;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagement;
import org.apache.cloudstack.outofbandmanagement.driver.OutOfBandManagementDriverResponse;
import org.joda.time.Duration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class IpmitoolWrapperTest {
    @Test
    public void testParsePowerCommandValid() {
        Assert.assertEquals(IpmitoolWrapper.parsePowerCommand(OutOfBandManagement.PowerOperation.ON), "on");
        Assert.assertEquals(IpmitoolWrapper.parsePowerCommand(OutOfBandManagement.PowerOperation.OFF), "off");
        Assert.assertEquals(IpmitoolWrapper.parsePowerCommand(OutOfBandManagement.PowerOperation.CYCLE), "cycle");
        Assert.assertEquals(IpmitoolWrapper.parsePowerCommand(OutOfBandManagement.PowerOperation.RESET), "reset");
        Assert.assertEquals(IpmitoolWrapper.parsePowerCommand(OutOfBandManagement.PowerOperation.SOFT), "soft");
        Assert.assertEquals(IpmitoolWrapper.parsePowerCommand(OutOfBandManagement.PowerOperation.STATUS), "status");
    }

    @Test
    public void testParsePowerCommandInvalid() {
        try {
            IpmitoolWrapper.parsePowerCommand(null);
            Assert.fail("IpmitoolWrapper.parsePowerCommand failed to throw exception on invalid power state");
        } catch (IllegalStateException e) {
        }
    }

    @Test
    public void testParsePowerState() {
        Assert.assertEquals(IpmitoolWrapper.parsePowerState(null), OutOfBandManagement.PowerState.Unknown);
        Assert.assertEquals(IpmitoolWrapper.parsePowerState(""), OutOfBandManagement.PowerState.Unknown);
        Assert.assertEquals(IpmitoolWrapper.parsePowerState(" "), OutOfBandManagement.PowerState.Unknown);
        Assert.assertEquals(IpmitoolWrapper.parsePowerState("invalid data"), OutOfBandManagement.PowerState.Unknown);
        Assert.assertEquals(IpmitoolWrapper.parsePowerState("Chassis Power is on"), OutOfBandManagement.PowerState.On);
        Assert.assertEquals(IpmitoolWrapper.parsePowerState("Chassis Power is off"), OutOfBandManagement.PowerState.Off);
    }

    @Test
    public void testGetIpmiToolCommandArgs() {
        List<String> args = IpmitoolWrapper.getIpmiToolCommandArgs("binpath", "intf", "1", null);
        assert args != null;
        Assert.assertEquals(args.size(), 6);
        Assert.assertArrayEquals(args.toArray(), new String[]{"binpath", "-I", "intf", "-R", "1", "-v"});

        ImmutableMap.Builder<OutOfBandManagement.Option, String> argMap = new ImmutableMap.Builder<>();
        argMap.put(OutOfBandManagement.Option.DRIVER, "ipmitool");
        argMap.put(OutOfBandManagement.Option.ADDRESS, "127.0.0.1");
        List<String> argsWithOpts = IpmitoolWrapper.getIpmiToolCommandArgs("binpath", "intf", "1", argMap.build(), "user", "list");
        assert argsWithOpts != null;
        Assert.assertEquals(argsWithOpts.size(), 10);
        Assert.assertArrayEquals(argsWithOpts.toArray(), new String[]{"binpath", "-I", "intf", "-R", "1", "-v", "-H", "127.0.0.1", "user", "list"});
    }

    @Test
    public void testFindIpmiUser() {
        // Invalid data
        try {
            IpmitoolWrapper.findIpmiUser("some invalid string\n", "admin");
            Assert.fail("IpmitoolWrapper.findIpmiUser failed to throw exception on invalid data");
        } catch (CloudRuntimeException ignore) {
        }
        Assert.assertEquals(IpmitoolWrapper.findIpmiUser("some\ninvalid\ndata\n", "admin"), null);

        // Valid data
        String usersList = "ID  Name\t     Callin  Link Auth\tIPMI Msg   Channel Priv Limit\n" +
                           "1   admin            true    true       true       ADMINISTRATOR\n" +
                           "2   operator         true    false      false      OPERATOR\n" +
                           "3   user             true    true       true       USER\n";
        Assert.assertEquals(IpmitoolWrapper.findIpmiUser(usersList, "admin"), "1");
        Assert.assertEquals(IpmitoolWrapper.findIpmiUser(usersList, "operator"), "2");
        Assert.assertEquals(IpmitoolWrapper.findIpmiUser(usersList, "user"), "3");
    }

    @Test
    public void testExecuteCommands() {
        OutOfBandManagementDriverResponse r = IpmitoolWrapper.executeCommands(Arrays.asList("ls", "/tmp"), Duration.ZERO);
        Assert.assertTrue(r.isSuccess());
        Assert.assertTrue(r.getResult().length() > 0);
    }
}