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

package org.apache.cloudstack.storage.datastore.util;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.config.impl.ConfigurationVO;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.storage.datastore.util.StorPoolUtil.SpApiResponse;
import org.apache.cloudstack.storage.snapshot.StorPoolConfigurationManager;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.commons.collections4.CollectionUtils;

import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.kvm.storage.StorPoolStorageAdaptor;
import com.cloud.server.ResourceTag;
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDetailsDao;
import com.cloud.storage.dao.SnapshotDetailsVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;

public class StorPoolHelper {

    private static final String UPDATE_SNAPSHOT_DETAILS_VALUE = "UPDATE `cloud`.`snapshot_details` SET value=? WHERE id=?";
    private static final String UPDATE_VOLUME_DETAILS_NAME = "UPDATE `cloud`.`volume_details` SET name=? WHERE id=?";
    public static final String PrimaryStorageDownloadWait = "primary.storage.download.wait";
    public static final String CopyVolumeWait = "copy.volume.wait";
    public static final String BackupSnapshotWait = "backup.snapshot.wait";

    public static void updateVolumeInfo(VolumeObjectTO volumeObjectTO, Long size, SpApiResponse resp,
            VolumeDao volumeDao) {
        String volumePath = StorPoolUtil.devPath(StorPoolUtil.getNameFromResponse(resp, false));
        VolumeVO volume = volumeDao.findById(volumeObjectTO.getId());
        if (volume != null) {
            volumeObjectTO.setSize(size);
            volumeObjectTO.setPath(volumePath);
            volume.setSize(size);
            volume.setPath(volumePath);
            volumeDao.update(volumeObjectTO.getId(), volume);
        }
    }

    // If volume is deleted, CloudStack removes records of snapshots created on Primary storage only in database.
    // That's why we keep information in snapshot_details table, about all snapshots created on StorPool and we can operate with them
    public static void addSnapshotDetails(final Long id, final String uuid, final String snapshotName,
            SnapshotDetailsDao snapshotDetailsDao) {
        SnapshotDetailsVO details = new SnapshotDetailsVO(id, uuid, snapshotName, false);
        snapshotDetailsDao.persist(details);
    }

    public static String getSnapshotName(Long snapshotId, String snapshotUuid, SnapshotDataStoreDao snapshotStoreDao,
            SnapshotDetailsDao snapshotDetailsDao) {

        SnapshotDetailsVO snapshotDetails = snapshotDetailsDao.findDetail(snapshotId, snapshotUuid);

        if (snapshotDetails != null) {
            return StorPoolStorageAdaptor.getVolumeNameFromPath(snapshotDetails.getValue(), true);
        } else {
            List<SnapshotDataStoreVO> snapshots = snapshotStoreDao.findBySnapshotId(snapshotId);
            if (!CollectionUtils.isEmpty(snapshots)) {
                for (SnapshotDataStoreVO snapshotDataStoreVO : snapshots) {
                    String name = StorPoolStorageAdaptor.getVolumeNameFromPath(snapshotDataStoreVO.getInstallPath(), true);
                    if (name == null) {
                        continue;
                    } else {
                        addSnapshotDetails(snapshotId, snapshotUuid, snapshotDataStoreVO.getInstallPath(), snapshotDetailsDao);
                        return name;
                    }
                }
            }
        }
        return null;
    }

    public static void updateSnapshotDetailsValue(Long id, String valueOrName, String snapshotOrVolume) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;
        try {
            String sql = null;
            if (snapshotOrVolume.equals("snapshot")) {
                sql = UPDATE_SNAPSHOT_DETAILS_VALUE;
            } else if (snapshotOrVolume.equals("volume")) {
                sql = UPDATE_VOLUME_DETAILS_NAME;
            } else {
                StorPoolUtil.spLog("Could not update snapshot detail with id=%s", id);
            }
            if (sql != null) {
                pstmt = txn.prepareAutoCloseStatement(sql);
                pstmt.setString(1, valueOrName);
                pstmt.setLong(2, id);
                pstmt.executeUpdate();
                txn.commit();
            }
        } catch (Exception e) {
            txn.rollback();
            StorPoolUtil.spLog("Could not update snapshot detail with id=%s", id);
        }
    }

    public static String getVcPolicyTag(Long vmId, ResourceTagDao resourceTagDao) {
        if (vmId != null) {
            ResourceTag tag = resourceTagDao.findByKey(vmId, ResourceObjectType.UserVm, StorPoolUtil.SP_VC_POLICY);
            if (tag != null) {
                return tag.getValue();
            }
        }
        return null;
    }

    public static String getVMInstanceUUID(Long id, VMInstanceDao vmInstanceDao) {
        if (id != null) {
            VMInstanceVO vmInstance = vmInstanceDao.findById(id);
            if (vmInstance != null) {
                return vmInstance.getUuid();
            }
        }
        return null;
    }

    public static Map<String, String> addStorPoolTags(String name, String vmUuid, String csTag, String vcPolicy) {
        Map<String, String> tags = new HashMap<>();
        tags.put("uuid", name);
        tags.put("cvm", vmUuid);
        tags.put(StorPoolUtil.SP_VC_POLICY, vcPolicy);
        if (csTag != null) {
            tags.put("cs", csTag);
        }
        return tags;
    }

    // Initialize custom logger for updated volume and snapshots
