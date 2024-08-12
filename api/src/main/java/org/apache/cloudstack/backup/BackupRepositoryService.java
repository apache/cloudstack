package org.apache.cloudstack.backup;

import com.cloud.utils.Pair;
import org.apache.cloudstack.api.command.user.backup.repository.AddBackupRepositoryCmd;
import org.apache.cloudstack.api.command.user.backup.repository.DeleteBackupRepositoryCmd;
import org.apache.cloudstack.api.command.user.backup.repository.ListBackupRepositoriesCmd;

import java.util.List;

public interface BackupRepositoryService {
    BackupRepository addBackupRepository(AddBackupRepositoryCmd cmd);
    boolean deleteBackupRepository(DeleteBackupRepositoryCmd cmd);
    Pair<List<BackupRepository>, Integer> listBackupRepositories(ListBackupRepositoriesCmd cmd);

}
