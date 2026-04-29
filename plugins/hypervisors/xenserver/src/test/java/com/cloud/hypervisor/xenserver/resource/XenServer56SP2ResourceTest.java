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

import com.cloud.utils.exception.CloudRuntimeException;
import com.xensource.xenapi.Types.XenAPIException;

public class XenServer56SP2ResourceTest extends CitrixResourceBaseTest {

    @Override
    @Before
    public void beforeTest() throws XenAPIException, XmlRpcException {
        super.citrixResourceBase = Mockito.spy(new XenServer56SP2Resource());
        super.beforeTest();
    }

    @Test
    public void testPatchFilePath() {
        String patchFilePath = citrixResourceBase.getPatchFilePath();
        String patch = "scripts/vm/hypervisor/xenserver/xenserver56fp1/patch";

        Assert.assertEquals(patch, patchFilePath);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testGetFiles() {
        testGetPathFilesException();
    }

    @Test
    public void testGetFilesListReturned() {
        testGetPathFilesListReturned();
    }
}
