package org.apache.cloudstack.backup;

import com.cloud.user.AccountManager;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.api.command.user.backup.repository.AddBackupRepositoryCmd;
import org.apache.cloudstack.api.command.user.backup.repository.DeleteBackupRepositoryCmd;
import org.apache.cloudstack.api.command.user.backup.repository.ListBackupRepositoriesCmd;
import org.apache.cloudstack.backup.dao.BackupRepositoryDao;
import org.apache.cloudstack.context.CallContext;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BackupRepositoryServiceImpl extends ManagerBase implements BackupRepositoryService {

    @Inject
    private BackupRepositoryDao repositoryDao;
    @Inject
    private AccountManager accountManager;

    @Override
    public BackupRepository addBackupRepository(AddBackupRepositoryCmd cmd) {
        BackupRepositoryVO repository = new BackupRepositoryVO(cmd.getZoneId(), cmd.getProvider(), cmd.getName(),
                cmd.getType(), cmd.getAddress(), cmd.getMountOptions(), cmd.getCapacityBytes());
        repositoryDao.persist(repository);

        return repository;
    }

    @Override
    public boolean deleteBackupRepository(DeleteBackupRepositoryCmd cmd) {
        BackupRepositoryVO backupRepositoryVO = repositoryDao.findById(cmd.getId());
        if (Objects.isNull(backupRepositoryVO)) {
            logger.debug("Backup repository appears to already be deleted");
            return true;
        }
        repositoryDao.remove(backupRepositoryVO.getId());
        return true;
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
