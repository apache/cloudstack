package org.apache.cloudstack.storage.command;

import org.apache.cloudstack.storage.to.ImageOnPrimayDataStoreTO;

import com.cloud.agent.api.Command;

public class CopyCmd extends Command implements StorageSubSystemCommand {

    private ImageOnPrimayDataStoreTO imageTO;

    protected CopyCmd() {
        super();
    }

    public CopyCmd(String destUri, String srcUri) {
        super();
       // this.imageTO = image;
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
