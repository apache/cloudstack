package com.cloud.agent.api.storage;

import com.cloud.agent.api.Command;

public class CreateEntityDownloadURLCommand extends AbstractDownloadCommand {

    public CreateEntityDownloadURLCommand(String installPath, String uuid) {
        super();
        this.installPath = installPath;
        this.extractLinkUUID = uuid;
    }

    public CreateEntityDownloadURLCommand() {
    }

    private String installPath;
    private String extractLinkUUID;
    
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

	public String getExtractLinkUUID() {
		return extractLinkUUID;
	}

	public void setExtractLinkUUID(String extractLinkUUID) {
		this.extractLinkUUID = extractLinkUUID;
	}

}
