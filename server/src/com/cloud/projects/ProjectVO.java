/**
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.projects;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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

    @Column(name="project_account_id")
    long projectAccountId;
    
    @Column(name="state")
    @Enumerated(value=EnumType.STRING)
    private State state;
    
    @Column(name=GenericDao.CREATED_COLUMN)
    private Date created;
    
    @Column(name=GenericDao.REMOVED_COLUMN)
    private Date removed;
    
    protected ProjectVO(){
    }
    
    public ProjectVO(String name, String displayText, long domainId, long projectAccountId) {
        this.name = name;
        this.displayText = displayText;
        this.projectAccountId = projectAccountId;
        this.domainId = domainId;
        this.state = State.Active;
    }
    
    @Override
    public String getName() {
        return name;
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
    public String toString() {
        StringBuilder buf = new StringBuilder("Project[");
        buf.append(id).append("|name=").append(name).append("|domainid=").append(domainId).append("]");
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

    @Override
    public long getProjectAccountId() {
        return projectAccountId;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void setState(State state) {
        this.state = state;
    }
    
}
