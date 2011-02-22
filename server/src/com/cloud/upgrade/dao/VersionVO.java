/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
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
package com.cloud.upgrade.dao;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name="version")
public class VersionVO {
    public enum Step {
        Dump,
        Upgrade,
        Cleanup,
        Complete
    };
    
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    long id;
    
    @Column(name="version")
    String version;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="updated")
    Date updated;
    
    @Enumerated(value=EnumType.STRING)
    @Column(name="step")
    Step step;
    
    public VersionVO(String version) {
        this.version = version; 
        this.updated = new Date();
        this.step = Step.Upgrade;
    }
    
    protected VersionVO() {
    }
    
    public long getId() {
        return id;
    }
    
    public String getVersion() {
        return version;
    }
    
    public Date getUpdated() {
        return updated;
    }
    
    public void setUpdated(Date updated) {
        this.updated = updated;
    }
    
    public Step getStep() {
        return step;
    }
    
    public void setStep(Step step) {
        this.step = step;
    }
    
}
