package org.apache.cloudstack.storage.datastore.command;

import org.apache.cloudstack.storage.datastore.client.BaseCommand;
import org.apache.cloudstack.storage.datastore.response.CreateVolumeCmdResponse;



public class CreateVolumeCmd extends BaseCommand{

	public CreateVolumeCmd()
	{
		super("createVolume", new CreateVolumeCmdResponse());
		
	}
	
	
}
