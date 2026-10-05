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
package org.apache.cloudstack.storage.datastore.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.cloud.utils.exception.CloudRuntimeException;
import com.linbit.linstor.api.ApiException;
import com.linbit.linstor.api.DevelopersApi;
import com.linbit.linstor.api.model.Node;
import com.linbit.linstor.api.model.ResourceDefinition;
import com.linbit.linstor.api.model.ResourceState;
import com.linbit.linstor.api.model.ResourceWithVolumes;
import com.linbit.linstor.api.model.VolumeDefinition;
import org.apache.cloudstack.storage.volume.VolumeOnStorageTO;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class LinstorImportHelperTest {
    private DevelopersApi api;

    @Before
    public void setup() throws Exception {
        api = mock(DevelopersApi.class);
        when(api.nodeList(Collections.emptyList(), Collections.emptyList(), 1000, 0))
                .thenReturn(Collections.singletonList(new Node().name("remote-node").connectionStatus(Node.ConnectionStatusEnum.ONLINE)));
    }

    private ResourceDefinition definition(String name, String group) {
        return new ResourceDefinition().name("cs-" + name).resourceGroupName(group)
                .volumeDefinitions(Collections.singletonList(new VolumeDefinition().volumeNumber(0).sizeKib(1024L)));
    }

    private ResourceWithVolumes resource(String name, Boolean inUse) {
        ResourceWithVolumes resource = new ResourceWithVolumes();
        resource.setName("cs-" + name);
        resource.setNodeName("remote-node");
        resource.setState(new ResourceState().inUse(inUse));
        resource.setVolumes(Collections.singletonList(new com.linbit.linstor.api.model.Volume().volumeNumber(0)));
        return resource;
    }

    private void respond(List<ResourceDefinition> definitions, List<ResourceWithVolumes> resources) throws Exception {
        when(api.resourceDefinitionList(anyList(), eq(true), isNull(), eq(1000), eq(0))).thenReturn(definitions);
        when(api.viewResources(anyList(), anyList(), anyList(), isNull(), eq(1000), eq(0))).thenReturn(resources);
    }

    private List<VolumeOnStorageTO> list(String path) {
        return LinstorImportHelper.getVolumesForImport(api, "rg-ssd", path);
    }

    @Test
    public void listingIncludesRemoteVolumesDeduplicatesReplicasAndFiltersGroup() throws Exception {
        respond(Arrays.asList(definition("z", "rg-ssd"), definition("a", "rg-ssd"), definition("foreign", "rg-hdd")),
                Arrays.asList(resource("z", false), resource("z", false), resource("a", false)));
        List<VolumeOnStorageTO> volumes = list(null);
        assertEquals(2, volumes.size());
        assertEquals("a", volumes.get(0).getPath());
        assertEquals("z", volumes.get(1).getPath());
        assertEquals("RAW", volumes.get(0).getFormat());
        assertEquals(1024L * 1024L, volumes.get(0).getVirtualSize());
        assertEquals("false", volumes.get(0).getDetails().get(VolumeOnStorageTO.Detail.IS_LOCKED));
        verify(api).resourceDefinitionList(Collections.emptyList(), true, null, 1000, 0);
        verify(api).viewResources(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, 1000, 0);
        verify(api).nodeList(Collections.emptyList(), Collections.emptyList(), 1000, 0);
        verifyNoMoreInteractions(api); // In particular: no make-available, delete or property changes.
    }

    @Test
    public void singleVolumeIsReadOnlyAndUsesExactResourceName() throws Exception {
        respond(Collections.singletonList(definition("x", "rg-ssd")), Collections.singletonList(resource("x", false)));
        assertEquals("x", list("x").get(0).getPath());
        verify(api).resourceDefinitionList(Collections.singletonList("cs-x"), true, null, 1000, 0);
        verify(api).viewResources(Collections.emptyList(), Collections.singletonList("cs-x"), Collections.emptyList(), null, 1000, 0);
        verify(api).nodeList(Collections.emptyList(), Collections.emptyList(), 1000, 0);
        verifyNoMoreInteractions(api);
    }

    @Test
    public void busyReplicaLocksVolumeEvenIfOtherReplicaIsFree() throws Exception {
        respond(Collections.singletonList(definition("x", "rg-ssd")), Arrays.asList(resource("x", false), resource("x", true)));
        assertEquals("true", list("x").get(0).getDetails().get(VolumeOnStorageTO.Detail.IS_LOCKED));
    }

    @Test
    public void unknownReplicaStateLocksVolume() throws Exception {
        respond(Collections.singletonList(definition("x", "rg-ssd")), Arrays.asList(resource("x", false), resource("x", null)));
        assertEquals("true", list("x").get(0).getDetails().get(VolumeOnStorageTO.Detail.IS_LOCKED));
    }

    @Test
    public void absentStateLocksVolume() throws Exception {
        ResourceWithVolumes resource = resource("x", false);
        resource.setState(null);
        respond(Collections.singletonList(definition("x", "rg-ssd")), Collections.singletonList(resource));
        assertEquals("true", list("x").get(0).getDetails().get(VolumeOnStorageTO.Detail.IS_LOCKED));
    }

    @Test
    public void noReplicasLocksVolume() throws Exception {
        respond(Collections.singletonList(definition("x", "rg-ssd")), Collections.emptyList());
        assertEquals("true", list("x").get(0).getDetails().get(VolumeOnStorageTO.Detail.IS_LOCKED));
    }

    @Test
    public void offlineSatelliteCannotReportAFreeVolume() throws Exception {
        respond(Collections.singletonList(definition("x", "rg-ssd")), Collections.singletonList(resource("x", false)));
        when(api.nodeList(Collections.emptyList(), Collections.emptyList(), 1000, 0)).thenReturn(Collections.emptyList());
        assertEquals("true", list("x").get(0).getDetails().get(VolumeOnStorageTO.Detail.IS_LOCKED));
    }

    @Test
    public void incompleteResourceCannotBeAdopted() throws Exception {
        ResourceWithVolumes resource = resource("x", false);
        resource.setVolumes(Collections.emptyList());
        respond(Collections.singletonList(definition("x", "rg-ssd")), Collections.singletonList(resource));
        assertEquals("true", list("x").get(0).getDetails().get(VolumeOnStorageTO.Detail.IS_LOCKED));
    }

    @Test
    public void deletingResourceIsNotImportable() throws Exception {
        respond(Collections.singletonList(definition("x", "rg-ssd").flags(Collections.singletonList("DELETE"))),
                Collections.singletonList(resource("x", false)));
        assertEquals("true", list("x").get(0).getDetails().get(VolumeOnStorageTO.Detail.IS_LOCKED));
    }

    @Test
    public void wrongGroupIsRejectedBeforeReadingUsage() throws Exception {
        when(api.resourceDefinitionList(anyList(), eq(true), isNull(), eq(1000), eq(0)))
                .thenReturn(Collections.singletonList(definition("x", "rg-hdd")));
        CloudRuntimeException error = assertThrows(CloudRuntimeException.class, () -> list("x"));
        assertTrue(error.getMessage().contains("rg-hdd"));
        assertTrue(error.getMessage().contains("rg-ssd"));
        verify(api).resourceDefinitionList(Collections.singletonList("cs-x"), true, null, 1000, 0);
        verifyNoMoreInteractions(api);
    }

    @Test
    public void managerValidationRejectsWrongGroupEvenWithoutRunningVm() throws Exception {
        when(api.resourceDefinitionList(anyList(), eq(true), isNull(), eq(1000), eq(0)))
                .thenReturn(Collections.singletonList(definition("x", "rg-hdd")));
        assertThrows(CloudRuntimeException.class, () -> LinstorImportHelper.validateResourceGroup(api, "rg-ssd", "x"));
        verify(api).resourceDefinitionList(Collections.singletonList("cs-x"), true, null, 1000, 0);
        verifyNoMoreInteractions(api);
    }

    @Test
    public void managerValidationAcceptsMatchingGroup() throws Exception {
        when(api.resourceDefinitionList(anyList(), eq(true), isNull(), eq(1000), eq(0)))
                .thenReturn(Collections.singletonList(definition("x", "rg-ssd")));
        LinstorImportHelper.validateResourceGroup(api, "rg-ssd", "x");
        verify(api).resourceDefinitionList(Collections.singletonList("cs-x"), true, null, 1000, 0);
        verifyNoMoreInteractions(api);
    }

    @Test
    public void missingDefinitionIsRejected() throws Exception {
        when(api.resourceDefinitionList(anyList(), eq(true), isNull(), eq(1000), eq(0))).thenReturn(Collections.emptyList());
        assertThrows(CloudRuntimeException.class, () -> list("x"));
    }

    @Test
    public void unavailableControllerFailsClosed() throws Exception {
        when(api.resourceDefinitionList(anyList(), eq(true), isNull(), eq(1000), eq(0)))
                .thenThrow(new ApiException("unreachable"));
        assertThrows(CloudRuntimeException.class, () -> list("x"));
        assertThrows(CloudRuntimeException.class, () -> LinstorImportHelper.validateResourceGroup(api, "rg-ssd", "x"));
    }

    @Test
    public void nullUsageResponseIsAnErrorNotAnEmptySuccessfulListing() throws Exception {
        respond(Collections.singletonList(definition("x", "rg-ssd")), null);
        assertThrows(CloudRuntimeException.class, () -> list(null));
    }

    @Test
    public void nullDefinitionsFailClosed() throws Exception {
        when(api.resourceDefinitionList(anyList(), eq(true), isNull(), eq(1000), eq(0))).thenReturn(null);
        assertThrows(CloudRuntimeException.class, () -> list("x"));
    }

    @Test
    public void missingPoolGroupFailsClosed() {
        assertThrows(CloudRuntimeException.class, () -> LinstorImportHelper.getVolumesForImport(api, null, "x"));
        verifyNoInteractions(api);
    }

    @Test
    public void devicePathCannotBypassResourceNameValidation() {
        assertThrows(CloudRuntimeException.class, () -> list("/dev/drbd/by-res/cs-x/0"));
        verifyNoInteractions(api);
    }

    @Test
    public void malformedVolumeDefinitionsFailClosed() throws Exception {
        ResourceDefinition definition = definition("x", "rg-ssd");
        definition.setVolumeDefinitions(Collections.emptyList());
        respond(Collections.singletonList(definition), Collections.singletonList(resource("x", false)));
        assertThrows(CloudRuntimeException.class, () -> list("x"));
    }

    @Test
    public void sizeOverflowFailsClosed() throws Exception {
        ResourceDefinition definition = definition("x", "rg-ssd");
        definition.getVolumeDefinitions().get(0).setSizeKib(Long.MAX_VALUE);
        respond(Collections.singletonList(definition), Collections.singletonList(resource("x", false)));
        assertThrows(ArithmeticException.class, () -> list("x"));
    }

    @Test
    public void batchReadsFollowPagination() throws Exception {
        when(api.resourceDefinitionList(anyList(), eq(true), isNull(), eq(1000), eq(0)))
                .thenReturn(Collections.nCopies(1000, definition("foreign", "rg-hdd")));
        when(api.resourceDefinitionList(anyList(), eq(true), isNull(), eq(1000), eq(1000)))
                .thenReturn(Collections.singletonList(definition("x", "rg-ssd")));
        when(api.viewResources(anyList(), anyList(), anyList(), isNull(), eq(1000), eq(0)))
                .thenReturn(Collections.nCopies(1000, resource("foreign", false)));
        when(api.viewResources(anyList(), anyList(), anyList(), isNull(), eq(1000), eq(1000)))
                .thenReturn(Collections.singletonList(resource("x", false)));
        assertEquals("x", list(null).get(0).getPath());
        verify(api).resourceDefinitionList(Collections.emptyList(), true, null, 1000, 1000);
        verify(api).viewResources(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, 1000, 1000);
    }
}