//    public static void appendLogger(Logger log, String filePath, String kindOfLog) {
//        Appender appender = null;
//        PatternLayout patternLayout = new PatternLayout();
//        patternLayout.setConversionPattern("%d{YYYY-MM-dd HH:mm:ss.SSS}  %m%n");
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
//        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
//        String path = filePath + "-" + sdf.format(timestamp) + ".log";
//        try {
//            appender = new RollingFileAppender(patternLayout, path);
//            log.setAdditivity(false);
//            log.addAppender(appender);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        if (kindOfLog.equals("update")) {
//            StorPoolUtil.spLog(
//                    "You can find information about volumes and snapshots, which will be updated in Database with their globalIs in %s log file",
//                    path);
//        } else if (kindOfLog.equals("abandon")) {
//            StorPoolUtil.spLog(
//                    "You can find information about volumes and snapshots, for which CloudStack doesn't have information in %s log file",
//                    path);
//        }
//    }

    public static void setSpClusterIdIfNeeded(long hostId, String clusterId, ClusterDao clusterDao, HostDao hostDao,
            ClusterDetailsDao clusterDetails) {
        HostVO host = hostDao.findById(hostId);
        if (host != null && host.getClusterId() != null) {
            ClusterVO cluster = clusterDao.findById(host.getClusterId());
            ClusterDetailsVO clusterDetailsVo = clusterDetails.findDetail(cluster.getId(),
                    StorPoolConfigurationManager.StorPoolClusterId.key());
            if (clusterDetailsVo == null) {
                clusterDetails.persist(
                        new ClusterDetailsVO(cluster.getId(), StorPoolConfigurationManager.StorPoolClusterId.key(), clusterId));
            } else if (clusterDetailsVo.getValue() == null || !clusterDetailsVo.getValue().equals(clusterId)) {
                clusterDetailsVo.setValue(clusterId);
                clusterDetails.update(clusterDetailsVo.getId(), clusterDetailsVo);
            }
        }
    }

    public static Long findClusterIdByGlobalId(String globalId, ClusterDao clusterDao) {
        List<ClusterVO> clusterVo = clusterDao.listAll();
        if (clusterVo.size() == 1) {
            StorPoolUtil.spLog("There is only one cluster, sending backup to secondary command");
            return null;
        }
        for (ClusterVO clusterVO2 : clusterVo) {
            if (globalId != null && StorPoolConfigurationManager.StorPoolClusterId.valueIn(clusterVO2.getId()) != null
                    && globalId.contains(StorPoolConfigurationManager.StorPoolClusterId.valueIn(clusterVO2.getId()).toString())) {
                StorPoolUtil.spLog("Found cluster with id=%s for object with globalId=%s", clusterVO2.getId(),
                        globalId);
                return clusterVO2.getId();
            }
        }
        throw new CloudRuntimeException(
                "Could not find the right clusterId. to send command. To use snapshot backup to secondary for each CloudStack cluster in its settings set the value of StorPool's cluster-id in \"sp.cluster.id\".");
    }

    public static HostVO findHostByCluster(Long clusterId, HostDao hostDao) {
        List<HostVO> host = hostDao.findByClusterId(clusterId);
        return host != null ? host.get(0) : null;
    }

    public static int getTimeout(String cfg, ConfigurationDao configDao) {
        final ConfigurationVO value = configDao.findByName(cfg);
        return NumbersUtil.parseInt(value.getValue(), Integer.parseInt(value.getDefaultValue()));
    }

    public static VMTemplateStoragePoolVO findByPoolTemplate(long poolId, long templateId) {
        QueryBuilder<VMTemplateStoragePoolVO> sc = QueryBuilder.create(VMTemplateStoragePoolVO.class);
        sc.and(sc.entity().getPoolId(), Op.EQ, poolId);
        sc.and(sc.entity().getTemplateId(), Op.EQ, templateId);
        return sc.find();
    }

    public static void updateVmStoreTemplate(Long id, DataStoreRole role, String path,
            TemplateDataStoreDao templStoreDao) {
        TemplateDataStoreVO templ = templStoreDao.findByTemplate(id, role);
        templ.setLocalDownloadPath(path);
        templStoreDao.persist(templ);
    }

    public static List<StoragePoolDetailVO> listFeaturesUpdates(StoragePoolDetailsDao storagePoolDetails, long poolId) {
        SearchBuilder<StoragePoolDetailVO> sb = storagePoolDetails.createSearchBuilder();
        sb.and("pool_id", sb.entity().getResourceId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
        SearchCriteria<StoragePoolDetailVO> sc = sb.create();
        sc.setParameters("pool_id", poolId);
        sc.setParameters("name", "SP-FEATURE" + "%");
        return storagePoolDetails.search(sc, null);
    }

    public static boolean isPoolSupportsAllFunctionalityFromPreviousVersion(StoragePoolDetailsDao storagePoolDetails, List<String> currentPluginFeatures, List<StoragePoolDetailVO> poolFeaturesBeforeUpgrade, long poolId) {
        if (CollectionUtils.isEmpty(currentPluginFeatures) && CollectionUtils.isEmpty(poolFeaturesBeforeUpgrade)) {
            return true;
        }

        List<String> poolDetails = poolFeaturesBeforeUpgrade.stream().map(StoragePoolDetailVO::getName).collect(Collectors.toList());
        List<String> detailsNotContainedInCurrent = new ArrayList<>(CollectionUtils.removeAll(poolDetails, currentPluginFeatures));
        List<String> detailsNotContainedInDataBase = new ArrayList<>(CollectionUtils.removeAll(currentPluginFeatures, poolDetails));
        if (!CollectionUtils.isEmpty(detailsNotContainedInCurrent)) {
            return false;
        } else if (!CollectionUtils.isEmpty(detailsNotContainedInDataBase)) {
            for (String features : detailsNotContainedInDataBase) {
                StoragePoolDetailVO storageNewFeatures = new StoragePoolDetailVO(poolId, features, features, false);
                storagePoolDetails.persist(storageNewFeatures);
            }
            return true;
        }
        return true;
    }
}
