package org.apache.cloudstack.storage.datastore.command;

import org.apache.cloudstack.storage.datastore.client.BaseCommand;
import org.apache.cloudstack.storage.datastore.response.ListHAPoolResponse;


public class ListHAPoolCmd extends BaseCommand {
	
	public ListHAPoolCmd() {
		super("listHAPool", new ListHAPoolResponse() );
	}

}
