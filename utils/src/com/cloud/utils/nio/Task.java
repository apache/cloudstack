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
package com.cloud.utils.nio;

import org.apache.log4j.Logger;



/**
 * Task represents one todo item for the AgentManager or the AgentManager
 * received and is assigning to the handler threads.
 *
 */
public abstract class Task implements Runnable {
	private static final Logger s_logger = Logger.getLogger(Task.class);
	
    public enum Type {
        CONNECT,     // Process a new connection.
        DISCONNECT,  // Process an existing connection disconnecting.
        DATA,        // data incoming.
        CONNECT_FAILED, // Connection failed.
        OTHER        // Allows other tasks to be defined by the caller.
    };
    
    Object _data;
    Type _type;
    Link _link;

    public Task(Type type, Link link, byte[] data) {
        _data = data;
        _type = type;
        _link = link;
    }
    
    public Task(Type type, Link link, Object data) {
        _data = data;
        _type = type;
        _link = link;
    }
    
    protected Task() {
    }
    
    public Type getType() {
        return _type;
    }
    
    public Link getLink() {
        return _link;
    }
    
    public byte[] getData() {
        return (byte[])_data;
    }
    
    public Object get() {
        return _data;
    }
    
    @Override
	public String toString() {
        return _type.toString();
    }
    
    abstract protected void doTask(Task task) throws Exception;
    
    @Override
    public final void run() {
    	try {
    		doTask(this);
    	} catch (Exception e) {
    		s_logger.warn("Caught the following exception but pushing on", e);
    	}
    }
}
