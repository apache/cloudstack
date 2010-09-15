package com.cloud.vm;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.Table;

import com.cloud.user.OwnedBy;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name="instance_group")
@SecondaryTable(name="account",
        pkJoinColumns={@PrimaryKeyJoinColumn(name="account_id", referencedColumnName="id")})
public class InstanceGroupVO implements OwnedBy{
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;
    
    @Column(name="name")
    String name;
    
    @Column(name="account_id")
    private long accountId;
    
    @Column(name="domain_id", table="account", insertable=false, updatable=false)
    private long domainId;
    
    @Column(name=GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name=GenericDao.CREATED_COLUMN)
    private Date created;
    
    public InstanceGroupVO(String name, long accountId) {
        this.name = name;
        this.accountId = accountId;
    }
    
    protected InstanceGroupVO() {
        super();
    }
    
    public long getId() {
    	return id;
    }
    
    public String getName() {
    	return name; 
    }
    
    public long getAccountId() {
        return accountId;
    }
    
    public long getDomainId() {
        return domainId;
    }
    
    public Date getRemoved() {
        return removed;
    }
    
	public Date getCreated() {
		return created;
	}
    
    public void setName(String name) {
    	this.name = name;
    }

}
