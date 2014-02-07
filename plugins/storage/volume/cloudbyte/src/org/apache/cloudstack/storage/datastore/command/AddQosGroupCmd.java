package org.apache.cloudstack.storage.datastore.command;

import org.apache.cloudstack.storage.datastore.client.BaseCommand;
import org.apache.cloudstack.storage.datastore.response.AddQosGroupCmdResponse;




public class AddQosGroupCmd extends BaseCommand {

public AddQosGroupCmd(){
		
		super("addQosGroup", new AddQosGroupCmdResponse());

	}
	
	
}
