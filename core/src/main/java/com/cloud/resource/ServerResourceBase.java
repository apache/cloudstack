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

package com.cloud.resource;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.script.Script;

public abstract class ServerResourceBase implements ServerResource {
    private static final Logger s_logger = Logger.getLogger(ServerResourceBase.class);
    protected String _name;
    private ArrayList<String> _warnings = new ArrayList<String>();
    private ArrayList<String> _errors = new ArrayList<String>();
    protected NetworkInterface _publicNic;
    protected NetworkInterface _privateNic;
    protected NetworkInterface _storageNic;
    protected NetworkInterface _storageNic2;
    protected IAgentControl _agentControl;

    @Override
    public String getName() {
        return _name;
    }

    protected String findScript(String script) {
        return Script.findScript(getDefaultScriptsDir(), script);
    }

    protected abstract String getDefaultScriptsDir();

    @Override
    public boolean configure(final String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;

        String publicNic = (String)params.get("public.network.device");
        if (publicNic == null) {
            publicNic = "xenbr1";
        }
        String privateNic = (String)params.get("private.network.device");
        if (privateNic == null) {
            privateNic = "xenbr0";
        }
        final String storageNic = (String)params.get("storage.network.device");
        final String storageNic2 = (String)params.get("storage.network.device.2");

        _privateNic = getNetworkInterface(privateNic);
        _publicNic = getNetworkInterface(publicNic);
        _storageNic = getNetworkInterface(storageNic);
        _storageNic2 = getNetworkInterface(storageNic2);

        if (_privateNic == null) {
            s_logger.warn("Nics are not specified in properties file/db, will try to autodiscover");

            Enumeration<NetworkInterface> nics = null;
            try {
                nics = NetworkInterface.getNetworkInterfaces();
                if (nics == null || !nics.hasMoreElements()) {
                    throw new ConfigurationException("Private NIC is not configured");
                }
            } catch (final SocketException e) {
                throw new ConfigurationException("Private NIC is not configured");
            }

            while (nics.hasMoreElements()) {
                final NetworkInterface nic = nics.nextElement();
                final String nicName = nic.getName();
                //  try {
                if (//!nic.isLoopback() &&
                        //nic.isUp() &&
                        !nic.isVirtual() && !nicName.startsWith("vnif") && !nicName.startsWith("vnbr") && !nicName.startsWith("peth") && !nicName.startsWith("vif") &&
                        !nicName.startsWith("virbr") && !nicName.contains(":")) {
                    final String[] info = NetUtils.getNicParams(nicName);
                    if (info != null && info[0] != null) {
                        _privateNic = nic;
                        s_logger.info("Designating private to be nic " + nicName);
                        break;
                    }
                }
                //      } catch (final SocketException e) {
                //        s_logger.warn("Error looking at " + nicName, e);
                //  }
                s_logger.debug("Skipping nic " + nicName);
            }

            if (_privateNic == null) {
                throw new ConfigurationException("Private NIC is not configured");
            }
        }
        String infos[] = NetUtils.getNetworkParams(_privateNic);
        if (infos == null) {
            s_logger.warn("Incorrect details for private Nic during initialization of ServerResourceBase");
            return false;
        }
        params.put("host.ip", infos[0]);
        params.put("host.mac.address", infos[1]);

        return true;
    }

    protected NetworkInterface getNetworkInterface(String nicName) {
        s_logger.debug("Retrieving network interface: " + nicName);
        if (nicName == null) {
            return null;
        }

        if (nicName.trim().length() == 0) {
            return null;
        }

        nicName = nicName.trim();

        NetworkInterface nic;
        try {
            nic = NetworkInterface.getByName(nicName);
            if (nic == null) {
                s_logger.debug("Unable to get network interface for " + nicName);
                return null;
            }

            return nic;
        } catch (final SocketException e) {
            s_logger.warn("Unable to get network interface for " + nicName, e);
            return null;
        }
    }

    protected void fillNetworkInformation(final StartupCommand cmd) {
        String[] info = null;
        if (_privateNic != null) {
            info = NetUtils.getNetworkParams(_privateNic);
            if (info != null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Parameters for private nic: " + info[0] + " - " + info[1] + "-" + info[2]);
                }
                cmd.setPrivateIpAddress(info[0]);
                cmd.setPrivateMacAddress(info[1]);
                cmd.setPrivateNetmask(info[2]);
            }
        }

        if (_storageNic != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Storage has its now nic: " + _storageNic.getName());
            }
            info = NetUtils.getNetworkParams(_storageNic);
        }

        // NOTE: In case you're wondering, this is not here by mistake.
        if (info != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Parameters for storage nic: " + info[0] + " - " + info[1] + "-" + info[2]);
            }
            cmd.setStorageIpAddress(info[0]);
            cmd.setStorageMacAddress(info[1]);
            cmd.setStorageNetmask(info[2]);
        }

        if (_publicNic != null) {
            info = NetUtils.getNetworkParams(_publicNic);
            if (info != null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Parameters for public nic: " + info[0] + " - " + info[1] + "-" + info[2]);
                }
                cmd.setPublicIpAddress(info[0]);
                cmd.setPublicMacAddress(info[1]);
                cmd.setPublicNetmask(info[2]);
            }
        }

        if (_storageNic2 != null) {
            info = NetUtils.getNetworkParams(_storageNic2);
            if (info != null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Parameters for storage nic 2: " + info[0] + " - " + info[1] + "-" + info[2]);
                }
                cmd.setStorageIpAddressDeux(info[0]);
                cmd.setStorageMacAddressDeux(info[1]);
                cmd.setStorageNetmaskDeux(info[2]);
            }
        }
    }

    @Override
    public void disconnected() {
    }

    @Override
    public IAgentControl getAgentControl() {
        return _agentControl;
    }

    @Override
    public void setAgentControl(IAgentControl agentControl) {
        _agentControl = agentControl;
    }

    protected void recordWarning(final String msg, final Throwable th) {
        final String str = getLogStr(msg, th);
        synchronized (_warnings) {
            _warnings.add(str);
        }
    }

    protected void recordWarning(final String msg) {
        recordWarning(msg, null);
    }

    protected List<String> getWarnings() {
        synchronized (_warnings) {
            final List<String> results = new LinkedList<String>(_warnings);
            _warnings.clear();
            return results;
        }
    }

    protected List<String> getErrors() {
        synchronized (_errors) {
            final List<String> result = new LinkedList<String>(_errors);
            _errors.clear();
            return result;
        }
    }

    protected void recordError(final String msg, final Throwable th) {
        final String str = getLogStr(msg, th);
        synchronized (_errors) {
            _errors.add(str);
        }
    }

    protected void recordError(final String msg) {
        recordError(msg, null);
    }

    protected Answer createErrorAnswer(final Command cmd, final String msg, final Throwable th) {
        final StringWriter writer = new StringWriter();
        if (msg != null) {
            writer.append(msg);
        }
        writer.append("===>Stack<===");
        th.printStackTrace(new PrintWriter(writer));
        return new Answer(cmd, false, writer.toString());
    }

    protected String createErrorDetail(final String msg, final Throwable th) {
        final StringWriter writer = new StringWriter();
        if (msg != null) {
            writer.append(msg);
        }
        writer.append("===>Stack<===");
        th.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    protected String getLogStr(final String msg, final Throwable th) {
        final StringWriter writer = new StringWriter();
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
        return true;
    }
}
