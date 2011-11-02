package com.cloud.projects;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.api.Identity;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name="project_invitations")
public class ProjectInvitationVO implements ProjectInvitation, Identity {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;
    
    @Column(name="project_id")
    private long projectId;

    @Column(name="account_id")
    private Long accountId;
    
    @Column(name="domain_id")
    private Long domainId;
    
    @Column(name="token")
    private String token;
    
    @Column(name="email")
    private String email;
    
    @Column(name="state")
    @Enumerated(value=EnumType.STRING)
    private State state = State.Pending;
    
    @Column(name=GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name="uuid")
    private String uuid;
    
    protected ProjectInvitationVO(){
    	this.uuid = UUID.randomUUID().toString();
    }
    
    public ProjectInvitationVO(long projectId, Long accountId, Long domainId, String email, String token) {
       this.accountId = accountId;
       this.domainId = domainId;
       this.projectId = projectId;
       this.email = email;
       this.token = token;
   		this.uuid = UUID.randomUUID().toString();
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public long getProjectId() {
        return projectId;
    }

    @Override
    public Long getAccountId() {
        return accountId;
    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public Date getCreated() {
        return created;
    } 
    
    @Override
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }
    
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("ProjectInvitation[");
        buf.append(id).append("|projectId=").append(projectId).append("|accountId=").append(accountId).append("]");
        return buf.toString();
    }

    @Override
    public Long getDomainId() {
        return domainId;
    }
    
    @Override
    public String getUuid() {
    	return this.uuid;
    }
    
    public void setUuid(String uuid) {
    	this.uuid = uuid;
    }
}
