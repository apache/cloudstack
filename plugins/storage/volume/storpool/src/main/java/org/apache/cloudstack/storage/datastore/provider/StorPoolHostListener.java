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
package org.apache.cloudstack.storage.datastore.provider;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.util.StorPoolFeaturesAndFixes;
import org.apache.cloudstack.storage.datastore.util.StorPoolHelper;
import org.apache.cloudstack.storage.datastore.util.StorPoolUtil;
import org.apache.cloudstack.storage.datastore.util.StorPoolUtil.SpApiResponse;
import org.apache.cloudstack.storage.datastore.util.StorPoolUtil.SpConnectionDesc;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.StorPoolModifyStoragePoolAnswer;
import com.cloud.agent.api.storage.StorPoolModifyStoragePoolCommand;
import com.cloud.agent.manager.AgentAttache;
import com.cloud.alert.AlertManager;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.exception.StorageConflictException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.kvm.storage.StorPoolStorageAdaptor;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.utils.exception.CloudRuntimeException;

public class StorPoolHostListener implements HypervisorHostListener {
    private static final Logger log = Logger.getLogger(StorPoolHostListener .class);

    @Inject
    private AgentManager agentMgr;
    @Inject
    private DataStoreManager dataStoreMgr;
    @Inject
    private AlertManager alertMgr;
    @Inject
    private StoragePoolHostDao storagePoolHostDao;
    @Inject
    private PrimaryDataStoreDao primaryStoreDao;
    @Inject
    private HostDao hostDao;
    @Inject
    private ClusterDao clusterDao;
    @Inject
    private ClusterDetailsDao clusterDetailsDao;
    @Inject
    private StoragePoolDetailsDao storagePoolDetailsDao;

    @Override
    public boolean hostConnect(long hostId, long poolId) throws StorageConflictException {
        //Will update storage pool's connection details if they aren't updated in DB, before connecting pool to host
        StoragePoolVO poolVO = primaryStoreDao.findById(poolId);

        SpConnectionDesc conn = null;
        try {
            conn = StorPoolUtil.getSpConnection(poolVO.getUuid(), poolId, storagePoolDetailsDao, primaryStoreDao);
        } catch (Exception e) {
            return false;
        }

        StoragePool pool = (StoragePool)this.dataStoreMgr.getDataStore(poolId, DataStoreRole.Primary);

        HostVO host = hostDao.findById(hostId);
        StoragePoolDetailVO volumeOnPool = verifyVolumeIsOnCluster(poolId, conn, host.getClusterId());
        if (volumeOnPool == null) {
            return false;
        }

        if (host.isInMaintenanceStates()) {
            addModifyCommandToCommandsAllowedInMaintenanceMode();
        }

        List<String> driverSupportedFeatures = StorPoolFeaturesAndFixes.getAllClassConstants();
        List<StoragePoolDetailVO> driverFeaturesBeforeUpgrade = StorPoolHelper.listFeaturesUpdates(storagePoolDetailsDao, poolId);
        boolean isCurrentVersionSupportsEverythingFromPrevious = StorPoolHelper.isPoolSupportsAllFunctionalityFromPreviousVersion(storagePoolDetailsDao, driverSupportedFeatures, driverFeaturesBeforeUpgrade, poolId);
        if (!isCurrentVersionSupportsEverythingFromPrevious) {
            String msg = "The current StorPool driver does not support all functionality from the one before upgrade to CS";
            StorPoolUtil.spLog("Storage pool [%s] is not connected to host [%s] because the functionality after the upgrade is not full",
                    poolId, hostId);
            alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST, pool.getDataCenterId(), pool.getPodId(), msg, msg);
            return false;
        }

        StorPoolModifyStoragePoolCommand cmd = new StorPoolModifyStoragePoolCommand(true, pool, volumeOnPool.getValue());
        final Answer answer = agentMgr.easySend(hostId, cmd);

        StoragePoolHostVO poolHost = storagePoolHostDao.findByPoolHost(pool.getId(), hostId);
        boolean isPoolConnectedToTheHost = poolHost != null;

        if (answer == null) {
            StorPoolUtil.spLog("Storage pool [%s] is not connected to the host [%s]", poolVO.getName(), host.getName());
            deleteVolumeWhenHostCannotConnectPool(conn, volumeOnPool);
            removePoolOnHost(poolHost, isPoolConnectedToTheHost);
            throw new CloudRuntimeException("Unable to get an answer to the modify storage pool command" + pool.getId());
        }

