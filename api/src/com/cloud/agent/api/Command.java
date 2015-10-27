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
package com.cloud.agent.api;

import java.util.HashMap;
import java.util.Map;

import com.cloud.agent.api.LogLevel.Log4jLevel;

/**
 * implemented by classes that extends the Command class. Command specifies
 *
 */
public abstract class Command {

    public static enum OnError {
        Continue, Stop
    }

    public static final String HYPERVISOR_TYPE = "hypervisorType";

    // allow command to carry over hypervisor or other environment related context info
    @LogLevel(Log4jLevel.Trace)
    protected Map<String, String> contextMap = new HashMap<String, String>();
    private int wait;  //in second

    protected Command() {
        this.wait = 0;
    }

    public int getWait() {
        return wait;
    }

    public void setWait(int wait) {
        this.wait = wait;
    }

    @Override
    public final String toString() {
        return this.getClass().getName();
    }

    /**
     * @return Does this command need to be executed in sequence on the agent?
     *         When this is set to true, the commands are executed by a single
     *         thread on the agent.
     */
    public abstract boolean executeInSequence();

    public void setContextParam(String name, String value) {
        contextMap.put(name, value);
    }

    public String getContextParam(String name) {
        return contextMap.get(name);
    }

    public boolean allowCaching() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Command)) return false;

        Command command = (Command) o;

        if (wait != command.wait) return false;
        if (contextMap != null ? !contextMap.equals(command.contextMap) : command.contextMap != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = contextMap != null ? contextMap.hashCode() : 0;
        result = 31 * result + wait;
        return result;
    }
}
