package com.cloud.projects;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name="projects")
public class ProjectVO implements Project{
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;
    
    @Column(name="name")
    private String name;
    
    @Column(name="display_text")
    String displayText;
    
    @Column(name="domain_id")
    long domainId;

    @Column(name="account_id")
    long accountId;
    
    @Column(name="data_center_id")
    long dataCenterId;
    
    @Column(name=GenericDao.CREATED_COLUMN)
    private Date created;
    
    @Column(name=GenericDao.REMOVED_COLUMN)
    private Date removed;
    
    @Column(name="cleanup_needed")
    private boolean needsCleanup = false;
    
    protected ProjectVO(){
    }
    
    public ProjectVO(String name, String displayText, long dataCenterId, long accountId, long domainId) {
        this.name = name;
        this.displayText = displayText;
        this.accountId = accountId;
        this.domainId = domainId;
        this.dataCenterId = dataCenterId;
    }
    
    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    public void setDomainId(long domainId) {
        this.domainId = domainId;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public Date getRemoved() {
        return removed;
    }
    
    @Override
    public long getDataCenterId() {
        return dataCenterId;
    }
    
    public void setNeedsCleanup(boolean value) {
        needsCleanup = value;
    }
    
    public boolean getNeedsCleanup() {
        return needsCleanup;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("Project[");
        buf.append(id).append("|").append(name).append("|domainid=").append(domainId).append("]");
        return buf.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ProjectVO)) {
            return false;
        }
        ProjectVO that = (ProjectVO)obj;
        if (this.id != that.id) {
            return false;
        }
        
        return true;
    }
}
