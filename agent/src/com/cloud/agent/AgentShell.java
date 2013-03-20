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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.naming.ConfigurationException;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;

import com.cloud.agent.Agent.ExitStatus;
import com.cloud.agent.dao.StorageComponent;
import com.cloud.agent.dao.impl.PropertiesStorage;
import com.cloud.host.Host;
import com.cloud.resource.ServerResource;
import com.cloud.utils.LogUtils;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.ProcessUtil;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.backoff.BackoffAlgorithm;
import com.cloud.utils.backoff.impl.ConstantTimeBackoff;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class AgentShell implements IAgentShell {
    private static final Logger s_logger = Logger.getLogger(AgentShell.class
            .getName());
    private static final MultiThreadedHttpConnectionManager s_httpClientManager = new MultiThreadedHttpConnectionManager();

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
    private int _nextAgentId = 1;
    private volatile boolean _exit = false;
    private int _pingRetries;
    private Thread _consoleProxyMain = null;
    private final List<Agent> _agents = new ArrayList<Agent>();

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
    public String getHost() {
        return _host;
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

    @Override
    public void upgradeAgent(final String url) {
        s_logger.info("Updating agent with binary from " + url);
        synchronized (this) {
            final Class<?> c = this.getClass();
            String path = c.getResource(c.getSimpleName() + ".class")
                    .toExternalForm();
            final int begin = path.indexOf(File.separator);
            int end = path.lastIndexOf("!");
            end = path.lastIndexOf(File.separator, end);
            path = path.substring(begin, end);

            s_logger.debug("Current binaries reside at " + path);

            File file = null;
            try {
                file = File.createTempFile("agent-",
                        "-" + Long.toString(new Date().getTime()));
                wget(url, file);
            } catch (final IOException e) {
                s_logger.warn(
                        "Exception while downloading agent update package, ", e);
                throw new CloudRuntimeException("Unable to update from " + url
                        + ", exception:" + e.getMessage(), e);
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unzipping " + file.getAbsolutePath() + " to "
                        + path);
            }

            final Script unzip = new Script("unzip", 120000, s_logger);
            unzip.add("-o", "-q"); // overwrite and quiet
            unzip.add(file.getAbsolutePath());
            unzip.add("-d", path);

            final String result = unzip.execute();
            if (result != null) {
                throw new CloudRuntimeException(
                        "Unable to unzip the retrieved file: " + result);
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Closing the connection to the management server");
            }
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Exiting to start the new agent.");
        }
        System.exit(ExitStatus.Upgrade.value());
    }

    public static void wget(String url, File file) throws IOException {
        final HttpClient client = new HttpClient(s_httpClientManager);
        final GetMethod method = new GetMethod(url);
        int response;
        response = client.executeMethod(method);
        if (response != HttpURLConnection.HTTP_OK) {
            method.releaseConnection();
            s_logger.warn("Retrieving from " + url + " gives response code: "
                    + response);
            throw new CloudRuntimeException("Unable to download from " + url
                    + ".  Response code is " + response);
        }

        final InputStream is = method.getResponseBodyAsStream();
        s_logger.debug("Downloading content into " + file.getAbsolutePath());

        final FileOutputStream fos = new FileOutputStream(file);
        byte[] buffer = new byte[4096];
        int len = 0;
        while ((len = is.read(buffer)) > 0)
            fos.write(buffer, 0, len);
        fos.close();

        try {
            is.close();
        } catch (IOException e) {
            s_logger.warn("Exception while closing download stream from  "
                    + url + ", ", e);
        }
        method.releaseConnection();
    }

    private void loadProperties() throws ConfigurationException {
        final File file = PropertiesUtil.findConfigFile("agent.properties");
        if (file == null) {
            throw new ConfigurationException("Unable to find agent.properties.");
        }

        s_logger.info("agent.properties found at " + file.getAbsolutePath());

        try {
            _properties.load(new FileInputStream(file));
        } catch (final FileNotFoundException ex) {
            throw new CloudRuntimeException("Cannot find the file: "
                    + file.getAbsolutePath(), ex);
        } catch (final IOException ex) {
            throw new CloudRuntimeException("IOException in reading "
                    + file.getAbsolutePath(), ex);
        }
    }

    protected boolean parseCommand(final String[] args)
            throws ConfigurationException {
        String host = null;
        String workers = null;
        String port = null;
        String zone = null;
        String pod = null;
        String guid = null;
        for (int i = 0; i < args.length; i++) {
            final String[] tokens = args[i].split("=");
            if (tokens.length != 2) {
                System.out.println("Invalid Parameter: " + args[i]);
                continue;
            }

            // save command line properties
            _cmdLineProperties.put(tokens[0], tokens[1]);

            if (tokens[0].equalsIgnoreCase("port")) {
                port = tokens[1];
            } else if (tokens[0].equalsIgnoreCase("threads") || tokens[0].equalsIgnoreCase("workers")) {
                workers = tokens[1];
            } else if (tokens[0].equalsIgnoreCase("host")) {
                host = tokens[1];
            } else if (tokens[0].equalsIgnoreCase("zone")) {
                zone = tokens[1];
            } else if (tokens[0].equalsIgnoreCase("pod")) {
                pod = tokens[1];
            } else if (tokens[0].equalsIgnoreCase("guid")) {
                guid = tokens[1];
            } else if (tokens[0].equalsIgnoreCase("eth1ip")) {
                _privateIp = tokens[1];
            }
        }

        if (port == null) {
            port = getProperty(null, "port");
        }

        _port = NumbersUtil.parseInt(port, 8250);

        _proxyPort = NumbersUtil.parseInt(
                getProperty(null, "consoleproxy.httpListenPort"), 443);

        if (workers == null) {
            workers = getProperty(null, "workers");
        }

        _workers = NumbersUtil.parseInt(workers, 5);

        if (host == null) {
            host = getProperty(null, "host");
        }

        if (host == null) {
            host = "localhost";
        }
        _host = host;

        if (zone != null)
            _zone = zone;
        else
            _zone = getProperty(null, "zone");
        if (_zone == null || (_zone.startsWith("@") && _zone.endsWith("@"))) {
            _zone = "default";
        }

        if (pod != null)
            _pod = pod;
        else
            _pod = getProperty(null, "pod");
        if (_pod == null || (_pod.startsWith("@") && _pod.endsWith("@"))) {
            _pod = "default";
        }

        if (_host == null || (_host.startsWith("@") && _host.endsWith("@"))) {
            throw new ConfigurationException(
                    "Host is not configured correctly: " + _host);
        }

        final String retries = getProperty(null, "ping.retries");
        _pingRetries = NumbersUtil.parseInt(retries, 5);

        String value = getProperty(null, "developer");
        boolean developer = Boolean.parseBoolean(value);

        if (guid != null)
            _guid = guid;
        else
            _guid = getProperty(null, "guid");
        if (_guid == null) {
            if (!developer) {
                throw new ConfigurationException("Unable to find the guid");
            }
            _guid = UUID.randomUUID().toString();
        }

        return true;
    }

    public void init(String[] args) throws ConfigurationException {

    	// PropertiesUtil is used both in management server and agent packages,
    	// it searches path under class path and common J2EE containers
    	// For KVM agent, do it specially here
    	
    	File file = new File("/etc/cloudstack/agent/log4j-cloud.xml");
    	if(file == null || !file.exists()) {
    		file = PropertiesUtil.findConfigFile("log4j-cloud.xml");
    	}
    	DOMConfigurator.configureAndWatch(file.getAbsolutePath());

    	s_logger.info("Agent started");
    	
        final Class<?> c = this.getClass();
        _version = c.getPackage().getImplementationVersion();
        if (_version == null) {
            throw new CloudRuntimeException(
                    "Unable to find the implementation version of this agent");
        }
        s_logger.info("Implementation Version is " + _version);

        loadProperties();
        parseCommand(args);

        List<String> properties = Collections.list((Enumeration<String>)_properties.propertyNames());
        for (String property:properties){
            s_logger.debug("Found property: " + property);
        }

        s_logger.info("Defaulting to using properties file for storage");
        _storage = new PropertiesStorage();
        _storage.configure("Storage", new HashMap<String, Object>());

        // merge with properties from command line to let resource access
        // command line parameters
        for (Map.Entry<String, Object> cmdLineProp : getCmdLineProperties()
                .entrySet()) {
            _properties.put(cmdLineProp.getKey(), cmdLineProp.getValue());
        }

        s_logger.info("Defaulting to the constant time backoff algorithm");
        _backoff = new ConstantTimeBackoff();
        _backoff.configure("ConstantTimeBackoff", new HashMap<String, Object>());
    }

    private void launchAgent() throws ConfigurationException {
        String resourceClassNames = getProperty(null, "resource");
        s_logger.trace("resource=" + resourceClassNames);
        if (resourceClassNames != null) {
            launchAgentFromClassInfo(resourceClassNames);
            return;
        }

        launchAgentFromTypeInfo();
    }

    private boolean needConsoleProxy() {
        for (Agent agent : _agents) {
            if (agent.getResource().getType().equals(Host.Type.ConsoleProxy)
                    || agent.getResource().getType().equals(Host.Type.Routing))
                return true;
        }
        return false;
    }

    private int getConsoleProxyPort() {
        int port = NumbersUtil.parseInt(
                getProperty(null, "consoleproxy.httpListenPort"), 443);
        return port;
    }

    private void openPortWithIptables(int port) {
        // TODO
    }

    private void launchConsoleProxy() throws ConfigurationException {
        if (!needConsoleProxy()) {
            if (s_logger.isInfoEnabled())
                s_logger.info("Storage only agent, no need to start console proxy on it");
            return;
        }

        int port = getConsoleProxyPort();
        openPortWithIptables(port);

        _consoleProxyMain = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Class<?> consoleProxyClazz = Class.forName("com.cloud.consoleproxy.ConsoleProxy");

                    try {
                        Method method = consoleProxyClazz.getMethod("start",
                                Properties.class);
                        method.invoke(null, _properties);
                    } catch (SecurityException e) {
                        s_logger.error("Unable to launch console proxy due to SecurityException");
                        System.exit(ExitStatus.Error.value());
                    } catch (NoSuchMethodException e) {
                        s_logger.error("Unable to launch console proxy due to NoSuchMethodException");
                        System.exit(ExitStatus.Error.value());
                    } catch (IllegalArgumentException e) {
                        s_logger.error("Unable to launch console proxy due to IllegalArgumentException");
                        System.exit(ExitStatus.Error.value());
                    } catch (IllegalAccessException e) {
                        s_logger.error("Unable to launch console proxy due to IllegalAccessException");
                        System.exit(ExitStatus.Error.value());
                    } catch (InvocationTargetException e) {
                        s_logger.error("Unable to launch console proxy due to InvocationTargetException");
                        System.exit(ExitStatus.Error.value());
                    }
                } catch (final ClassNotFoundException e) {
                    s_logger.error("Unable to launch console proxy due to ClassNotFoundException");
                    System.exit(ExitStatus.Error.value());
                }
            }
        }, "Console-Proxy-Main");
        _consoleProxyMain.setDaemon(true);
        _consoleProxyMain.start();
    }

    private void launchAgentFromClassInfo(String resourceClassNames)
            throws ConfigurationException {
        String[] names = resourceClassNames.split("\\|");
        for (String name : names) {
            Class<?> impl;
            try {
                impl = Class.forName(name);
                final Constructor<?> constructor = impl
                        .getDeclaredConstructor();
                constructor.setAccessible(true);
                ServerResource resource = (ServerResource) constructor
                        .newInstance();
                launchAgent(getNextAgentId(), resource);
            } catch (final ClassNotFoundException e) {
                throw new ConfigurationException("Resource class not found: "
                        + name + " due to: " + e.toString());
            } catch (final SecurityException e) {
                throw new ConfigurationException(
                        "Security excetion when loading resource: " + name
                        + " due to: " + e.toString());
            } catch (final NoSuchMethodException e) {
                throw new ConfigurationException(
                        "Method not found excetion when loading resource: "
                                + name + " due to: " + e.toString());
            } catch (final IllegalArgumentException e) {
                throw new ConfigurationException(
                        "Illegal argument excetion when loading resource: "
                                + name + " due to: " + e.toString());
            } catch (final InstantiationException e) {
                throw new ConfigurationException(
                        "Instantiation excetion when loading resource: " + name
                        + " due to: " + e.toString());
            } catch (final IllegalAccessException e) {
                throw new ConfigurationException(
                        "Illegal access exception when loading resource: "
                                + name + " due to: " + e.toString());
            } catch (final InvocationTargetException e) {
                throw new ConfigurationException(
                        "Invocation target exception when loading resource: "
                                + name + " due to: " + e.toString());
            }
        }
    }

    private void launchAgentFromTypeInfo() throws ConfigurationException {
        String typeInfo = getProperty(null, "type");
        if (typeInfo == null) {
            s_logger.error("Unable to retrieve the type");
            throw new ConfigurationException(
                    "Unable to retrieve the type of this agent.");
        }
        s_logger.trace("Launching agent based on type=" + typeInfo);
    }

    private void launchAgent(int localAgentId, ServerResource resource)
            throws ConfigurationException {
        // we don't track agent after it is launched for now
        Agent agent = new Agent(this, localAgentId, resource);
        _agents.add(agent);
        agent.start();
    }

    public synchronized int getNextAgentId() {
        return _nextAgentId++;
    }

    public void start() {
        try {
            /* By default we only search for log4j.xml */
            LogUtils.initLog4j("log4j-cloud.xml");

            System.setProperty("java.net.preferIPv4Stack", "true");

            String instance = getProperty(null, "instance");
            if (instance == null) {
                if (Boolean.parseBoolean(getProperty(null, "developer"))) {
                    instance = UUID.randomUUID().toString();
                } else {
                    instance = "";
                }
            } else {
                instance += ".";
            }

            String pidDir = getProperty(null, "piddir");

            final String run = "agent." + instance + "pid";
            s_logger.debug("Checking to see if " + run + " exists.");
            ProcessUtil.pidCheck(pidDir, run);

            launchAgent();

            //
            // For both KVM & Xen-Server hypervisor, we have switched to
            // VM-based console proxy solution, disable launching
            // of console proxy here
            //
            // launchConsoleProxy();
            //

            try {
                while (!_exit)
                    Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

        } catch (final ConfigurationException e) {
            s_logger.error("Unable to start agent: " + e.getMessage());
            System.out.println("Unable to start agent: " + e.getMessage());
            System.exit(ExitStatus.Configuration.value());
        } catch (final Exception e) {
            s_logger.error("Unable to start agent: ", e);
            System.out.println("Unable to start agent: " + e.getMessage());
            System.exit(ExitStatus.Error.value());
        }
    }

    public void stop() {
        _exit = true;
        if (_consoleProxyMain != null) {
            _consoleProxyMain.interrupt();
        }
    }

    public void destroy() {

    }

    public static void main(String[] args) {
        try {
            AgentShell shell = new AgentShell();
            shell.init(args);
            shell.start();
        } catch (ConfigurationException e) {
            System.out.println(e.getMessage());
        }
    }
}
