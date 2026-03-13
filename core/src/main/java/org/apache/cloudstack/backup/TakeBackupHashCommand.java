package org.apache.cloudstack.backup;

import com.cloud.agent.api.Command;
import org.apache.cloudstack.storage.to.BackupDeltaTO;

import java.util.List;

public class TakeBackupHashCommand extends Command {

    private List<BackupDeltaTO> backupDeltaTOList;

    private String backupUuid;

    public TakeBackupHashCommand(List<BackupDeltaTO> backupDeltaTOList, String backupUuid) {
        this.backupDeltaTOList = backupDeltaTOList;
        this.backupUuid = backupUuid;
    }

    public List<BackupDeltaTO> getBackupDeltaTOList() {
        return backupDeltaTOList;
    }

    public String getBackupUuid() {
        return backupUuid;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
