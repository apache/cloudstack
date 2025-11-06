/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProviderManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.Event;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotService;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy.SnapshotOperation;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageStrategyFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateService;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateService.TemplateApiResult;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService.VolumeApiResult;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.storage.LocalHostEndpoint;
import org.apache.cloudstack.storage.MockLocalNfsSecondaryStorageResource;
import org.apache.cloudstack.storage.RemoteHostEndPoint;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Command;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorGuruManager;
import com.cloud.org.Cluster.ClusterType;
import com.cloud.org.Managed.ManagedState;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Snapshot.LocationType;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.UuidUtils;
import com.cloud.utils.component.ComponentContext;

import junit.framework.Assert;

@ContextConfiguration(locations = {"classpath:/storageContext.xml"})
public class SnapshotTest extends CloudStackTestNGBase {
    @Inject
    ImageStoreDao imageStoreDao;
    ImageStoreVO imageStore;
    Long dcId;
    Long clusterId;
    Long podId;
    HostVO host;
    String primaryName = "my primary data store";
    DataStore primaryStore;
    @Inject
    HostDao hostDao;
    @Inject
    TemplateService imageService;
    @Inject
    VolumeService volumeService;
    @Inject
    VMTemplateDao imageDataDao;
    @Inject
    HostPodDao podDao;
    @Inject
    ClusterDao clusterDao;
    @Inject
    DataCenterDao dcDao;
    @Inject
    PrimaryDataStoreDao primaryStoreDao;
    @Inject
    DataStoreProviderManager dataStoreProviderMgr;
    @Inject
    TemplateDataStoreDao templateStoreDao;
    @Inject
    TemplateDataFactory templateFactory;
    @Inject
    PrimaryDataStoreDao primaryDataStoreDao;
    @Inject
    AgentManager agentMgr;
    @Inject
    HypervisorGuruManager hyGuruMgr;
    @Inject
    DataStoreManager dataStoreMgr;
    @Inject
    ResourceManager resourceMgr;
    @Inject
    VolumeDataFactory volFactory;
    @Inject
    SnapshotDataFactory snapshotFactory;
    @Inject
    StorageStrategyFactory storageStrategyFactory;
    @Inject
    List<SnapshotStrategy> snapshotStrategies;
    @Inject
    SnapshotService snapshotSvr;
    @Inject
    SnapshotDao snapshotDao;
    @Inject
    EndPointSelector epSelector;
    @Inject
    VolumeDao volumeDao;

    long primaryStoreId;
    VMTemplateVO image;
    String imageStoreName = "testImageStore";
    RemoteHostEndPoint remoteEp;

