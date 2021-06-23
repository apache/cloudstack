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

package org.apache.cloudstack.storage.datastore.client;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.storage.datastore.api.Sdc;
import org.apache.cloudstack.storage.datastore.api.SnapshotGroup;
import org.apache.cloudstack.storage.datastore.api.StoragePool;
import org.apache.cloudstack.storage.datastore.api.StoragePoolStatistics;
import org.apache.cloudstack.storage.datastore.api.Volume;
import org.apache.cloudstack.storage.datastore.api.VolumeStatistics;

import com.cloud.storage.Storage;

public interface ScaleIOGatewayClient {
    String GATEWAY_API_ENDPOINT = "powerflex.gw.url";
    String GATEWAY_API_USERNAME = "powerflex.gw.username";
    String GATEWAY_API_PASSWORD = "powerflex.gw.password";
    String STORAGE_POOL_NAME = "powerflex.storagepool.name";
    String STORAGE_POOL_SYSTEM_ID = "powerflex.storagepool.system.id";

    static ScaleIOGatewayClient getClient(final String url, final String username, final String password,
                                          final boolean validateCertificate, final int timeout, final int maxConnections) throws NoSuchAlgorithmException, KeyManagementException, URISyntaxException {
        return new ScaleIOGatewayClientImpl(url, username, password, validateCertificate, timeout, maxConnections);
    }

    // Volume APIs
    Volume createVolume(final String name, final String storagePoolId,
                        final Integer sizeInGb, final Storage.ProvisioningType volumeType);
    List<Volume> listVolumes();
    List<Volume> listSnapshotVolumes();
    Volume getVolume(String volumeId);
    Volume getVolumeByName(String name);
    boolean renameVolume(final String volumeId, final String newName);
    Volume resizeVolume(final String volumeId, final Integer sizeInGb);
    Volume cloneVolume(final String sourceVolumeId, final String destVolumeName);
    boolean deleteVolume(final String volumeId);
    boolean migrateVolume(final String srcVolumeId, final String destPoolId, final int timeoutInSecs);

    boolean mapVolumeToSdc(final String volumeId, final String sdcId);
    boolean mapVolumeToSdcWithLimits(final String volumeId, final String sdcId, final Long iopsLimit, final Long bandwidthLimitInKbps);
    boolean unmapVolumeFromSdc(final String volumeId, final String sdcId);
    boolean unmapVolumeFromAllSdcs(final String volumeId);
    boolean isVolumeMappedToSdc(final String volumeId, final String sdcId);

    // Snapshot APIs
    SnapshotGroup takeSnapshot(final Map<String, String> srcVolumeDestSnapshotMap);
    boolean revertSnapshot(final String systemId, final Map<String, String> srcSnapshotDestVolumeMap);
    int deleteSnapshotGroup(final String systemId, final String snapshotGroupId);
    Volume takeSnapshot(final String volumeId, final String snapshotVolumeName);
    boolean revertSnapshot(final String sourceSnapshotVolumeId, final String destVolumeId);

    // Storage Pool APIs
    List<StoragePool> listStoragePools();
    StoragePool getStoragePool(String poolId);
    StoragePoolStatistics getStoragePoolStatistics(String poolId);
    VolumeStatistics getVolumeStatistics(String volumeId);
    String getSystemId(String protectionDomainId);
    List<Volume> listVolumesInStoragePool(String poolId);

    // SDC APIs
    List<Sdc> listSdcs();
    Sdc getSdc(String sdcId);
    Sdc getSdcByIp(String ipAddress);
    Sdc getConnectedSdcByIp(String ipAddress);
    List<String> listConnectedSdcIps();
    boolean isSdcConnected(String ipAddress);
}
