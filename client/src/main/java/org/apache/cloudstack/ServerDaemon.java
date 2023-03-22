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
package org.apache.cloudstack;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.Properties;

import com.cloud.utils.Pair;
import com.cloud.utils.server.ServerProperties;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.MovedContextHandler;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.util.ssl.KeyStoreScanner;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.apache.log4j.Logger;

import com.cloud.utils.PropertiesUtil;
import org.apache.commons.lang3.StringUtils;

/***
 * The ServerDaemon class implements the embedded server, it can be started either
 * using JSVC or directly from the JAR along with additional jars not shaded in the uber-jar.
 * Configuration parameters are read from server.properties file available on the classpath.
 */
public class ServerDaemon implements Daemon {
    private static final Logger LOG = Logger.getLogger(ServerDaemon.class);
    private static final String WEB_XML = "META-INF/webapp/WEB-INF/web.xml";

    /////////////////////////////////////////////////////
    /////////////// Server Properties ///////////////////
    /////////////////////////////////////////////////////

    private static final String BIND_INTERFACE = "bind.interface";
    private static final String CONTEXT_PATH = "context.path";
    private static final String SESSION_TIMEOUT = "session.timeout";
    private static final String HTTP_ENABLE = "http.enable";
    private static final String HTTP_PORT = "http.port";
    private static final String HTTPS_ENABLE = "https.enable";
    private static final String HTTPS_PORT = "https.port";
    private static final String KEYSTORE_FILE = "https.keystore";
    private static final String KEYSTORE_PASSWORD = "https.keystore.password";
    private static final String WEBAPP_DIR = "webapp.dir";
    private static final String ACCESS_LOG = "access.log";

    ////////////////////////////////////////////////////////
    /////////////// Server Configuration ///////////////////
    ////////////////////////////////////////////////////////

    private Server server;

    private boolean httpEnable = true;
    private int httpPort = 8080;
    private int httpsPort = 8443;
    private int sessionTimeout = 30;
    private boolean httpsEnable = false;
    private String accessLogFile = "access.log";
    private String bindInterface = null;
    private String contextPath = "/client";
    private String keystoreFile;
    private String keystorePassword;
    private String webAppLocation;

    //////////////////////////////////////////////////
    /////////////// Public methods ///////////////////
    //////////////////////////////////////////////////

    public static void main(final String... anArgs) throws Exception {
        final ServerDaemon daemon = new ServerDaemon();
        daemon.init(null);
        daemon.start();
    }

    @Override
    public void init(final DaemonContext context) {
        final File confFile = PropertiesUtil.findConfigFile("server.properties");
        if (confFile == null) {
            LOG.warn(String.format("Server configuration file not found. Initializing server daemon on %s, with http.enable=%s, http.port=%s, https.enable=%s, https.port=%s, context.path=%s",
                    bindInterface, httpEnable, httpPort, httpsEnable, httpsPort, contextPath));
            return;
        }

        LOG.info("Server configuration file found: " + confFile.getAbsolutePath());

        try {
            InputStream is = new FileInputStream(confFile);
            final Properties properties = ServerProperties.getServerProperties(is);
            if (properties == null) {
                return;
            }
            setBindInterface(properties.getProperty(BIND_INTERFACE, null));
            setContextPath(properties.getProperty(CONTEXT_PATH, "/client"));
            setHttpEnable(Boolean.valueOf(properties.getProperty(HTTP_ENABLE, "true")));
            setHttpPort(Integer.valueOf(properties.getProperty(HTTP_PORT, "8080")));
            setHttpsEnable(Boolean.valueOf(properties.getProperty(HTTPS_ENABLE, "false")));
            setHttpsPort(Integer.valueOf(properties.getProperty(HTTPS_PORT, "8443")));
            setKeystoreFile(properties.getProperty(KEYSTORE_FILE));
            setKeystorePassword(properties.getProperty(KEYSTORE_PASSWORD));
            setWebAppLocation(properties.getProperty(WEBAPP_DIR));
            setAccessLogFile(properties.getProperty(ACCESS_LOG, "access.log"));
            setSessionTimeout(Integer.valueOf(properties.getProperty(SESSION_TIMEOUT, "30")));
        } catch (final IOException e) {
            LOG.warn("Failed to read configuration from server.properties file", e);
        } finally {
            // make sure that at least HTTP is enabled if both of them are set to false (misconfiguration)
            if (!httpEnable && !httpsEnable) {
                setHttpEnable(true);
                LOG.warn("Server configuration malformed, neither http nor https is enabled, http will be enabled.");
            }
        }
        LOG.info(String.format("Initializing server daemon on %s, with http.enable=%s, http.port=%s, https.enable=%s, https.port=%s, context.path=%s",
                bindInterface, httpEnable, httpPort, httpsEnable, httpsPort, contextPath));
    }

