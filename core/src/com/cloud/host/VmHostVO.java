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
package com.cloud.host;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "op_vm_host")
public class VmHostVO {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    long id;
    
    @Column(name = "vnc_ports", nullable = false)
    long vncPorts;
    
    @Column(name = "start_at", nullable = false)
    int startAt;

    public VmHostVO(long id, long vncPorts, int startAt) {
        super();
        this.id = id;
        this.vncPorts = vncPorts;
        this.startAt = startAt;
    }
    
    protected VmHostVO() {
    }
    
    public VmHostVO(long id, int[] vncPorts) {
        this.id = id;
        
        for (int port : vncPorts) {
            this.vncPorts = markPortUsed(this.vncPorts, port);
        }
    }

    public long getVncPorts() {
        return vncPorts;
    }

    public void setVncPorts(long vncPorts) {
        this.vncPorts = vncPorts;
    }

    public int getStartAt() {
        return startAt;
    }

    public void setStartAt(int startAt) {
        this.startAt = startAt;
    }

    public long getId() {
        return id;
    }
    
    public static long markPortAvailable(long ports, int port) {
        assert (port < 63) : "Only supports 63 ports";
        return ports & ~(1l << port);
    }
    
    public static long markPortUsed(long ports, int port) {
        assert (port < 63) : "Only supports 63 ports";
        return ports | (1l << port);
    }
}
