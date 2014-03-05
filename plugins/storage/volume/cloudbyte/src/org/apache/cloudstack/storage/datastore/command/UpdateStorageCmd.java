package org.apache.cloudstack.storage.datastore.command;

import org.apache.cloudstack.storage.datastore.client.BaseCommand;
import org.apache.cloudstack.storage.datastore.response.ListTsmsResponse;
import org.apache.cloudstack.storage.datastore.response.UpdateFileSystemCmdResponse;
import org.apache.cloudstack.storage.datastore.response.UpdateStorageCmdResponse;

public class UpdateStorageCmd extends BaseCommand {
	
	public UpdateStorageCmd() {
		super("updateStorage", new UpdateStorageCmdResponse() );
	}

}
