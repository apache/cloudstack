package org.apache.cloudstack.engine.cloud.entity.api.db;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "vm_root_disk_tags")
public class VMRootDiskTagVO implements InternalIdentity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "vm_id")
    private long vmId;

    @Column(name = "compute_tag")
    private String rootDiskTag;

    /**
     * There should never be a public constructor for this class. Since it's
     * only here to define the table for the DAO class.
     */
    protected VMRootDiskTagVO() {
    }

    public VMRootDiskTagVO(long vmId, String rootDiskTag) {
        this.vmId = vmId;
        this.rootDiskTag = rootDiskTag;
    }

    public long getId() {
        return id;
    }

    public long getVmId() {
        return vmId;
    }

    public String getRootDiskTag() {
        return rootDiskTag;
    }

}
