package org.apache.cloudstack.storage.datastore.command;

import org.apache.cloudstack.storage.datastore.client.BaseCommand;
import org.apache.cloudstack.storage.datastore.response.DeleteTsmResponse;


public class DeleteTsmCmd extends BaseCommand {
	
	public DeleteTsmCmd() {
		super("deleteTsm", new DeleteTsmResponse() );
	}

}
