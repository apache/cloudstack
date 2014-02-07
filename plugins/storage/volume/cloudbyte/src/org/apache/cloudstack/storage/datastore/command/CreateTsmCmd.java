package org.apache.cloudstack.storage.datastore.command;

import org.apache.cloudstack.storage.datastore.client.BaseCommand;
import org.apache.cloudstack.storage.datastore.response.CreateTsmCmdResponse;



public class CreateTsmCmd extends BaseCommand{

	public CreateTsmCmd()
	{
		super("createTsm", new CreateTsmCmdResponse());
		
	}
	
	
}
