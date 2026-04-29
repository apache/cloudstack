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

package org.apache.cloudstack.backup;

import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.user.AccountManager;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.command.user.backup.repository.AddBackupRepositoryCmd;
import org.apache.cloudstack.api.command.user.backup.repository.DeleteBackupRepositoryCmd;
import org.apache.cloudstack.api.command.user.backup.repository.ListBackupRepositoriesCmd;
import org.apache.cloudstack.api.command.user.backup.repository.UpdateBackupRepositoryCmd;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.dao.BackupOfferingDao;
import org.apache.cloudstack.backup.dao.BackupRepositoryDao;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BackupRepositoryServiceImpl extends ManagerBase implements BackupRepositoryService {

    @Inject
    private BackupRepositoryDao repositoryDao;
    @Inject
    private BackupOfferingDao backupOfferingDao;
    @Inject
    private BackupDao backupDao;
    @Inject
    private AccountManager accountManager;

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_BACKUP_REPOSITORY_ADD, eventDescription = "add backup repository")
    public BackupRepository addBackupRepository(AddBackupRepositoryCmd cmd) {
        BackupRepositoryVO repository = new BackupRepositoryVO(cmd.getZoneId(), cmd.getProvider(), cmd.getName(),
                cmd.getType(), cmd.getAddress(), cmd.getMountOptions(), cmd.getCapacityBytes(), cmd.crossZoneInstanceCreationEnabled());
        return repositoryDao.persist(repository);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_BACKUP_REPOSITORY_UPDATE, eventDescription = "update backup repository")
    public BackupRepository updateBackupRepository(UpdateBackupRepositoryCmd cmd) {
        Long id = cmd.getId();
        String name = cmd.getName();
        String address = cmd.getAddress();
        String mountOptions = cmd.getMountOptions();
        Boolean crossZoneInstanceCreation = cmd.crossZoneInstanceCreationEnabled();

        BackupRepositoryVO backupRepository = repositoryDao.findById(id);
        if (Objects.isNull(backupRepository)) {
            logger.debug("Backup repository appears to already be deleted");
            return null;
        }
        BackupRepositoryVO backupRepositoryVO = repositoryDao.createForUpdate(id);
        List<String> fields = new ArrayList<>();
        if (name != null) {
            backupRepositoryVO.setName(name);
            fields.add("name: " + name);
        }

        if (address != null) {
            backupRepositoryVO.setAddress(address);
            fields.add("address: " + address);
        }

        if (mountOptions != null) {
            backupRepositoryVO.setMountOptions(mountOptions);
        }

        if (crossZoneInstanceCreation != null){
            backupRepositoryVO.setCrossZoneInstanceCreation(crossZoneInstanceCreation);
            fields.add("crossZoneInstanceCreation: " + crossZoneInstanceCreation);
        }

        if (!repositoryDao.update(id, backupRepositoryVO)) {
            logger.warn(String.format("Couldn't update Backup repository (%s) with [%s].", backupRepositoryVO, String.join(", ", fields)));
            return null;
        }

        BackupRepositoryVO repositoryVO = repositoryDao.findById(id);
        CallContext.current().setEventDetails(String.format("Backup Repository updated [%s].",
                ReflectionToStringBuilderUtils.reflectOnlySelectedFields(repositoryVO, "id", "name", "description", "userDrivenBackupAllowed", "externalId", "crossZoneInstanceCreation")));
        return repositoryVO;
    }

    @Override
    public boolean deleteBackupRepository(DeleteBackupRepositoryCmd cmd) {
        BackupRepositoryVO backupRepositoryVO = repositoryDao.findById(cmd.getId());
        if (Objects.isNull(backupRepositoryVO)) {
            logger.debug("Backup repository appears to already be deleted");
            return false;
        }
        BackupOffering offeringVO = backupOfferingDao.findByExternalId(backupRepositoryVO.getUuid(), backupRepositoryVO.getZoneId());
        if (Objects.nonNull(offeringVO)) {
            List<Backup> backups = backupDao.listByOfferingId(offeringVO.getId());
            if (!backups.isEmpty()) {
                throw new CloudRuntimeException("Failed to delete backup repository as there are backups present on it");
            }
        }
        return repositoryDao.remove(backupRepositoryVO.getId());
    }

    @Override
    public Pair<List<BackupRepository>, Integer> listBackupRepositories(ListBackupRepositoriesCmd cmd) {
        Long zoneId = accountManager.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), cmd.getZoneId());
        Long id = cmd.getId();
        String name = cmd.getName();
        String provider = cmd.getProvider();
        String keyword = cmd.getKeyword();

        SearchBuilder<BackupRepositoryVO> sb = repositoryDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("zoneId", sb.entity().getZoneId(), SearchCriteria.Op.EQ);
        sb.and("provider", sb.entity().getProvider(), SearchCriteria.Op.EQ);

        SearchCriteria<BackupRepositoryVO> sc = sb.create();
        if (keyword != null) {
            SearchCriteria<BackupRepositoryVO> ssc = repositoryDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("provider", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }
        if (Objects.nonNull(id)) {
            sc.setParameters("id", id);
        }
        if (Objects.nonNull(name)) {
            sc.setParameters("name", name);
        }
        if (Objects.nonNull(zoneId)) {
            sc.setParameters("zoneId", zoneId);
        }
        if (Objects.nonNull(provider)) {
            sc.setParameters("provider", provider);
        }

        // search Store details by ids
        Pair<List<BackupRepositoryVO>, Integer> repositoryVOPair = repositoryDao.searchAndCount(sc, null);
        return new Pair<>(new ArrayList<>(repositoryVOPair.first()), repositoryVOPair.second());
    }
}
