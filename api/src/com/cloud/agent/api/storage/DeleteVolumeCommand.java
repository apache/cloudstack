package com.cloud.agent.api.storage;

public class DeleteVolumeCommand extends ssCommand {
	private String volumePath;

	public DeleteVolumeCommand() {	
	}
	
	public DeleteVolumeCommand(String secUrl, String volumePath) {
	    this.setSecUrl(secUrl);
    	this.volumePath = volumePath;
    }
	
	@Override
    public boolean executeInSequence() {
        return true;
    }
	
	public String getVolumePath() {
		return volumePath;
	}
}
