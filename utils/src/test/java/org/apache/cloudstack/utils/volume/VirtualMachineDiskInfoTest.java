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

package org.apache.cloudstack.utils.volume;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class VirtualMachineDiskInfoTest {

    @Test
    public void testGetControllerFromDeviceBusName() {
        VirtualMachineDiskInfo vmDiskInfo = new VirtualMachineDiskInfo();
        vmDiskInfo.setDiskDeviceBusName("scsi0:0");
        String[] diskChain = new String[]{"[somedatastore] i-3-VM-somePath/ROOT-1.vmdk"};
        vmDiskInfo.setDiskChain(diskChain);
        Assert.assertEquals(vmDiskInfo.getControllerFromDeviceBusName(), "scsi");
        Assert.assertArrayEquals(vmDiskInfo.getDiskChain(), diskChain);
    }

    @Test
    public void testGetControllerFromDeviceBusNameWithInvalidBusName() {
        VirtualMachineDiskInfo vmDiskInfo = new VirtualMachineDiskInfo();
        vmDiskInfo.setDiskDeviceBusName("scsi0");
        Assert.assertEquals(vmDiskInfo.getControllerFromDeviceBusName(), null);
    }

    @Test
    public void testGSonDeserialization() throws JsonParseException {
        VirtualMachineDiskInfo infoInChain = new GsonBuilder().create().fromJson("{\"diskDeviceBusName\":\"scsi0:0\",\"diskChain\":[\"[somedatastore] i-3-VM-somePath/ROOT-1.vmdk\"]}", VirtualMachineDiskInfo.class);
        Assert.assertEquals(infoInChain.getDiskDeviceBusName(), "scsi0:0");
        Assert.assertEquals(infoInChain.getControllerFromDeviceBusName(), "scsi");
    }
}
