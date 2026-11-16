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

package com.cloud.hypervisor.xenserver.discoverer;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.template.TemplateManager;

@RunWith(MockitoJUnitRunner.class)
public class XcpServerDiscovererTest {

    @Spy
    @InjectMocks
    private XcpServerDiscoverer xcpServerDiscoverer;

    @Mock
    private VMTemplateDao vmTemplateDao;

    @Test
    public void createXenServerToolsIsoEntryInDatabaseTestNoEntryFound() {
        Mockito.when(vmTemplateDao.findByTemplateName(TemplateManager.XS_TOOLS_ISO)).thenReturn(null);
        Mockito.when(vmTemplateDao.getNextInSequence(Long.class, "id")).thenReturn(1L);

        xcpServerDiscoverer.createXenServerToolsIsoEntryInDatabase();

        InOrder inOrder = Mockito.inOrder(vmTemplateDao);
        inOrder.verify(vmTemplateDao).findByTemplateName(TemplateManager.XS_TOOLS_ISO);
        inOrder.verify(vmTemplateDao).getNextInSequence(Long.class, "id");
        inOrder.verify(vmTemplateDao).persist(Mockito.any(VMTemplateVO.class));
    }

    @Test
    public void createXenServerToolsIsoEntryInDatabaseTestEntryAlreadyExist() {
        VMTemplateVO vmTemplateVOMock = Mockito.mock(VMTemplateVO.class);
        Mockito.when(vmTemplateDao.findByTemplateName(TemplateManager.XS_TOOLS_ISO)).thenReturn(vmTemplateVOMock);
        Mockito.when(vmTemplateVOMock.getId()).thenReturn(1L);

        xcpServerDiscoverer.createXenServerToolsIsoEntryInDatabase();

        InOrder inOrder = Mockito.inOrder(vmTemplateDao, vmTemplateVOMock);
        inOrder.verify(vmTemplateDao).findByTemplateName(TemplateManager.XS_TOOLS_ISO);
        inOrder.verify(vmTemplateDao, Mockito.times(0)).getNextInSequence(Long.class, "id");
        inOrder.verify(vmTemplateVOMock).setTemplateType(TemplateType.PERHOST);
        inOrder.verify(vmTemplateVOMock).setUrl(null);
        inOrder.verify(vmTemplateVOMock).setDisplayText("XenServer Tools Installer ISO (xen-pv-drv-iso)");
        inOrder.verify(vmTemplateDao).update(1L, vmTemplateVOMock);
    }

    @Test
    public void uefiSupportedVersionTest() {
        Assert.assertTrue(XcpServerDiscoverer.isUefiSupported("8.2"));
        Assert.assertTrue(XcpServerDiscoverer.isUefiSupported("8.2.0"));
        Assert.assertTrue(XcpServerDiscoverer.isUefiSupported("8.2.1"));
        Assert.assertTrue(XcpServerDiscoverer.isUefiSupported("9"));
        Assert.assertTrue(XcpServerDiscoverer.isUefiSupported("9.1"));
        Assert.assertTrue(XcpServerDiscoverer.isUefiSupported("9.1.0"));
        Assert.assertTrue(XcpServerDiscoverer.isUefiSupported("10"));
        Assert.assertTrue(XcpServerDiscoverer.isUefiSupported("10.1"));
        Assert.assertTrue(XcpServerDiscoverer.isUefiSupported("10.1.0"));
        Assert.assertFalse(XcpServerDiscoverer.isUefiSupported(null));
        Assert.assertFalse(XcpServerDiscoverer.isUefiSupported(""));
        Assert.assertFalse(XcpServerDiscoverer.isUefiSupported("abc"));
        Assert.assertFalse(XcpServerDiscoverer.isUefiSupported("0"));
        Assert.assertFalse(XcpServerDiscoverer.isUefiSupported("7.4"));
        Assert.assertFalse(XcpServerDiscoverer.isUefiSupported("8"));
        Assert.assertFalse(XcpServerDiscoverer.isUefiSupported("8.1"));
        Assert.assertFalse(XcpServerDiscoverer.isUefiSupported("8.1.0"));
    }
}
