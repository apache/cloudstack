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
package com.cloud.consoleproxy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.config.Configurator;
import org.eclipse.jetty.websocket.api.Session;

import com.cloud.consoleproxy.util.Logger;
import com.cloud.utils.PropertiesUtil;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;

/**
 *
 * ConsoleProxy, singleton class that manages overall activities in console proxy process. To make legacy code work, we still
 */
public class ConsoleProxy {
    protected static Logger LOGGER = Logger.getLogger(ConsoleProxy.class);

    public static final int KEYBOARD_RAW = 0;
    public static final int KEYBOARD_COOKED = 1;

    public static final int VIEWER_LINGER_SECONDS = 180;

    public static Object context;

    // this has become more ugly, to store keystore info passed from management server (we now use management server managed keystore to support
    // dynamically changing to customer supplied certificate)
    public static byte[] ksBits;
    public static String ksPassword;
    public static Boolean isSourceIpCheckEnabled;

    public static Method authMethod;
    public static Method reportMethod;
    public static Method ensureRouteMethod;

    static Hashtable<String, ConsoleProxyClient> connectionMap = new Hashtable<String, ConsoleProxyClient>();
    static Set<String> removedSessionsSet = ConcurrentHashMap.newKeySet();
    static int httpListenPort = 80;
    static int httpCmdListenPort = 8001;
    static int reconnectMaxRetry = 5;
    static int readTimeoutSeconds = 90;
    static int keyboardType = KEYBOARD_RAW;
    static String factoryClzName;
    static boolean standaloneStart = false;

    static String encryptorPassword = "Dummy";
    static final String[] skipProperties = new String[]{"certificate", "cacertificate", "keystore_password", "privatekey"};

    static Set<String> allowedSessions = new HashSet<>();

    public static void addAllowedSession(String sessionUuid) {
        allowedSessions.add(sessionUuid);
    }

    private static void configLog4j() {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL configUrl = loader.getResource("/conf/log4j-cloud.xml");
        if (configUrl == null)
            configUrl = ClassLoader.getSystemResource("log4j-cloud.xml");

        if (configUrl == null)
            configUrl = ClassLoader.getSystemResource("conf/log4j-cloud.xml");

        if (configUrl != null) {
            try {
                System.out.println("Configure log4j using " + configUrl.toURI().toString());
            } catch (URISyntaxException e1) {
                e1.printStackTrace();
            }

            try {
                File file = new File(configUrl.toURI());

                System.out.println("Log4j configuration from : " + file.getAbsolutePath());
                Configurator.initialize(null, file.getAbsolutePath());
            } catch (URISyntaxException e) {
                System.out.println("Unable to convert log4j configuration Url to URI");
            }
            // DOMConfigurator.configure(configUrl);
        } else {
            System.out.println("Configure log4j with default properties");
        }
    }

    private static void configProxy(Properties conf) {
        LOGGER.info("Configure console proxy...");
        for (Object key : conf.keySet()) {
            LOGGER.info("Property " + (String)key + ": " + conf.getProperty((String)key));
            if (!ArrayUtils.contains(skipProperties, key)) {
                LOGGER.info("Property " + (String)key + ": " + conf.getProperty((String)key));
            }
        }

        String s = conf.getProperty("consoleproxy.httpListenPort");
        if (s != null) {
            httpListenPort = Integer.parseInt(s);
            LOGGER.info("Setting httpListenPort=" + s);
        }

        s = conf.getProperty("premium");
        if (s != null && s.equalsIgnoreCase("true")) {
            LOGGER.info("Premium setting will override settings from consoleproxy.properties, listen at port 443");
            httpListenPort = 443;
            factoryClzName = "com.cloud.consoleproxy.ConsoleProxySecureServerFactoryImpl";
        } else {
            factoryClzName = ConsoleProxyBaseServerFactoryImpl.class.getName();
        }

        s = conf.getProperty("consoleproxy.httpCmdListenPort");
        if (s != null) {
            httpCmdListenPort = Integer.parseInt(s);
            LOGGER.info("Setting httpCmdListenPort=" + s);
        }

        s = conf.getProperty("consoleproxy.reconnectMaxRetry");
        if (s != null) {
            reconnectMaxRetry = Integer.parseInt(s);
            LOGGER.info("Setting reconnectMaxRetry=" + reconnectMaxRetry);
        }

        s = conf.getProperty("consoleproxy.readTimeoutSeconds");
        if (s != null) {
            readTimeoutSeconds = Integer.parseInt(s);
            LOGGER.info("Setting readTimeoutSeconds=" + readTimeoutSeconds);
        }
    }

