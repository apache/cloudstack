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
package org.apache.cloudstack.api.command.user.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anySet;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.response.ResourceIconResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.cloud.server.ResourceIcon;
import com.cloud.server.ResourceIconManager;
import com.cloud.server.ResourceTag;
import com.cloud.storage.GuestOS;
import com.cloud.utils.db.EntityManager;

public class ListVMsCmdTest {

    EntityManager _entityMgr;
    ResourceIconManager resourceIconManager;
    ResponseGenerator _responseGenerator;

    ListVMsCmd cmd;

    @Before
    public void setup() {
        _entityMgr = mock(EntityManager.class);
        resourceIconManager = mock(ResourceIconManager.class);
        _responseGenerator = mock(ResponseGenerator.class);
        cmd = spy(ListVMsCmd.class);
        cmd._entityMgr = _entityMgr;
        cmd.resourceIconManager = resourceIconManager;
        cmd._responseGenerator = _responseGenerator;
    }

    @Test
    public void testUpdateVMResponse_withMixedIcons() {
        String vm1Uuid = UUID.randomUUID().toString();
        UserVmResponse vm1 = mock(UserVmResponse.class);
        when(vm1.getId()).thenReturn(vm1Uuid);
        String vm2Uuid = UUID.randomUUID().toString();
        UserVmResponse vm2 = mock(UserVmResponse.class);
        when(vm2.getId()).thenReturn(vm2Uuid);
        List<UserVmResponse> responses = Arrays.asList(vm1, vm2);
        ResourceIcon icon1 = mock(ResourceIcon.class);
        ResourceIcon icon2 = mock(ResourceIcon.class);
        Map<String, ResourceIcon> initialIcons = new HashMap<>();
        initialIcons.put(vm1Uuid, icon1);
        when(resourceIconManager.getByResourceTypeAndUuids(ResourceTag.ResourceObjectType.UserVm, Set.of(vm1Uuid, vm2Uuid)))
                .thenReturn(initialIcons);
        Map<String, ResourceIcon> fallbackIcons = Map.of(vm2Uuid, icon2);
        doReturn(fallbackIcons).when(cmd).getResourceIconsForUsingTemplateIso(anyList());
        ResourceIconResponse iconResponse1 = new ResourceIconResponse();
        ResourceIconResponse iconResponse2 = new ResourceIconResponse();
        when(_responseGenerator.createResourceIconResponse(icon1)).thenReturn(iconResponse1);
        when(_responseGenerator.createResourceIconResponse(icon2)).thenReturn(iconResponse2);
        cmd.updateVMResponse(responses);
        verify(vm1).setResourceIconResponse(iconResponse1);
        verify(vm2).setResourceIconResponse(iconResponse2);
    }

    @Test
    public void testUpdateVMResponse_withEmptyList() {
        cmd.updateVMResponse(Collections.emptyList());
        verify(resourceIconManager, never()).getByResourceTypeAndIds(Mockito.any(), Mockito.anyCollection());
    }

    @Test
    public void testGetResourceIconsForUsingTemplateIso_withValidData() {
        String vm1Uuid = UUID.randomUUID().toString();
        String template1Uuid = UUID.randomUUID().toString();
        UserVmResponse vm1 = mock(UserVmResponse.class);
        when(vm1.getId()).thenReturn(vm1Uuid);
        when(vm1.getTemplateId()).thenReturn(template1Uuid);
        when(vm1.getIsoId()).thenReturn(null);
        String vm2Uuid = UUID.randomUUID().toString();
        String iso2Uuid = UUID.randomUUID().toString();
        UserVmResponse vm2 = mock(UserVmResponse.class);
        when(vm2.getId()).thenReturn(vm2Uuid);
        when(vm2.getTemplateId()).thenReturn(null);
        when(vm2.getIsoId()).thenReturn(iso2Uuid);
        List<UserVmResponse> responses = Arrays.asList(vm1, vm2);
        Map<String, ResourceIcon> templateIcons = new HashMap<>();
        templateIcons.put(template1Uuid, mock(ResourceIcon.class));
        Map<String, ResourceIcon> isoIcons = new HashMap<>();
        isoIcons.put(iso2Uuid, mock(ResourceIcon.class));
        when(resourceIconManager.getByResourceTypeAndUuids(ResourceTag.ResourceObjectType.Template, Set.of(template1Uuid)))
                .thenReturn(templateIcons);
        when(resourceIconManager.getByResourceTypeAndUuids(ResourceTag.ResourceObjectType.ISO, Set.of(iso2Uuid)))
                .thenReturn(isoIcons);
        doReturn(Collections.emptyMap()).when(cmd).getResourceIconsUsingOsCategory(anyList());
        Map<String, ResourceIcon> result = cmd.getResourceIconsForUsingTemplateIso(responses);
        assertEquals(2, result.size());
        assertTrue(result.containsKey(vm1Uuid));
        assertTrue(result.containsKey(vm2Uuid));
        assertEquals(templateIcons.get(template1Uuid), result.get(vm1Uuid));
        assertEquals(isoIcons.get(iso2Uuid), result.get(vm2Uuid));
    }

