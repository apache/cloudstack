package org.apache.cloudstack.backup;

import com.cloud.agent.api.Command;
import org.apache.cloudstack.storage.to.KnibTO;

import java.util.List;

public class CleanupKnibBackupErrorCommand extends Command {

    private boolean runningVM;

    private String vmName;

    private String imageStoreUrl;

    private List<KnibTO> knibTOs;

    public CleanupKnibBackupErrorCommand(boolean runningVM, String vmName, String imageStoreUrl, List<KnibTO> knibTOs) {
        this.runningVM = runningVM;
        this.vmName = vmName;
        this.imageStoreUrl = imageStoreUrl;
        this.knibTOs = knibTOs;
    }

    public boolean isRunningVM() {
        return runningVM;
    }

    public String getVmName() {
        return vmName;
    }

    public String getImageStoreUrl() {
        return imageStoreUrl;
    }

    public List<KnibTO> getKnibTOs() {
        return knibTOs;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
