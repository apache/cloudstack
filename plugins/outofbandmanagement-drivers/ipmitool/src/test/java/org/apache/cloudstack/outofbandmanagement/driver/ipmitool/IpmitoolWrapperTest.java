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

import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.common.collect.ImmutableMap;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagement;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RunWith(MockitoJUnitRunner.class)
public class IpmitoolWrapperTest {

    private static final ExecutorService ipmitoolExecutor = Executors.newFixedThreadPool(20, new NamedThreadFactory("IpmiToolDriverTest"));
    private static final IpmitoolWrapper IPMITOOL = new IpmitoolWrapper(ipmitoolExecutor);

    @Test
    public void testParsePowerCommandValid() {
        Assert.assertEquals(IPMITOOL.parsePowerCommand(OutOfBandManagement.PowerOperation.ON), "on");
        Assert.assertEquals(IPMITOOL.parsePowerCommand(OutOfBandManagement.PowerOperation.OFF), "off");
        Assert.assertEquals(IPMITOOL.parsePowerCommand(OutOfBandManagement.PowerOperation.CYCLE), "cycle");
        Assert.assertEquals(IPMITOOL.parsePowerCommand(OutOfBandManagement.PowerOperation.RESET), "reset");
        Assert.assertEquals(IPMITOOL.parsePowerCommand(OutOfBandManagement.PowerOperation.SOFT), "soft");
        Assert.assertEquals(IPMITOOL.parsePowerCommand(OutOfBandManagement.PowerOperation.STATUS), "status");
    }

    @Test(expected = IllegalStateException.class)
    public void testParsePowerCommandInvalid() {
        IPMITOOL.parsePowerCommand(null);
        Assert.fail("IpmitoolWrapper.parsePowerCommand failed to throw exception on invalid power state");
    }

    @Test
    public void testParsePowerState() {
        Assert.assertEquals(IPMITOOL.parsePowerState(null), OutOfBandManagement.PowerState.Unknown);
        Assert.assertEquals(IPMITOOL.parsePowerState(""), OutOfBandManagement.PowerState.Unknown);
        Assert.assertEquals(IPMITOOL.parsePowerState(" "), OutOfBandManagement.PowerState.Unknown);
        Assert.assertEquals(IPMITOOL.parsePowerState("invalid data"), OutOfBandManagement.PowerState.Unknown);
        Assert.assertEquals(IPMITOOL.parsePowerState("Chassis Power is on"), OutOfBandManagement.PowerState.On);
        Assert.assertEquals(IPMITOOL.parsePowerState("Chassis Power is off"), OutOfBandManagement.PowerState.Off);
    }

    @Test
    public void testGetIpmiToolCommandArgs() {
        List<String> args = IPMITOOL.getIpmiToolCommandArgs("binpath", "intf", "1", null);
        assert args != null;
        Assert.assertEquals(args.size(), 6);
        Assert.assertArrayEquals(args.toArray(), new String[]{"binpath", "-I", "intf", "-R", "1", "-v"});

        ImmutableMap.Builder<OutOfBandManagement.Option, String> argMap = new ImmutableMap.Builder<>();
        argMap.put(OutOfBandManagement.Option.DRIVER, "ipmitool");
        argMap.put(OutOfBandManagement.Option.ADDRESS, "127.0.0.1");
        List<String> argsWithOpts = IPMITOOL.getIpmiToolCommandArgs("binpath", "intf", "1", argMap.build(), "user", "list");
        assert argsWithOpts != null;
        Assert.assertEquals(argsWithOpts.size(), 10);
        Assert.assertArrayEquals(argsWithOpts.toArray(), new String[]{"binpath", "-I", "intf", "-R", "1", "-v", "-H", "127.0.0.1", "user", "list"});
    }

    @Test(expected = CloudRuntimeException.class)
    public void testFindIpmiUserInvalid() {
        IPMITOOL.findIpmiUser("some invalid string\n", "admin");
        Assert.fail("IpmitoolWrapper.findIpmiUser failed to throw exception on invalid data");

        Assert.assertEquals(IPMITOOL.findIpmiUser("some\ninvalid\ndata\n", "admin"), null);
    }

    @Test
    public void testFindIpmiUserNull() {
        Assert.assertEquals(IPMITOOL.findIpmiUser("some\ninvalid\ndata\n", "admin"), null);
    }

    @Test
    public void testFindIpmiUserValid() {
        String usersList = "ID  Name\t     Callin  Link Auth\tIPMI Msg   Channel Priv Limit\n" +
                           "1   admin            true    true       true       ADMINISTRATOR\n" +
                           "2   operator         true    false      false      OPERATOR\n" +
                           "3   user             true    true       true       USER\n";
        Assert.assertEquals(IPMITOOL.findIpmiUser(usersList, "admin"), "1");
        Assert.assertEquals(IPMITOOL.findIpmiUser(usersList, "operator"), "2");
        Assert.assertEquals(IPMITOOL.findIpmiUser(usersList, "user"), "3");
    }
}
