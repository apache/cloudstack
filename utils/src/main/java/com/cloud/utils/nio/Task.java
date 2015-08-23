//
// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//

package com.cloud.utils.nio;

import org.apache.log4j.Logger;

/**
 * Task represents one todo item for the AgentManager or the AgentManager
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
        } catch (Throwable e) {
            s_logger.warn("Caught the following exception but pushing on", e);
        }
    }
}
