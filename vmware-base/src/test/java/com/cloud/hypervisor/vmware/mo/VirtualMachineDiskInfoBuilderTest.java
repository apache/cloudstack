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
package com.cloud.hypervisor.vmware.mo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.utils.volume.VirtualMachineDiskInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class VirtualMachineDiskInfoBuilderTest {

    @Test
    public void getDiskInfoByBackingFileBaseNameTestFindDisk() {
        VirtualMachineDiskInfoBuilder virtualMachineDiskInfoBuilder = new VirtualMachineDiskInfoBuilder();
        Map<String, List<String>> disks = new HashMap<String, List<String>>();
        String[] diskChain = new String[]{"[somedatastorename] i-3-VM-somePath/ROOT-1.vmdk"};
        disks.put("scsi0:0", Arrays.asList(diskChain));
        virtualMachineDiskInfoBuilder.disks = disks;
        VirtualMachineDiskInfo findedDisk = virtualMachineDiskInfoBuilder.getDiskInfoByBackingFileBaseName("ROOT-1", "somedatastorename", "scsi0:0");
        assertEquals("scsi", findedDisk.getControllerFromDeviceBusName());
        assertArrayEquals(findedDisk.getDiskChain(), diskChain);
    }

    @Test
    public void getDiskInfoByBackingFileBaseNameTestNotFindDisk() {
        VirtualMachineDiskInfoBuilder virtualMachineDiskInfoBuilder = new VirtualMachineDiskInfoBuilder();
        Map<String, List<String>> disks = new HashMap<String, List<String>>();
        disks.put("scsi0:0", Arrays.asList(new String[]{"[somedatastorename] i-3-VM-somePath/ROOT-1.vmdk"}));
        virtualMachineDiskInfoBuilder.disks = disks;
        VirtualMachineDiskInfo findedDisk = virtualMachineDiskInfoBuilder.getDiskInfoByBackingFileBaseName("ROOT-1", "somedatastorename", "ide0:0");
        assertEquals(null, findedDisk);
    }
}
