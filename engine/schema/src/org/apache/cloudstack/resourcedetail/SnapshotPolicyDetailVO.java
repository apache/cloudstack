package org.apache.cloudstack.resourcedetail;

import org.apache.cloudstack.api.ResourceDetail;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "snapshot_policy_details")
public class SnapshotPolicyDetailVO implements ResourceDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "policy_id")
    private long resourceId;

    @Column(name = "name")
    private String name;

    @Column(name = "value", length = 1024)
    private String value;

    @Column(name = "display")
    private boolean display = true;

    public SnapshotPolicyDetailVO() {
    }

    public SnapshotPolicyDetailVO(long id, String name, String value) {
        this.resourceId = id;
        this.name = name;
        this.value = value;
    }

    @Override
    public long getId() {
        return id;
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
    public long getResourceId() {
        return resourceId;
    }

    @Override
    public boolean isDisplay() {
        return display;
    }
}
