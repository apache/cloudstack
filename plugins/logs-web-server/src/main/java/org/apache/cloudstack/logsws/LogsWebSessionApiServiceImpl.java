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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.logsws.api.command.admin.CreateLogsWebSessionCmd;
import org.apache.cloudstack.logsws.api.command.admin.DeleteLogsWebSession;
import org.apache.cloudstack.logsws.api.command.admin.ListLogsWebSessionsCmd;
import org.apache.cloudstack.logsws.api.response.LogsWebSessionResponse;
import org.apache.cloudstack.logsws.api.response.LogsWebSessionWebSocketResponse;
import org.apache.cloudstack.logsws.dao.LogsWebSessionDao;
import org.apache.cloudstack.logsws.vo.LogsWebSessionVO;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.api.ApiServlet;
import com.cloud.domain.Domain;
import com.cloud.exception.InternalErrorException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.DomainService;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;

public class LogsWebSessionApiServiceImpl extends ManagerBase implements LogsWebSessionApiService {

    @Inject
    LogsWebSessionManager logsWSManager;
    @Inject
    LogsWebSessionDao logsWebSessionDao;
    @Inject
    AccountService accountService;
    @Inject
    DomainService domainService;

    @Override
    public ListResponse<LogsWebSessionResponse> listLogsWebSessions(ListLogsWebSessionsCmd cmd) {
        final Long id = cmd.getId();
        if (!accountService.isRootAdmin(CallContext.current().getCallingAccountId())) {
            throw new PermissionDeniedException("Invalid request");
        }
        List<LogsWebSessionResponse> responsesList = new ArrayList<>();
        SearchBuilder<LogsWebSessionVO> sb = logsWebSessionDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        SearchCriteria<LogsWebSessionVO> sc = sb.create();
        if (id != null) {
            sc.setParameters("id", id);
        }

        Filter searchFilter = new Filter(LogsWebSessionVO.class, "id", true, cmd.getStartIndex(),
                cmd.getPageSizeVal());
        Pair<List<LogsWebSessionVO>, Integer> logsWebSessionsAndCount = logsWebSessionDao.searchAndCount(sc, searchFilter);
        for (LogsWebSessionVO session : logsWebSessionsAndCount.first()) {
            try {
                LogsWebSessionResponse response = createLogsWebSessionResponse(session);
                responsesList.add(response);
            } catch (InternalErrorException exception) {
                logger.error("Failed to create response for {}", session, exception);
            }
        }
        ListResponse<LogsWebSessionResponse> response = new ListResponse<>();
        response.setResponses(responsesList, logsWebSessionsAndCount.second());
        return response;
    }

    @Override
    public LogsWebSessionResponse createLogsWebSession(CreateLogsWebSessionCmd cmd) throws CloudRuntimeException {
        final Account caller = CallContext.current().getCallingAccount();
        final List<String> filters = cmd.getFilters();
        final Map<String, String> params = cmd.getFullUrlParams();
        String clientAddress;
        if (MapUtils.isNotEmpty(params)) {
            clientAddress = params.get(ApiServlet.CLIENT_INET_ADDRESS_KEY);
        } else {
            clientAddress = null;
        }
        if (!accountService.isRootAdmin(caller.getAccountId())) {
            throw new PermissionDeniedException("Invalid request");
        }
        for (String filter : filters) {
            if (StringUtils.isBlank(filter)) {
                throw new InvalidParameterValueException(String.format("Invalid value for parameter - %s",
                        ApiConstants.FILTERS));
            }
        }
        if (!logsWSManager.canCreateNewLogsWebSession()) {
            throw new CloudRuntimeException("Failed to create Logs Web Session as max session limit reached");
        }
        try {
            return Transaction.execute((TransactionCallbackWithException<LogsWebSessionResponse, InternalErrorException>) status -> {
                LogsWebSessionVO logsWebSessionVO = new LogsWebSessionVO(filters, caller.getDomainId(), caller.getAccountId(),
                        clientAddress);
                logsWebSessionVO = logsWebSessionDao.persist(logsWebSessionVO);
                return createLogsWebSessionResponse(logsWebSessionVO);
            });
        } catch (InternalErrorException e) {
            throw new CloudRuntimeException("Failed to create Logs Web Session as unable to prepare response", e);
        }
    }

