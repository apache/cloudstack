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
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * implemented by classes that extends the Command class. Command specifies
 *
 */
public abstract class Command {

    protected transient Logger logger = LogManager.getLogger(getClass());

    public static enum OnError {
        Continue, Stop
    }

    public enum State {
        CREATED,        // Command is created by management server
        STARTED,        // Command is started by agent
        PROCESSING,     // Processing by agent
        PROCESSING_IN_BACKEND,  // Processing in backend by agent
        COMPLETED,      // Operation succeeds by agent or management server
        FAILED,         // Operation fails by agent
        RECONCILE_RETRY,        // Ready for retry of reconciliation
        RECONCILING,    // Being reconciled by management server
        RECONCILED,     // Reconciled by management server
        RECONCILE_SKIPPED,  // Skip the reconciliation as the resource state is inconsistent with the command
        RECONCILE_FAILED,       // Fail to reconcile by management server
        TIMED_OUT,      // Timed out on management server or agent
        INTERRUPTED,    // Interrupted by management server or agent (for example agent is restarted),
        DANGLED_IN_BACKEND     // Backend process which cannot be processed normally (for example agent is restarted)
    }

    public static final String HYPERVISOR_TYPE = "hypervisorType";

    // allow command to carry over hypervisor or other environment related context info
    @LogLevel(Log4jLevel.Trace)
    protected Map<String, String> contextMap = new HashMap<String, String>();
    private int wait;  //in second
    private boolean bypassHostMaintenance = false;
    private transient long requestSequence = 0L;

    protected Command() {
        this.wait = 0;
    }

    public int getWait() {
        return wait;
    }

    /**
     * This is the time in seconds that the agent will wait before waiting for an answer from the endpoint.
     * The actual wait time is twice the value of this variable.
     * See {@link com.cloud.agent.manager.AgentAttache#send(com.cloud.agent.transport.Request, int) AgentAttache#send}  implementation for more details.
     *
     * @param wait
     **/
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

    public Map<String, String> getContextMap() {
        return contextMap;
    }

    public boolean allowCaching() {
        return true;
    }

    public boolean isBypassHostMaintenance() {
        return bypassHostMaintenance;
    }

    public void setBypassHostMaintenance(boolean bypassHostMaintenance) {
        this.bypassHostMaintenance = bypassHostMaintenance;
    }

    public boolean isReconcile() {
        return false;
    }

    public long getRequestSequence() {
        return requestSequence;
    }

    public void setRequestSequence(long requestSequence) {
        this.requestSequence = requestSequence;
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
