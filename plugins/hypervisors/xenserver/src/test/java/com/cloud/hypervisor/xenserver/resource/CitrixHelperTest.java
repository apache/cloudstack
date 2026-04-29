/*
 * Copyright 2015 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloud.hypervisor.xenserver.resource;

import junit.framework.Assert;
import org.junit.Test;


/**
 * Created by ajna123 on 12/11/2015.
 */
public class CitrixHelperTest {

    @Test
    public void testGetPVbootloaderArgs() throws Exception {

        String os_name_Suse10Sp2_64 = "SUSE Linux Enterprise Server 10 SP2 (64-bit)";
        String os_name_Suse10Sp2_32 = "SUSE Linux Enterprise Server 10 SP2 (32-bit)";
        String os_name_Suse11Sp3_64 = "SUSE Linux Enterprise Server 11 SP3 (64-bit)";
        String os_name_Suse11Sp3_32 = "SUSE Linux Enterprise Server 11 SP3 (32-bit)";

        String os_name_Windows8_64 = "Windows 8 (64-bit)";
        String os_name_Windows8_32 = "Windows 8 (32-bit)";

        String pvBootLoaderArgs_32 = "--kernel /boot/vmlinuz-xenpae --ramdisk /boot/initrd-xenpae";
        String pvBootLoaderArgs_64 = "--kernel /boot/vmlinuz-xen --ramdisk /boot/initrd-xen";

        Assert.assertEquals(CitrixHelper.getPVbootloaderArgs(os_name_Suse10Sp2_32), pvBootLoaderArgs_32);
        Assert.assertEquals(CitrixHelper.getPVbootloaderArgs(os_name_Suse10Sp2_64),pvBootLoaderArgs_64);
        Assert.assertEquals(CitrixHelper.getPVbootloaderArgs(os_name_Suse11Sp3_32),pvBootLoaderArgs_32);
        Assert.assertEquals(CitrixHelper.getPVbootloaderArgs(os_name_Suse11Sp3_64),pvBootLoaderArgs_64);

        Assert.assertEquals(CitrixHelper.getPVbootloaderArgs(os_name_Windows8_32),"");
        Assert.assertEquals(CitrixHelper.getPVbootloaderArgs(os_name_Windows8_64),"");
    }
}
