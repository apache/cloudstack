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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;

import javax.servlet.DispatcherType;

import org.apache.cloudstack.utils.server.ServerPropertiesUtil;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.MovedContextHandler;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.KeyStoreScanner;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.webapp.WebAppContext;

import com.cloud.api.ApiServer;
import com.cloud.utils.Pair;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.server.ServerProperties;

/***
 * The ServerDaemon class implements the embedded server, it can be started either
 * using JSVC or directly from the JAR along with additional jars not shaded in the uber-jar.
 * Configuration parameters are read from server.properties file available on the classpath.
 */
public class ServerDaemon implements Daemon {
    protected Logger logger = LogManager.getLogger(getClass());
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
    private static final String REQUEST_CONTENT_SIZE_KEY = "request.content.size";
    private static final int DEFAULT_REQUEST_CONTENT_SIZE = 1048576;
    private static final String REQUEST_MAX_FORM_KEYS_KEY = "request.max.form.keys";
    private static final int DEFAULT_REQUEST_MAX_FORM_KEYS = 5000;
    private static final String THREADS_MIN = "threads.min";
    private static final String THREADS_MAX = "threads.max";

    ////////////////////////////////////////////////////////
    /////////////// Server Configuration ///////////////////
    ////////////////////////////////////////////////////////

    private Server server;

    private boolean httpEnable = true;
    private int httpPort = 8080;
    private int httpsPort = 8443;
    private int sessionTimeout = 30;
    private int maxFormContentSize = DEFAULT_REQUEST_CONTENT_SIZE;
    private int maxFormKeys = DEFAULT_REQUEST_MAX_FORM_KEYS;
    private boolean httpsEnable = false;
    private String accessLogFile = "access.log";
    private String bindInterface = null;
    private String contextPath = "/client";
    private String keystoreFile;
    private String keystorePassword;
    private String webAppLocation;
    private int minThreads;
    private int maxThreads;

    private boolean shareEnabled = false;
    private String shareBaseDir;
    private String shareCacheCtl;
    private boolean shareDirList = false;
    private String shareSecret;

    //////////////////////////////////////////////////
    /////////////// Public methods ///////////////////
    //////////////////////////////////////////////////

    public static void main(final String... anArgs) throws Exception {
        final ServerDaemon daemon = new ServerDaemon();
        daemon.init(null);
        daemon.start();
    }

    protected void initShareConfigFromProperties() {
        setShareEnabled(ServerPropertiesUtil.getShareEnabled());
        setShareBaseDir(ServerPropertiesUtil.getShareBaseDirectory());
        setShareCacheCtl(ServerPropertiesUtil.getShareCacheControl());
        setShareDirList(ServerPropertiesUtil.getShareDirAllowed());
        setShareSecret(ServerPropertiesUtil.getShareSecret());
    }

