package org.apache.cloudstack.backup;

import com.cloud.agent.api.Command;
import org.apache.cloudstack.storage.to.KbossTO;

import java.util.List;

public class CleanupKbossBackupErrorCommand extends Command {

    private boolean runningVM;

    private String vmName;

    private String imageStoreUrl;

    private List<KbossTO> kbossTOS;

    public CleanupKbossBackupErrorCommand(boolean runningVM, String vmName, String imageStoreUrl, List<KbossTO> kbossTOS) {
        this.runningVM = runningVM;
        this.vmName = vmName;
        this.imageStoreUrl = imageStoreUrl;
        this.kbossTOS = kbossTOS;
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

    public List<KbossTO> getKbossTOs() {
        return kbossTOS;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
