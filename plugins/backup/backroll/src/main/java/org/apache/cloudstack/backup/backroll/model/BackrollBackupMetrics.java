package org.apache.cloudstack.backup.backroll.model;

public class BackrollBackupMetrics {
    private long size;
    private long deduplicated;

    public long getSize() {
        return size;
    }

    public long getDeduplicated() {
        return deduplicated;
    }

    public BackrollBackupMetrics(long size, long deduplicated) {
        this.size = size;
        this.deduplicated = deduplicated;
    }
}