    @Override
    public void start() throws Exception {
        // Thread pool
        final QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMinThreads(10);
        threadPool.setMaxThreads(500);

        // Jetty Server
        server = new Server(threadPool);

        // Setup Scheduler
        server.addBean(new ScheduledExecutorScheduler());

        // Setup JMX
        final MBeanContainer mbeanContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
        server.addBean(mbeanContainer);

        // HTTP config
        final HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.addCustomizer( new ForwardedRequestCustomizer() );
        httpConfig.setSecureScheme("https");
        httpConfig.setSecurePort(httpsPort);
        httpConfig.setOutputBufferSize(32768);
        httpConfig.setRequestHeaderSize(8192);
        httpConfig.setResponseHeaderSize(8192);
        httpConfig.setSendServerVersion(false);
        httpConfig.setSendDateHeader(false);

        // HTTP Connector
        createHttpConnector(httpConfig);

        // Setup handlers
        Pair<SessionHandler,HandlerCollection> pair = createHandlers();
        server.setHandler(pair.second());

        // Extra config options
        server.setStopAtShutdown(true);

        // HTTPS Connector
        createHttpsConnector(httpConfig);

        server.start();
        // Must set the session timeout after the server has started
        pair.first().setMaxInactiveInterval(sessionTimeout * 60);
        server.join();
    }

    @Override
    public void stop() throws Exception {
        server.stop();
    }

    @Override
    public void destroy() {
        server.destroy();
    }

    ///////////////////////////////////////////////////
    /////////////// Private methods ///////////////////
    ///////////////////////////////////////////////////

    private void createHttpConnector(final HttpConfiguration httpConfig) {
        if (httpEnable) {
            final ServerConnector httpConnector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
            httpConnector.setPort(httpPort);
            httpConnector.setHost(bindInterface);
            httpConnector.setIdleTimeout(30000);
            server.addConnector(httpConnector);
        }
    }

    private void createHttpsConnector(final HttpConfiguration httpConfig) {
        // Configure SSL
        if (httpsEnable && StringUtils.isNotEmpty(keystoreFile) && new File(keystoreFile).exists()) {
            // SSL Context
            final SslContextFactory sslContextFactory = new SslContextFactory.Server();

            // Define keystore path and passwords
            sslContextFactory.setKeyStorePath(keystoreFile);
            sslContextFactory.setKeyStorePassword(keystorePassword);
            sslContextFactory.setKeyManagerPassword(keystorePassword);

            // HTTPS config
            final HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
            httpsConfig.addCustomizer(new SecureRequestCustomizer());

            // HTTPS Connector
            final ServerConnector sslConnector = new ServerConnector(server,
                    new SslConnectionFactory(sslContextFactory, "http/1.1"),
                    new HttpConnectionFactory(httpsConfig));
            sslConnector.setPort(httpsPort);
            sslConnector.setHost(bindInterface);
            server.addConnector(sslConnector);

            // add scanner to auto-reload certs
            try {
                KeyStoreScanner scanner = new KeyStoreScanner(sslContextFactory);
                server.addBean(scanner);
            } catch (Exception ex) {
                LOG.error("failed to set up keystore scanner, manual refresh of certificates will be required", ex);
            }
        }
    }

    private Pair<SessionHandler,HandlerCollection> createHandlers() {
        final WebAppContext webApp = new WebAppContext();
        webApp.setContextPath(contextPath);
        webApp.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");

        // GZIP handler
        final GzipHandler gzipHandler = new GzipHandler();
        gzipHandler.addIncludedMimeTypes("text/html", "text/xml", "text/css", "text/plain", "text/javascript", "application/javascript", "application/json", "application/xml");
        gzipHandler.setIncludedMethods("GET", "POST");
        gzipHandler.setCompressionLevel(9);
        gzipHandler.setHandler(webApp);

        if (StringUtils.isEmpty(webAppLocation)) {
            webApp.setWar(getShadedWarUrl());
        } else {
            webApp.setWar(webAppLocation);
        }

        // Request log handler
        final RequestLogHandler log = new RequestLogHandler();
        log.setRequestLog(createRequestLog());

        // Redirect root context handler_war
        MovedContextHandler rootRedirect = new MovedContextHandler();
        rootRedirect.setContextPath("/");
        rootRedirect.setNewContextURL(contextPath);
        rootRedirect.setPermanent(true);

        // Put rootRedirect at the end!
        return new Pair<>(webApp.getSessionHandler(), new HandlerCollection(log, gzipHandler, rootRedirect));
    }

    private RequestLog createRequestLog() {
        final NCSARequestLog log = new NCSARequestLog();
        final File logPath = new File(accessLogFile);
        final File parentFile = logPath.getParentFile();
        if (parentFile != null) {
            parentFile.mkdirs();
        }
        log.setFilename(logPath.getPath());
        log.setAppend(true);
        log.setLogTimeZone("GMT");
        log.setLogLatency(true);
        return log;
    }

    private URL getResource(String aResource) {
        return Thread.currentThread().getContextClassLoader().getResource(aResource);
    }

    private String getShadedWarUrl() {
        final String urlStr = getResource(WEB_XML).toString();
        return urlStr.substring(0, urlStr.length() - 15);
    }

    ///////////////////////////////////////////
    /////////////// Setters ///////////////////
    ///////////////////////////////////////////

    public void setBindInterface(String bindInterface) {
        this.bindInterface = bindInterface;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public void setHttpEnable(boolean httpEnable) {
        this.httpEnable = httpEnable;
    }

    public void setHttpsPort(int httpsPort) {
        this.httpsPort = httpsPort;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public void setHttpsEnable(boolean httpsEnable) {
        this.httpsEnable = httpsEnable;
    }

    public void setKeystoreFile(String keystoreFile) {
        this.keystoreFile = keystoreFile;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public void setAccessLogFile(String accessLogFile) {
        this.accessLogFile = accessLogFile;
    }

    public void setWebAppLocation(String webAppLocation) {
        this.webAppLocation = webAppLocation;
    }

    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }
}
