package com.cloud.agent.api.storage;

import com.cloud.agent.api.Command;

public class CreateEntityDownloadURLCommand extends AbstractDownloadCommand {

    public CreateEntityDownloadURLCommand(String installPath) {
        super();
        this.installPath = installPath;
    }

    public CreateEntityDownloadURLCommand() {
    }

    private String installPath;
    
    @Override
    public boolean executeInSequence() {
        return false;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

}
