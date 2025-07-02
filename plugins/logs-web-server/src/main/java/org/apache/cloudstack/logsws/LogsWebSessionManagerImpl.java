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

package org.apache.cloudstack.logsws;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.logsws.dao.LogsWebSessionDao;
import org.apache.cloudstack.logsws.server.LogsWebSocketServer;
import org.apache.cloudstack.logsws.server.LogsWebSocketServerHelper;
import org.apache.cloudstack.logsws.vo.LogsWebSessionVO;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.management.ManagementServerHost;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.commons.lang3.StringUtils;

import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.GlobalLock;

public class LogsWebSessionManagerImpl extends ManagerBase implements LogsWebSessionManager, LogsWebSocketServerHelper {

    @Inject
    LogsWebSessionDao logsWebSessionDao;
    @Inject
    ManagementServerHostDao managementServerHostDao;

    private int serverPort;
    private String serverPath;
    private int serverIdleTimeoutSeconds;
    private LogsWebSocketServer loggerWebSocketServer;
    private ScheduledExecutorService staleLogsWebSessionCleanupExecutor;
    private Long managementServerId = null;

    protected Long getManagementServerId() {
        if (managementServerId != null) {
            ManagementServerHostVO managementServerHostVO =
                    managementServerHostDao.findByMsid(ManagementServerNode.getManagementServerId());
            if (managementServerHostVO != null) {
                managementServerId = managementServerHostVO.getId();
            }
        }
        return managementServerId;
    }

    @Override
    public String getConfigComponentName() {
        return LogsWebSessionManager.class.getSimpleName();
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        try {
            staleLogsWebSessionCleanupExecutor = Executors.newScheduledThreadPool(1,
                    new NamedThreadFactory("Logs-Web-Sessions-Stale-Cleanup-Worker"));
        } catch (final Exception e) {
            throw new ConfigurationException("Unable to to configure " + LogsWebSessionManagerImpl.class.getSimpleName());
        }
        return true;
    }

