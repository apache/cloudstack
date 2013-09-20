package org.apache.cloudstack.acl;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = ("acl_api_permission"))
public class AclApiPermissionVO implements AclApiPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "role_id")
    private long aclRoleId;

    @Column(name = "api")
    private String apiName;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Override
    public long getId() {
        return id;
    }

    @Override
    public Long getAclRoleId() {
        return aclRoleId;
    }

    @Override
    public String getApiName() {
        return apiName;
    }

    public Date getRemoved() {
        return removed;
    }

    public Date getCreated() {
        return created;
    }
}
