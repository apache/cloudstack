package org.apache.cloudstack.storage.command;

import org.apache.cloudstack.storage.to.ImageOnPrimayDataStoreTO;
import org.apache.cloudstack.storage.to.TemplateTO;
import org.apache.cloudstack.storage.to.VolumeTO;

import com.cloud.agent.api.Command;

public class CopyTemplateToPrimaryStorage extends Command {

	private ImageOnPrimayDataStoreTO imageTO;
	
	protected CopyTemplateToPrimaryStorage() {
		super();
	}
	
	public CopyTemplateToPrimaryStorage(ImageOnPrimayDataStoreTO image) {
		super();
		this.imageTO = image;
	}
	
	@Override
	public boolean executeInSequence() {
		// TODO Auto-generated method stub
		return false;
	}

}
