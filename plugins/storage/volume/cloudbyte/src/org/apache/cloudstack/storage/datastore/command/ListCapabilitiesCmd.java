package org.apache.cloudstack.storage.datastore.command;

import org.apache.cloudstack.storage.datastore.client.BaseCommand;
import org.apache.cloudstack.storage.datastore.response.ListCapabilitiesResponse;

public class ListCapabilitiesCmd extends BaseCommand {

    public ListCapabilitiesCmd() {
        super("listCapabilities", new ListCapabilitiesResponse());
    }

}
