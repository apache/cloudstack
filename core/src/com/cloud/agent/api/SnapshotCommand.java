//
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
//

package com.cloud.agent.api;

import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.storage.StoragePool;

/**
 * This currently assumes that both primary and secondary storage are mounted on
 * the XenServer.
 */
public class SnapshotCommand extends Command {
    protected String primaryStoragePoolNameLabel;
    StorageFilerTO primaryPool;
    private String snapshotUuid;
    private String snapshotName;
    private String secondaryStorageUrl;
    private Long dcId;
    private Long accountId;
    private Long volumeId;
    private String volumePath;

    protected SnapshotCommand() {

    }

    /**
     * @param primaryStoragePoolNameLabel
     *            The primary storage Pool
     * @param snapshotUuid
     *            The UUID of the snapshot which is going to be backed up
     * @param secondaryStoragePoolURL
     *            This is what shows up in the UI when you click on Secondary
     *            storage. In the code, it is present as: In the
     *            vmops.host_details table, there is a field mount.parent. This
     *            is the value of that field If you have better ideas on how to
     *            get it, you are welcome.
     */
    public SnapshotCommand(final StoragePool pool, final String secondaryStorageUrl, final String snapshotUuid, final String snapshotName, final Long dcId, final Long accountId, final Long volumeId) {
        primaryStoragePoolNameLabel = pool.getUuid();
        primaryPool = new StorageFilerTO(pool);
        this.snapshotUuid = snapshotUuid;
        this.secondaryStorageUrl = secondaryStorageUrl;
        this.dcId = dcId;
        this.accountId = accountId;
        this.volumeId = volumeId;
        this.snapshotName = snapshotName;
    }

    /**
     * @return the primaryStoragePoolNameLabel
     */
    public String getPrimaryStoragePoolNameLabel() {
        return primaryStoragePoolNameLabel;
    }

    /**
     * @return the primaryPool
     */
    public StorageFilerTO getPool() {
        return primaryPool;
    }

    /**
     * @return the snapshotUuid
     */
    public String getSnapshotUuid() {
        return snapshotUuid;
    }

    public String getSnapshotName() {
        return snapshotName;
    }

    /**
     * @return the secondaryStoragePoolURL
     */
    public String getSecondaryStorageUrl() {
        return secondaryStorageUrl;
    }

    public Long getDataCenterId() {
        return dcId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Long getVolumeId() {
        return volumeId;
    }

    public String getVolumePath() {
        return volumePath;
    }

    public void setVolumePath(final String path) {
        volumePath = path;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean executeInSequence() {
        return false;
    }

}
