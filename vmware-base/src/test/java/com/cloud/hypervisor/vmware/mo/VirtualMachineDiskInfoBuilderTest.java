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
