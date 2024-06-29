package org.apache.cloudstack.backup.backroll.model;

import java.util.Date;

public class BackrollVmBackup {
    private String id;
    private String name;
    private Date date;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Date getDate() {
        return date;
    }

    public BackrollVmBackup(String id, String name, Date date) {
        this.id = id;
        this.name = name;
        this.date = date;
    }
}