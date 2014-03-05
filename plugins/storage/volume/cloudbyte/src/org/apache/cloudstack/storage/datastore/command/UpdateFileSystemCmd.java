package org.apache.cloudstack.storage.datastore.command;

import org.apache.cloudstack.storage.datastore.client.BaseCommand;
import org.apache.cloudstack.storage.datastore.response.ListTsmsResponse;
import org.apache.cloudstack.storage.datastore.response.UpdateFileSystemCmdResponse;

public class UpdateFileSystemCmd extends BaseCommand {
	
	public UpdateFileSystemCmd() {
		super("updateFileSystem", new UpdateFileSystemCmdResponse() );
	}

}