        if (!answer.getResult()) {
            StorPoolUtil.spLog("Storage pool [%s] is not connected to the host [%s]", poolVO.getName(), host.getName());
            removePoolOnHost(poolHost, isPoolConnectedToTheHost);

            if (answer.getDetails() != null && isStorPoolVolumeOrStorageNotExistsOnHost(answer)) {
                deleteVolumeWhenHostCannotConnectPool(conn, volumeOnPool);
                return false;
            }
            String msg = "Unable to attach storage pool" + poolId + " to the host" + hostId;
            alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST, pool.getDataCenterId(), pool.getPodId(), msg, msg);
            throw new CloudRuntimeException("Unable establish connection from storage head to storage pool " + pool.getId() + " due to " + answer.getDetails() +
                pool.getId());
        }

        StorPoolModifyStoragePoolAnswer mspAnswer = (StorPoolModifyStoragePoolAnswer)answer;
        if (mspAnswer.getLocalDatastoreName() != null && pool.isShared()) {
            String datastoreName = mspAnswer.getLocalDatastoreName();
            List<StoragePoolVO> localStoragePools = primaryStoreDao.listLocalStoragePoolByPath(pool.getDataCenterId(), datastoreName);
            for (StoragePoolVO localStoragePool : localStoragePools) {
                if (datastoreName.equals(localStoragePool.getPath())) {
                    log.warn("Storage pool: " + pool.getId() + " has already been added as local storage: " + localStoragePool.getName());
                    throw new StorageConflictException("Cannot add shared storage pool: " + pool.getId() + " because it has already been added as local storage:"
                            + localStoragePool.getName());
                }
            }
        }

        if (!isPoolConnectedToTheHost) {
            poolHost = new StoragePoolHostVO(pool.getId(), hostId, mspAnswer.getPoolInfo().getLocalPath().replaceAll("//", "/"));
            storagePoolHostDao.persist(poolHost);
        } else {
            poolHost.setLocalPath(mspAnswer.getPoolInfo().getLocalPath().replaceAll("//", "/"));
        }

        StorPoolHelper.setSpClusterIdIfNeeded(hostId, mspAnswer.getClusterId(), clusterDao, hostDao, clusterDetailsDao);

        StorPoolUtil.spLog("Connection established between storage pool [%s] and host [%s]", poolVO.getName(), host.getName());
        return true;
    }

    private boolean isStorPoolVolumeOrStorageNotExistsOnHost(final Answer answer) {
        return StringUtils.equalsAny(answer.getDetails(), "objectDoesNotExist", "spNotFound");
    }

    private void deleteVolumeWhenHostCannotConnectPool(SpConnectionDesc conn, StoragePoolDetailVO volumeOnPool) {
        StorPoolUtil.volumeDelete(StorPoolStorageAdaptor.getVolumeNameFromPath(volumeOnPool.getValue(), true), conn);
        storagePoolDetailsDao.remove(volumeOnPool.getId());
    }

    private void removePoolOnHost(StoragePoolHostVO poolHost, boolean isPoolConnectedToTheHost) {
        if (isPoolConnectedToTheHost) {
            storagePoolHostDao.remove(poolHost.getId());
        }
    }

    private synchronized StoragePoolDetailVO verifyVolumeIsOnCluster(long poolId, SpConnectionDesc conn, long clusterId) {
        StoragePoolDetailVO volumeOnPool = storagePoolDetailsDao.findDetail(poolId, StorPoolUtil.SP_VOLUME_ON_CLUSTER + "-" + clusterId);
        if (volumeOnPool == null) {
            SpApiResponse resp = StorPoolUtil.volumeCreate(conn);
            if (resp.getError() != null) {
                return volumeOnPool;
            }
            String volumeName = StorPoolUtil.getNameFromResponse(resp, false);
            volumeOnPool = new StoragePoolDetailVO(poolId, StorPoolUtil.SP_VOLUME_ON_CLUSTER  + "-" + clusterId, StorPoolUtil.devPath(volumeName), false);
            storagePoolDetailsDao.persist(volumeOnPool);
        }
        return volumeOnPool;
    }

    @Override
    public boolean hostAdded(long hostId) {
        return true;
    }

    @Override
    public boolean hostDisconnected(long hostId, long poolId) {
        StorPoolUtil.spLog("hostDisconnected: hostId=%d, poolId=%d", hostId, poolId);
        return true;
    }

    @Override
    public boolean hostAboutToBeRemoved(long hostId) {
        return true;
    }

    @Override
    public boolean hostRemoved(long hostId, long clusterId) {
        return true;
    }

    //workaround: we need this "hack" to add our command StorPoolModifyStoragePoolCommand in AgentAttache.s_commandsAllowedInMaintenanceMode
    //which checks the allowed commands when the host is in maintenance mode
    private void addModifyCommandToCommandsAllowedInMaintenanceMode() {

        Class<AgentAttache> cls = AgentAttache.class;
        try {
            Field field = cls.getDeclaredField("s_commandsAllowedInMaintenanceMode");
            field.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            List<String> allowedCmdsInMaintenance = new ArrayList<String>(Arrays.asList(AgentAttache.s_commandsAllowedInMaintenanceMode));
            allowedCmdsInMaintenance.add(StorPoolModifyStoragePoolCommand.class.toString());
            String[] allowedCmdsInMaintenanceNew = new String[allowedCmdsInMaintenance.size()];
            allowedCmdsInMaintenance.toArray(allowedCmdsInMaintenanceNew);
            Arrays.sort(allowedCmdsInMaintenanceNew);
            field.set(null, allowedCmdsInMaintenanceNew);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            String err = "Could not add StorPoolModifyStoragePoolCommand to s_commandsAllowedInMaintenanceMode array due to: %s";
            StorPoolUtil.spLog(err, e.getMessage());
            log.warn(String.format(err, e.getMessage()));
        }
    }

    @Override
    public boolean hostEnabled(long hostId) {
        return true;
    }
}
