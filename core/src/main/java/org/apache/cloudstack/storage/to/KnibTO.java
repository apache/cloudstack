package org.apache.cloudstack.storage.to;
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
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;
import java.util.stream.Collectors;

public class KnibTO {

    private String pathBackupParentOnSecondary;
    private VolumeObjectTO volumeObjectTO;

    private DeltaMergeTreeTO deltaMergeTreeTO;

    List<String> vmSnapshotDeltaPaths;

    public KnibTO(VolumeObjectTO volumeObjectTO, List<SnapshotDataStoreVO> snapshotDataStoreVOs) {
        this.volumeObjectTO = volumeObjectTO;
        this.vmSnapshotDeltaPaths = snapshotDataStoreVOs.stream().map(SnapshotDataStoreVO::getInstallPath).collect(Collectors.toList());
    }

    public String getPathBackupParentOnSecondary() {
        return pathBackupParentOnSecondary;
    }

    public VolumeObjectTO getVolumeObjectTO() {
        return volumeObjectTO;
    }

    public DeltaMergeTreeTO getDeltaMergeTreeTO() {
        return deltaMergeTreeTO;
    }

    public List<String> getVmSnapshotDeltaPaths() {
        return vmSnapshotDeltaPaths;
    }

    public void setPathBackupParentOnSecondary(String pathBackupParentOnSecondary) {
        this.pathBackupParentOnSecondary = pathBackupParentOnSecondary;
    }

    public void setDeltaMergeTreeTO(DeltaMergeTreeTO deltaMergeTreeTO) {
        this.deltaMergeTreeTO = deltaMergeTreeTO;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.JSON_STYLE);
    }
}
