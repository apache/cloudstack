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
package com.cloud.vm;

import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.framework.jobs.impl.JobSerializerHelper;
import org.junit.Assert;
import org.junit.Test;

public class VmWorkRebootTest {

    @Test
    public void testToString() {
        VmWork vmWork = new VmWork(1l, 1l, 1l, "testhandler");
        Map<VirtualMachineProfile.Param, Object> params = new HashMap<>();
        String lastHost = "rO0ABXQABHRydWU";
        String lastHostSerialized = JobSerializerHelper.toObjectSerializedString(lastHost);
        params.put(VirtualMachineProfile.Param.ConsiderLastHost, lastHost);
        params.put(VirtualMachineProfile.Param.VmPassword, "rO0ABXQADnNhdmVkX3Bhc3N3b3Jk");
        VmWorkReboot workInfo = new VmWorkReboot(vmWork, params);
        String expectedVmWorkRebootStr = "{\"accountId\":1,\"vmId\":1,\"handlerName\":\"testhandler\",\"userId\":1,\"rawParams\":{\"ConsiderLastHost\":\"" + lastHostSerialized + "\"}}";

        String vmWorkRebootStr = workInfo.toString();
        Assert.assertEquals(expectedVmWorkRebootStr, vmWorkRebootStr);
    }
}
