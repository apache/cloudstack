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
package com.cloud.agent;

import com.cloud.agent.Agent.ExitStatus;
import com.cloud.agent.dao.StorageComponent;
import com.cloud.agent.dao.impl.PropertiesStorage;
import com.cloud.agent.properties.AgentProperties;
import com.cloud.agent.properties.AgentPropertiesFileHandler;
import com.cloud.resource.ServerResource;
import com.cloud.utils.LogUtils;
import com.cloud.utils.ProcessUtil;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.backoff.BackoffAlgorithm;
import com.cloud.utils.backoff.impl.ConstantTimeBackoff;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;

import javax.naming.ConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

public class AgentShell implements IAgentShell, Daemon {
    protected static Logger LOGGER = LogManager.getLogger(AgentShell.class);

    private final Properties _properties = new Properties();
    private final Map<String, Object> _cmdLineProperties = new HashMap<String, Object>();
    private StorageComponent _storage;
    private BackoffAlgorithm _backoff;
    private String _version;
    private String _zone;
    private String _pod;
    private String _host;
    private String _privateIp;
    private int _port;
    private int _proxyPort;
    private int _workers;
    private String _guid;
    private int _hostCounter = 0;
    private int _nextAgentId = 1;
    private volatile boolean _exit = false;
    private int _pingRetries;
    private final List<Agent> _agents = new ArrayList<Agent>();
    private String hostToConnect;
    private String connectedHost;
    private Long preferredHostCheckInterval;
    protected AgentProperties agentProperties = new AgentProperties();

    public AgentShell() {
    }

    @Override
    public Properties getProperties() {
        return _properties;
    }

    @Override
    public BackoffAlgorithm getBackoffAlgorithm() {
        return _backoff;
    }

    @Override
    public int getPingRetries() {
        return _pingRetries;
    }

    @Override
    public String getVersion() {
        return _version;
    }

    @Override
    public String getZone() {
        return _zone;
    }

    @Override
    public String getPod() {
        return _pod;
    }

    @Override
    public String getNextHost() {
        final String[] hosts = getHosts();
        if (_hostCounter >= hosts.length) {
            _hostCounter = 0;
        }
        hostToConnect = hosts[_hostCounter % hosts.length];
        _hostCounter++;
        return hostToConnect;
    }

    @Override
    public String getConnectedHost() {
        return connectedHost;
    }

    @Override
    public long getLbCheckerInterval(final Long receivedLbInterval) {
        if (preferredHostCheckInterval != null) {
            return preferredHostCheckInterval * 1000L;
        }
        if (receivedLbInterval != null) {
            return receivedLbInterval * 1000L;
        }
        return 0L;
    }

    @Override
    public void updateConnectedHost() {
        connectedHost = hostToConnect;
    }


    @Override
    public void resetHostCounter() {
        _hostCounter = 0;
    }

    @Override
    public String[] getHosts() {
        return _host.split(",");
    }

    @Override
    public void setHosts(final String host) {
        if (StringUtils.isNotEmpty(host)) {
            _host = host.split(hostLbAlgorithmSeparator)[0];
            resetHostCounter();
        }
    }

    @Override
    public String getPrivateIp() {
        return _privateIp;
    }

    @Override
    public int getPort() {
        return _port;
    }

    @Override
    public int getProxyPort() {
        return _proxyPort;
    }

    @Override
    public int getWorkers() {
        return _workers;
    }

    @Override
    public String getGuid() {
        return _guid;
    }

    @Override
    public Map<String, Object> getCmdLineProperties() {
        return _cmdLineProperties;
    }

    public String getProperty(String prefix, String name) {
        if (prefix != null)
            return _properties.getProperty(prefix + "." + name);

        return _properties.getProperty(name);
    }

    @Override
    public String getPersistentProperty(String prefix, String name) {
        if (prefix != null)
            return _storage.get(prefix + "." + name);
        return _storage.get(name);
    }

    @Override
    public void setPersistentProperty(String prefix, String name, String value) {
        if (prefix != null)
            _storage.persist(prefix + "." + name, value);
        else
            _storage.persist(name, value);
    }

    void loadProperties() throws ConfigurationException {
        final File file = PropertiesUtil.findConfigFile("agent.properties");

        if (null == file) {
            throw new ConfigurationException("Unable to find agent.properties.");
        }

        LOGGER.info("agent.properties found at {}", file.getAbsolutePath());

        try {
            PropertiesUtil.loadFromFile(_properties, file);
        } catch (final FileNotFoundException ex) {
            throw new CloudRuntimeException("Cannot find the file: " + file.getAbsolutePath(), ex);
        } catch (final IOException ex) {
            throw new CloudRuntimeException("IOException in reading " + file.getAbsolutePath(), ex);
        }
    }