    public static ConsoleProxyServerFactory getHttpServerFactory() {
        try {
            Class<?> clz = Class.forName(factoryClzName);
            try {
                ConsoleProxyServerFactory factory = (ConsoleProxyServerFactory)clz.newInstance();
                factory.init(ConsoleProxy.ksBits, ConsoleProxy.ksPassword);
                return factory;
            } catch (InstantiationException e) {
                LOGGER.error(e.getMessage(), e);
                return null;
            } catch (IllegalAccessException e) {
                LOGGER.error(e.getMessage(), e);
                return null;
            }
        } catch (ClassNotFoundException e) {
            LOGGER.warn("Unable to find http server factory class: " + factoryClzName);
            return new ConsoleProxyBaseServerFactoryImpl();
        }
    }

    public static ConsoleProxyAuthenticationResult authenticateConsoleAccess(ConsoleProxyClientParam param, boolean reauthentication) {

        ConsoleProxyAuthenticationResult authResult = new ConsoleProxyAuthenticationResult();
        authResult.setSuccess(true);
        authResult.setReauthentication(reauthentication);
        authResult.setHost(param.getClientHostAddress());
        authResult.setPort(param.getClientHostPort());

        if (org.apache.commons.lang3.StringUtils.isNotBlank(param.getExtraSecurityToken())) {
            String extraToken = param.getExtraSecurityToken();
            String clientProvidedToken = param.getClientProvidedExtraSecurityToken();
            LOGGER.debug(String.format("Extra security validation for the console access, provided %s " +
                    "to validate against %s", clientProvidedToken, extraToken));

            if (!extraToken.equals(clientProvidedToken)) {
                LOGGER.error("The provided extra token does not match the expected value for this console endpoint");
                authResult.setSuccess(false);
                return authResult;
            }
        }

        String sessionUuid = param.getSessionUuid();
        if (allowedSessions.contains(sessionUuid)) {
            LOGGER.debug("Acquiring the session " + sessionUuid + " not available for future use");
            allowedSessions.remove(sessionUuid);
        } else {
            LOGGER.info("Session " + sessionUuid + " has already been used, cannot connect");
            authResult.setSuccess(false);
            return authResult;
        }

        String websocketUrl = param.getWebsocketUrl();
        if (StringUtils.isNotBlank(websocketUrl)) {
            return authResult;
        }

        if (standaloneStart) {
            return authResult;
        }

        if (authMethod != null) {
            Object result;
            try {
                result =
                        authMethod.invoke(ConsoleProxy.context, param.getClientHostAddress(), String.valueOf(param.getClientHostPort()), param.getClientTag(),
                                param.getClientHostPassword(), param.getTicket(), reauthentication, param.getSessionUuid());
            } catch (IllegalAccessException e) {
                LOGGER.error("Unable to invoke authenticateConsoleAccess due to IllegalAccessException" + " for vm: " + param.getClientTag(), e);
                authResult.setSuccess(false);
                return authResult;
            } catch (InvocationTargetException e) {
                LOGGER.error("Unable to invoke authenticateConsoleAccess due to InvocationTargetException " + " for vm: " + param.getClientTag(), e);
                authResult.setSuccess(false);
                return authResult;
            }

            if (result != null && result instanceof String) {
                authResult = new Gson().fromJson((String)result, ConsoleProxyAuthenticationResult.class);
            } else {
                LOGGER.error("Invalid authentication return object " + result + " for vm: " + param.getClientTag() + ", decline the access");
                authResult.setSuccess(false);
            }
        } else {
            LOGGER.warn("Private channel towards management server is not setup. Switch to offline mode and allow access to vm: " + param.getClientTag());
        }

        return authResult;
    }

    public static void reportLoadInfo(String gsonLoadInfo) {
        if (reportMethod != null) {
            try {
                reportMethod.invoke(ConsoleProxy.context, gsonLoadInfo);
            } catch (IllegalAccessException e) {
                LOGGER.error("Unable to invoke reportLoadInfo due to " + e.getMessage());
            } catch (InvocationTargetException e) {
                LOGGER.error("Unable to invoke reportLoadInfo due to " + e.getMessage());
            }
        } else {
            LOGGER.warn("Private channel towards management server is not setup. Switch to offline mode and ignore load report");
        }
    }

