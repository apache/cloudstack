/**
 *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
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
package com.cloud.network.element;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.network.VirtualRouterProvider;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name=("virtual_router_providers"))
public class VirtualRouterProviderVO implements VirtualRouterProvider {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    long id;
    
    @Column(name="type")
    @Enumerated(EnumType.STRING)
    private VirtualRouterProviderType type;
    
    @Column(name="enabled")
    private boolean enabled;
    
    @Column(name="nsp_id")
    private long nspId;
    
    @Column(name="uuid")
    private String uuid;
    
    @Column(name=GenericDao.REMOVED_COLUMN)
    Date removed;

    public VirtualRouterProviderVO() {
    }
    
    public VirtualRouterProviderVO(long nspId, String uuid, VirtualRouterProviderType type) {
        this.nspId = nspId;
        this.uuid = uuid;
        this.type = type;
    }

    @Override
    public long getNspId() {
        return nspId;
    }

    public String getUuid() {
        return uuid;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public VirtualRouterProviderType getType() {
        return this.type;
    }
    
    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setType(VirtualRouterProviderType type) {
        this.type = type;
    }

    public void setNspId(long nspId) {
        this.nspId = nspId;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
