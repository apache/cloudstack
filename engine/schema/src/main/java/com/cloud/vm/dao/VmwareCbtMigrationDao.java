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
package com.cloud.vm.dao;

import com.cloud.utils.Pair;
import com.cloud.utils.db.GenericDao;
import com.cloud.vm.VmwareCbtMigrationVO;
import org.apache.cloudstack.vm.VmwareCbtMigration;

import java.util.Date;
import java.util.List;

public interface VmwareCbtMigrationDao extends GenericDao<VmwareCbtMigrationVO, Long> {
    Pair<List<VmwareCbtMigrationVO>, Integer> listMigrations(Long id, Long zoneId, Long accountId, String vcenter,
                                                             String sourceVmName, VmwareCbtMigration.State state,
                                                             Long startIndex, Long pageSizeVal);

    List<VmwareCbtMigrationVO> listByConvertHostId(Long convertHostId);

    /**
     * Persists the given migration only while the stored record has not reached a terminal
     * state (Completed, Failed or Cancelled), as a single conditional statement.
     *
     * Long-running replication and cutover jobs use this instead of a plain update so a
     * migration that an operator cancelled mid-flight is not resurrected by a job that started
     * before the cancellation: cancelling removes the target disks, so a later blind write of a
     * non-terminal state would leave a record claiming to replicate onto disks that no longer
     * exist, and which can no longer be deleted because it is not terminal.
     *
     * @return true if the record was still non-terminal and was updated, false if it had
     *         already reached a terminal state and was therefore left untouched.
     */
    boolean updateIfNotTerminal(VmwareCbtMigrationVO migration);

    /**
     * Atomically records a successful CloudStack VM import while the migration is still
     * non-terminal. The VM reference, terminal state and credential removal must be one
     * conditional update so a concurrent cancellation cannot be overwritten.
     */
    boolean completeImportIfNotTerminal(long migrationId, long vmId, Date updated);

    /**
     * Records the VM created by an import that raced with a terminal state without changing that
     * terminal state. This keeps the imported VM discoverable for operator reconciliation and
     * removes stored source credentials.
     */
    boolean recordImportedVmAndClearCredentials(long migrationId, long vmId, Date updated);
}