    @Test(priority = -1)
    public void setUp() {
        ComponentContext.initComponentsLifeCycle();

        host = hostDao.findByGuid(this.getHostGuid());
        if (host != null) {
            dcId = host.getDataCenterId();
            clusterId = host.getClusterId();
            podId = host.getPodId();
            imageStore = this.imageStoreDao.findByName(imageStoreName);
        } else {
            // create data center
            DataCenterVO dc =
                    new DataCenterVO(UUID.randomUUID().toString(), "test", "8.8.8.8", null, "10.0.0.1", null, "10.0.0.1/24", null, null, NetworkType.Basic, null, null, true,
                            true, null, null);
            dc = dcDao.persist(dc);
            dcId = dc.getId();
            // create pod

            HostPodVO pod = new HostPodVO(UUID.randomUUID().toString(), dc.getId(), this.getHostGateway(), this.getHostCidr(), 8, "test");
            pod = podDao.persist(pod);
            podId = pod.getId();
            // create xenserver cluster
            ClusterVO cluster = new ClusterVO(dc.getId(), pod.getId(), "devcloud cluster");
            cluster.setHypervisorType(this.getHypervisor().toString());
            cluster.setClusterType(ClusterType.CloudManaged);
            cluster.setManagedState(ManagedState.Managed);
            cluster = clusterDao.persist(cluster);
            clusterId = cluster.getId();
            // create xenserver host

            host = new HostVO(this.getHostGuid());
            host.setName("devcloud xenserver host");
            host.setType(Host.Type.Routing);
            host.setPrivateIpAddress(this.getHostIp());
            host.setDataCenterId(dc.getId());
            host.setVersion("6.0.1");
            host.setAvailable(true);
            host.setSetup(true);
            host.setPodId(podId);
            host.setLastPinged(0);
            host.setResourceState(ResourceState.Enabled);
            host.setHypervisorType(this.getHypervisor());
            host.setClusterId(cluster.getId());

            host = hostDao.persist(host);

            imageStore = new ImageStoreVO();
            imageStore.setName(imageStoreName);
            imageStore.setDataCenterId(dcId);
            imageStore.setProviderName(DataStoreProvider.NFS_IMAGE);
            imageStore.setRole(DataStoreRole.Image);
            imageStore.setUrl(this.getSecondaryStorage());
            imageStore.setUuid(UUID.randomUUID().toString());
            imageStore.setProtocol("nfs");
            imageStore = imageStoreDao.persist(imageStore);
        }

        image = new VMTemplateVO();
        image.setTemplateType(TemplateType.USER);
        image.setUrl(this.getTemplateUrl());
        image.setUniqueName(UUID.randomUUID().toString());
        image.setName(UUID.randomUUID().toString());
        image.setPublicTemplate(true);
        image.setFeatured(true);
        image.setRequiresHvm(true);
        image.setBits(64);
        image.setFormat(Storage.ImageFormat.VHD);
        image.setEnablePassword(true);
        image.setEnableSshKey(true);
        image.setGuestOSId(1);
        image.setBootable(true);
        image.setPrepopulate(true);
        image.setCrossZones(true);
        image.setExtractable(true);

        image = imageDataDao.persist(image);

        /*
         * TemplateDataStoreVO templateStore = new TemplateDataStoreVO();
         *
         * templateStore.setDataStoreId(imageStore.getId());
         * templateStore.setDownloadPercent(100);
         * templateStore.setDownloadState(Status.DOWNLOADED);
         * templateStore.setDownloadUrl(imageStore.getUrl());
         * templateStore.setInstallPath(this.getImageInstallPath());
         * templateStore.setTemplateId(image.getId());
         * templateStoreDao.persist(templateStore);
         */

        DataStore store = this.dataStoreMgr.getDataStore(imageStore.getId(), DataStoreRole.Image);
        TemplateInfo template = templateFactory.getTemplate(image.getId(), DataStoreRole.Image);
        DataObject templateOnStore = store.create(template);
        TemplateObjectTO to = new TemplateObjectTO();
        to.setPath(this.getImageInstallPath());
        to.setFormat(ImageFormat.VHD);
        to.setSize(1000L);
        CopyCmdAnswer answer = new CopyCmdAnswer(to);
        templateOnStore.processEvent(Event.CreateOnlyRequested);
        templateOnStore.processEvent(Event.OperationSuccessed, answer);

    }

    @Override
    protected void injectMockito() {
        List<HostVO> hosts = new ArrayList<HostVO>();
        hosts.add(this.host);
        Mockito.when(resourceMgr.listAllUpAndEnabledHosts((Type)Matchers.any(), Matchers.anyLong(), Matchers.anyLong(), Matchers.anyLong())).thenReturn(hosts);
        remoteEp = RemoteHostEndPoint.getHypervisorHostEndPoint(this.host);
        Mockito.when(epSelector.select(Matchers.any(DataObject.class), Matchers.any(DataObject.class))).thenReturn(remoteEp);
        Mockito.when(epSelector.select(Matchers.any(DataObject.class))).thenReturn(remoteEp);
        Mockito.when(epSelector.select(Matchers.any(DataStore.class))).thenReturn(remoteEp);
        Mockito.when(hyGuruMgr.getGuruProcessedCommandTargetHost(Matchers.anyLong(), Matchers.any(Command.class))).thenReturn(this.host.getId());

    }

    public DataStore createPrimaryDataStore() {
        try {
            String uuid = UuidUtils.nameUUIDFromBytes(this.getPrimaryStorageUrl().getBytes()).toString();
            List<StoragePoolVO> pools = primaryDataStoreDao.findPoolByName(this.primaryName);
            if (pools.size() > 0) {
                return this.dataStoreMgr.getPrimaryDataStore(pools.get(0).getId());
            }

            /*
             * DataStoreProvider provider =
             * dataStoreProviderMgr.getDataStoreProvider
             * ("cloudstack primary data store provider"); Map<String, Object>
             * params = new HashMap<String, Object>(); URI uri = new
             * URI(this.getPrimaryStorageUrl()); params.put("url",
             * this.getPrimaryStorageUrl()); params.put("server",
             * uri.getHost()); params.put("path", uri.getPath());
             * params.put("protocol",
             * Storage.StoragePoolType.NetworkFilesystem); params.put("zoneId",
             * dcId); params.put("clusterId", clusterId); params.put("name",
             * this.primaryName); params.put("port", 1); params.put("podId",
             * this.podId); params.put("roles",
             * DataStoreRole.Primary.toString()); params.put("uuid", uuid);
             * params.put("providerName", String.valueOf(provider.getName()));
             *
             * DataStoreLifeCycle lifeCycle = provider.getDataStoreLifeCycle();
             * DataStore store = lifeCycle.initialize(params); ClusterScope
             * scope = new ClusterScope(clusterId, podId, dcId);
             * lifeCycle.attachCluster(store, scope);
             */

            StoragePoolVO pool = new StoragePoolVO();
            pool.setClusterId(clusterId);
            pool.setDataCenterId(dcId);
            URI uri = new URI(this.getPrimaryStorageUrl());
            pool.setHostAddress(uri.getHost());
            pool.setPath(uri.getPath());
            pool.setPort(0);
            pool.setName(this.primaryName);
            pool.setUuid(this.getPrimaryStorageUuid());
            pool.setStatus(StoragePoolStatus.Up);
            pool.setPoolType(StoragePoolType.NetworkFilesystem);
            pool.setPodId(podId);
            pool.setScope(ScopeType.CLUSTER);
            pool.setStorageProviderName(DataStoreProvider.DEFAULT_PRIMARY);
            pool = this.primaryStoreDao.persist(pool);
            DataStore store = this.dataStoreMgr.getPrimaryDataStore(pool.getId());
            return store;
        } catch (Exception e) {
            return null;
        }
    }

