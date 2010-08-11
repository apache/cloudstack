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
package com.cloud.maint;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;


@Entity
@Table(name="op_host_upgrade")
public class AgentUpgradeVO {
    @Id
    @Column(name="host_id")
    private long id;
    
    @Column(name="version")
    private String version;
    
    @Column(name="state")
    @Enumerated(value=EnumType.STRING)
    private UpgradeManager.State state;
    
    protected AgentUpgradeVO() {
    }
    
    public AgentUpgradeVO(long id, String version, UpgradeManager.State state) {
        this.id = id;
        this.version = version;
        this.state = state;
    }
    
    public long getId() {
        return id;
    }
    
    public String getVersion() {
        return version;
    }
    
    public UpgradeManager.State getState() {
        return state;
    }
}
