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
package org.apache.cloudstack.storage.vmsnapshot;

import java.util.List;

import org.apache.cloudstack.storage.to.VolumeObjectTO;

import com.cloud.agent.api.VMSnapshotTO;
import com.cloud.storage.Storage;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotVO;

public interface VMSnapshotHelper {
    boolean vmSnapshotStateTransitTo(VMSnapshot vsnp, VMSnapshot.Event event) throws NoTransitionException;

    Long pickRunningHost(Long vmId);

    List<VolumeObjectTO> getVolumeTOList(Long vmId);

    VMSnapshotTO getSnapshotWithParents(VMSnapshotVO snapshot);

    Long getStoragePoolForVM(Long vmId);

    Storage.StoragePoolType getStoragePoolType(Long poolId);
}
