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
package org.apache.cloudstack.engine.subsystem.api.storage;

import com.cloud.vm.snapshot.VMSnapshot;

public interface VMSnapshotStrategy {
    VMSnapshot takeVMSnapshot(VMSnapshot vmSnapshot);

    boolean deleteVMSnapshot(VMSnapshot vmSnapshot);

    boolean revertVMSnapshot(VMSnapshot vmSnapshot);

    StrategyPriority canHandle(VMSnapshot vmSnapshot);

    /**
     * Delete vm snapshot only from database. Introduced as a Vmware optimization in which vm snapshots are deleted when
     * the vm gets deleted on hypervisor (no need to delete each vm snapshot before deleting vm, just mark them as deleted on DB)
     * @param vmSnapshot vm snapshot to be marked as deleted.
     * @return true if vm snapshot removed from DB, false if not.
     */
    boolean deleteVMSnapshotFromDB(VMSnapshot vmSnapshot);
}