    @Override
    public boolean deleteLogsWebSession(DeleteLogsWebSession cmd) throws CloudRuntimeException {
        final long id = cmd.getId();
        if (!accountService.isRootAdmin(CallContext.current().getCallingAccountId())) {
            throw new PermissionDeniedException("Invalid request");
        }
        return logsWebSessionDao.remove(id);
    }

    protected String getRealIp4Address() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || iface.isPointToPoint() || !iface.isUp()) continue;

                for (InterfaceAddress addr : iface.getInterfaceAddresses()) {
                    InetAddress inetAddr = addr.getAddress();
                    if (inetAddr instanceof Inet4Address && !inetAddr.isLoopbackAddress()) {
                        return inetAddr.getHostAddress();
                    }
                }
            }
        } catch (SocketException ignored) {}
        return null;
    }

    protected Set<LogsWebSessionWebSocketResponse> getLogsWebSessionWebSocketResponses(
            final LogsWebSessionVO logsWebSessionVO) throws InternalErrorException {
        Set<LogsWebSessionWebSocketResponse> responses = new HashSet<>();
        List<LogsWebSessionWebSocket> webSockets = logsWSManager.getLogsWebSessionWebSockets(logsWebSessionVO);
        if (CollectionUtils.isEmpty(webSockets)) {
            throw new CloudRuntimeException(String.format("Failed to get websocket endpoints for Logs Web Session %s",
                    logsWebSessionVO.getUuid()));
        }
        for (LogsWebSessionWebSocket socket : webSockets) {
            LogsWebSessionWebSocketResponse webSocketResponse = new LogsWebSessionWebSocketResponse();
            webSocketResponse.setManagementServerId(socket.getManagementServerHost().getUuid());
            webSocketResponse.setManagementServerName(socket.getManagementServerHost().getName());
            if (LogsWebSessionManager.LogsWebServerDirectConnect.value()) {
                String serviceIp = socket.getManagementServerHost().getServiceIP();
                if (ManagementServerNode.getManagementServerId() == socket.getManagementServerHost().getMsid() &&
                        NetUtils.isLocalAddress(serviceIp)) {
                    String realIp = getRealIp4Address();
                    if (realIp != null) {
                        serviceIp = realIp;
                    }
                }
                webSocketResponse.setHost(serviceIp);
            }
            webSocketResponse.setPort(socket.getPort());
            webSocketResponse.setPath(socket.getPath());
            webSocketResponse.setSsl(socket.isSsl());
            responses.add(webSocketResponse);
        }
        return responses;
    }

    protected LogsWebSessionResponse createLogsWebSessionResponse(final LogsWebSessionVO logsWebSessionVO) throws InternalErrorException {
        LogsWebSessionResponse response = new LogsWebSessionResponse();
        response.setObjectName("logswebsession");
        response.setId(logsWebSessionVO.getUuid());
        response.setFilters(logsWebSessionVO.getFilters());
        Account account = accountService.getAccount(logsWebSessionVO.getAccountId());
        response.setAccountName(account.getAccountName());
        Domain domain = domainService.getDomain(logsWebSessionVO.getDomainId());
        response.setDomainId(domain.getUuid());
        response.setDomainName(domain.getName());
        response.setDomainPath(domain.getName());
        response.setCreatorAddress(logsWebSessionVO.getCreatorAddress());
        response.setConnected(logsWebSessionVO.getConnections());
        response.setClientAddress(logsWebSessionVO.getClientAddress());
        response.setCreated(logsWebSessionVO.getCreated());
        response.setWebsocketResponse(getLogsWebSessionWebSocketResponses(logsWebSessionVO));
        return response;
    }

    @Override
    public List<Class<?>> getCommands() {
        if (!LogsWebSessionManager.LogsWebServerEnabled.value()) {
            return Collections.emptyList();
        }
        return List.of(
                CreateLogsWebSessionCmd.class,
                ListLogsWebSessionsCmd.class,
                DeleteLogsWebSession.class
        );
    }
}
