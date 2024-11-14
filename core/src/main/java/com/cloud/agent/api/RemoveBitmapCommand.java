package com.cloud.agent.api;

import org.apache.cloudstack.storage.to.SnapshotObjectTO;

public class RemoveBitmapCommand extends Command {

    private SnapshotObjectTO snapshotObjectTO;

    private boolean isVmRunning;

    public RemoveBitmapCommand(SnapshotObjectTO snapshotObjectTO, boolean isVmRunning) {
        this.snapshotObjectTO = snapshotObjectTO;
        this.isVmRunning = isVmRunning;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }

    public SnapshotObjectTO getSnapshotObjectTO() {
        return snapshotObjectTO;
    }

    public boolean isVmRunning() {
        return isVmRunning;
    }
}
