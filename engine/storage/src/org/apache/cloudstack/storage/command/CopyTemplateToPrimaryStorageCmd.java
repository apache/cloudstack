package org.apache.cloudstack.storage.command;

import org.apache.cloudstack.storage.to.ImageOnPrimayDataStoreTO;

import com.cloud.agent.api.Command;

public class CopyTemplateToPrimaryStorageCmd extends Command implements StorageSubSystemCommand {

    private ImageOnPrimayDataStoreTO imageTO;

    protected CopyTemplateToPrimaryStorageCmd() {
        super();
    }

    public CopyTemplateToPrimaryStorageCmd(ImageOnPrimayDataStoreTO image) {
        super();
        this.imageTO = image;
    }
    
    public ImageOnPrimayDataStoreTO getImage() {
        return this.imageTO;
    }

    @Override
    public boolean executeInSequence() {
        // TODO Auto-generated method stub
        return false;
    }

}
