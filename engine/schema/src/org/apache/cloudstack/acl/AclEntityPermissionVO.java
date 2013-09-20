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
    private Long entityId;

    @Column(name = "access_type")
    @Enumerated(value = EnumType.STRING)
    AccessType accessType;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

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

    @Override
    public AccessType getAccessType() {
        return accessType;
    }

    public Date getRemoved() {
        return removed;
    }

    public Date getCreated() {
        return created;
    }
}
