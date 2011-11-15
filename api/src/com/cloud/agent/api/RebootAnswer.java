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
package com.cloud.agent.api;

public class RebootAnswer extends Answer {
    Long bytesSent;
    Long bytesReceived;
    Integer vncPort;
    
    protected RebootAnswer() {
    }
    
    public RebootAnswer(RebootCommand cmd, String details, Long bytesSent, Long bytesReceived, Integer vncport) {
        super(cmd, true, details);
        this.bytesReceived = bytesReceived;
        this.bytesSent = bytesSent;
        this.vncPort = vncport;
    }
    
    public RebootAnswer(RebootCommand cmd, String details, Long bytesSent, Long bytesReceived) {
        super(cmd, true, details);
        this.bytesReceived = bytesReceived;
        this.bytesSent = bytesSent;
        this.vncPort = null;
    }
    
    public RebootAnswer(RebootCommand cmd, String details) {
        super(cmd, false, details);
        bytesSent = null;
        bytesReceived = null;
    }
    
    public RebootAnswer(RebootCommand cmd, Exception e) {
        super(cmd, e);
    }
    
    public void setBytesReceived(Long bytesReceived) {
        this.bytesReceived = bytesReceived;
    }
    
    public Long getBytesReceived() {
        return bytesReceived;
    }
    
    public void setBytesSent(Long bytesSent) {
        this.bytesSent = bytesSent;
    }
    
    public Long getBytesSent() {
        return bytesSent;
    }
    public Integer getVncPort() {
    	return vncPort;
    }
}
