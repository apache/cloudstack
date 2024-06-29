package org.apache.cloudstack.backup.backroll.model;

import java.util.Date;

import org.apache.cloudstack.backup.BackupOffering;

public class BackrollOffering implements BackupOffering {

    private String name;
    private String uid;

    public BackrollOffering(String name, String uid) {
        this.name = name;
        this.uid = uid;
    }

    @Override
    public String getExternalId() {
        return uid;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return "Backroll Backup Offering (Job)";
    }

    @Override
    public long getZoneId() {
        return -1;
    }

    @Override
    public boolean isUserDrivenBackupAllowed() {
        return false;
    }

    @Override
    public String getProvider() {
        return "backroll";
    }

    @Override
    public Date getCreated() {
        return null;
    }

    @Override
    public String getUuid() {
        return uid;
    }

    @Override
    public long getId() {
        return -1;
    }
}
