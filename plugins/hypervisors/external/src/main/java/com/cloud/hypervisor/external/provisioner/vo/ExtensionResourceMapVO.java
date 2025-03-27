package com.cloud.hypervisor.external.provisioner.vo;

import org.apache.cloudstack.api.InternalIdentity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

@Entity
@Table(name = "extension_resource_map")
public class ExtensionResourceMapVO implements InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "extension_id", nullable = false)
    private Long extensionId;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "resource_type", nullable = false)
    private String resourceType;

    @Column(name = "created", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;

    @Column(name = "removed")
    @Temporal(TemporalType.TIMESTAMP)
    private Date removed;

    public ExtensionResourceMapVO() {
    }

    public ExtensionResourceMapVO(long extensionId, long resourceId, String resourceType) {
        this.extensionId = extensionId;
        this.resourceId = resourceId;
        this.resourceType = resourceType;
    }

    public Long getExtensionId() {
        return extensionId;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setExtensionId(Long extensionId) {
        this.extensionId = extensionId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    @Override
    public long getId() {
        return id;
    }
}