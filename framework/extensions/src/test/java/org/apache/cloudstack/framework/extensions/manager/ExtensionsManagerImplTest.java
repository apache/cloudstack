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
import static org.junit.Assert.assertNull;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.extension.Extension;
import org.apache.cloudstack.extension.ExtensionCustomAction;
import org.apache.cloudstack.extension.ExtensionResourceMap;
import org.apache.cloudstack.framework.extensions.command.PrepareExtensionEntryPointCommand;
import org.apache.cloudstack.framework.extensions.dao.ExtensionCustomActionDao;
import org.apache.cloudstack.framework.extensions.dao.ExtensionCustomActionDetailsDao;
import org.apache.cloudstack.framework.extensions.dao.ExtensionDao;
import org.apache.cloudstack.framework.extensions.dao.ExtensionDetailsDao;
import org.apache.cloudstack.framework.extensions.dao.ExtensionResourceMapDao;
import org.apache.cloudstack.framework.extensions.dao.ExtensionResourceMapDetailsDao;
import org.apache.cloudstack.framework.extensions.vo.ExtensionResourceMapVO;
import org.apache.cloudstack.framework.extensions.vo.ExtensionVO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.alert.AlertManager;
import com.cloud.cluster.ClusterManager;
import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.hypervisor.ExternalProvisioner;
import com.cloud.serializer.GsonHelper;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.utils.Pair;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;
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
}
