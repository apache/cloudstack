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

import java.util.concurrent.Callable;

import com.cloud.utils.exception.TaskExecutionException;

/**
 * Task represents one todo item for the AgentManager or the AgentManager
 */
public abstract class Task implements Callable<Boolean> {

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

    public Task(final Type type, final Link link, final byte[] data) {
        _data = data;
        _type = type;
        _link = link;
    }

    public Task(final Type type, final Link link, final Object data) {
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

    abstract protected void doTask(Task task) throws TaskExecutionException;

    @Override
    public Boolean call() throws TaskExecutionException {
        doTask(this);
        return true;
    }
}