    @Override
    public boolean start() {
        if (!LogsWebServerEnabled.value()) {
            return true;
        }
        serverPort = LogsWebServerPort.valueIn(getManagementServerId());
        serverPath = LogsWebServerPath.valueIn(getManagementServerId());
        serverIdleTimeoutSeconds = LogsWebServerSessionIdleTimeout.valueIn(getManagementServerId());
        startWebSocketServer();
        long staleLogsWebSessionCleanupInterval = LogsWebServerSessionStaleCleanupInterval.value();
        staleLogsWebSessionCleanupExecutor.scheduleWithFixedDelay(new StaleLogsWebSessionCleanupWorker(),
                staleLogsWebSessionCleanupInterval, staleLogsWebSessionCleanupInterval, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public boolean stop() {
        stopWebSocketServer(1);
        logsWebSessionDao.markAllActiveAsDisconnected();
        return true;
    }

    @Override
    public void startWebSocketServer() {
        if (loggerWebSocketServer != null && loggerWebSocketServer.isRunning()) {
            logger.info("Logger Web Socket Server is already running!");
            return;
        }
        loggerWebSocketServer = new LogsWebSocketServer(serverPort, serverPath, serverIdleTimeoutSeconds,
                this);
        try {
            loggerWebSocketServer.start();
        } catch (InterruptedException e) {
            logger.error("Failed to start Logger Web Socket Server", e);
        }
    }

    protected void stopWebSocketServer(Integer maxWaitSeconds) {
        if (loggerWebSocketServer == null || !loggerWebSocketServer.isRunning()) {
            logger.info("Logger Web Socket Server is already stopped!");
            return;
        }
        loggerWebSocketServer.stop(maxWaitSeconds == null ? 5 : maxWaitSeconds);
        loggerWebSocketServer = null;
    }

    @Override
    public void stopWebSocketServer() {
        stopWebSocketServer(null);
    }

    private String getLogsWebSessionWebSocketPathUsingVO(long msId, LogsWebSession session) {
        LogsWebSessionVO sessionVO = null;
        if (session instanceof LogsWebSessionVO) {
            sessionVO = (LogsWebSessionVO)session;
        } else {
            sessionVO = logsWebSessionDao.findById(session.getId());
        }
        String path = serverPath;
        if (!Objects.equals(msId, getManagementServerId())) {
            serverPath = LogsWebServerPath.valueIn(msId);
        }
        return String.format("%s/%s", path, sessionVO.getUuid());
    }

    @Override
    public List<Class<?>> getCommands() {
        return List.of();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[]{
                LogsWebServerEnabled,
                LogsWebServerPort,
                LogsWebServerPath,
                LogsWebServerConcurrentSessions,
                LogsWebServerLogFile,
                LogsWebServerSessionTailExistingLines,
                LogsWebServerSessionIdleTimeout,
                LogsWebServerSessionStaleCleanupInterval
        };
    }

    @Override
    public String getServerPath() {
        return serverPath;
    }

    @Override
    public String getLogFile() {
        return LogsWebServerLogFile.valueIn(getManagementServerId());
    }

    @Override
    public int getMaxReadExistingLines() {
        return LogsWebServerSessionTailExistingLines.valueIn(getManagementServerId());
    }

    @Override
    public LogsWebSession getSession(String route) {
        if (StringUtils.isBlank(route)) {
            return null;
        }
        return logsWebSessionDao.findByUuid(route);
    }

    @Override
    public void updateSessionConnection(long sessionId, String clientAddress) {
        LogsWebSessionVO logsWebSessionVO = logsWebSessionDao.findById(sessionId);
        if (logsWebSessionVO == null) {
            return;
        }
        if (StringUtils.isNotBlank(clientAddress)) {
            logsWebSessionVO.setConnections(logsWebSessionVO.getConnections() + 1);
            logsWebSessionVO.setConnectedTime(new Date());
            logsWebSessionVO.setClientAddress(clientAddress);
        } else {
            if (logsWebSessionVO.getConnections() == 0) {
                return;
            }
            logsWebSessionVO.setConnections(Math.max(0, logsWebSessionVO.getConnections() - 1));
        }
        logger.trace("Updating session: {}, is connected: {}, connections: {}",
                logsWebSessionVO.getUuid(),
                StringUtils.isBlank(clientAddress),
                logsWebSessionVO.getConnections());
        logsWebSessionDao.update(sessionId, logsWebSessionVO);
    }

    @Override
    public List<LogsWebSessionWebSocket> getLogsWebSessionWebSockets(final LogsWebSession logsWebSession) {
        List<LogsWebSessionWebSocket> webSockets = new ArrayList<>();
        final List<ManagementServerHostVO> activeMsList =
                managementServerHostDao.listBy(ManagementServerHost.State.Up);
        for (ManagementServerHostVO managementServerHostVO : activeMsList) {
            LogsWebSessionWebSocket logsWebSessionWebSocket = new LogsWebSessionWebSocket(managementServerHostVO,
                    LogsWebServerPort.valueIn(managementServerHostVO.getId()),
                    getLogsWebSessionWebSocketPathUsingVO(managementServerHostVO.getId(), logsWebSession));
            webSockets.add(logsWebSessionWebSocket);
        }
        return webSockets;
    }

    @Override
    public boolean canCreateNewLogsWebSession() {
        int maxSessions = LogsWebServerConcurrentSessions.valueIn(getManagementServerId());
        if (maxSessions <= 0) {
            return true;
        }
        return maxSessions > logsWebSessionDao.countConnected();
    }

    public class StaleLogsWebSessionCleanupWorker extends ManagedContextRunnable {

        protected void runCleanupForStaleLogsWebSessions() {
            try {
                ManagementServerHostVO msHost = managementServerHostDao.findOneByLongestRuntime();
                if (msHost == null || (msHost.getMsid() != ManagementServerNode.getManagementServerId())) {
                    logger.debug("Skipping the stale logs web sessions cleanup task on this management server");
                    return;
                }
                long cutOffSeconds = LogsWebServerSessionStaleCleanupInterval.value();
                Date cutOffDate = new Date(System.currentTimeMillis() - (cutOffSeconds * 1000));
                String cutOffDateString = DateUtil.getOutputString(cutOffDate);
                logger.debug("Clearing stale stale logs web sessions older than {} using management server {}",
                        cutOffDateString, msHost);
                long processed = logsWebSessionDao.removeStaleForCutOff(cutOffDate);
                logger.debug("Cleared {} stale stale logs web sessions older than {}", processed,
                        cutOffDateString);
            } catch (Exception e) {
                logger.warn("Cleanup task failed to stale logs web sessions", e);
            }
        }

        @Override
        protected void runInContext() {
            GlobalLock gcLock = GlobalLock.getInternLock("LogsWebSessionsCleanup");
            try {
                if (gcLock.lock(3)) {
                    try {
                        runCleanupForStaleLogsWebSessions();
                    } finally {
                        gcLock.unlock();
                    }
                }
            } finally {
                gcLock.releaseRef();
            }
        }
    }
}
