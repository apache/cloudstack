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

package org.apache.cloudstack.outofbandmanagement;

import com.cloud.utils.exception.CloudRuntimeException;
import com.google.common.collect.ImmutableMap;
import org.apache.cloudstack.outofbandmanagement.driver.OutOfBandManagementDriverResponse;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OutOfBandManagementServiceTest {

    OutOfBandManagementServiceImpl oobmService = new OutOfBandManagementServiceImpl();

    @Test
    public void testOutOfBandManagementDriverResponseEvent() {
        OutOfBandManagementDriverResponse r = new OutOfBandManagementDriverResponse("some result", "some error", false);

        r.setSuccess(false);
        r.setAuthFailure(false);
        Assert.assertEquals(r.toEvent(), OutOfBandManagement.PowerState.Event.Unknown);

        r.setSuccess(false);
        r.setAuthFailure(true);
        Assert.assertEquals(r.toEvent(), OutOfBandManagement.PowerState.Event.AuthError);

        r.setAuthFailure(false);
        r.setSuccess(true);
        r.setPowerState(OutOfBandManagement.PowerState.On);
        Assert.assertEquals(r.toEvent(), OutOfBandManagement.PowerState.Event.On);

        r.setPowerState(OutOfBandManagement.PowerState.Off);
        Assert.assertEquals(r.toEvent(), OutOfBandManagement.PowerState.Event.Off);

        r.setPowerState(OutOfBandManagement.PowerState.Disabled);
        Assert.assertEquals(r.toEvent(), OutOfBandManagement.PowerState.Event.Disabled);
    }

    private ImmutableMap<OutOfBandManagement.Option, String> buildRandomOptionsMap() {
        ImmutableMap.Builder<OutOfBandManagement.Option, String> builder = new ImmutableMap.Builder<>();
        builder.put(OutOfBandManagement.Option.ADDRESS, "localhost");
        builder.put(OutOfBandManagement.Option.DRIVER, "ipmitool");
        return builder.build();
    }

    @Test
    public void testUpdateOutOfBandManagementConfigValid() {
        OutOfBandManagement config = new OutOfBandManagementVO(123L);
        Assert.assertEquals(config.getPowerState(), OutOfBandManagement.PowerState.Disabled);
        config = oobmService.updateConfig(config, buildRandomOptionsMap());
        Assert.assertEquals(config.getAddress(), "localhost");
        Assert.assertEquals(config.getDriver(), "ipmitool");
        Assert.assertEquals(config.getPowerState(), OutOfBandManagement.PowerState.Disabled);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testUpdateOutOfBandManagementNullConfigValidOptions() {
        oobmService.updateConfig(null, buildRandomOptionsMap());
        Assert.fail("CloudRuntimeException was expect for out-of-band management not configured for the host");
    }

    @Test(expected = CloudRuntimeException.class)
    public void testUpdateOutOfBandManagementNullConfigNullOptions() {
        oobmService.updateConfig(null, null);
        Assert.fail("CloudRuntimeException was expect for out-of-band management not configured for the host");
    }

    @Test
    public void testUpdateOutOfBandManagementValidConfigValidOptions() {
        OutOfBandManagement config = new OutOfBandManagementVO(123L);
        config.setAddress(null);
        config = oobmService.updateConfig(config, null);
        Assert.assertEquals(config.getAddress(), null);
        Assert.assertEquals(config.getPowerState(), OutOfBandManagement.PowerState.Disabled);
    }

    @Test
    public void testGetOutOfBandManagementOptionsValid() {
        OutOfBandManagement configEmpty = new OutOfBandManagementVO(123L);
        ImmutableMap<OutOfBandManagement.Option, String> optionsEmpty = oobmService.getOptions(configEmpty);
        Assert.assertEquals(optionsEmpty.size(), 0);

        OutOfBandManagement config = new OutOfBandManagementVO(123L);
        config.setAddress("localhost");
        config.setDriver("ipmitool");
        config.setPort("1234");
        ImmutableMap<OutOfBandManagement.Option, String> options = oobmService.getOptions(config);
        Assert.assertEquals(options.get(OutOfBandManagement.Option.ADDRESS), "localhost");
        Assert.assertEquals(options.get(OutOfBandManagement.Option.DRIVER), "ipmitool");
        Assert.assertEquals(options.get(OutOfBandManagement.Option.PORT), "1234");
    }

    @Test(expected = CloudRuntimeException.class)
    public void testGetOutOfBandManagementOptionsInvalid() {
        oobmService.getOptions(null);
        Assert.fail("CloudRuntimeException was expected for finding options of host with out-of-band management configuration");
    }
}
