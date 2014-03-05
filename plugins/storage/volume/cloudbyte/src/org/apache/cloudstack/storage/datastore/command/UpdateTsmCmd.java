package org.apache.cloudstack.storage.datastore.command;

import org.apache.cloudstack.storage.datastore.client.BaseCommand;
import org.apache.cloudstack.storage.datastore.response.ListTsmsResponse;
import org.apache.cloudstack.storage.datastore.response.UpdateFileSystemCmdResponse;
import org.apache.cloudstack.storage.datastore.response.UpdateQosGroupCmdResponse;
import org.apache.cloudstack.storage.datastore.response.UpdateTsmCmdResponse;

public class UpdateTsmCmd extends BaseCommand {
	
	public UpdateTsmCmd() {
		super("updateTsm", new UpdateTsmCmdResponse() );
	}

}
