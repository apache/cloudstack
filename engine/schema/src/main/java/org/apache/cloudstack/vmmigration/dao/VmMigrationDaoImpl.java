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

package org.apache.cloudstack.vmmigration.dao;

import com.cloud.event.VmMigrationEvent;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.vmmigration.VmMigrationVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VmMigrationDaoImpl extends GenericDaoBase<VmMigrationVO, Long> implements VmMigrationDao {
    public static final Logger s_logger = Logger.getLogger(VmMigrationDaoImpl.class);

    @Override
    public List<VmMigrationVO> searchCompletedMigrations() {
        SearchCriteria<VmMigrationVO> sc = createSearchCriteria();
        sc.setParameters("state", VmMigrationEvent.State.Completed);
        return listIncludingRemovedBy(sc, null);
    }

    @Override
    public List<VmMigrationVO> searchFailedMigrations() {
        SearchCriteria<VmMigrationVO> sc = createSearchCriteria();
        sc.setParameters("state", VmMigrationEvent.State.Failed);
        return listIncludingRemovedBy(sc, null);
    }

    @Override
    public List<VmMigrationVO> searchMigratedUserVms() {
        SearchCriteria<VmMigrationVO> sc = createSearchCriteria();
        sc.setParameters("state", VmMigrationEvent.State.Completed);
        sc.setParameters("vm_type", VmMigrationEvent.VmType.User);
        return listIncludingRemovedBy(sc, null);
    }

    @Override
    public List<VmMigrationVO> searchMigratedDomainRouters() {
        SearchCriteria<VmMigrationVO> sc = createSearchCriteria();
        sc.setParameters("state", VmMigrationEvent.State.Completed);
        sc.setParameters("vm_type", VmMigrationEvent.VmType.DomainRouter);
        return listIncludingRemovedBy(sc, null);
    }

    @Override
    public List<VmMigrationVO> searchMigratedSystemVms() {
        SearchCriteria<VmMigrationVO> sc = createSearchCriteria();
        sc.addOr("vm_type", SearchCriteria.Op.EQ, VmMigrationEvent.VmType.ConsoleProxy);
        sc.addOr("vm_type", SearchCriteria.Op.EQ, VmMigrationEvent.VmType.SecondaryStorage);
        return listIncludingRemovedBy(sc, null);
    }

    @Override
    public List<VmMigrationVO> searchByInstanceId(Long instanceId) {
        SearchCriteria<VmMigrationVO> sc = createSearchCriteria();
        sc.setParameters("instance_id", instanceId);
        return listIncludingRemovedBy(sc, null);
    }
}
