package org.apache.cloudstack.acl;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.acl.SecurityChecker.AccessType;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = ("acl_entity_permission"))
public class AclEntityPermissionVO implements AclEntityPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "group_id")
    private long aclGroupId;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private long entityId;
    
    @Column(name = "entity_uuid")
    private String entityUuid;

    @Column(name = "access_type")
    @Enumerated(value = EnumType.STRING)
    AccessType accessType;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    public AclEntityPermissionVO() {

    }

    public AclEntityPermissionVO(long groupId, String entityType, long entityId, String entityUuid, AccessType atype) {
        aclGroupId = groupId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.entityUuid = entityUuid;
        accessType = atype;
    }
    
    @Override
    public long getId() {
        return id;
    }

    @Override
    public Long getAclGroupId() {
        return aclGroupId;
    }

    @Override
    public String getEntityType() {
        return entityType;
    }

    @Override
    public Long getEntityId() {
        return entityId;
    }

    public String getEntityUuid() {
        return entityUuid;
    }

    @Override
    public AccessType getAccessType() {
        return accessType;
    }


    public void setAclGroupId(long aclGroupId) {
        this.aclGroupId = aclGroupId;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public void setEntityId(long entityId) {
        this.entityId = entityId;
    }

    public void setEntityUuid(String entityUuid) {
        this.entityUuid = entityUuid;
    }

    public void setAccessType(AccessType accessType) {
        this.accessType = accessType;
    }

    public Date getRemoved() {
        return removed;
    }

    public Date getCreated() {
        return created;
    }
}
