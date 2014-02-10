package org.apache.cloudstack.storage.datastore.command;

import org.apache.cloudstack.storage.datastore.client.BaseCommand;
import org.apache.cloudstack.storage.datastore.response.DeleteVolumeResponse;

public class DeleteVolumeCmd extends BaseCommand {

    public DeleteVolumeCmd() {
        super("deleteFileSystem", new DeleteVolumeResponse());
    }

}
