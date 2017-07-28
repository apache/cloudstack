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

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.NetworkTrafficServerConnector;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/***
 * Daemon server class to start the embedded server, either through JSVC or directly inside a JAR.
 * Parameter to configure the jetty server are:
 * - jetty.port: to start jetty on the specific port (default: 8080)
 * - jetty.host: to bind to specific interface (default: null = all)
 * - jetty.requestlog: path to log file for requests (default: request.log)
 */
public class ServerDaemon implements Daemon {
    private static final Logger logger = LoggerFactory.getLogger(ServerDaemon.class);
    private static final String WEB_XML = "META-INF/webapp/WEB-INF/web.xml";
    private static final String REQUEST_LOG = "request.log";

    private Server jettyServer;
    private int port;
    private String bindInterface;
    private String requestLogFile;
    private String webAppLocation;

    public static void main(String... anArgs) throws Exception {
        ServerDaemon csServer = new ServerDaemon();
        csServer.init(null);
        csServer.start();
        csServer.join();
    }

    @Override
    public void init(DaemonContext context) {
        Properties props = System.getProperties();
        setPort(Integer.parseInt(props.getProperty("port", "8080")));
        setBindInterface(props.getProperty("host"));
        setWebAppLocation(props.getProperty("webapp"));
        setRequestLogFile(props.getProperty("requestlog", REQUEST_LOG));
        StringBuilder sb = new StringBuilder("Initializing server daemon on ");
        sb.append(bindInterface == null ? "*" : bindInterface);
        sb.append(":");
        sb.append(port);
        logger.info(sb.toString());
    }

    @Override
    public void start() throws Exception {
        jettyServer = new Server(createThreadPool());

        // Setup JMX
        MBeanContainer mbeanContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
        jettyServer.addBean(mbeanContainer);

        NetworkTrafficServerConnector connector = createConnector();
        jettyServer.addConnector(connector);

        // This webapp will use jsps and jstl. We need to enable the
        // AnnotationConfiguration in order to correctly
        // set up the jsp container
        Configuration.ClassList classlist = Configuration.ClassList
                .setServerDefault( jettyServer );
        classlist.addBefore(
                "org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
                "org.eclipse.jetty.annotations.AnnotationConfiguration" );



        jettyServer.setHandler(createHandlers());
        jettyServer.setStopAtShutdown(true);

        jettyServer.start();
    }

    public void join() throws InterruptedException {
        jettyServer.join();
    }

    @Override
    public void stop() throws Exception {
        jettyServer.stop();
    }

    @Override
    public void destroy() {
        jettyServer.destroy();
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setBindInterface(String bindInterface) {
        this.bindInterface = bindInterface;
    }

    public void setRequestLogFile(String requestLogFile) {
        this.requestLogFile = requestLogFile;
    }

    public void setWebAppLocation(String webAppLocation) {
        this.webAppLocation = webAppLocation;
    }

    private ThreadPool createThreadPool() {
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMinThreads(10);
        threadPool.setMaxThreads(100);
        return threadPool;
    }

    private NetworkTrafficServerConnector createConnector() {
        NetworkTrafficServerConnector connector = new NetworkTrafficServerConnector(jettyServer);
        connector.setPort(port);
        connector.setHost(bindInterface);
        return connector;
    }

    private HandlerCollection createHandlers() {
        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/client");

        if (webAppLocation == null) {
            webapp.setWar(getShadedWarUrl());
        } else {
            webapp.setWar(webAppLocation);
        }

        List<Handler> handlers = new ArrayList<>();
        handlers.add(webapp);

        HandlerList contexts = new HandlerList();
        contexts.setHandlers(handlers.toArray(new Handler[0]));

        RequestLogHandler log = new RequestLogHandler();
        log.setRequestLog(createRequestLog());

        HandlerCollection result = new HandlerCollection();
        result.setHandlers(new Handler[]{log, contexts});

        return result;
    }

    private RequestLog createRequestLog() {
        NCSARequestLog log = new NCSARequestLog();
        File logPath = new File(requestLogFile);
        File parentFile = logPath.getParentFile();
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
        String urlStr = getResource(WEB_XML).toString();
        return urlStr.substring(0, urlStr.length() - 15);
    }
}