    public static void ensureRoute(String address) {
        if (ensureRouteMethod != null) {
            try {
                ensureRouteMethod.invoke(ConsoleProxy.context, address);
            } catch (IllegalAccessException e) {
                LOGGER.error("Unable to invoke ensureRoute due to " + e.getMessage());
            } catch (InvocationTargetException e) {
                LOGGER.error("Unable to invoke ensureRoute due to " + e.getMessage());
            }
        } else {
            LOGGER.warn("Unable to find ensureRoute method, console proxy agent is not up to date");
        }
    }

    public static void startWithContext(Properties conf, Object context, byte[] ksBits, String ksPassword, String password, Boolean isSourceIpCheckEnabled) {
        setEncryptorPassword(password);
        configLog4j();
        Logger.setFactory(new ConsoleProxyLoggerFactory());
        LOGGER.info("Start console proxy with context");

        if (conf != null) {
            for (Object key : conf.keySet()) {
                if (!ArrayUtils.contains(skipProperties, key)) {
                    LOGGER.info("Context property " + (String) key + ": " + conf.getProperty((String) key));
                }
            }
        }

        // Using reflection to setup private/secure communication channel towards management server
        ConsoleProxy.context = context;
        ConsoleProxy.ksBits = ksBits;
        ConsoleProxy.ksPassword = ksPassword;
        ConsoleProxy.isSourceIpCheckEnabled = isSourceIpCheckEnabled;
        try {
            final ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Class<?> contextClazz = loader.loadClass("com.cloud.agent.resource.consoleproxy.ConsoleProxyResource");
            authMethod = contextClazz.getDeclaredMethod("authenticateConsoleAccess", String.class, String.class,
                    String.class, String.class, String.class, Boolean.class, String.class);
            reportMethod = contextClazz.getDeclaredMethod("reportLoadInfo", String.class);
            ensureRouteMethod = contextClazz.getDeclaredMethod("ensureRoute", String.class);
        } catch (SecurityException e) {
            LOGGER.error("Unable to setup private channel due to SecurityException", e);
        } catch (NoSuchMethodException e) {
            LOGGER.error("Unable to setup private channel due to NoSuchMethodException", e);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Unable to setup private channel due to IllegalArgumentException", e);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Unable to setup private channel due to ClassNotFoundException", e);
        }

        // merge properties from conf file
        InputStream confs = ConsoleProxy.class.getResourceAsStream("/conf/consoleproxy.properties");
        Properties props = new Properties();
        if (confs == null) {
            final File file = PropertiesUtil.findConfigFile("consoleproxy.properties");
            if (file == null)
                LOGGER.info("Can't load consoleproxy.properties from classpath, will use default configuration");
            else
                try {
                    confs = new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    LOGGER.info("Ignoring file not found exception and using defaults");
                }
        }
        if (confs != null) {
            try {
                props.load(confs);

                for (Object key : props.keySet()) {
                    // give properties passed via context high priority, treat properties from consoleproxy.properties
                    // as default values
                    if (conf.get(key) == null)
                        conf.put(key, props.get(key));
                }
            } catch (Exception e) {
                LOGGER.error(e.toString(), e);
            }
        }
        try {
            confs.close();
        } catch (IOException e) {
            LOGGER.error("Failed to close consolepropxy.properties : " + e.toString(), e);
        }

