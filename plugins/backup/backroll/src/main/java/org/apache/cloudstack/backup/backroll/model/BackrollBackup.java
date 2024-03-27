package org.apache.cloudstack.backup.backroll.model;

public class BackrollBackup {
    private String archive;

    public String getArchive() {
        return archive;
    }

    public BackrollBackup(String archive) {
        this.archive = archive;
    }
}