    @Override
    public void init(final DaemonContext context) {
        final File confFile = PropertiesUtil.findConfigFile("server.properties");
        if (confFile == null) {
            logger.warn(String.format("Server configuration file not found. Initializing server daemon on %s, with http.enable=%s, http.port=%s, https.enable=%s, https.port=%s, context.path=%s",
                    bindInterface, httpEnable, httpPort, httpsEnable, httpsPort, contextPath));
            return;
        }

        logger.info("Server configuration file found: " + confFile.getAbsolutePath());

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
            setMaxFormContentSize(Integer.valueOf(properties.getProperty(REQUEST_CONTENT_SIZE_KEY, String.valueOf(DEFAULT_REQUEST_CONTENT_SIZE))));
            setMaxFormKeys(Integer.valueOf(properties.getProperty(REQUEST_MAX_FORM_KEYS_KEY, String.valueOf(DEFAULT_REQUEST_MAX_FORM_KEYS))));
            setMinThreads(Integer.valueOf(properties.getProperty(THREADS_MIN, "10")));
            setMaxThreads(Integer.valueOf(properties.getProperty(THREADS_MAX, "500")));
            initShareConfigFromProperties();
        } catch (final IOException e) {
            logger.warn("Failed to read configuration from server.properties file", e);
        } finally {
            // make sure that at least HTTP is enabled if both of them are set to false (misconfiguration)
            if (!httpEnable && !httpsEnable) {
                setHttpEnable(true);
                logger.warn("Server configuration malformed, neither http nor https is enabled, http will be enabled.");
            }
        }
        logger.info(String.format("Initializing server daemon on %s, with http.enable=%s, http.port=%s, https.enable=%s, https.port=%s, context.path=%s",
                bindInterface, httpEnable, httpPort, httpsEnable, httpsPort, contextPath));
    }

    @Override
    public void start() throws Exception {
        // Thread pool
        final QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMinThreads(minThreads);
        threadPool.setMaxThreads(maxThreads);

        // Jetty Server
        server = new Server(threadPool);

        // Setup Scheduler
        server.addBean(new ScheduledExecutorScheduler());

        // Setup JMX
        final MBeanContainer mbeanContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
        server.addBean(mbeanContainer);

        // HTTP config
        final HttpConfiguration httpConfig = new HttpConfiguration();
// it would be nice to make this dynamic but we take care of this ourselves for now: httpConfig.addCustomizer( new ForwardedRequestCustomizer() );
        httpConfig.setSecureScheme("https");
        httpConfig.setSecurePort(httpsPort);
        httpConfig.setOutputBufferSize(32768);
        httpConfig.setRequestHeaderSize(8192);
        httpConfig.setResponseHeaderSize(8192);
        httpConfig.setSendServerVersion(false);
        httpConfig.setSendDateHeader(false);
        addForwardingCustomiser(httpConfig);

        // HTTP Connector
        createHttpConnector(httpConfig);

        // Setup handlers
        Pair<SessionHandler,HandlerCollection> pair = createHandlers();
        server.setHandler(pair.second());

        // Extra config options
        server.setStopAtShutdown(true);
        server.setAttribute(ContextHandler.MAX_FORM_CONTENT_SIZE_KEY, maxFormContentSize);
        server.setAttribute(ContextHandler.MAX_FORM_KEYS_KEY, maxFormKeys);

        // HTTPS Connector
        createHttpsConnector(httpConfig);

        server.start();
        // Must set the session timeout after the server has started
        pair.first().setMaxInactiveInterval(sessionTimeout * 60);
        server.join();
    }

    /**
     * Adds a ForwardedRequestCustomizer to the HTTP configuration to handle forwarded headers.
     * The header used for forwarding is determined by the ApiServer.listOfForwardHeaders property.
     * Only non empty headers are considered and only the first of the comma-separated list is used.
     * @param httpConfig the HTTP configuration to which the customizer will be added
     */
    private static void addForwardingCustomiser(HttpConfiguration httpConfig) {
        ForwardedRequestCustomizer customiser = new ForwardedRequestCustomizer();
        String header = Arrays.stream(ApiServer.listOfForwardHeaders.value().split(",")).findFirst().orElse(null);
        if (com.cloud.utils.StringUtils.isNotEmpty(header)) {
            customiser.setForwardedForHeader(header);
        }
        httpConfig.addCustomizer(customiser);
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
                logger.error("failed to set up keystore scanner, manual refresh of certificates will be required", ex);
            }
        }
    }

    /**
     * Creates a Jetty context at /share to serve static files for modules (e.g. Extensions Framework).
     * Controlled via server properties
     *
     * @return a configured Handler or null if disabled.
     */
    private Handler createShareContextHandler() throws IOException {
        if (!shareEnabled) {
            logger.info("/{} context not mounted", ServerPropertiesUtil.SHARE_DIR);
            return null;
        }

        final Path base = Paths.get(shareBaseDir);
        Files.createDirectories(base);

        final ServletContextHandler shareCtx = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        shareCtx.setContextPath("/" + ServerPropertiesUtil.SHARE_DIR);
        shareCtx.setBaseResource(Resource.newResource(base.toAbsolutePath().toUri()));

        // Efficient static file serving
        ServletHolder def = shareCtx.addServlet(DefaultServlet.class, "/*");
        def.setInitParameter("dirAllowed", Boolean.toString(shareDirList));
        def.setInitParameter("etags", "true");
        def.setInitParameter("cacheControl", shareCacheCtl);
        def.setInitParameter("useFileMappedBuffer", "true");
        def.setInitParameter("acceptRanges", "true");

        // Gzip using modern Jetty handler
        org.eclipse.jetty.server.handler.gzip.GzipHandler gzipHandler =
                new org.eclipse.jetty.server.handler.gzip.GzipHandler();
        gzipHandler.setMinGzipSize(1024);
        gzipHandler.setIncludedMimeTypes(
                "text/html", "text/plain", "text/css", "text/javascript",
                "application/javascript", "application/json", "application/xml");
        gzipHandler.setHandler(shareCtx);

        // Optional signed-URL guard (path + "|" + exp => HMAC-SHA256, base64url)
        if (StringUtils.isNotBlank(shareSecret)) {
            shareCtx.addFilter(new FilterHolder(new ShareSignedUrlFilter(shareSecret)),
                    "/*", EnumSet.of(DispatcherType.REQUEST));
        }

        logger.info("Mounted /{} static context at baseDir={}", ServerPropertiesUtil.SHARE_DIR, base);
        return shareCtx;
    }

    private Pair<SessionHandler,HandlerCollection> createHandlers() {
        final WebAppContext webApp = new WebAppContext();
        webApp.setContextPath(contextPath);
        webApp.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
        webApp.setMaxFormContentSize(maxFormContentSize);
        webApp.setMaxFormKeys(maxFormKeys);

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

        // Optional /share handler (served by createShareContextHandler)
        Handler shareHandler = null;
        try {
            shareHandler = createShareContextHandler();
        } catch (IOException e) {
            logger.error("Failed to initialize /share context", e);
        }

        List<Handler> handlers = new java.util.ArrayList<>();
        handlers.add(log);
        handlers.add(gzipHandler);
        if (shareHandler != null) {
            handlers.add(shareHandler);
        }
        // Put rootRedirect at the end!
        handlers.add(rootRedirect);
        return new Pair<>(webApp.getSessionHandler(), new HandlerCollection(handlers.toArray(new Handler[0])));
    }

    private RequestLog createRequestLog() {
        final ACSRequestLog log = new ACSRequestLog();
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

    public void setMaxFormContentSize(int maxFormContentSize) {
        this.maxFormContentSize = maxFormContentSize;
    }

    public void setMaxFormKeys(int maxFormKeys) {
        this.maxFormKeys = maxFormKeys;
    }

    public void setMinThreads(int minThreads) {
        this.minThreads = minThreads;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public void setShareEnabled(boolean shareEnabled) {
        this.shareEnabled = shareEnabled;
    }

    public void setShareBaseDir(String shareBaseDir) {
        this.shareBaseDir = shareBaseDir;
    }

    public void setShareCacheCtl(String shareCacheCtl) {
        this.shareCacheCtl = shareCacheCtl;
    }

    public void setShareDirList(boolean shareDirList) {
        this.shareDirList = shareDirList;
    }

    public void setShareSecret(String shareSecret) {
        this.shareSecret = shareSecret;
    }
}
