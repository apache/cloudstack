package org.apache.cloudstack.backup;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.ResourceDetail;

@Entity
@Table(name = "backup_offering_details")
public class BackupOfferingDetailsVO implements ResourceDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "backup_offering_id")
    private long resourceId;

    @Column(name = "name")
    private String name;

    @Column(name = "value")
    private String value;

    @Column(name = "display")
    private boolean display = true;

    protected BackupOfferingDetailsVO() {
    }

    public BackupOfferingDetailsVO(long backupOfferingId, String name, String value, boolean display) {
        this.resourceId = backupOfferingId;
        this.name = name;
        this.value = value;
        this.display = display;
    }

    @Override
    public long getResourceId() {
        return resourceId;
    }

    public void setResourceId(long backupOfferingId) {
        this.resourceId = backupOfferingId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public boolean isDisplay() {
        return display;
    }
}

