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

import org.apache.xmlrpc.XmlRpcException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.cloud.utils.ExecutionResult;
import com.cloud.utils.exception.CloudRuntimeException;
import com.xensource.xenapi.Types.XenAPIException;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;

public class XcpServerResourceTest extends CitrixResourceBaseTest {

    @Before
    @Override
    public void beforeTest() throws XenAPIException, XmlRpcException {
        super.citrixResourceBase = Mockito.spy(new XcpServerResource());
        super.beforeTest();
    }

    @Test
    public void testPatchFilePath() {
        String patchFilePath = citrixResourceBase.getPatchFilePath();
        String patch = "scripts/vm/hypervisor/xenserver/xcpserver/patch";

        Assert.assertEquals(patch, patchFilePath);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testGetFilesExeption() {
        testGetPathFilesExeption();
    }

    @Test
    public void testGetFilesListReturned() {
        testGetPathFilesListReturned();
    }

    private void testNetworkUsageInternal(String option, String publicIp, String expectedArgs) {
        String result = citrixResourceBase.networkUsage(null, "10.10.10.10", option, "eth0", publicIp);

        Assert.assertEquals("result", result);
        Mockito.verify(citrixResourceBase).executeInVR("10.10.10.10", "netusage.sh", expectedArgs);
    }

    @Test
    public void testNetworkUsage() {
        ExecutionResult executionResult = new ExecutionResult(true, "result");

        doReturn(executionResult).when(citrixResourceBase).executeInVR(anyString(), anyString(), anyString());

        testNetworkUsageInternal("get", "", "-g");
        testNetworkUsageInternal("get", "20.20.20.20", "-g -l 20.20.20.20");
        testNetworkUsageInternal("create", "", "-c");
        testNetworkUsageInternal("reset", "", "-r");
        testNetworkUsageInternal("addVif", "", "-a eth0");
        testNetworkUsageInternal("deleteVif", "", "-d eth0");
    }
}
