package org.apache.cloudstack.storage.datastore.command;

import org.apache.cloudstack.storage.datastore.client.BaseCommand;
import org.apache.cloudstack.storage.datastore.response.ListTsmsResponse;
import org.apache.cloudstack.storage.datastore.response.UpdateFileSystemCmdResponse;
import org.apache.cloudstack.storage.datastore.response.UpdateQosGroupCmdResponse;

public class UpdateQosGroupCmd extends BaseCommand {
	
	public UpdateQosGroupCmd() {
		super("updateQosGroup", new UpdateQosGroupCmdResponse() );
	}

}