    private SnapshotVO createSnapshotInDb(VolumeInfo volume) {
        Snapshot.Type snapshotType = Snapshot.Type.MANUAL;
        SnapshotVO snapshotVO =
                new SnapshotVO(volume.getDataCenterId(), 2, 1, volume.getId(), 1L, UUID.randomUUID().toString(), (short)snapshotType.ordinal(), snapshotType.name(),
                        volume.getSize(), 1L, 100L, HypervisorType.XenServer, LocationType.PRIMARY);
        return this.snapshotDao.persist(snapshotVO);
    }

    private VolumeVO createVolume(Long templateId, long dataStoreId) {
        VolumeVO volume = new VolumeVO(Volume.Type.DATADISK, UUID.randomUUID().toString(), this.dcId, 1L, 1L, 1L, Storage.ProvisioningType.THIN, 1000, 0L, 0L, "");
        volume.setDataCenterId(this.dcId);
        volume.setPoolId(dataStoreId);
        volume = volumeDao.persist(volume);
        return volume;
    }

    public VolumeInfo createCopyBaseImage() throws InterruptedException, ExecutionException {
        DataStore primaryStore = createPrimaryDataStore();
        primaryStoreId = primaryStore.getId();
        primaryStore = this.dataStoreMgr.getPrimaryDataStore(primaryStoreId);
        VolumeVO volume = createVolume(image.getId(), primaryStore.getId());
        VolumeInfo volInfo = this.volFactory.getVolume(volume.getId());
        AsyncCallFuture<VolumeApiResult> future =
                this.volumeService.createVolumeFromTemplateAsync(volInfo, this.primaryStoreId, this.templateFactory.getTemplate(this.image.getId(), DataStoreRole.Image));

        VolumeApiResult result;
        result = future.get();
        Assert.assertTrue(result.isSuccess());
        return result.getVolume();

    }

    private VMTemplateVO createTemplateInDb() {
        VMTemplateVO image = new VMTemplateVO();
        image.setTemplateType(TemplateType.USER);

        image.setUniqueName(UUID.randomUUID().toString());
        image.setName(UUID.randomUUID().toString());
        image.setPublicTemplate(true);
        image.setFeatured(true);
        image.setRequiresHvm(true);
        image.setBits(64);
        image.setFormat(Storage.ImageFormat.VHD);
        image.setEnablePassword(true);
        image.setEnableSshKey(true);
        image.setGuestOSId(1);
        image.setBootable(true);
        image.setPrepopulate(true);
        image.setCrossZones(true);
        image.setExtractable(true);
        image = imageDataDao.persist(image);
        return image;
    }

    @Test
    public void createVolumeFromSnapshot() throws InterruptedException, ExecutionException {
        VolumeInfo vol = createCopyBaseImage();
        SnapshotVO snapshotVO = createSnapshotInDb(vol);
        SnapshotInfo snapshot = this.snapshotFactory.getSnapshot(snapshotVO.getId(), vol.getDataStore());
        boolean result = false;

        SnapshotStrategy snapshotStrategy = storageStrategyFactory.getSnapshotStrategy(snapshot, SnapshotOperation.TAKE);
        if (snapshotStrategy != null) {
            snapshot = snapshotStrategy.takeSnapshot(snapshot);
            result = true;
        }

        AssertJUnit.assertTrue(result);

        VolumeVO volVO = createVolume(vol.getTemplateId(), vol.getPoolId());
        VolumeInfo newVol = this.volFactory.getVolume(volVO.getId());
        AsyncCallFuture<VolumeApiResult> volFuture = this.volumeService.createVolumeFromSnapshot(newVol, newVol.getDataStore(), snapshot);
        VolumeApiResult apiResult = volFuture.get();
        Assert.assertTrue(apiResult.isSuccess());
    }

