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

package org.apache.cloudstack.framework.extensions.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.response.ExtensionCustomActionResponse;
import org.apache.cloudstack.api.response.ExtensionResponse;
import org.apache.cloudstack.extension.CustomActionResultResponse;
import org.apache.cloudstack.extension.Extension;
import org.apache.cloudstack.extension.ExtensionCustomAction;
import org.apache.cloudstack.extension.ExtensionResourceMap;
import org.apache.cloudstack.framework.extensions.api.AddCustomActionCmd;
import org.apache.cloudstack.framework.extensions.api.CreateExtensionCmd;
import org.apache.cloudstack.framework.extensions.api.DeleteCustomActionCmd;
import org.apache.cloudstack.framework.extensions.api.DeleteExtensionCmd;
import org.apache.cloudstack.framework.extensions.api.ListCustomActionCmd;
import org.apache.cloudstack.framework.extensions.api.ListExtensionsCmd;
import org.apache.cloudstack.framework.extensions.api.RegisterExtensionCmd;
import org.apache.cloudstack.framework.extensions.api.RunCustomActionCmd;
import org.apache.cloudstack.framework.extensions.api.UnregisterExtensionCmd;
import org.apache.cloudstack.framework.extensions.api.UpdateCustomActionCmd;
import org.apache.cloudstack.framework.extensions.api.UpdateExtensionCmd;
import org.apache.cloudstack.framework.extensions.command.CleanupExtensionFilesCommand;
import org.apache.cloudstack.framework.extensions.command.ExtensionServerActionBaseCommand;
import org.apache.cloudstack.framework.extensions.command.GetExtensionEntryPointChecksumCommand;
import org.apache.cloudstack.framework.extensions.command.PrepareExtensionEntryPointCommand;
import org.apache.cloudstack.framework.extensions.dao.ExtensionCustomActionDao;
import org.apache.cloudstack.framework.extensions.dao.ExtensionCustomActionDetailsDao;
import org.apache.cloudstack.framework.extensions.dao.ExtensionDao;
import org.apache.cloudstack.framework.extensions.dao.ExtensionDetailsDao;
import org.apache.cloudstack.framework.extensions.dao.ExtensionResourceMapDao;
import org.apache.cloudstack.framework.extensions.dao.ExtensionResourceMapDetailsDao;
import org.apache.cloudstack.framework.extensions.vo.ExtensionCustomActionVO;
import org.apache.cloudstack.framework.extensions.vo.ExtensionResourceMapVO;
import org.apache.cloudstack.framework.extensions.vo.ExtensionVO;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.RunCustomActionAnswer;
import com.cloud.alert.AlertManager;
import com.cloud.cluster.ClusterManager;
import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.hypervisor.ExternalProvisioner;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.serializer.GsonHelper;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.utils.Pair;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.VMInstanceDao;

@RunWith(MockitoJUnitRunner.class)
public class ExtensionsManagerImplTest {

    @Spy
    @InjectMocks
    private ExtensionsManagerImpl extensionsManager;

    @Mock
    private ExtensionDao extensionDao;
    @Mock
    private ExtensionDetailsDao extensionDetailsDao;
    @Mock
    private ExtensionResourceMapDao extensionResourceMapDao;
    @Mock
    private ExtensionResourceMapDetailsDao extensionResourceMapDetailsDao;
    @Mock
    private ClusterDao clusterDao;
    @Mock
    private AgentManager agentMgr;
    @Mock
    private HostDao hostDao;
    @Mock
    private HostDetailsDao hostDetailsDao;
    @Mock
    private ExternalProvisioner externalProvisioner;
    @Mock
    private ExtensionCustomActionDao extensionCustomActionDao;
    @Mock
    private ExtensionCustomActionDetailsDao extensionCustomActionDetailsDao;
    @Mock
    private VMInstanceDao vmInstanceDao;
    @Mock
    private VirtualMachineManager virtualMachineManager;
    @Mock
    private EntityManager entityManager;
    @Mock
    private ManagementServerHostDao managementServerHostDao;
    @Mock
    private ClusterManager clusterManager;
    @Mock
    private AlertManager alertManager;
    @Mock
    private VMTemplateDao templateDao;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void getDefaultExtensionRelativeEntryPointReturnsExpectedPath() {
        String name = "testExtension";
        String expected = Extension.getDirectoryName(name) + File.separator + Extension.getDirectoryName(name) + ".sh";
        String result = extensionsManager.getDefaultExtensionRelativeEntryPoint(name);
        assertEquals(expected, result);
    }

    @Test
    public void getValidatedExtensionRelativeEntryPointReturnsNormalizedPath() {
        String name = "ext";
        String path = "ext/entry.sh";
        String result = extensionsManager.getValidatedExtensionRelativeEntryPoint(name, path);
        assertTrue(result.startsWith("ext/"));
    }

    @Test(expected = InvalidParameterException.class)
    public void getValidatedExtensionRelativeEntryPointThrowsForDeepPath() {
        String name = "ext";
        String path = "ext/a/b/c/entry.sh";
        extensionsManager.getValidatedExtensionRelativeEntryPoint(name, path);
    }

    @Test
    public void getResultFromAnswersStringReturnsSuccess() {
        Extension ext = mock(Extension.class);
        Answer[] answers = new Answer[]{new Answer(mock(PrepareExtensionEntryPointCommand.class), true, "ok")};
        String json = GsonHelper.getGson().toJson(answers);
        ManagementServerHostVO msHost = mock(ManagementServerHostVO.class);
        Pair<Boolean, String> result = extensionsManager.getResultFromAnswersString(json, ext, msHost, "op");
        assertTrue(result.first());
        assertEquals("ok", result.second());
    }

    @Test
    public void getResultFromAnswersStringReturnsFailure() {
        Extension ext = mock(Extension.class);
        Answer[] answers = new Answer[]{new Answer(mock(PrepareExtensionEntryPointCommand.class), false, "fail")};
        String json = GsonHelper.getGson().toJson(answers);
        ManagementServerHostVO msHost = mock(ManagementServerHostVO.class);
        Pair<Boolean, String> result = extensionsManager.getResultFromAnswersString(json, ext, msHost, "op");
        assertFalse(result.first());
        assertEquals("fail", result.second());
    }

    @Test
    public void prepareExtensionEntryPointOnMSPeerReturnsTrueOnSuccess() {
        Extension ext = mock(Extension.class);
        ManagementServerHostVO msHost = mock(ManagementServerHostVO.class);
        when(msHost.getMsid()).thenReturn(1L);
        when(clusterManager.execute(anyString(), anyLong(), anyString(), eq(true)))
                .thenReturn("answer");
        doReturn(new Pair<>(true, "ok")).when(extensionsManager).getResultFromAnswersString(anyString(), eq(ext), eq(msHost), anyString());
        assertTrue(extensionsManager.prepareExtensionEntryPointOnMSPeer(ext, msHost));
    }

    @Test
    public void prepareExtensionEntryPointOnCurrentServerReturnsSuccess() {
        doNothing().when(externalProvisioner).prepareExtensionEntryPoint(anyString(), anyBoolean(), anyString());
        Pair<Boolean, String> result = extensionsManager.prepareExtensionEntryPointOnCurrentServer("name", true, "entry");
        assertTrue(result.first());
        assertNull(result.second());
    }

