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

import java.security.GeneralSecurityException;
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
import org.apache.cloudstack.framework.websocket.server.manager.WebSocketServerManager;
import org.apache.cloudstack.logsws.command.GetLogsSessionWebSocketAnswer;
import org.apache.cloudstack.logsws.command.GetLogsSessionWebSocketCommand;
import org.apache.cloudstack.logsws.dao.LogsWebSessionDao;
import org.apache.cloudstack.logsws.server.LogsWebSocketRouteManager;
import org.apache.cloudstack.logsws.server.LogsWebSocketRoutingHandler;
import org.apache.cloudstack.logsws.server.LogsWebSocketServerHelper;
import org.apache.cloudstack.logsws.vo.LogsWebSessionVO;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.cluster.ClusterCommandProcessor;
import org.apache.cloudstack.management.ManagementServerHost;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.commons.lang3.StringUtils;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.cluster.ClusterManager;
import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.exception.InternalErrorException;
import com.cloud.serializer.GsonHelper;
import com.cloud.utils.DateUtil;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.GlobalLock;

public class LogsWebSessionManagerImpl extends ManagerBase implements LogsWebSessionManager, LogsWebSocketServerHelper,
        ClusterCommandProcessor {

    @Inject
    WebSocketServerManager webSocketServerManager;
    @Inject
    LogsWebSessionDao logsWebSessionDao;
    @Inject
    ManagementServerHostDao managementServerHostDao;
    @Inject
    ClusterManager clusterManager;

    private String serverPath;
    private int idleTimeoutSeconds;
    private ScheduledExecutorService staleLogsWebSessionCleanupExecutor;
    private ManagementServerHostVO managementServer = null;
    private LogsWebSocketRouteManager logsWebSocketRouteManager;

    private final static List<Class<?>> SUPPORTED_COMMANDS = List.of(
            GetLogsSessionWebSocketCommand.class
    );

    protected ManagementServerHostVO getCurrentManagementServer() {
        if (managementServer == null) {
            managementServer =
                    managementServerHostDao.findByMsid(ManagementServerNode.getManagementServerId());
        }
        return managementServer;
    }

    protected Long getManagementServerId() {
        return getCurrentManagementServer().getId();
    }

    protected Long getManagementServerRunId() {
        return getCurrentManagementServer().getRunid();
    }

    protected void registerLogsWebSocketServerRoute() {
        logsWebSocketRouteManager = new LogsWebSocketRouteManager();
        logger.info("Registering Logs WebSocket server at path: {}", serverPath);
        webSocketServerManager.registerRoute(serverPath + "/", new LogsWebSocketRoutingHandler(
                logsWebSocketRouteManager, this), idleTimeoutSeconds);
    }

    protected LogsWebSessionWebSocket getLogsWebSessionWebSocket(LogsWebSessionTokenPayload payload) throws
            InternalErrorException {
        if (webSocketServerManager.isServerEnabled()) {
            logger.warn("WebSocket server not running on this management server, websocket can not be " +
                    "returned for LogsWebSession ID: {}", payload.getSessionUuid());
            return null;
        }
        ManagementServerHostVO managementServerHostVO = getCurrentManagementServer();
        return new LogsWebSessionWebSocket(managementServerHostVO,
                webSocketServerManager.getServerPort(),
                getLogsWebSessionWebSocketPathForManagementServer(managementServerHostVO, payload),
                webSocketServerManager.isServerSslEnabled());
    }

    protected GetLogsSessionWebSocketAnswer processGetLogsSessionWebSocketCommand(
            GetLogsSessionWebSocketCommand cmd) {
        LogsWebSessionTokenPayload payload = cmd.getTokenPayload();
        LogsWebSessionWebSocket webSocket;
        try {
            webSocket = getLogsWebSessionWebSocket(payload);
        } catch (InternalErrorException e) {
            logger.error("Failed to process GetLogsSessionWebSocketCommand command for ID: {}",
                    cmd.getSessionId(), e);
            return new GetLogsSessionWebSocketAnswer(cmd, e.getMessage());
        }
        if (webSocket == null) {
            return new GetLogsSessionWebSocketAnswer(cmd, "WebSocket server not running");
        }
        return new GetLogsSessionWebSocketAnswer(cmd, webSocket.getPort(), webSocket.getPath(), webSocket.isSsl());
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
            throw new ConfigurationException("Unable to to configure " +
                    LogsWebSessionManagerImpl.class.getSimpleName());
        }
        return true;
    }

    @Override
    public boolean start() {
        if (!LogsWebServerEnabled.value()) {
            return true;
        }
        serverPath = LogsWebServerPath.valueIn(getManagementServerId());
        idleTimeoutSeconds = LogsWebServerSessionIdleTimeout.value();
        long staleLogsWebSessionCleanupInterval = LogsWebServerSessionStaleCleanupInterval.value();
        staleLogsWebSessionCleanupExecutor.scheduleWithFixedDelay(new StaleLogsWebSessionCleanupWorker(),
                staleLogsWebSessionCleanupInterval, staleLogsWebSessionCleanupInterval, TimeUnit.SECONDS);
        registerLogsWebSocketServerRoute();
        return true;
    }

    @Override
    public boolean stop() {
        logsWebSessionDao.markAllActiveAsDisconnected();
        webSocketServerManager.unregisterRoute(serverPath + "/");
        return true;
    }

    protected LogsWebSessionTokenPayload getLogsWebSessionWebSocketTokenPayloadUsingVO(LogsWebSession session) {
        LogsWebSessionVO sessionVO;
        if (session instanceof LogsWebSessionVO) {
            sessionVO = (LogsWebSessionVO) session;
        } else {
            sessionVO = logsWebSessionDao.findById(session.getId());
        }
        return new LogsWebSessionTokenPayload(sessionVO.getUuid(), sessionVO.getCreatorAddress());
    }

    protected String getLogsWebSessionWebSocketPathForManagementServer(ManagementServerHostVO managementServerHostVO,
                   LogsWebSessionTokenPayload payload) throws InternalErrorException {
        String path = serverPath;
        if (!Objects.equals(managementServerHostVO.getId(), getManagementServerId())) {
            path = LogsWebServerPath.valueIn(managementServerHostVO.getId());
        }
        try {
            return String.format("%s%s/%s",
                    webSocketServerManager.getWebSocketBasePath(),
                    path,
                    LogsWebSessionTokenCryptoUtil.encrypt(payload, String.valueOf(managementServerHostVO.getRunid())));
        } catch (GeneralSecurityException e) {
            logger.error("Failed to encrypt token payload: {}", payload, e);
            throw new InternalErrorException("Failed to encrypt token payload: " + payload, e);
        }
    }

    @Override
    public List<Class<?>> getCommands() {
        return List.of();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[]{
                LogsWebServerEnabled,
                LogsWebServerPath,
                LogsWebServerSessionIdleTimeout,
                LogsWebServerConcurrentSessions,
                LogsWebServerLogFile,
                LogsWebServerSessionTailExistingLines,
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
    public LogsWebSessionTokenPayload parseToken(String token) {
        try {
            return LogsWebSessionTokenCryptoUtil.decrypt(token, String.valueOf(getManagementServerRunId()));
        } catch (GeneralSecurityException e) {
            logger.error("Failed to decrypt route token: {}", token, e);
        }
        return null;
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
            logsWebSocketRouteManager.removeRoute(logsWebSessionVO.getUuid());
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

    protected LogsWebSessionWebSocket getWebSocketResultFromAnswersString(String answersStr,
                  LogsWebSession logsWebSession, ManagementServerHostVO msHost) {
        Answer[] answers;
        try {
            answers = GsonHelper.getGson().fromJson(answersStr, Answer[].class);
        } catch (Exception e) {
            logger.error("Failed to parse answer JSON during get websocket for {} on {}: {}",
                    logsWebSession, msHost, e.getMessage(), e);
            return null;
        }
        Answer answer = answers != null && answers.length > 0 ? answers[0] : null;
        String details = "Unknown error";
        if (answer instanceof GetLogsSessionWebSocketAnswer && answer.getResult()) {
            GetLogsSessionWebSocketAnswer wsAnswer = (GetLogsSessionWebSocketAnswer) answer;
            return new LogsWebSessionWebSocket(msHost, wsAnswer.getPort(), wsAnswer.getPath(), wsAnswer.isSsl());
        }
        if (answer != null) {
            details = answer.getDetails();
        }
        logger.error("Failed to get websocket for {} on {} due to {}", logsWebSession, msHost, details);
        return null;
    }

    @Override
    public List<LogsWebSessionWebSocket> getLogsWebSessionWebSockets(final LogsWebSession logsWebSession) throws
            InternalErrorException {
        List<LogsWebSessionWebSocket> webSockets = new ArrayList<>();
        final List<ManagementServerHostVO> activeMsList =
                managementServerHostDao.listBy(ManagementServerHost.State.Up);
        LogsWebSessionTokenPayload payload = getLogsWebSessionWebSocketTokenPayloadUsingVO(logsWebSession);
        LogsWebSessionWebSocket localWebSocket = getLogsWebSessionWebSocket(payload);
        if (localWebSocket != null) {
            webSockets.add(localWebSocket);
        }
        for (ManagementServerHostVO msHost : activeMsList) {
            if (Objects.equals(msHost.getId(), getManagementServerId())) {
                continue;
            }
            final String msPeer = Long.toString(msHost.getMsid());
            logger.debug("Sending get websocket command for {} to MS: {}", logsWebSession, msPeer);
            final Command[] commands = new Command[1];
            commands[0] = new GetLogsSessionWebSocketCommand(logsWebSession.getId(), payload);
            String answersStr = clusterManager.execute(msPeer, 0L, GsonHelper.getGson().toJson(commands), true);
            LogsWebSessionWebSocket webSocket = getWebSocketResultFromAnswersString(answersStr, logsWebSession, msHost);
            if (webSocket != null) {
                webSockets.add(webSocket);
            }
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

    @Override
    public boolean supportsCommand(Class<?> clazz) {
        return clazz != null && SUPPORTED_COMMANDS.contains(clazz);
    }

    @Override
    public String processCommand(Command cmd) {
        logger.debug("Processing command: {}", cmd);
        String commandClass = cmd.getClass().getName();
        if (cmd instanceof GetLogsSessionWebSocketCommand) {
            GetLogsSessionWebSocketCommand getCmd = (GetLogsSessionWebSocketCommand) cmd;
            GetLogsSessionWebSocketAnswer answer = processGetLogsSessionWebSocketCommand(getCmd);
            return GsonHelper.getGson().toJson(answer);
        }
        return GsonHelper.getGson().toJson(new Answer(cmd, false,
                "Unsupported command: " + commandClass));
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
