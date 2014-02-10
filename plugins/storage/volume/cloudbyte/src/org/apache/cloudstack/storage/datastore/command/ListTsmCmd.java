package org.apache.cloudstack.storage.datastore.command;

import org.apache.cloudstack.storage.datastore.client.BaseCommand;
import org.apache.cloudstack.storage.datastore.response.ListTsmsResponse;

public class ListTsmCmd extends BaseCommand {

    public ListTsmCmd() {
        super("listTsm", new ListTsmsResponse());
    }

}