    protected boolean parseCommand(final String[] args) throws ConfigurationException {
        String host = null;
        String workers = null;
        String port = null;
        String zone = null;
        String pod = null;
        String guid = null;
        for (String param : args) {
            final String[] tokens = param.split("=");
            if (tokens.length != 2) {
                System.out.println("Invalid Parameter: " + param);
                continue;
            }
            final String paramName = tokens[0];
            final String paramValue = tokens[1];

            // save command line properties
            _cmdLineProperties.put(paramName, paramValue);

            if (paramName.equalsIgnoreCase("port")) {
                port = paramValue;
            } else if (paramName.equalsIgnoreCase("threads") || paramName.equalsIgnoreCase("workers")) {
                workers = paramValue;
            } else if (paramName.equalsIgnoreCase("host")) {
                host = paramValue;
            } else if (paramName.equalsIgnoreCase("zone")) {
                zone = paramValue;
            } else if (paramName.equalsIgnoreCase("pod")) {
                pod = paramValue;
            } else if (paramName.equalsIgnoreCase("guid")) {
                guid = paramValue;
            } else if (paramName.equalsIgnoreCase("eth1ip")) {
                _privateIp = paramValue;
            }
        }

        setHost(host);

        _guid = getGuid(guid);
        _properties.setProperty(AgentProperties.GUID.getName(), _guid);

        _port = getPortOrWorkers(port, AgentProperties.PORT);
        _workers = getWorkers(workers);
        _zone = getZoneOrPod(zone, AgentProperties.ZONE);
        _pod = getZoneOrPod(pod, AgentProperties.POD);

        _proxyPort = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.CONSOLEPROXY_HTTPLISTENPORT);
        _pingRetries = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.PING_RETRIES);
        preferredHostCheckInterval = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.HOST_LB_CHECK_INTERVAL);

        return true;
    }

    protected void setHost(String host) throws ConfigurationException {
        if (host == null) {
            host = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.HOST);
        }

        if (isValueStartingAndEndingWithAtSign(host)) {
            throw new ConfigurationException(String.format("Host [%s] is not configured correctly.", host));
        }

        setHosts(host);
    }

    protected boolean isValueStartingAndEndingWithAtSign(String value) {
        return value.startsWith("@") && value.endsWith("@");
    }

    protected String getGuid(String guid) throws ConfigurationException {
        guid = StringUtils.defaultString(guid, AgentPropertiesFileHandler.getPropertyValue(AgentProperties.GUID));
        if (guid != null) {
            return guid;
        }

        if (BooleanUtils.isFalse(AgentPropertiesFileHandler.getPropertyValue(AgentProperties.DEVELOPER))) {
            throw new ConfigurationException("Unable to find the guid");
        }

        return UUID.randomUUID().toString();
    }

    protected String getZoneOrPod(String zoneOrPod, AgentProperties.Property<String> property) {
        String value = zoneOrPod;

        if (value == null) {
            value = AgentPropertiesFileHandler.getPropertyValue(property);
        }

        if (isValueStartingAndEndingWithAtSign(value)) {
            value = property.getDefaultValue();
        }

        return value;
    }

    protected int getWorkers(String workersString) {
        AgentProperties.Property<Integer> propertyWorkers = agentProperties.getWorkers();
        int workers = getPortOrWorkers(workersString, propertyWorkers);

        if (workers <= 0) {
            workers = propertyWorkers.getDefaultValue();
        }

        return workers;
    }

    protected int getPortOrWorkers(String portOrWorkers, AgentProperties.Property<Integer> property) {
        if (portOrWorkers == null) {
            return AgentPropertiesFileHandler.getPropertyValue(property);
        }

        return NumberUtils.toInt(portOrWorkers, property.getDefaultValue());
    }

    @Override
    public void init(DaemonContext dc) throws DaemonInitException {
        LOGGER.debug("Initializing AgentShell from JSVC");
        try {
            init(dc.getArguments());
        } catch (ConfigurationException ex) {
            throw new DaemonInitException("Initialization failed", ex);
        }
    }

    public void init(String[] args) throws ConfigurationException {

        // PropertiesUtil is used both in management server and agent packages,
        // it searches path under class path and common J2EE containers
        // For KVM agent, do it specially here

        File file = new File("/etc/cloudstack/agent/log4j-cloud.xml");
        if (!file.exists()) {
            file = PropertiesUtil.findConfigFile("log4j-cloud.xml");
        }

        if (null != file) {
            Configurator.initialize(null, file.getAbsolutePath());

            LOGGER.info("Agent started");
        } else {
            LOGGER.error("Could not start the Agent because the absolute path of the \"log4j-cloud.xml\" file cannot be determined.");
        }

        final Class<?> c = this.getClass();
        _version = c.getPackage().getImplementationVersion();
        if (_version == null) {
            throw new CloudRuntimeException("Unable to find the implementation version of this agent");
        }
        LOGGER.info("Implementation Version is {}", _version);

        loadProperties();
        parseCommand(args);

        if (LOGGER.isDebugEnabled()) {
            List<String> properties = Collections.list((Enumeration<String>)_properties.propertyNames());
            for (String property : properties) {
                LOGGER.debug("Found property: {}", property);
            }
        }

        LOGGER.info("Defaulting to using properties file for storage");
        _storage = new PropertiesStorage();
        _storage.configure("Storage", new HashMap<String, Object>());

        // merge with properties from command line to let resource access
        // command line parameters
        for (Map.Entry<String, Object> cmdLineProp : getCmdLineProperties().entrySet()) {
            _properties.put(cmdLineProp.getKey(), cmdLineProp.getValue());
        }

        LOGGER.info("Defaulting to the constant time backoff algorithm");
        _backoff = new ConstantTimeBackoff();
        _backoff.configure("ConstantTimeBackoff", new HashMap<String, Object>());
    }

    private void launchAgent() throws ConfigurationException {
        String resourceClassNames = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.RESOURCE);
        LOGGER.trace("resource={}", resourceClassNames);
        if (resourceClassNames != null) {
            launchAgentFromClassInfo(resourceClassNames);
            return;
        }

        launchAgentFromTypeInfo();
    }

    private void launchAgentFromClassInfo(String resourceClassNames) throws ConfigurationException {
        String[] names = resourceClassNames.split("\\|");
        for (String name : names) {
            Class<?> impl;
            try {
                impl = Class.forName(name);
                Constructor<?> constructor = impl.getDeclaredConstructor();
                constructor.setAccessible(true);
                ServerResource resource = (ServerResource)constructor.newInstance();
                launchNewAgent(resource);
            } catch (final ClassNotFoundException | SecurityException | NoSuchMethodException | IllegalArgumentException | InstantiationException |
                    IllegalAccessException | InvocationTargetException e) {
                ConfigurationException configurationException = new ConfigurationException(String.format("Error while creating Agent with class [%s].", name));
                configurationException.setRootCause(e);
                throw configurationException;            }
        }
    }

    private void launchAgentFromTypeInfo() throws ConfigurationException {
        String typeInfo = getProperty(null, "type");
        if (typeInfo == null) {
            LOGGER.error("Unable to retrieve the type");
            throw new ConfigurationException("Unable to retrieve the type of this agent.");
        }
        LOGGER.trace("Launching agent based on type={}", typeInfo);
    }

    public void launchNewAgent(ServerResource resource) throws ConfigurationException {
        // we don't track agent after it is launched for now
        _agents.clear();
        Agent agent = new Agent(this, getNextAgentId(), resource);
        _agents.add(agent);
        agent.start();
    }

    public synchronized int getNextAgentId() {
        return _nextAgentId++;
    }

    @Override
    public void start() {
        try {
            /* By default we only search for log4j.xml */
            LogUtils.initLog4j("log4j-cloud.xml");

            boolean ipv6disabled = false;
            String ipv6 = getProperty(null, "ipv6disabled");
            if (ipv6 != null) {
                ipv6disabled = Boolean.parseBoolean(ipv6);
            }

            boolean ipv6prefer = false;
            String ipv6p = getProperty(null, "ipv6prefer");
            if (ipv6p != null) {
                ipv6prefer = Boolean.parseBoolean(ipv6p);
            }

            if (ipv6disabled) {
                LOGGER.info("Preferring IPv4 address family for agent connection");
                System.setProperty("java.net.preferIPv4Stack", "true");
                if (ipv6prefer) {
                    LOGGER.info("ipv6prefer is set to true, but ipv6disabled is false. Not preferring IPv6 for agent connection");
                }
            } else {
                if (ipv6prefer) {
                    LOGGER.info("Preferring IPv6 address family for agent connection");
                    System.setProperty("java.net.preferIPv6Addresses", "true");
                } else {
                    LOGGER.info("Using default Java settings for IPv6 preference for agent connection");
                }
            }

            String instance = getProperty(null, "instance");
            if (instance == null) {
                if (BooleanUtils.isTrue(AgentPropertiesFileHandler.getPropertyValue(AgentProperties.DEVELOPER))) {
                    instance = UUID.randomUUID().toString();
                } else {
                    instance = "";
                }
            } else {
                instance += ".";
            }

            String pidDir = getProperty(null, "piddir");

            final String run = "agent." + instance + "pid";
            LOGGER.debug("Checking to see if {} exists.", run);
            ProcessUtil.pidCheck(pidDir, run);

            launchAgent();

            try {
                while (!_exit)
                    Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOGGER.debug("[ignored] AgentShell was interrupted.");
            }

        } catch (final Exception e) {
            LOGGER.error("Unable to start agent: ", e);
            System.exit(ExitStatus.Error.value());
        }
    }

    @Override
    public void stop() {
        _exit = true;
    }

    @Override
    public void destroy() {

    }

    public static void main(String[] args) {
        try {
            LOGGER.debug("Initializing AgentShell from main");
            AgentShell shell = new AgentShell();
            shell.init(args);
            shell.start();
        } catch (ConfigurationException e) {
            System.out.println(e.getMessage());
        }
    }

}