    @Test
    public void deleteSnapshot() throws InterruptedException, ExecutionException {
        VolumeInfo vol = createCopyBaseImage();
        SnapshotVO snapshotVO = createSnapshotInDb(vol);
        SnapshotInfo snapshot = this.snapshotFactory.getSnapshot(snapshotVO.getId(), vol.getDataStore());
        SnapshotInfo newSnapshot = null;

        SnapshotStrategy snapshotStrategy = storageStrategyFactory.getSnapshotStrategy(snapshot, SnapshotOperation.TAKE);
        if (snapshotStrategy != null) {
            newSnapshot = snapshotStrategy.takeSnapshot(snapshot);

        }
        AssertJUnit.assertNotNull(newSnapshot);

        // create another snapshot
        for (SnapshotStrategy strategy : this.snapshotStrategies) {
            if (strategy.canHandle(snapshot, SnapshotOperation.DELETE) != StrategyPriority.CANT_HANDLE) {
                strategy.deleteSnapshot(newSnapshot.getId());
            }
        }

    }

    @Test
    public void createTemplateFromSnapshot() throws InterruptedException, ExecutionException {
        VolumeInfo vol = createCopyBaseImage();
        SnapshotVO snapshotVO = createSnapshotInDb(vol);
        SnapshotInfo snapshot = this.snapshotFactory.getSnapshot(snapshotVO.getId(), vol.getDataStore());
        boolean result = false;

        SnapshotStrategy snapshotStrategy = storageStrategyFactory.getSnapshotStrategy(snapshot, SnapshotOperation.TAKE);
        if (snapshotStrategy != null) {
            snapshot = snapshotStrategy.takeSnapshot(snapshot);
            result = true;
        }

        AssertJUnit.assertTrue(result);
        LocalHostEndpoint ep = new LocalHostEndpoint();
        ep.setResource(new MockLocalNfsSecondaryStorageResource());
        Mockito.when(epSelector.select(Matchers.any(DataObject.class), Matchers.any(DataObject.class))).thenReturn(ep);

        try {
            VMTemplateVO templateVO = createTemplateInDb();
            TemplateInfo tmpl = this.templateFactory.getTemplate(templateVO.getId(), DataStoreRole.Image);
            DataStore imageStore = this.dataStoreMgr.getImageStore(this.dcId);
            AsyncCallFuture<TemplateApiResult> templateFuture = this.imageService.createTemplateFromSnapshotAsync(snapshot, tmpl, imageStore);
            TemplateApiResult apiResult = templateFuture.get();
            Assert.assertTrue(apiResult.isSuccess());
        } finally {
            Mockito.when(epSelector.select(Matchers.any(DataObject.class), Matchers.any(DataObject.class))).thenReturn(remoteEp);
        }
    }

    @Test
    public void createSnapshot() throws InterruptedException, ExecutionException {
        VolumeInfo vol = createCopyBaseImage();
        SnapshotVO snapshotVO = createSnapshotInDb(vol);
        SnapshotInfo snapshot = this.snapshotFactory.getSnapshot(snapshotVO.getId(), vol.getDataStore());
        SnapshotInfo newSnapshot = null;

        SnapshotStrategy snapshotStrategy = storageStrategyFactory.getSnapshotStrategy(snapshot, SnapshotOperation.TAKE);
        if (snapshotStrategy != null) {
            newSnapshot = snapshotStrategy.takeSnapshot(snapshot);
        }

        AssertJUnit.assertNotNull(newSnapshot);

        LocalHostEndpoint ep = new MockLocalHostEndPoint();
        ep.setResource(new MockLocalNfsSecondaryStorageResource());
        Mockito.when(epSelector.select(Matchers.any(DataStore.class))).thenReturn(ep);

        try {
            for (SnapshotStrategy strategy : this.snapshotStrategies) {
                if (strategy.canHandle(snapshot, SnapshotOperation.DELETE) != StrategyPriority.CANT_HANDLE) {
                    boolean res = strategy.deleteSnapshot(newSnapshot.getId());
                    Assert.assertTrue(res);
                }
            }
        } finally {
            Mockito.when(epSelector.select(Matchers.any(DataStore.class))).thenReturn(remoteEp);
        }
    }

}
