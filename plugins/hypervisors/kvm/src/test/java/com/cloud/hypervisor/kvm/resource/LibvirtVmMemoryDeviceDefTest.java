/*
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

package com.cloud.hypervisor.kvm.resource;

import org.apache.cloudstack.utils.bytescale.ByteScaleUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtVmMemoryDeviceDefTest {

    @Test
    public void validateToString(){
        long memorySize = ByteScaleUtils.KiB;

        StringBuilder expectedToString = new StringBuilder();
        expectedToString.append("<memory model='dimm'>");
        expectedToString.append("<target>");
        expectedToString.append(String.format("<size unit='KiB'>%s</size>", memorySize));
        expectedToString.append("<node>0</node>");
        expectedToString.append("</target>");
        expectedToString.append("</memory>");

        Assert.assertEquals(expectedToString.toString(), new LibvirtVmMemoryDeviceDef(memorySize).toString());
    }

}