    @Test
    public void cleanupExtensionFilesOnMSPeerReturnsTrueOnSuccess() {
        Extension ext = mock(Extension.class);
        ManagementServerHostVO msHost = mock(ManagementServerHostVO.class);
        when(msHost.getMsid()).thenReturn(1L);
        when(clusterManager.execute(anyString(), anyLong(), anyString(), eq(true)))
                .thenReturn("answer");
        doReturn(new Pair<>(true, "ok")).when(extensionsManager).getResultFromAnswersString(anyString(), eq(ext), eq(msHost), anyString());
        assertTrue(extensionsManager.cleanupExtensionFilesOnMSPeer(ext, msHost));
    }

    @Test
    public void cleanupExtensionFilesOnCurrentServerReturnsSuccess() {
        Pair<Boolean, String> result = extensionsManager.cleanupExtensionFilesOnCurrentServer("name", "entry");
        assertTrue(result.first());
    }

    @Test
    public void getParametersListFromMapReturnsEmptyListForNull() {
        List<ExtensionCustomAction.Parameter> result = extensionsManager.getParametersListFromMap("action", null);
        assertTrue(result.isEmpty());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void unregisterExtensionWithClusterThrowsIfClusterNotFound() {
        when(clusterDao.findByUuid(anyString())).thenReturn(null);
        extensionsManager.unregisterExtensionWithCluster("uuid", 1L);
    }

    @Test
    public void getExtensionFromResourceReturnsNullIfEntityNotFound() {
        when(entityManager.findByUuid(any(), anyString())).thenReturn(null);
        assertNull(extensionsManager.getExtensionFromResource(ExtensionCustomAction.ResourceType.VirtualMachine, "uuid"));
    }

    @Test
    public void getActionMessageReturnsDefaultOnBlank() {
        ExtensionCustomAction action = mock(ExtensionCustomAction.class);
        Extension ext = mock(Extension.class);
        when(action.getSuccessMessage()).thenReturn(null);
        String msg = extensionsManager.getActionMessage(true, action, ext, ExtensionCustomAction.ResourceType.VirtualMachine, null);
        assertTrue(msg.contains("Successfully completed"));
    }

    @Test
    public void getFilteredExternalDetailsReturnsFilteredMap() {
        Map<String, String> details = new HashMap<>();
        String key = "detail.key";
        details.put(VmDetailConstants.EXTERNAL_DETAIL_PREFIX + key, "value");
        details.put("other.key", "value2");
        Map<String, String> filtered = extensionsManager.getFilteredExternalDetails(details);
        assertTrue(filtered.containsKey(key));
        assertFalse(filtered.containsKey("other.key"));
    }

    @Test
    public void sendExtensionEntryPointOutOfSyncAlertCallsAlertManager() {
        Extension ext = mock(Extension.class);
        extensionsManager.sendExtensionEntryPointOutOfSyncAlert(ext);
        verify(alertManager, atLeastOnce()).sendAlert(any(), anyLong(), anyLong(), anyString(), anyString());
    }

    @Test
    public void updateExtensionEntryPointReadyUpdatesWhenStateDiffers() {
        Extension ext = mock(Extension.class);
        when(ext.getId()).thenReturn(1L);
        when(ext.isEntryPointReady()).thenReturn(false);
        ExtensionVO vo = mock(ExtensionVO.class);
        when(extensionDao.createForUpdate(1L)).thenReturn(vo);
        when(extensionDao.update(1L, vo)).thenReturn(true);
        extensionsManager.updateExtensionEntryPointReady(ext, true);
        verify(extensionDao).update(1L, vo);
    }

    @Test
    public void disableExtensionUpdatesState() {
        ExtensionVO vo = mock(ExtensionVO.class);
        when(extensionDao.createForUpdate(1L)).thenReturn(vo);
        when(extensionDao.update(1L, vo)).thenReturn(true);
        extensionsManager.disableExtension(1L);
        verify(extensionDao).update(1L, vo);
    }

    @Test
    public void getExternalAccessDetailsReturnsMapWithHostAndExtension() {
        Map<String, String> actionDetails = new HashMap<>();
        actionDetails.put("external.detail.key", "value");
        long hostId = 1L;
        ExtensionResourceMap resourceMap = mock(ExtensionResourceMap.class);
        when(resourceMap.getId()).thenReturn(2L);
        when(resourceMap.getExtensionId()).thenReturn(3L);
        when(hostDetailsDao.findDetails(hostId)).thenReturn(actionDetails);
        when(extensionResourceMapDetailsDao.listDetailsKeyPairs(2L, true)).thenReturn(actionDetails);
        when(extensionDetailsDao.listDetailsKeyPairs(3L, true)).thenReturn(actionDetails);
        Map<String, Object> result = extensionsManager.getExternalAccessDetails(actionDetails, hostId, resourceMap);
        assertTrue(result.containsKey(ApiConstants.HOST));
        assertTrue(result.containsKey(ApiConstants.EXTENSION));
    }

    @Test(expected = CloudRuntimeException.class)
    public void checkOrchestratorTemplatesThrowsIfTemplatesExist() {
        when(templateDao.listIdsByExtensionId(1L)).thenReturn(Arrays.asList(1L, 2L));
        extensionsManager.checkOrchestratorTemplates(1L);
    }

    @Test
    public void getExtensionsPathReturnsProvisionerPath() {
        when(externalProvisioner.getExtensionsPath()).thenReturn("/tmp/extensions");
        assertEquals("/tmp/extensions", extensionsManager.getExtensionsPath());
    }

    @Test
    public void getExtensionIdForClusterReturnsNullIfNoMap() {
        when(extensionResourceMapDao.findByResourceIdAndType(anyLong(), any())).thenReturn(null);
        assertNull(extensionsManager.getExtensionIdForCluster(1L));
    }

    @Test
    public void getExtensionIdForClusterReturnsIdIfMapExists() {
        ExtensionResourceMapVO map = mock(ExtensionResourceMapVO.class);
        when(map.getExtensionId()).thenReturn(5L);
        when(extensionResourceMapDao.findByResourceIdAndType(anyLong(), any())).thenReturn(map);
        assertEquals(Long.valueOf(5L), extensionsManager.getExtensionIdForCluster(1L));
    }

    @Test
    public void getExtensionReturnsExtension() {
        ExtensionVO ext = mock(ExtensionVO.class);
        when(extensionDao.findById(1L)).thenReturn(ext);
        assertEquals(ext, extensionsManager.getExtension(1L));
    }

    @Test
    public void getExtensionForClusterReturnsNullIfNoId() {
        when(extensionResourceMapDao.findByResourceIdAndType(anyLong(), any())).thenReturn(null);
        assertNull(extensionsManager.getExtensionForCluster(1L));
    }

    @Test
    public void getExtensionForClusterReturnsExtensionIfIdExists() {
        ExtensionResourceMapVO map = mock(ExtensionResourceMapVO.class);
        when(map.getExtensionId()).thenReturn(5L);
        when(extensionResourceMapDao.findByResourceIdAndType(anyLong(), any())).thenReturn(map);
        ExtensionVO ext = mock(ExtensionVO.class);
        when(extensionDao.findById(5L)).thenReturn(ext);
        assertEquals(ext, extensionsManager.getExtensionForCluster(1L));
    }

    @Test
    public void testCreateExtension_Success() {
        CreateExtensionCmd cmd = mock(CreateExtensionCmd.class);
        when(cmd.getName()).thenReturn("ext1");
        when(cmd.getDescription()).thenReturn("desc");
        when(cmd.getType()).thenReturn("Orchestrator");
        when(cmd.getEntryPoint()).thenReturn(null);
        when(cmd.isOrchestratorRequiresPrepareVm()).thenReturn(null);
        when(cmd.getState()).thenReturn(null);
        when(extensionDao.findByName("ext1")).thenReturn(null);
        when(extensionDao.persist(any())).thenAnswer(inv -> {
            ExtensionVO extensionVO = inv.getArgument(0);
            ReflectionTestUtils.setField(extensionVO, "id", 1L);
            return extensionVO;
        });
        when(managementServerHostDao.listBy(any())).thenReturn(Collections.emptyList());

        Extension ext = extensionsManager.createExtension(cmd);

        assertEquals("ext1", ext.getName());
        verify(extensionDao).persist(any());
    }

    @Test
    public void testCreateExtension_DuplicateName() {
        CreateExtensionCmd cmd = mock(CreateExtensionCmd.class);
        when(cmd.getName()).thenReturn("ext1");
        when(extensionDao.findByName("ext1")).thenReturn(mock(ExtensionVO.class));

        assertThrows(CloudRuntimeException.class, () -> extensionsManager.createExtension(cmd));
    }

    @Test
    public void prepareExtensionEntryPointAcrossServersReturnsTrueWhenAllServersSucceed() {
        Extension ext = mock(Extension.class);
        when(ext.getName()).thenReturn("ext");
        when(ext.isUserDefined()).thenReturn(true);
        when(ext.getRelativeEntryPoint()).thenReturn("entry.sh");
        when(ext.getId()).thenReturn(1L);
        when(ext.isEntryPointReady()).thenReturn(false);

        ManagementServerHostVO msHost1 = mock(ManagementServerHostVO.class);
        ManagementServerHostVO msHost2 = mock(ManagementServerHostVO.class);
        when(msHost1.getMsid()).thenReturn(100L);
        when(msHost2.getMsid()).thenReturn(200L);

        when(managementServerHostDao.listBy(any())).thenReturn(Arrays.asList(msHost1, msHost2));

        try (MockedStatic<ManagementServerNode> managementServerNodeMockedStatic = Mockito.mockStatic(ManagementServerNode.class)) {
            managementServerNodeMockedStatic.when(ManagementServerNode::getManagementServerId).thenReturn(101L);
            doReturn(new Pair<>(true, "ok")).when(extensionsManager).prepareExtensionEntryPointOnCurrentServer(anyString(), anyBoolean(), anyString());
            doReturn(true).when(extensionsManager).prepareExtensionEntryPointOnMSPeer(eq(ext), eq(msHost2));

            // Simulate current server is msHost1
            when(msHost1.getMsid()).thenReturn(101L);

            // Extension entry point ready state should be updated
            ExtensionVO updateExt = mock(ExtensionVO.class);
            when(extensionDao.createForUpdate(1L)).thenReturn(updateExt);
            when(extensionDao.update(1L, updateExt)).thenReturn(true);

            boolean result = extensionsManager.prepareExtensionEntryPointAcrossServers(ext);
            assertTrue(result);
            verify(extensionDao).update(1L, updateExt);
        }
    }

    @Test
    public void prepareExtensionEntryPointAcrossServersReturnsFalseWhenAnyServerFails() {
        Extension ext = mock(Extension.class);
        when(ext.getName()).thenReturn("ext");
        when(ext.isUserDefined()).thenReturn(true);
        when(ext.getRelativeEntryPoint()).thenReturn("entry.sh");
        when(ext.getId()).thenReturn(1L);
        when(ext.isEntryPointReady()).thenReturn(true);

        ManagementServerHostVO msHost1 = mock(ManagementServerHostVO.class);
        ManagementServerHostVO msHost2 = mock(ManagementServerHostVO.class);
        when(msHost1.getMsid()).thenReturn(101L);
        when(msHost2.getMsid()).thenReturn(200L);

        when(managementServerHostDao.listBy(any())).thenReturn(Arrays.asList(msHost1, msHost2));

        try (MockedStatic<ManagementServerNode> managementServerNodeMockedStatic = Mockito.mockStatic(ManagementServerNode.class)) {
            managementServerNodeMockedStatic.when(ManagementServerNode::getManagementServerId).thenReturn(101L);
            doReturn(new Pair<>(true, "ok")).when(extensionsManager).prepareExtensionEntryPointOnCurrentServer(anyString(), anyBoolean(), anyString());
            doReturn(false).when(extensionsManager).prepareExtensionEntryPointOnMSPeer(eq(ext), eq(msHost2));

            ExtensionVO updateExt = mock(ExtensionVO.class);
            when(extensionDao.createForUpdate(1L)).thenReturn(updateExt);
            when(extensionDao.update(1L, updateExt)).thenReturn(true);

            boolean result = extensionsManager.prepareExtensionEntryPointAcrossServers(ext);
            assertFalse(result);
            verify(extensionDao).update(1L, updateExt);
        }
    }

    @Test
    public void prepareExtensionEntryPointAcrossServersDoesNotUpdateIfStateUnchanged() {
        Extension ext = mock(Extension.class);
        when(ext.getName()).thenReturn("ext");
        when(ext.isUserDefined()).thenReturn(true);
        when(ext.getRelativeEntryPoint()).thenReturn("entry.sh");
        when(ext.isEntryPointReady()).thenReturn(true);

        ManagementServerHostVO msHost = mock(ManagementServerHostVO.class);
        when(msHost.getMsid()).thenReturn(101L);

        when(managementServerHostDao.listBy(any())).thenReturn(Collections.singletonList(msHost));

        try (MockedStatic<ManagementServerNode> managementServerNodeMockedStatic = Mockito.mockStatic(ManagementServerNode.class)) {
            managementServerNodeMockedStatic.when(ManagementServerNode::getManagementServerId).thenReturn(101L);
            doReturn(new Pair<>(true, "ok")).when(extensionsManager).prepareExtensionEntryPointOnCurrentServer(anyString(), anyBoolean(), anyString());

            boolean result = extensionsManager.prepareExtensionEntryPointAcrossServers(ext);
            assertTrue(result);
            verify(extensionDao, never()).update(anyLong(), any());
        }
    }

    @Test
    public void testListExtensionsReturnsResponses() {
        ListExtensionsCmd cmd = mock(ListExtensionsCmd.class);
        when(cmd.getExtensionId()).thenReturn(null);
        when(cmd.getName()).thenReturn(null);
        when(cmd.getKeyword()).thenReturn(null);
        when(cmd.getStartIndex()).thenReturn(0L);
        when(cmd.getPageSizeVal()).thenReturn(10L);
        when(cmd.getDetails()).thenReturn(null);

        ExtensionVO ext1 = mock(ExtensionVO.class);
        ExtensionVO ext2 = mock(ExtensionVO.class);
        List<ExtensionVO> extList = Arrays.asList(ext1, ext2);
        SearchBuilder<ExtensionVO> sb = mock(SearchBuilder.class);
        when(sb.create()).thenReturn(mock(SearchCriteria.class));
        when(sb.entity()).thenReturn(mock(ExtensionVO.class));
        when(extensionDao.createSearchBuilder()).thenReturn(sb);
        when(extensionDao.searchAndCount(any(), any())).thenReturn(new Pair<>(extList, 2));

        // Spy createExtensionResponse to return a dummy response
        ExtensionResponse resp1 = mock(ExtensionResponse.class);
        ExtensionResponse resp2 = mock(ExtensionResponse.class);
        doReturn(resp1).when(extensionsManager).createExtensionResponse(eq(ext1), any());
        doReturn(resp2).when(extensionsManager).createExtensionResponse(eq(ext2), any());

        List<ExtensionResponse> result = extensionsManager.listExtensions(cmd);

        assertEquals(2, result.size());
        assertTrue(result.contains(resp1));
        assertTrue(result.contains(resp2));
    }

    @Test
    public void testListExtensionsWithId() {
        ListExtensionsCmd cmd = mock(ListExtensionsCmd.class);
        when(cmd.getExtensionId()).thenReturn(42L);
        when(cmd.getName()).thenReturn(null);
        when(cmd.getKeyword()).thenReturn(null);
        when(cmd.getStartIndex()).thenReturn(0L);
        when(cmd.getPageSizeVal()).thenReturn(10L);
        when(cmd.getDetails()).thenReturn(null);

        ExtensionVO ext = mock(ExtensionVO.class);
        SearchBuilder<ExtensionVO> sb = mock(SearchBuilder.class);
        when(sb.create()).thenReturn(mock(SearchCriteria.class));
        when(sb.entity()).thenReturn(mock(ExtensionVO.class));
        when(extensionDao.createSearchBuilder()).thenReturn(sb);
        when(extensionDao.searchAndCount(any(), any())).thenReturn(new Pair<>(Collections.singletonList(ext), 1));
        ExtensionResponse resp = mock(ExtensionResponse.class);
        doReturn(resp).when(extensionsManager).createExtensionResponse(eq(ext), any());

        List<ExtensionResponse> result = extensionsManager.listExtensions(cmd);

        assertEquals(1, result.size());
        assertEquals(resp, result.get(0));
    }

    @Test
    public void testListExtensionsWithNameAndKeyword() {
        ListExtensionsCmd cmd = mock(ListExtensionsCmd.class);
        when(cmd.getExtensionId()).thenReturn(null);
        when(cmd.getName()).thenReturn("testName");
        when(cmd.getKeyword()).thenReturn("key");
        when(cmd.getStartIndex()).thenReturn(0L);
        when(cmd.getPageSizeVal()).thenReturn(10L);
        when(cmd.getDetails()).thenReturn(null);

        ExtensionVO ext = mock(ExtensionVO.class);
        SearchBuilder<ExtensionVO> sb = mock(SearchBuilder.class);
        when(sb.create()).thenReturn(mock(SearchCriteria.class));
        when(sb.entity()).thenReturn(mock(ExtensionVO.class));
        when(extensionDao.createSearchBuilder()).thenReturn(sb);
        when(extensionDao.searchAndCount(any(), any())).thenReturn(new Pair<>(Collections.singletonList(ext), 1));
        ExtensionResponse resp = mock(ExtensionResponse.class);
        doReturn(resp).when(extensionsManager).createExtensionResponse(eq(ext), any());

        List<ExtensionResponse> result = extensionsManager.listExtensions(cmd);

        assertEquals(1, result.size());
        assertEquals(resp, result.get(0));
    }

    @Test
    public void testUpdateExtension_SuccessfulDescriptionUpdate() {
        UpdateExtensionCmd cmd = mock(UpdateExtensionCmd.class);
        when(cmd.getId()).thenReturn(1L);
        when(cmd.getDescription()).thenReturn("new desc");
        when(cmd.isOrchestratorRequiresPrepareVm()).thenReturn(null);
        when(cmd.getState()).thenReturn(null);
        when(cmd.getDetails()).thenReturn(null);
        when(cmd.isCleanupDetails()).thenReturn(false);

        ExtensionVO ext = mock(ExtensionVO.class);
        when(ext.getDescription()).thenReturn("old desc");
        when(extensionDao.findById(1L)).thenReturn(ext);
        when(extensionDao.update(1L, ext)).thenReturn(true);

        Extension result = extensionsManager.updateExtension(cmd);

        assertEquals(ext, result);
        verify(ext).setDescription("new desc");
        verify(extensionDao, atLeastOnce()).update(1L, ext);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testUpdateExtension_NotFound() {
        UpdateExtensionCmd cmd = mock(UpdateExtensionCmd.class);
        when(cmd.getId()).thenReturn(2L);
        when(extensionDao.findById(2L)).thenReturn(null);

        extensionsManager.updateExtension(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testUpdateExtension_InvalidOrchestratorFlag() {
        UpdateExtensionCmd cmd = mock(UpdateExtensionCmd.class);
        when(cmd.getId()).thenReturn(3L);
        when(cmd.isOrchestratorRequiresPrepareVm()).thenReturn(true);

        ExtensionVO ext = mock(ExtensionVO.class);
        when(ext.getType()).thenReturn(null);
        when(extensionDao.findById(3L)).thenReturn(ext);

        extensionsManager.updateExtension(cmd);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testUpdateExtension_UpdateFails() {
        UpdateExtensionCmd cmd = mock(UpdateExtensionCmd.class);
        when(cmd.getId()).thenReturn(4L);
        when(cmd.getDescription()).thenReturn("desc");
        when(cmd.isOrchestratorRequiresPrepareVm()).thenReturn(null);
        when(cmd.getState()).thenReturn(null);
        when(cmd.getDetails()).thenReturn(null);
        when(cmd.isCleanupDetails()).thenReturn(false);

        ExtensionVO ext = mock(ExtensionVO.class);
        when(ext.getDescription()).thenReturn("old");
        when(extensionDao.findById(4L)).thenReturn(ext);
        when(extensionDao.update(4L, ext)).thenReturn(false);

        extensionsManager.updateExtension(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testUpdateExtension_InvalidState() {
        UpdateExtensionCmd cmd = mock(UpdateExtensionCmd.class);
        when(cmd.getId()).thenReturn(5L);
        when(cmd.getState()).thenReturn("NonExistentState");

        ExtensionVO ext = mock(ExtensionVO.class);
        when(ext.getType()).thenReturn(Extension.Type.Orchestrator);
        when(ext.getState()).thenReturn(Extension.State.Enabled);
        when(extensionDao.findById(5L)).thenReturn(ext);

        extensionsManager.updateExtension(cmd);
    }

    @Test
    public void testDeleteExtension_Success() {
        DeleteExtensionCmd cmd = mock(DeleteExtensionCmd.class);
        when(cmd.getId()).thenReturn(1L);
        when(cmd.isCleanup()).thenReturn(false);
        ExtensionVO ext = mock(ExtensionVO.class);
        when(ext.isUserDefined()).thenReturn(true);
        when(extensionDao.findById(1L)).thenReturn(ext);
        when(extensionResourceMapDao.listByExtensionId(1L)).thenReturn(Collections.emptyList());
        when(extensionCustomActionDao.listIdsByExtensionId(1L)).thenReturn(Collections.emptyList());
        doNothing().when(extensionDetailsDao).removeDetails(1L);
        when(extensionDao.remove(1L)).thenReturn(true);

        assertTrue(extensionsManager.deleteExtension(cmd));
        verify(extensionDao).remove(1L);
    }

    @Test
    public void testRegisterExtensionWithResource_InvalidResourceType() {
        RegisterExtensionCmd cmd = mock(RegisterExtensionCmd.class);
        when(cmd.getResourceType()).thenReturn("InvalidType");

        assertThrows(InvalidParameterValueException.class, () -> extensionsManager.registerExtensionWithResource(cmd));
    }

    @Test
    public void testUnregisterExtensionWithResource_InvalidResourceType() {
        UnregisterExtensionCmd cmd = mock(UnregisterExtensionCmd.class);
        when(cmd.getResourceType()).thenReturn("InvalidType");

        assertThrows(InvalidParameterValueException.class, () -> extensionsManager.unregisterExtensionWithResource(cmd));
    }

    @Test
    public void testCreateExtensionResponse_BasicFields() {
        Extension extension = mock(Extension.class);
        when(extension.getUuid()).thenReturn("uuid-1");
        when(extension.getName()).thenReturn("ext1");
        when(extension.getDescription()).thenReturn("desc");
        when(extension.getType()).thenReturn(Extension.Type.Orchestrator);
        when(extension.getCreated()).thenReturn(new Date());
        when(extension.getRelativeEntryPoint()).thenReturn("entry.sh");
        when(extension.isEntryPointReady()).thenReturn(true);
        when(extension.isUserDefined()).thenReturn(true);
        when(extension.getState()).thenReturn(Extension.State.Enabled);
        when(extension.getId()).thenReturn(1L);

        // Mock externalProvisioner
        when(externalProvisioner.getExtensionEntryPoint("entry.sh")).thenReturn("/some/path/entry.sh");

        // Mock detailsDao
        Pair<Map<String, String>, Map<String, String>> detailsPair = new Pair<>(Map.of("foo", "bar"),
                Map.of(ApiConstants.ORCHESTRATOR_REQUIRES_PREPARE_VM, "true"));
        when(extensionDetailsDao.listDetailsKeyPairsWithVisibility(1L)).thenReturn(detailsPair);

        EnumSet<ApiConstants.ExtensionDetails> viewDetails = EnumSet.of(ApiConstants.ExtensionDetails.all);

        ExtensionResponse response = extensionsManager.createExtensionResponse(extension, viewDetails);

        assertEquals("uuid-1", response.getId());
        assertEquals("ext1", response.getName());
        assertEquals("desc", response.getDescription());
        assertEquals("Orchestrator", response.getType());
        assertEquals("/some/path/entry.sh", response.getEntryPoint());
        assertTrue(response.isEntryPointReady());
        assertTrue(response.isUserDefined());
        assertEquals("Enabled", response.getState());
        assertEquals("bar", response.getDetails().get("foo"));
        assertTrue(response.isOrchestratorRequiresPrepareVm());
        assertEquals("extension", response.getObjectName());
    }

    @Test
    public void testCreateExtensionResponse_HiddenDetailsOnly() {
        Extension extension = mock(Extension.class);
        when(extension.getUuid()).thenReturn("uuid-2");
        when(extension.getName()).thenReturn("ext2");
        when(extension.getDescription()).thenReturn("desc2");
        when(extension.getType()).thenReturn(Extension.Type.Orchestrator);
        when(extension.getCreated()).thenReturn(new Date());
        when(extension.getRelativeEntryPoint()).thenReturn("entry2.sh");
        when(extension.isEntryPointReady()).thenReturn(false);
        when(extension.isUserDefined()).thenReturn(false);
        when(extension.getState()).thenReturn(Extension.State.Disabled);
        when(extension.getId()).thenReturn(2L);

        when(externalProvisioner.getExtensionEntryPoint("entry2.sh")).thenReturn("/some/path/entry2.sh");

        Map<String, String> hiddenDetails = Map.of(ApiConstants.ORCHESTRATOR_REQUIRES_PREPARE_VM, "false");
        when(extensionDetailsDao.listDetailsKeyPairs(2L, List.of(ApiConstants.ORCHESTRATOR_REQUIRES_PREPARE_VM)))
                .thenReturn(hiddenDetails);

        EnumSet<ApiConstants.ExtensionDetails> viewDetails = EnumSet.noneOf(ApiConstants.ExtensionDetails.class);

        ExtensionResponse response = extensionsManager.createExtensionResponse(extension, viewDetails);

        assertEquals("uuid-2", response.getId());
        assertEquals("ext2", response.getName());
        assertEquals("desc2", response.getDescription());
        assertEquals("Orchestrator", response.getType());
        assertEquals("/some/path/entry2.sh", response.getEntryPoint());
        assertFalse(response.isEntryPointReady());
        assertFalse(response.isUserDefined());
        assertEquals("Disabled", response.getState());
        assertFalse(response.isOrchestratorRequiresPrepareVm());
        assertEquals("extension", response.getObjectName());
    }

    @Test
    public void testAddCustomAction_Success() {
        AddCustomActionCmd cmd = mock(AddCustomActionCmd.class);
        when(cmd.getName()).thenReturn("action1");
        when(cmd.getDescription()).thenReturn("desc");
        when(cmd.getExtensionId()).thenReturn(1L);
        when(cmd.getResourceType()).thenReturn("VirtualMachine");
        when(cmd.getAllowedRoleTypes()).thenReturn(List.of("Admin"));
        when(cmd.getTimeout()).thenReturn(5);
        when(cmd.isEnabled()).thenReturn(true);
        when(cmd.getParametersMap()).thenReturn(null);
        when(cmd.getSuccessMessage()).thenReturn("ok");
        when(cmd.getErrorMessage()).thenReturn("fail");
        when(cmd.getDetails()).thenReturn(null);

        when(extensionCustomActionDao.findByNameAndExtensionId(1L, "action1")).thenReturn(null);
        ExtensionVO ext = mock(ExtensionVO.class);
        when(extensionDao.findById(1L)).thenReturn(ext);

        ExtensionCustomActionVO actionVO = mock(ExtensionCustomActionVO.class);
        when(extensionCustomActionDao.persist(any())).thenReturn(actionVO);

        ExtensionCustomAction result = extensionsManager.addCustomAction(cmd);

        assertEquals(actionVO, result);
        verify(extensionCustomActionDao).persist(any());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testAddCustomAction_DuplicateName() {
        AddCustomActionCmd cmd = mock(AddCustomActionCmd.class);
        when(cmd.getName()).thenReturn("action1");
        when(cmd.getExtensionId()).thenReturn(1L);
        when(extensionCustomActionDao.findByNameAndExtensionId(1L, "action1")).thenReturn(mock(ExtensionCustomActionVO.class));

        extensionsManager.addCustomAction(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testAddCustomAction_ExtensionNotFound() {
        AddCustomActionCmd cmd = mock(AddCustomActionCmd.class);
        when(cmd.getName()).thenReturn("action1");
        when(cmd.getExtensionId()).thenReturn(2L);
        when(extensionCustomActionDao.findByNameAndExtensionId(2L, "action1")).thenReturn(null);
        when(extensionDao.findById(2L)).thenReturn(null);

        extensionsManager.addCustomAction(cmd);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testAddCustomAction_InvalidResourceType() {
        AddCustomActionCmd cmd = mock(AddCustomActionCmd.class);
        when(cmd.getName()).thenReturn("action1");
        when(cmd.getExtensionId()).thenReturn(1L);
        when(cmd.getResourceType()).thenReturn("InvalidType");
        when(extensionCustomActionDao.findByNameAndExtensionId(1L, "action1")).thenReturn(null);
        ExtensionVO ext = mock(ExtensionVO.class);
        when(extensionDao.findById(1L)).thenReturn(ext);

        extensionsManager.addCustomAction(cmd);
    }

    @Test
    public void deleteCustomAction_RemovesActionAndDetails_ReturnsTrue() {
        long actionId = 10L;
        DeleteCustomActionCmd cmd = mock(DeleteCustomActionCmd.class);
        when(cmd.getId()).thenReturn(actionId);

        ExtensionCustomActionVO actionVO = mock(ExtensionCustomActionVO.class);
        when(extensionCustomActionDao.findById(actionId)).thenReturn(actionVO);
        when(extensionCustomActionDao.remove(actionId)).thenReturn(true);

        boolean result = extensionsManager.deleteCustomAction(cmd);

        assertTrue(result);
        verify(extensionCustomActionDetailsDao).removeDetails(actionId);
        verify(extensionCustomActionDao).remove(actionId);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void deleteCustomAction_ActionNotFound() {
        long actionId = 20L;
        DeleteCustomActionCmd cmd = mock(DeleteCustomActionCmd.class);
        when(cmd.getId()).thenReturn(actionId);
        when(extensionCustomActionDao.findById(actionId)).thenReturn(null);
        extensionsManager.deleteCustomAction(cmd);
        verify(extensionCustomActionDetailsDao, never()).removeDetails(anyLong());
        verify(extensionCustomActionDao, never()).remove(anyLong());
    }

    @Test(expected = CloudRuntimeException.class)
    public void deleteCustomAction_RemoveFails() {
        long actionId = 30L;
        DeleteCustomActionCmd cmd = mock(DeleteCustomActionCmd.class);
        when(cmd.getId()).thenReturn(actionId);
        ExtensionCustomActionVO actionVO = mock(ExtensionCustomActionVO.class);
        when(extensionCustomActionDao.findById(actionId)).thenReturn(actionVO);
        when(extensionCustomActionDao.remove(actionId)).thenReturn(false);
        extensionsManager.deleteCustomAction(cmd);
        verify(extensionCustomActionDetailsDao).removeDetails(actionId);
        verify(extensionCustomActionDao).remove(actionId);
    }

    @Test
    public void testListCustomActions_ReturnsResponses() {
        ListCustomActionCmd cmd = mock(ListCustomActionCmd.class);
        when(cmd.getId()).thenReturn(null);
        when(cmd.getName()).thenReturn(null);
        when(cmd.getExtensionId()).thenReturn(1L);
        when(cmd.getKeyword()).thenReturn(null);
        when(cmd.getResourceType()).thenReturn(null);
        when(cmd.getResourceId()).thenReturn(null);
        when(cmd.isEnabled()).thenReturn(null);
        when(cmd.getStartIndex()).thenReturn(0L);
        when(cmd.getPageSizeVal()).thenReturn(10L);

        ExtensionCustomActionVO action1 = mock(ExtensionCustomActionVO.class);
        ExtensionCustomActionVO action2 = mock(ExtensionCustomActionVO.class);
        List<ExtensionCustomActionVO> actions = Arrays.asList(action1, action2);
        SearchBuilder<ExtensionCustomActionVO> sb = mock(SearchBuilder.class);
        when(sb.create()).thenReturn(mock(SearchCriteria.class));
        when(sb.entity()).thenReturn(mock(ExtensionCustomActionVO.class));
        when(extensionCustomActionDao.createSearchBuilder()).thenReturn(sb);
        when(extensionCustomActionDao.searchAndCount(any(), any())).thenReturn(new Pair<>(actions, 2));

        ExtensionCustomActionResponse resp1 = mock(ExtensionCustomActionResponse.class);
        ExtensionCustomActionResponse resp2 = mock(ExtensionCustomActionResponse.class);
        doReturn(resp1).when(extensionsManager).createCustomActionResponse(eq(action1));
        doReturn(resp2).when(extensionsManager).createCustomActionResponse(eq(action2));

        List<ExtensionCustomActionResponse> result = extensionsManager.listCustomActions(cmd);

        assertEquals(2, result.size());
        assertTrue(result.contains(resp1));
        assertTrue(result.contains(resp2));
    }

    @Test
    public void testUpdateCustomAction_UpdatesFields() {
        long actionId = 1L;
        String newDescription = "Updated description";
        String newResourceType = "VirtualMachine";
        List<String> newRoles = List.of("Admin", "User");
        Boolean enabled = true;
        int timeout = 10;
        String successMsg = "Success!";
        String errorMsg = "Error!";
        Map<String, String> details = Map.of("key", "value");

        UpdateCustomActionCmd cmd = mock(UpdateCustomActionCmd.class);
        when(cmd.getId()).thenReturn(actionId);
        when(cmd.getDescription()).thenReturn(newDescription);
        when(cmd.getResourceType()).thenReturn(newResourceType);
        when(cmd.getAllowedRoleTypes()).thenReturn(newRoles);
        when(cmd.isEnabled()).thenReturn(enabled);
        when(cmd.getTimeout()).thenReturn(timeout);
        when(cmd.getSuccessMessage()).thenReturn(successMsg);
        when(cmd.getErrorMessage()).thenReturn(errorMsg);
        when(cmd.getParametersMap()).thenReturn(null);
        when(cmd.isCleanupParameters()).thenReturn(false);
        when(cmd.getDetails()).thenReturn(details);
        when(cmd.isCleanupDetails()).thenReturn(false);

        ExtensionCustomActionVO actionVO = new ExtensionCustomActionVO();
        ReflectionTestUtils.setField(actionVO, "id", 1L);
        when(extensionCustomActionDao.findById(actionId)).thenReturn(actionVO);
        when(extensionCustomActionDao.update(eq(actionId), any())).thenReturn(true);

        when(extensionCustomActionDetailsDao.listDetailsKeyPairs(eq(actionId), eq(false)))
                .thenReturn(new HashMap<>());

        ExtensionCustomAction result = extensionsManager.updateCustomAction(cmd);

        assertEquals(newDescription, result.getDescription());
        assertEquals(successMsg, result.getSuccessMessage());
        assertEquals(errorMsg, result.getErrorMessage());
        assertEquals(timeout, result.getTimeout());
        assertTrue(result.isEnabled());
        assertEquals(ExtensionCustomAction.ResourceType.VirtualMachine, result.getResourceType());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testUpdateCustomAction_ActionNotFound_ThrowsException() {
        UpdateCustomActionCmd cmd = mock(UpdateCustomActionCmd.class);
        when(cmd.getId()).thenReturn(99L);
        when(extensionCustomActionDao.findById(99L)).thenReturn(null);

        extensionsManager.updateCustomAction(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testUpdateCustomAction_InvalidResourceType_ThrowsException() {
        UpdateCustomActionCmd cmd = mock(UpdateCustomActionCmd.class);
        when(cmd.getId()).thenReturn(1L);
        ExtensionCustomActionVO action = mock(ExtensionCustomActionVO.class);
        when(extensionCustomActionDao.findById(1L)).thenReturn(action);
        when(cmd.getResourceType()).thenReturn("InvalidType");

        extensionsManager.updateCustomAction(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testUpdateCustomAction_InvalidRoleType_ThrowsException() {
        UpdateCustomActionCmd cmd = mock(UpdateCustomActionCmd.class);
        when(cmd.getId()).thenReturn(1L);
        ExtensionCustomActionVO action = mock(ExtensionCustomActionVO.class);
        when(extensionCustomActionDao.findById(1L)).thenReturn(action);
        when(cmd.getAllowedRoleTypes()).thenReturn(List.of("NotARole"));

        extensionsManager.updateCustomAction(cmd);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testUpdateCustomAction_DaoUpdateFails_ThrowsException() {
        UpdateCustomActionCmd cmd = mock(UpdateCustomActionCmd.class);
        when(cmd.getId()).thenReturn(1L);
        when(cmd.getDescription()).thenReturn("desc");
        ExtensionCustomActionVO action = mock(ExtensionCustomActionVO.class);
        when(extensionCustomActionDao.findById(1L)).thenReturn(action);
        when(extensionCustomActionDao.update(eq(1L), any())).thenReturn(false);

        extensionsManager.updateCustomAction(cmd);
    }

    @Test
    public void runCustomAction_SuccessfulExecution_ReturnsExpectedResult() throws Exception {
        RunCustomActionCmd cmd = mock(RunCustomActionCmd.class);
        when(cmd.getCustomActionId()).thenReturn(1L);
        when(cmd.getResourceId()).thenReturn("vm-123");
        when(cmd.getParameters()).thenReturn(Map.of("param1", "value1"));

        ExtensionCustomActionVO actionVO = mock(ExtensionCustomActionVO.class);
        when(extensionCustomActionDao.findById(1L)).thenReturn(actionVO);
        when(actionVO.isEnabled()).thenReturn(true);
        when(actionVO.getResourceType()).thenReturn(ExtensionCustomAction.ResourceType.VirtualMachine);

        ExtensionVO extensionVO = mock(ExtensionVO.class);
        when(extensionDao.findById(anyLong())).thenReturn(extensionVO);
        when(extensionVO.getState()).thenReturn(Extension.State.Enabled);

        RunCustomActionAnswer answer = mock(RunCustomActionAnswer.class);
        when(answer.getResult()).thenReturn(true);

        VirtualMachine vm = mock(VirtualMachine.class);
        when(vm.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.External);
        when(entityManager.findByUuid(eq(VirtualMachine.class), anyString())).thenReturn(vm);
        when(virtualMachineManager.findClusterAndHostIdForVm(vm, false)).thenReturn(new Pair<>(1L, 1L));

        when(extensionResourceMapDao.findByResourceIdAndType(1L, ExtensionResourceMap.ResourceType.Cluster)).thenReturn(mock(ExtensionResourceMapVO.class));
        when(extensionCustomActionDetailsDao.listDetailsKeyPairsWithVisibility(anyLong())).thenReturn(new Pair<>(new HashMap<>(), new HashMap<>()));

        when(agentMgr.send(anyLong(), any(Command.class))).thenReturn(answer);

        CustomActionResultResponse result = extensionsManager.runCustomAction(cmd);

        assertTrue(result.getSuccess());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void runCustomAction_ActionNotFound_ThrowsException() {
        RunCustomActionCmd cmd = mock(RunCustomActionCmd.class);
        when(cmd.getCustomActionId()).thenReturn(99L);
        when(extensionCustomActionDao.findById(99L)).thenReturn(null);

        extensionsManager.runCustomAction(cmd);
    }

    @Test(expected = CloudRuntimeException.class)
    public void runCustomAction_ActionDisabled_ThrowsException() {
        RunCustomActionCmd cmd = mock(RunCustomActionCmd.class);
        when(cmd.getCustomActionId()).thenReturn(2L);

        ExtensionCustomActionVO actionVO = mock(ExtensionCustomActionVO.class);
        when(extensionCustomActionDao.findById(2L)).thenReturn(actionVO);
        when(actionVO.isEnabled()).thenReturn(false);

        extensionsManager.runCustomAction(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void runCustomAction_InvalidResourceType_ThrowsException() {
        RunCustomActionCmd cmd = mock(RunCustomActionCmd.class);
        when(cmd.getCustomActionId()).thenReturn(3L);

        ExtensionCustomActionVO actionVO = mock(ExtensionCustomActionVO.class);
        when(extensionCustomActionDao.findById(3L)).thenReturn(actionVO);
        when(actionVO.isEnabled()).thenReturn(true);
        when(actionVO.getResourceType()).thenReturn(null);
        when(actionVO.getExtensionId()).thenReturn(1L);
        ExtensionVO extensionVO = mock(ExtensionVO.class);
        when(extensionVO.getState()).thenReturn(Extension.State.Enabled);
        when(extensionDao.findById(1L)).thenReturn(extensionVO);

        extensionsManager.runCustomAction(cmd);
    }

    @Test
    public void runCustomAction_ExecutionThrowsException() throws Exception {
        RunCustomActionCmd cmd = mock(RunCustomActionCmd.class);
        when(cmd.getCustomActionId()).thenReturn(1L);
        when(cmd.getResourceId()).thenReturn("vm-123");
        when(cmd.getParameters()).thenReturn(Map.of("param1", "value1"));

        ExtensionCustomActionVO actionVO = mock(ExtensionCustomActionVO.class);
        when(extensionCustomActionDao.findById(1L)).thenReturn(actionVO);
        when(actionVO.isEnabled()).thenReturn(true);
        when(actionVO.getResourceType()).thenReturn(ExtensionCustomAction.ResourceType.VirtualMachine);

        ExtensionVO extensionVO = mock(ExtensionVO.class);
        when(extensionDao.findById(anyLong())).thenReturn(extensionVO);
        when(extensionVO.getState()).thenReturn(Extension.State.Enabled);

        VirtualMachine vm = mock(VirtualMachine.class);
        when(vm.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.External);
        when(entityManager.findByUuid(eq(VirtualMachine.class), anyString())).thenReturn(vm);
        when(virtualMachineManager.findClusterAndHostIdForVm(vm, false)).thenReturn(new Pair<>(1L, 1L));

        when(extensionResourceMapDao.findByResourceIdAndType(1L, ExtensionResourceMap.ResourceType.Cluster)).thenReturn(mock(ExtensionResourceMapVO.class));
        when(extensionCustomActionDetailsDao.listDetailsKeyPairsWithVisibility(anyLong())).thenReturn(new Pair<>(new HashMap<>(), new HashMap<>()));

        when(agentMgr.send(anyLong(), any(Command.class))).thenThrow(OperationTimedoutException.class);

        CustomActionResultResponse result = extensionsManager.runCustomAction(cmd);

        assertFalse(result.getSuccess());
    }

    @Test
    public void createCustomActionResponse_SetsBasicFields() {
        ExtensionCustomAction action = mock(ExtensionCustomAction.class);
        when(action.getUuid()).thenReturn("uuid-1");
        when(action.getName()).thenReturn("action1");
        when(action.getDescription()).thenReturn("desc");
        when(action.getResourceType()).thenReturn(ExtensionCustomAction.ResourceType.VirtualMachine);

        when(extensionCustomActionDetailsDao.listDetailsKeyPairsWithVisibility(anyLong()))
                .thenReturn(new Pair<>(Map.of("foo", "bar"), Map.of()));

        ExtensionCustomActionResponse response = extensionsManager.createCustomActionResponse(action);

        assertEquals("uuid-1", response.getId());
        assertEquals("action1", response.getName());
        assertEquals("desc", response.getDescription());
        assertEquals("VirtualMachine", response.getResourceType());
        assertEquals("bar", response.getDetails().get("foo"));
    }

    @Test
    public void createCustomActionResponse_HandlesNullResourceType() {
        ExtensionCustomAction action = mock(ExtensionCustomAction.class);
        when(action.getUuid()).thenReturn("uuid-2");
        when(action.getName()).thenReturn("action2");
        when(action.getDescription()).thenReturn("desc2");
        when(action.getResourceType()).thenReturn(null);

        when(extensionCustomActionDetailsDao.listDetailsKeyPairsWithVisibility(anyLong()))
                .thenReturn(new Pair<>(Collections.emptyMap(), Collections.emptyMap()));

        ExtensionCustomActionResponse response = extensionsManager.createCustomActionResponse(action);

        assertEquals("uuid-2", response.getId());
        assertNull(response.getResourceType());
        assertTrue(response.getDetails().isEmpty());
    }

    @Test
    public void createCustomActionResponse_ParametersAreSetIfPresent() {
        ExtensionCustomAction action = mock(ExtensionCustomAction.class);
        when(action.getUuid()).thenReturn("uuid-3");
        when(action.getName()).thenReturn("action3");
        when(action.getDescription()).thenReturn("desc3");
        when(action.getResourceType()).thenReturn(ExtensionCustomAction.ResourceType.VirtualMachine);

        Map<String, String> details = Map.of("foo", "bar");
        ExtensionCustomAction.Parameter param = new ExtensionCustomAction.Parameter("param1",
                ExtensionCustomAction.Parameter.Type.STRING, ExtensionCustomAction.Parameter.ValidationFormat.NONE,
                null, false);
        Map<String, String> hidden = Map.of(ApiConstants.PARAMETERS,
                ExtensionCustomAction.Parameter.toJsonFromList(List.of(param)));
        when(extensionCustomActionDetailsDao.listDetailsKeyPairsWithVisibility(anyLong()))
                .thenReturn(new Pair<>(details, hidden));

        ExtensionCustomActionResponse response = extensionsManager.createCustomActionResponse(action);

        assertEquals(ExtensionCustomAction.ResourceType.VirtualMachine.name(), response.getResourceType());
        assertEquals("bar", response.getDetails().get("foo"));
        assertNotNull(response.getParameters());
        assertFalse(response.getParameters().isEmpty());
    }

    @Test
    public void handleExtensionServerCommands_GetChecksumCommand_ReturnsChecksumAnswer() {
        GetExtensionEntryPointChecksumCommand cmd = mock(GetExtensionEntryPointChecksumCommand.class);
        when(cmd.getExtensionName()).thenReturn("ext");
        when(cmd.getExtensionRelativeEntryPointPath()).thenReturn("ext/entry.sh");
        when(extensionsManager.externalProvisioner.getChecksumForExtensionEntryPoint(anyString(), anyString()))
                .thenReturn("checksum123");
        String json = extensionsManager.handleExtensionServerCommands(cmd);
        assertTrue(json.contains("checksum123"));
        assertTrue(json.contains("\"result\":true"));
    }

    @Test
    public void handleExtensionServerCommands_PrepareEntryPointCommand_ReturnsSuccessAnswer() {
        PrepareExtensionEntryPointCommand cmd = mock(PrepareExtensionEntryPointCommand.class);
        when(cmd.getExtensionName()).thenReturn("ext");
        when(cmd.getExtensionRelativeEntryPointPath()).thenReturn("ext/entry.sh");
        when(cmd.isExtensionUserDefined()).thenReturn(true);
        doReturn(new Pair<>(true, "ok")).when(extensionsManager)
                .prepareExtensionEntryPointOnCurrentServer(anyString(), anyBoolean(), anyString());

        String json = extensionsManager.handleExtensionServerCommands(cmd);
        assertTrue(json.contains("\"result\":true"));
        assertTrue(json.contains("ok"));
    }

    @Test
    public void handleExtensionServerCommands_CleanupFilesCommand_ReturnsSuccessAnswer() {
        CleanupExtensionFilesCommand cmd = mock(CleanupExtensionFilesCommand.class);
        when(cmd.getExtensionName()).thenReturn("ext");
        when(cmd.getExtensionRelativeEntryPointPath()).thenReturn("ext/entry.sh");
        doReturn(new Pair<>(true, "cleaned")).when(extensionsManager)
                .cleanupExtensionFilesOnCurrentServer(anyString(), anyString());

        String json = extensionsManager.handleExtensionServerCommands(cmd);
        assertTrue(json.contains("\"result\":true"));
        assertTrue(json.contains("cleaned"));
    }

    @Test
    public void handleExtensionServerCommands_UnsupportedCommand_ReturnsUnsupportedAnswer() {
        ExtensionServerActionBaseCommand cmd = mock(ExtensionServerActionBaseCommand.class);
        when(cmd.getExtensionName()).thenReturn("ext");
        when(cmd.getExtensionRelativeEntryPointPath()).thenReturn("ext/entry.sh");

        String json = extensionsManager.handleExtensionServerCommands(cmd);
        assertTrue(json.contains("Unsupported command"));
        assertTrue(json.contains("\"result\":false"));
    }

    @Test
    public void getExtensionIdForCluster_WhenMappingExists_ReturnsExtensionId() {
        long clusterId = 1L;
        long extensionId = 100L;
        ExtensionResourceMapVO mapVO = mock(ExtensionResourceMapVO.class);
        when(extensionResourceMapDao.findByResourceIdAndType(eq(clusterId), any()))
                .thenReturn(mapVO);
        when(mapVO.getExtensionId()).thenReturn(extensionId);

        Long result = extensionsManager.getExtensionIdForCluster(clusterId);

        assertEquals(Long.valueOf(extensionId), result);
    }

    @Test
    public void getExtensionIdForCluster_WhenNoMappingExists_ReturnsNull() {
        long clusterId = 42L;
        when(extensionResourceMapDao.findByResourceIdAndType(eq(clusterId), any()))
                .thenReturn(null);

        Long result = extensionsManager.getExtensionIdForCluster(clusterId);

        assertNull(result);
    }

    @Test
    public void getExtension_WhenExtensionExists_ReturnsExtension() {
        long id = 1L;
        ExtensionVO ext = mock(ExtensionVO.class);
        when(extensionDao.findById(id)).thenReturn(ext);

        Extension result = extensionsManager.getExtension(id);

        assertEquals(ext, result);
    }

    @Test
    public void getExtension_WhenExtensionDoesNotExist_ReturnsNull() {
        long id = 2L;
        when(extensionDao.findById(id)).thenReturn(null);

        Extension result = extensionsManager.getExtension(id);

        assertNull(result);
    }

    @Test
    public void getExtensionForCluster_WhenMappingExists_ReturnsExtension() {
        long clusterId = 10L;
        long extensionId = 20L;
        ExtensionVO ext = mock(ExtensionVO.class);
        when(extensionsManager.getExtensionIdForCluster(clusterId)).thenReturn(extensionId);
        when(extensionDao.findById(extensionId)).thenReturn(ext);
        Extension result = extensionsManager.getExtensionForCluster(clusterId);
        assertEquals(ext, result);
    }

    @Test
    public void getExtensionForCluster_WhenNoMappingExists_ReturnsNull() {
        long clusterId = 10L;
        when(extensionsManager.getExtensionIdForCluster(clusterId)).thenReturn(null);
        Extension result = extensionsManager.getExtensionForCluster(clusterId);
        assertNull(result);
    }
}