        start(conf);
    }

    public static void start(Properties conf) {
        System.setProperty("java.awt.headless", "true");

        configProxy(conf);

        ConsoleProxyServerFactory factory = getHttpServerFactory();
        if (factory == null) {
            LOGGER.error("Unable to load console proxy server factory");
            System.exit(1);
        }

        if (httpListenPort != 0) {
            startupHttpMain();
        } else {
            LOGGER.error("A valid HTTP server port is required to be specified, please check your consoleproxy.httpListenPort settings");
            System.exit(1);
        }

        if (httpCmdListenPort > 0) {
            startupHttpCmdPort();
        } else {
            LOGGER.info("HTTP command port is disabled");
        }

        ConsoleProxyGCThread cthread = new ConsoleProxyGCThread(connectionMap, removedSessionsSet);
        cthread.setName("Console Proxy GC Thread");
        cthread.start();
    }

    private static void startupHttpMain() {
        try {
            ConsoleProxyServerFactory factory = getHttpServerFactory();
            if (factory == null) {
                LOGGER.error("Unable to load HTTP server factory");
                System.exit(1);
            }

            HttpServer server = factory.createHttpServerInstance(httpListenPort);
            server.createContext("/getscreen", new ConsoleProxyThumbnailHandler());
            server.createContext("/resource/", new ConsoleProxyResourceHandler());
            server.createContext("/ajax", new ConsoleProxyAjaxHandler());
            server.createContext("/ajaximg", new ConsoleProxyAjaxImageHandler());
            server.setExecutor(new ThreadExecutor()); // creates a default executor
            server.start();

            ConsoleProxyNoVNCServer noVNCServer = getNoVNCServer();
            noVNCServer.start();

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            System.exit(1);
        }
    }

    private static ConsoleProxyNoVNCServer getNoVNCServer() {
        int vncPort = ConsoleProxyNoVNCServer.getVNCPort();
        return vncPort == ConsoleProxyNoVNCServer.WSS_PORT ?
                new ConsoleProxyNoVNCServer(ksBits, ksPassword) :
                new ConsoleProxyNoVNCServer();
    }

    private static void startupHttpCmdPort() {
        try {
            LOGGER.info("Listening for HTTP CMDs on port " + httpCmdListenPort);
            HttpServer cmdServer = HttpServer.create(new InetSocketAddress(httpCmdListenPort), 2);
            cmdServer.createContext("/cmd", new ConsoleProxyCmdHandler());
            cmdServer.setExecutor(new ThreadExecutor()); // creates a default executor
            cmdServer.start();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            System.exit(1);
        }
    }

    public static void main(String[] argv) {
        standaloneStart = true;
        configLog4j();
        Logger.setFactory(new ConsoleProxyLoggerFactory());

        InputStream confs = ConsoleProxy.class.getResourceAsStream("/conf/consoleproxy.properties");
        Properties conf = new Properties();
        if (confs == null) {
            LOGGER.info("Can't load consoleproxy.properties from classpath, will use default configuration");
        } else {
            try {
                conf.load(confs);
            } catch (Exception e) {
                LOGGER.error(e.toString(), e);
            } finally {
                try {
                    confs.close();
                } catch (IOException ioex) {
                    LOGGER.error(ioex.toString(), ioex);
                }
            }
        }
        start(conf);
    }

    public static ConsoleProxyClient getVncViewer(ConsoleProxyClientParam param) throws Exception {
        ConsoleProxyClient viewer = null;

        boolean reportLoadChange = false;
        String clientKey = param.getClientMapKey();
        synchronized (connectionMap) {
            viewer = connectionMap.get(clientKey);
            if (viewer == null || viewer.getClass() == ConsoleProxyNoVncClient.class) {
                viewer = getClient(param);
                viewer.initClient(param);
                connectionMap.put(clientKey, viewer);
                LOGGER.info("Added viewer object " + viewer);

                reportLoadChange = true;
            } else if (!viewer.isFrontEndAlive()) {
                LOGGER.info("The rfb thread died, reinitializing the viewer " + viewer);
                viewer.initClient(param);
            } else if (!param.getClientHostPassword().equals(viewer.getClientHostPassword())) {
                LOGGER.warn("Bad sid detected(VNC port may be reused). sid in session: " + viewer.getClientHostPassword() + ", sid in request: " +
                        param.getClientHostPassword());
                viewer.initClient(param);
            }
        }

        if (reportLoadChange) {
            ConsoleProxyClientStatsCollector statsCollector = getStatsCollector();
            String loadInfo = statsCollector.getStatsReport();
            reportLoadInfo(loadInfo);
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Report load change : " + loadInfo);
        }

        return viewer;
    }

    public static ConsoleProxyClient getAjaxVncViewer(ConsoleProxyClientParam param, String ajaxSession) throws Exception {

        boolean reportLoadChange = false;
        String clientKey = param.getClientMapKey();
        synchronized (connectionMap) {
            ConsoleProxyClient viewer = connectionMap.get(clientKey);
            if (viewer == null || viewer.getClass() == ConsoleProxyNoVncClient.class) {
                authenticationExternally(param);
                viewer = getClient(param);
                viewer.initClient(param);

                connectionMap.put(clientKey, viewer);
                LOGGER.info("Added viewer object " + viewer);
                reportLoadChange = true;
            } else {
                // protected against malicious attack by modifying URL content
                if (ajaxSession != null) {
                    long ajaxSessionIdFromUrl = Long.parseLong(ajaxSession);
                    if (ajaxSessionIdFromUrl != viewer.getAjaxSessionId())
                        throw new AuthenticationException("Cannot use the existing viewer " + viewer + ": modified AJAX session id");
                }

                if (param.getClientHostPassword() == null || param.getClientHostPassword().isEmpty() ||
                        !param.getClientHostPassword().equals(viewer.getClientHostPassword()))
                    throw new AuthenticationException("Cannot use the existing viewer " + viewer + ": bad sid");

                if (!viewer.isFrontEndAlive()) {

                    authenticationExternally(param);
                    viewer.initClient(param);
                    reportLoadChange = true;
                }
            }

            if (reportLoadChange) {
                ConsoleProxyClientStatsCollector statsCollector = getStatsCollector();
                String loadInfo = statsCollector.getStatsReport();
                reportLoadInfo(loadInfo);
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Report load change : " + loadInfo);
            }
            return viewer;
        }
    }

    private static ConsoleProxyClient getClient(ConsoleProxyClientParam param) {
        if (param.getHypervHost() != null) {
            return new ConsoleProxyRdpClient();
        } else {
            return new ConsoleProxyVncClient();
        }
    }

    public static void removeViewer(ConsoleProxyClient viewer) {
        synchronized (connectionMap) {
            for (Map.Entry<String, ConsoleProxyClient> entry : connectionMap.entrySet()) {
                if (entry.getValue() == viewer) {
                    connectionMap.remove(entry.getKey());
                    removedSessionsSet.add(viewer.getSessionUuid());
                    return;
                }
            }
        }
    }

    public static ConsoleProxyClientStatsCollector getStatsCollector() {
        synchronized (connectionMap) {
            return new ConsoleProxyClientStatsCollector(connectionMap);
        }
    }

    public static void authenticationExternally(ConsoleProxyClientParam param) throws AuthenticationException {
        ConsoleProxyAuthenticationResult authResult = authenticateConsoleAccess(param, false);

        if (authResult == null || !authResult.isSuccess()) {
            LOGGER.warn("External authenticator failed authentication request for vm " + param.getClientTag() + " with sid " + param.getClientHostPassword());

            throw new AuthenticationException("External authenticator failed request for vm " + param.getClientTag() + " with sid " + param.getClientHostPassword());
        }
    }

    public static ConsoleProxyAuthenticationResult reAuthenticationExternally(ConsoleProxyClientParam param) {
        return authenticateConsoleAccess(param, true);
    }

    public static String getEncryptorPassword() {
        return encryptorPassword;
    }

    public static void setEncryptorPassword(String password) {
        encryptorPassword = password;
    }

    public static void setIsSourceIpCheckEnabled(Boolean isEnabled) {
        isSourceIpCheckEnabled = isEnabled;
    }

    static class ThreadExecutor implements Executor {
        @Override
        public void execute(Runnable r) {
            new Thread(r).start();
        }
    }

    public static ConsoleProxyNoVncClient getNoVncViewer(ConsoleProxyClientParam param, String ajaxSession,
            Session session) throws AuthenticationException {
        boolean reportLoadChange = false;
        String clientKey = param.getClientMapKey();
        synchronized (connectionMap) {
            ConsoleProxyClient viewer = connectionMap.get(clientKey);
            if (viewer == null || viewer.getClass() != ConsoleProxyNoVncClient.class) {
                authenticationExternally(param);
                viewer = new ConsoleProxyNoVncClient(session);
                viewer.initClient(param);

                connectionMap.put(clientKey, viewer);
                reportLoadChange = true;
            } else {
                if (param.getClientHostPassword() == null || param.getClientHostPassword().isEmpty() ||
                        !param.getClientHostPassword().equals(viewer.getClientHostPassword()))
                    throw new AuthenticationException("Cannot use the existing viewer " + viewer + ": bad sid");

                try {
                    authenticationExternally(param);
                } catch (Exception e) {
                    LOGGER.error("Authentication failed for param: " + param);
                    return null;
                }
                LOGGER.info("Initializing new novnc client and disconnecting existing session");
                try {
                    ((ConsoleProxyNoVncClient)viewer).getSession().disconnect();
                } catch (IOException e) {
                    LOGGER.error("Exception while disconnect session of novnc viewer object: " + viewer, e);
                }
                removeViewer(viewer);
                viewer = new ConsoleProxyNoVncClient(session);
                viewer.initClient(param);
                connectionMap.put(clientKey, viewer);
                reportLoadChange = true;
            }

            if (reportLoadChange) {
                ConsoleProxyClientStatsCollector statsCollector = getStatsCollector();
                String loadInfo = statsCollector.getStatsReport();
                reportLoadInfo(loadInfo);
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Report load change : " + loadInfo);
            }
            return (ConsoleProxyNoVncClient)viewer;
        }
    }
}
