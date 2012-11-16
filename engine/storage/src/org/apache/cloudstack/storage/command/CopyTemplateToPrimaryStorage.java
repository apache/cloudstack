package org.apache.cloudstack.storage.command;

import org.apache.cloudstack.storage.to.TemplateTO;
import org.apache.cloudstack.storage.to.VolumeTO;

import com.cloud.agent.api.Command;

public class CopyTemplateToPrimaryStorage extends Command {

	private VolumeTO volume;
	private TemplateTO template;
	
	protected CopyTemplateToPrimaryStorage() {
		super();
	}
	
	public CopyTemplateToPrimaryStorage(TemplateTO template, VolumeTO volume) {
		super();
		this.volume = volume;
		this.template = template;
	}
	
	@Override
	public boolean executeInSequence() {
		// TODO Auto-generated method stub
		return false;
	}

}
