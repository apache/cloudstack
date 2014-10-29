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
package com.cloud.resource;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.log4j.Logger;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.manager.MockAgentManager;
import com.cloud.agent.manager.MockStorageManager;
import com.cloud.agent.manager.MockVmManager;
import com.cloud.agent.manager.SimulatorManager;
import com.cloud.agent.manager.SimulatorManager.AgentType;
import com.cloud.agent.manager.SimulatorManagerImpl;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.simulator.MockHost;
import com.cloud.utils.component.ComponentContext;

public class AgentResourceBase implements ServerResource {
    private static final Logger s_logger = Logger.getLogger(AgentResourceBase.class);

    protected String _name;
    private List<String> _warnings = new LinkedList<String>();
    private List<String> _errors = new LinkedList<String>();

    private transient IAgentControl _agentControl;

    protected long _instanceId;

    private Type _type;

    @Inject
    protected SimulatorManager _simMgr;
    protected MockHost agentHost = null;
    protected boolean stopped = false;
    protected String hostGuid = null;

    public AgentResourceBase(long instanceId, AgentType agentType, SimulatorManager simMgr, String hostGuid) {
        _instanceId = instanceId;

        if (s_logger.isDebugEnabled()) {
            s_logger.info("New Routing host instantiated with guid:" + hostGuid);
        }

        if (agentType == AgentType.Routing) {
            _type = Host.Type.Routing;
        } else {
            _type = Host.Type.Storage;
        }

        this.hostGuid = hostGuid;
    }

    protected MockVmManager getVmMgr() {
        return _simMgr.getVmMgr();
    }

    protected MockStorageManager getStorageMgr() {
        return _simMgr.getStorageMgr();
    }

    protected MockAgentManager getAgentMgr() {
        return _simMgr.getAgentMgr();
    }

    protected long getInstanceId() {
        return _instanceId;
    }

    public AgentResourceBase() {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Deserializing simulated agent on reconnect");
        }

    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public void setName(String name) {
        _name = name;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        hostGuid = (String)params.get("guid");

        _simMgr = ComponentContext.inject(SimulatorManagerImpl.class);
        _simMgr.configure(name, params);

        agentHost = getAgentMgr().getHost(hostGuid);
        return true;
    }

    private void reconnect(MockHost host) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Reconfiguring existing simulated host w/ name: " + host.getName() + " and guid: " + host.getGuid());
        }
        this.agentHost = host;
    }

    @Override
    public void disconnected() {
        this.stopped = true;
    }

    protected void recordWarning(String msg, Throwable th) {
        String str = getLogStr(msg, th);
        synchronized (_warnings) {
            _warnings.add(str);
        }
    }

    protected void recordWarning(String msg) {
        recordWarning(msg, null);
    }

    protected List<String> getWarnings() {
        synchronized (this) {
            List<String> results = _warnings;
            _warnings = new ArrayList<String>();
            return results;
        }
    }

    protected List<String> getErrors() {
        synchronized (this) {
            List<String> result = _errors;
            _errors = new ArrayList<String>();
            return result;
        }
    }

    protected void recordError(String msg, Throwable th) {
        String str = getLogStr(msg, th);
        synchronized (_errors) {
            _errors.add(str);
        }
    }

    protected void recordError(String msg) {
        recordError(msg, null);
    }

    protected Answer createErrorAnswer(Command cmd, String msg, Throwable th) {
        StringWriter writer = new StringWriter();
        if (msg != null) {
            writer.append(msg);
        }
        writer.append("===>Stack<===");
        th.printStackTrace(new PrintWriter(writer));
        return new Answer(cmd, false, writer.toString());
    }

    protected String createErrorDetail(String msg, Throwable th) {
        StringWriter writer = new StringWriter();
        if (msg != null) {
            writer.append(msg);
        }
        writer.append("===>Stack<===");
        th.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    protected String getLogStr(String msg, Throwable th) {
        StringWriter writer = new StringWriter();
        writer.append(new Date().toString()).append(": ").append(msg);
        if (th != null) {
            writer.append("\n  Exception: ");
            th.printStackTrace(new PrintWriter(writer));
        }
        return writer.toString();
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        this.stopped = true;
        return true;
    }

    @Override
    public IAgentControl getAgentControl() {
        return _agentControl;
    }

    @Override
    public void setAgentControl(IAgentControl agentControl) {
        _agentControl = agentControl;
    }

    protected String findScript(String script) {
        s_logger.debug("Looking for " + script + " in the classpath");
        URL url = ClassLoader.getSystemResource(script);
        File file = null;
        if (url == null) {
            file = new File("./" + script);
            s_logger.debug("Looking for " + script + " in " + file.getAbsolutePath());
            if (!file.exists()) {
                return null;
            }
        } else {
            file = new File(url.getFile());
        }
        return file.getAbsolutePath();
    }

    @Override
    public final Answer executeRequest(final Command cmd) {
        final AtomicReference<Answer> result = new AtomicReference<Answer>();
        new ManagedContextRunnable() {
            @Override
            protected void runInContext() {
                result.set(executeRequestInContext(cmd));
            }
        }.run();

        return result.get();
    }

    public Answer executeRequestInContext(Command cmd) {
        return null;
    }

    @Override
    public PingCommand getCurrentStatus(long id) {
        return null;
    }

    @Override
    public Type getType() {
        return _type;
    }

    public void setType(Host.Type type) {
        this._type = type;
    }

    @Override
    public StartupCommand[] initialize() {
        return null;
    }

    public SimulatorManager getSimulatorManager() {
        return _simMgr;
    }

    public void setSimulatorManager(SimulatorManager simMgr) {
        _simMgr = simMgr;
    }

    public boolean isStopped() {
        return this.stopped;
    }

    @Override
    public void setConfigParams(Map<String, Object> params) {
        // TODO Auto-generated method stub

    }

    @Override
    public Map<String, Object> getConfigParams() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getRunLevel() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setRunLevel(int level) {
        // TODO Auto-generated method stub

    }
}
