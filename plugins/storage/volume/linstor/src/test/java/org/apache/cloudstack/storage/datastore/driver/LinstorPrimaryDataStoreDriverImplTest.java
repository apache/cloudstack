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
package org.apache.cloudstack.storage.datastore.driver;

import com.linbit.linstor.api.ApiException;
import com.linbit.linstor.api.DevelopersApi;
import com.linbit.linstor.api.model.AutoSelectFilter;
import com.linbit.linstor.api.model.ControllerVersion;
import com.linbit.linstor.api.model.LayerType;
import com.linbit.linstor.api.model.ResourceDefinitionCloneRequest;
import com.linbit.linstor.api.model.ResourceGroup;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.storage.datastore.util.LinstorUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LinstorPrimaryDataStoreDriverImplTest {

    private DevelopersApi api;

    @InjectMocks
    private LinstorPrimaryDataStoreDriverImpl linstorPrimaryDataStoreDriver;

    @Before
    public void setUp() {
        api = mock(DevelopersApi.class);
    }

    @Test
    public void testGetEncryptedLayerList() throws ApiException  {
        ResourceGroup dfltRscGrp = new ResourceGroup();
        dfltRscGrp.setName("DfltRscGrp");

        ResourceGroup bCacheRscGrp = new ResourceGroup();
        bCacheRscGrp.setName("BcacheGrp");
        AutoSelectFilter asf = new AutoSelectFilter();
        asf.setLayerStack(Arrays.asList(LayerType.DRBD.name(), LayerType.BCACHE.name(), LayerType.STORAGE.name()));
        asf.setStoragePool("nvmePool");
        bCacheRscGrp.setSelectFilter(asf);

        ResourceGroup encryptedGrp = new ResourceGroup();
        encryptedGrp.setName("EncryptedGrp");
        AutoSelectFilter asf2 = new AutoSelectFilter();
        asf2.setLayerStack(Arrays.asList(LayerType.DRBD.name(), LayerType.LUKS.name(), LayerType.STORAGE.name()));
        asf2.setStoragePool("ssdPool");
        encryptedGrp.setSelectFilter(asf2);

        when(api.resourceGroupList(Collections.singletonList("DfltRscGrp"), Collections.emptyList(), null, null))
                .thenReturn(Collections.singletonList(dfltRscGrp));
        when(api.resourceGroupList(Collections.singletonList("BcacheGrp"), Collections.emptyList(), null, null))
                .thenReturn(Collections.singletonList(bCacheRscGrp));
        when(api.resourceGroupList(Collections.singletonList("EncryptedGrp"), Collections.emptyList(), null, null))
                .thenReturn(Collections.singletonList(encryptedGrp));

        List<LayerType> layers = LinstorUtil.getEncryptedLayerList(api, "DfltRscGrp");
        Assert.assertEquals(Arrays.asList(LayerType.DRBD, LayerType.LUKS, LayerType.STORAGE), layers);

        layers = LinstorUtil.getEncryptedLayerList(api, "BcacheGrp");
        Assert.assertEquals(Arrays.asList(LayerType.DRBD, LayerType.BCACHE, LayerType.LUKS, LayerType.STORAGE), layers);

        layers = LinstorUtil.getEncryptedLayerList(api, "EncryptedGrp");
        Assert.assertEquals(Arrays.asList(LayerType.DRBD, LayerType.LUKS, LayerType.STORAGE), layers);
    }

    private DevelopersApi mockApiWithRestVersion(String restApiVersion) throws ApiException {
        DevelopersApi apiMock = mock(DevelopersApi.class);
        ControllerVersion version = new ControllerVersion();
        version.setRestApiVersion(restApiVersion);
        when(apiMock.controllerVersion()).thenReturn(version);
        return apiMock;
    }

    @Test
    public void testApplyCloneSizeNewController() throws ApiException {
        DevelopersApi newCtrl = mockApiWithRestVersion("1.29.1");
        ResourceDefinitionCloneRequest req = new ResourceDefinitionCloneRequest();

        boolean resizeAfter = LinstorPrimaryDataStoreDriverImpl.applyCloneSize(newCtrl, req, 40L * 1024 * 1024 * 1024);

        Assert.assertFalse(resizeAfter);
        Assert.assertEquals(Collections.singletonList(40L * 1024 * 1024), req.getVolumeSizes());
    }

    @Test
    public void testApplyCloneSizeOldController() throws ApiException {
        DevelopersApi oldCtrl = mockApiWithRestVersion("1.28.0");
        ResourceDefinitionCloneRequest req = new ResourceDefinitionCloneRequest();

        boolean resizeAfter = LinstorPrimaryDataStoreDriverImpl.applyCloneSize(oldCtrl, req, 40L * 1024 * 1024 * 1024);

        Assert.assertTrue(resizeAfter);
        Assert.assertNull(req.getVolumeSizes());
    }

    @Test
    public void testApplyCloneSizeWithoutSize() throws ApiException {
        DevelopersApi newCtrl = mockApiWithRestVersion("1.29.1");
        ResourceDefinitionCloneRequest req = new ResourceDefinitionCloneRequest();

        Assert.assertFalse(LinstorPrimaryDataStoreDriverImpl.applyCloneSize(newCtrl, req, null));
        Assert.assertFalse(LinstorPrimaryDataStoreDriverImpl.applyCloneSize(newCtrl, req, 0L));
        Assert.assertNull(req.getVolumeSizes());
        verify(newCtrl, never()).controllerVersion();
    }
}
