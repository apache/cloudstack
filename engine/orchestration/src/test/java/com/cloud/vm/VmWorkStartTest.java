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

public class VmWorkStartTest {

    @Test
    public void testToStringWithParams() {
        VmWork vmWork = new VmWork(1l,  1l, 1l, "testhandler");
        VmWorkStart workInfo = new VmWorkStart(vmWork);
        Map<VirtualMachineProfile.Param, Object> params = new HashMap<>();
        String lastHost = "rO0ABXQABHRydWU";
        String lastHostSerialized = JobSerializerHelper.toObjectSerializedString(lastHost);
        params.put(VirtualMachineProfile.Param.ConsiderLastHost, lastHost);
        params.put(VirtualMachineProfile.Param.VmPassword, "rO0ABXQADnNhdmVkX3Bhc3N3b3Jk");
        workInfo.setParams(params);
        String expectedVmWorkStartStr = "{\"accountId\":1,\"dcId\":0,\"vmId\":1,\"handlerName\":\"testhandler\",\"userId\":1,\"rawParams\":{\"ConsiderLastHost\":\"" + lastHostSerialized + "\"}}";

        String vmWorkStartStr = workInfo.toString();
        Assert.assertEquals(expectedVmWorkStartStr, vmWorkStartStr);
    }

    @Test
    public void testToStringWithRawParams() {
        VmWork vmWork = new VmWork(1l,  1l, 1l, "testhandler");
        VmWorkStart workInfo = new VmWorkStart(vmWork);
        Map<String, String> rawParams = new HashMap<>();
        rawParams.put(VirtualMachineProfile.Param.ConsiderLastHost.getName(), "rO0ABXQABHRydWU");
        rawParams.put(VirtualMachineProfile.Param.VmPassword.getName(), "rO0ABXQADnNhdmVkX3Bhc3N3b3Jk");
        workInfo.setRawParams(rawParams);
        String expectedVmWorkStartStr = "{\"accountId\":1,\"dcId\":0,\"vmId\":1,\"handlerName\":\"testhandler\",\"userId\":1,\"rawParams\":{\"ConsiderLastHost\":\"rO0ABXQABHRydWU\"}}";

        String vmWorkStartStr = workInfo.toString();
        Assert.assertEquals(expectedVmWorkStartStr, vmWorkStartStr);
    }
}