    @Test
    public void testGetResourceIconsForUsingTemplateIso_withMissingIcons() {
        String vm1Uuid = UUID.randomUUID().toString();
        String template1Uuid = UUID.randomUUID().toString();
        UserVmResponse vm1 = mock(UserVmResponse.class);
        when(vm1.getId()).thenReturn(vm1Uuid);
        when(vm1.getTemplateId()).thenReturn(template1Uuid);
        when(vm1.getIsoId()).thenReturn(null);
        List<UserVmResponse> responses = List.of(vm1);
        when(resourceIconManager.getByResourceTypeAndUuids(eq(ResourceTag.ResourceObjectType.Template), anySet()))
                .thenReturn(Collections.emptyMap());
        when(resourceIconManager.getByResourceTypeAndUuids(eq(ResourceTag.ResourceObjectType.ISO), anySet()))
                .thenReturn(Collections.emptyMap());
        Map<String, ResourceIcon> fallbackIcons = Map.of(vm1Uuid, mock(ResourceIcon.class));
        doReturn(fallbackIcons).when(cmd).getResourceIconsUsingOsCategory(anyList());
        Map<String, ResourceIcon> result = cmd.getResourceIconsForUsingTemplateIso(responses);
        assertEquals(1, result.size());
        assertEquals(fallbackIcons.get("vm1"), result.get("vm1"));
    }

    @Test
    public void testGetResourceIconsUsingOsCategory_withValidData() {
        String vm1Uuid = UUID.randomUUID().toString();
        String os1Uuid = UUID.randomUUID().toString();
        UserVmResponse vm1 = mock(UserVmResponse.class);
        when(vm1.getGuestOsId()).thenReturn(os1Uuid);
        when(vm1.getId()).thenReturn(vm1Uuid);
        String vm2Uuid = UUID.randomUUID().toString();
        String os2Uuid = UUID.randomUUID().toString();
        UserVmResponse vm2 = mock(UserVmResponse.class);
        when(vm2.getGuestOsId()).thenReturn(os2Uuid);
        when(vm2.getId()).thenReturn(vm2Uuid);
        List<UserVmResponse> responses = Arrays.asList(vm1, vm2);
        GuestOS guestOS1 = mock(GuestOS.class);
        when(guestOS1.getUuid()).thenReturn(os1Uuid);
        when(guestOS1.getCategoryId()).thenReturn(10L);
        GuestOS guestOS2 = mock(GuestOS.class);
        when(guestOS2.getUuid()).thenReturn(os2Uuid);
        when(guestOS2.getCategoryId()).thenReturn(20L);
        when(_entityMgr.listByUuids(eq(GuestOS.class), anySet()))
                .thenReturn(Arrays.asList(guestOS1, guestOS2));
        ResourceIcon icon1 = mock(ResourceIcon.class);
        ResourceIcon icon2 = mock(ResourceIcon.class);
        Map<Long, ResourceIcon> categoryIcons = new HashMap<>();
        categoryIcons.put(10L, icon1);
        categoryIcons.put(20L, icon2);
        when(resourceIconManager.getByResourceTypeAndIds(eq(ResourceTag.ResourceObjectType.GuestOsCategory), anySet()))
                .thenReturn(categoryIcons);
        Map<String, ResourceIcon> result = cmd.getResourceIconsUsingOsCategory(responses);
        assertEquals(2, result.size());
        assertEquals(icon1, result.get(vm1Uuid));
        assertEquals(icon2, result.get(vm2Uuid));
    }

    @Test
    public void testGetResourceIconsUsingOsCategory_missingGuestOS() {
        String vm1Uuid = UUID.randomUUID().toString();
        String os1Uuid = UUID.randomUUID().toString();
        UserVmResponse vm1 = mock(UserVmResponse.class);
        when(vm1.getGuestOsId()).thenReturn(vm1Uuid);
        when(vm1.getId()).thenReturn(os1Uuid);
        List<UserVmResponse> responses = Collections.singletonList(vm1);
        when(_entityMgr.listByUuids(eq(GuestOS.class), anySet()))
                .thenReturn(Collections.emptyList());
        Map<String, ResourceIcon> result = cmd.getResourceIconsUsingOsCategory(responses);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetResourceIconsUsingOsCategory_missingIcon() {
        UserVmResponse vm1 = mock(UserVmResponse.class);
        String vmUuid = UUID.randomUUID().toString();
        String osUuid = UUID.randomUUID().toString();
        when(vm1.getGuestOsId()).thenReturn(osUuid);
        when(vm1.getId()).thenReturn(vmUuid);
        List<UserVmResponse> responses = Collections.singletonList(vm1);
        GuestOS guestOS1 = mock(GuestOS.class);
        when(guestOS1.getCategoryId()).thenReturn(10L);
        when(guestOS1.getUuid()).thenReturn(osUuid);
        when(_entityMgr.listByUuids(eq(GuestOS.class), anySet()))
                .thenReturn(Collections.singletonList(guestOS1));
        when(resourceIconManager.getByResourceTypeAndIds(eq(ResourceTag.ResourceObjectType.GuestOsCategory), anySet()))
                .thenReturn(Collections.emptyMap());
        Map<String, ResourceIcon> result = cmd.getResourceIconsUsingOsCategory(responses);
        assertTrue(result.containsKey(vmUuid));
        assertNull(result.get(vmUuid));
    }
}
