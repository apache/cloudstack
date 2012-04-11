package com.cloud.agent.api.storage;

import com.cloud.agent.api.LogLevel;
import com.cloud.agent.api.LogLevel.Log4jLevel;
import com.cloud.agent.api.to.SwiftTO;

public class ListVolumeCommand extends StorageCommand {

    private String secUrl;        
    
    public ListVolumeCommand() {
    }
    
	public ListVolumeCommand(String secUrl) {
	    this.secUrl = secUrl;        
	}	

    @Override
    public boolean executeInSequence() {
        return true;
    }

    public String getSecUrl() {
        return secUrl;
    }

}
