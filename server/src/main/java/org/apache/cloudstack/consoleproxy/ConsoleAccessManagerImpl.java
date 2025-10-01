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
package org.apache.cloudstack.consoleproxy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.command.user.consoleproxy.ConsoleEndpoint;
import org.apache.cloudstack.api.command.user.consoleproxy.ListConsoleSessionsCmd;
import org.apache.cloudstack.api.response.ConsoleSessionResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.security.keys.KeysManager;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetExternalConsoleAnswer;
import com.cloud.agent.api.GetVmVncTicketAnswer;
import com.cloud.agent.api.GetVmVncTicketCommand;
import com.cloud.consoleproxy.ConsoleProxyManager;
import com.cloud.dc.DataCenter;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.Domain;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.resource.ResourceState;
import com.cloud.server.ManagementServer;
import com.cloud.servlet.ConsoleProxyClientParam;
import com.cloud.servlet.ConsoleProxyPasswordBasedEncryptor;
import com.cloud.storage.GuestOSVO;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.ConsoleSessionVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.ConsoleSessionDao;
import com.cloud.vm.dao.VMInstanceDetailsDao;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import static com.cloud.consoleproxy.ConsoleProxyManager.NoVncConsoleShowDot;

public class ConsoleAccessManagerImpl extends ManagerBase implements ConsoleAccessManager {

    @Inject
    private AccountManager accountManager;
    @Inject
    private DomainDao domainDao;
    @Inject
    private VirtualMachineManager virtualMachineManager;
    @Inject
    private ManagementServer managementServer;
    @Inject
    private EntityManager entityManager;
    @Inject
    private VMInstanceDetailsDao vmInstanceDetailsDao;
    @Inject
    private KeysManager keysManager;
    @Inject
    private AgentManager agentManager;
    @Inject
    private ConsoleProxyManager consoleProxyManager;
    @Inject
    DataCenterDao dataCenterDao;
    @Inject
    private ConsoleSessionDao consoleSessionDao;
    @Inject
    private ResponseGenerator responseGenerator;

    private ScheduledExecutorService executorService = null;

    private static KeysManager secretKeysManager;
    private final Gson gson = new GsonBuilder().create();

    protected Logger logger = LogManager.getLogger(ConsoleAccessManagerImpl.class);

    private static final List<VirtualMachine.State> unsupportedConsoleVMState = Arrays.asList(
            VirtualMachine.State.Stopped, VirtualMachine.State.Restoring, VirtualMachine.State.Error, VirtualMachine.State.Destroyed
    );

    protected static final List<ResourceState> MAINTENANCE_RESOURCE_STATES = new ArrayList<>(Arrays.asList(
            ResourceState.ErrorInMaintenance, ResourceState.ErrorInPrepareForMaintenance
    ));
    protected static final String WEB_SOCKET_PATH= "websockify";

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        ConsoleAccessManagerImpl.secretKeysManager = keysManager;
        executorService = Executors.newScheduledThreadPool(1, new NamedThreadFactory("ConsoleSession-Scavenger"));
        return super.configure(name, params);
    }

    @Override
    public boolean start() {
        int consoleCleanupInterval = ConsoleAccessManager.ConsoleSessionCleanupInterval.value();
        if (consoleCleanupInterval > 0) {
            logger.info(String.format("The ConsoleSessionCleanupTask will run every %s hours", consoleCleanupInterval));
            executorService.scheduleWithFixedDelay(new ConsoleSessionCleanupTask(), consoleCleanupInterval, consoleCleanupInterval, TimeUnit.HOURS);
        }
        return true;
    }

    @Override
    public String getConfigComponentName() {
        return ConsoleAccessManager.class.getName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[] {
                ConsoleAccessManager.ConsoleSessionCleanupInterval,
                ConsoleAccessManager.ConsoleSessionCleanupRetentionHours
        };
    }

    public class ConsoleSessionCleanupTask extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            final GlobalLock gcLock = GlobalLock.getInternLock("ConsoleSession.Cleanup.Lock");
            try {
                if (gcLock.lock(3)) {
                    try {
                        reallyRun();
                    } finally {
                        gcLock.unlock();
                    }
                }
            } finally {
                gcLock.releaseRef();
            }
        }

        private void reallyRun() {
            if (logger.isDebugEnabled()) {
                logger.debug("Starting ConsoleSessionCleanupTask...");
            }
            Integer retentionHours = ConsoleAccessManager.ConsoleSessionCleanupRetentionHours.value();
            Date dateBefore = DateTime.now().minusHours(retentionHours).toDate();
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Retention hours: %s, checking for removed console session " +
                        "records to expunge older than: %s", retentionHours, dateBefore));
            }
            int sessionsExpunged = consoleSessionDao.expungeSessionsOlderThanDate(dateBefore);
            if (logger.isDebugEnabled()) {
                logger.debug(sessionsExpunged > 0 ?
                        String.format("Expunged %s removed console session records", sessionsExpunged) :
                        "No removed console session records expunged on this cleanup task run");
            }
        }
    }

    @Override
    public ListResponse<ConsoleSessionResponse> listConsoleSessions(ListConsoleSessionsCmd cmd) {
        Pair<List<ConsoleSessionVO>, Integer> consoleSessions = listConsoleSessionsInternal(cmd);
        ListResponse<ConsoleSessionResponse> response = new ListResponse<>();

        ResponseObject.ResponseView responseView = ResponseObject.ResponseView.Restricted;
        Long callerId = CallContext.current().getCallingAccountId();
        if (accountManager.isRootAdmin(callerId)) {
            responseView = ResponseObject.ResponseView.Full;
        }

        List<ConsoleSessionResponse> consoleSessionResponses = new ArrayList<>();
        for (ConsoleSessionVO consoleSession : consoleSessions.first()) {
            ConsoleSessionResponse consoleSessionResponse = responseGenerator.createConsoleSessionResponse(consoleSession, responseView);
            consoleSessionResponses.add(consoleSessionResponse);
        }

        response.setResponses(consoleSessionResponses, consoleSessions.second());
        return response;
    }

    protected Pair<List<ConsoleSessionVO>, Integer> listConsoleSessionsInternal(ListConsoleSessionsCmd cmd) {
        CallContext caller = CallContext.current();
        long domainId = getBaseDomainIdToListConsoleSessions(cmd.getDomainId());
        Long accountId = cmd.getAccountId();
        Long userId = cmd.getUserId();
        boolean isRecursive = cmd.isRecursive();

        boolean isCallerNormalUser = accountManager.isNormalUser(caller.getCallingAccountId());
        if (isCallerNormalUser) {
            accountId = caller.getCallingAccountId();
            userId = caller.getCallingUserId();
        }

        List<Long> domainIds = isRecursive ? domainDao.getDomainAndChildrenIds(domainId) : List.of(domainId);

        return consoleSessionDao.listConsoleSessions(cmd.getId(), domainIds, accountId, userId,
                cmd.getHostId(), cmd.getStartDate(), cmd.getEndDate(), cmd.getVmId(),
                cmd.getConsoleEndpointCreatorAddress(), cmd.getClientAddress(), cmd.isActiveOnly(),
                cmd.getAcquired(), cmd.getPageSizeVal(), cmd.getStartIndex());
    }

    /**
     * Determines the base domain ID for listing console sessions.
     *
     * If no domain ID is provided, returns the caller's domain ID. Otherwise,
     * checks if the caller has access to that domain and returns the provided domain ID.
     *
     * @param domainId The domain ID to check, can be null
     * @return The base domain ID to use for listing console sessions
     * @throws PermissionDeniedException if the caller does not have access to the specified domain
     */
    protected long getBaseDomainIdToListConsoleSessions(Long domainId) {
        Account caller = CallContext.current().getCallingAccount();
        if (domainId == null) {
            return caller.getDomainId();
        }

        Domain domain = domainDao.findById(domainId);
        if (domain == null) {
            throw new InvalidParameterValueException(String.format("Unable to find domain with ID [%s]. Verify the informed domain and try again.", domainId));
        }

        accountManager.checkAccess(caller, domain);
        return domainId;
    }

    @Override
    public ConsoleSession listConsoleSessionById(long id) {
        return consoleSessionDao.findByIdIncludingRemoved(id);
    }

    @Override
    public ConsoleEndpoint generateConsoleEndpoint(Long vmId, String extraSecurityToken, String clientAddress) {
        try {
            if (ObjectUtils.anyNull(accountManager, virtualMachineManager, managementServer)) {
                return new ConsoleEndpoint(false, null, "Console service is not ready");
            }

            if (keysManager.getHashKey() == null) {
                String msg = "Console access denied. Ticket service is not ready yet";
                logger.debug(msg);
                return new ConsoleEndpoint(false, null, msg);
            }

            Account account = CallContext.current().getCallingAccount();

            // Do a sanity check here to make sure the user hasn't already been deleted
            if (account == null) {
                logger.debug("Invalid user/account, reject console access");
                return new ConsoleEndpoint(false, null,"Access denied. Invalid or inconsistent account is found");
            }

            VirtualMachine vm = entityManager.findById(VirtualMachine.class, vmId);
            if (vm == null) {
                logger.info("Invalid console servlet command parameter: " + vmId);
                return new ConsoleEndpoint(false, null, "Cannot find VM with ID " + vmId);
            }

            if (!checkSessionPermission(vm, account)) {
                return new ConsoleEndpoint(false, null, "Permission denied");
            }

            DataCenter zone = dataCenterDao.findById(vm.getDataCenterId());
            if (zone != null && DataCenter.Type.Edge.equals(zone.getType())) {
                String errorMsg = "Console access is not supported for Edge zones";
                logger.error(errorMsg);
                return new ConsoleEndpoint(false, null, errorMsg);
            }

            String sessionUuid = UUID.randomUUID().toString();
            return generateAccessEndpoint(vmId, sessionUuid, extraSecurityToken, clientAddress);
        } catch (Exception e) {
            String errorMsg = String.format("Unexpected exception in ConsoleAccessManager - vmId: %s (%s), clientAddress: %s",
                    vmId, entityManager.findById(VirtualMachine.class, vmId), clientAddress);
            logger.error(errorMsg, e);
            return new ConsoleEndpoint(false, null, "Server Internal Error: " + e.getMessage());
        }
    }

    @Override
    public boolean isSessionAllowed(String sessionUuid) {
        return consoleSessionDao.isSessionAllowed(sessionUuid);
    }

    @Override
    public void removeSessions(String[] sessionUuids) {
        if (ArrayUtils.isNotEmpty(sessionUuids)) {
            for (String sessionUuid : sessionUuids) {
                removeSession(sessionUuid);
            }
        }
    }

    protected void removeSession(String sessionUuid) {
        consoleSessionDao.removeSession(sessionUuid);
    }

    @Override
    public void acquireSession(String sessionUuid, String clientAddress) {
        consoleSessionDao.acquireSession(sessionUuid, clientAddress);
    }

    protected boolean checkSessionPermission(VirtualMachine vm, Account account) {
        if (accountManager.isRootAdmin(account.getId())) {
            return true;
        }

        switch (vm.getType()) {
            case User:
                try {
                    accountManager.checkAccess(account, null, true, vm);
                } catch (PermissionDeniedException ex) {
                    if (accountManager.isNormalUser(account.getId())) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("VM access is denied for VM {}. VM owner " +
                                    "account {} does not match the account id in session {} and " +
                                    "caller is a normal user", vm,
                                    accountManager.getAccount(vm.getAccountId()), account);
                        }
                    } else if ((accountManager.isDomainAdmin(account.getId())
                            || account.getType() == Account.Type.READ_ONLY_ADMIN) && logger.isDebugEnabled()) {
                        logger.debug("VM access is denied for VM {}. VM owner account {}" +
                                " does not match the account id in session {} and the " +
                                "domain-admin caller does not manage the target domain",
                                vm, accountManager.getAccount(vm.getAccountId()), account);
                    }
                    return false;
                }
                break;

            case DomainRouter:
            case ConsoleProxy:
            case SecondaryStorageVm:
                return false;

            default:
                logger.warn("Unrecoginized virtual machine type, deny access by default. type: " + vm.getType());
                return false;
        }

        return true;
    }

    private ConsoleEndpoint generateAccessEndpoint(Long vmId, String sessionUuid, String extraSecurityToken, String clientAddress) {
        VirtualMachine vm = virtualMachineManager.findById(vmId);
        String msg;
        if (vm == null) {
            msg = "VM " + vmId + " does not exist, sending blank response for console access request";
            logger.warn(msg);
            throw new CloudRuntimeException(msg);
        }

        if (unsupportedConsoleVMState.contains(vm.getState())) {
            msg = String.format("VM %s must be running to connect console, sending blank response for console access request", vm);
            logger.warn(msg);
            throw new CloudRuntimeException(msg);
        }

        Long hostId = vm.getState() != VirtualMachine.State.Migrating ? vm.getHostId() : vm.getLastHostId();
        if (hostId == null) {
            msg = String.format("VM %s lost host info, sending blank response for console access request", vm);
            logger.warn(msg);
            throw new CloudRuntimeException(msg);
        }

        HostVO host = managementServer.getHostBy(hostId);
        if (host == null) {
            msg = String.format("Host for VM %s does not exist, sending blank response for console access request", vm);
            logger.warn(msg);
            throw new CloudRuntimeException(msg);
        }

        if (Hypervisor.HypervisorType.LXC.equals(vm.getHypervisorType())) {
            throw new CloudRuntimeException("Console access is not supported for LXC");
        }

        String rootUrl = managementServer.getConsoleAccessUrlRoot(vmId);
        if (rootUrl == null) {
            throw new CloudRuntimeException("Console access will be ready in a few minutes. Please try it again later.");
        }

        ConsoleEndpoint consoleEndpoint = composeConsoleAccessEndpoint(rootUrl, vm, host, clientAddress, sessionUuid, extraSecurityToken);
        logger.debug("The console URL is: " + consoleEndpoint.getUrl());
        return consoleEndpoint;
    }

    protected ConsoleConnectionDetails getConsoleConnectionDetailsForExternalVm(ConsoleConnectionDetails details,
                            VirtualMachine vm, HostVO host) {
        Answer answer = managementServer.getExternalVmConsole(vm, host);
        if (answer == null) {
            logger.error("Unable to get console access details for external {} on {}: answer is null.", vm, host);
            return null;
        }
        if (!answer.getResult()) {
            logger.error("Unable to get console access details for external {} on {}: answer result is false. Reason: {}", vm, host, answer.getDetails());
            return null;
        }
        if (!(answer instanceof GetExternalConsoleAnswer)) {
            logger.error("Unable to get console access details for external {} on {}: answer is not of type GetExternalConsoleAnswer.", vm, host);
            return null;
        }
        GetExternalConsoleAnswer getExternalConsoleAnswer = (GetExternalConsoleAnswer) answer;
        details.setModeFromExternalProtocol(getExternalConsoleAnswer.getProtocol());
        details.setDirectUrl(getExternalConsoleAnswer.getUrl());
        details.setHost(getExternalConsoleAnswer.getHost());
        if (getExternalConsoleAnswer.getPort() != null) {
            details.setPort(getExternalConsoleAnswer.getPort());
        }
        if (StringUtils.isNotBlank(getExternalConsoleAnswer.getPassword())) {
            details.setSid(getExternalConsoleAnswer.getPassword());
            details.setSessionRequiresNewViewer(getExternalConsoleAnswer.isPasswordOneTimeUseOnly());
        }
        return details;
    }

    protected Pair<String, Integer> getHostAndPortForKVMMaintenanceHostIfNeeded(HostVO host,
                            Map<String, String> vmDetails) {
        if (!Hypervisor.HypervisorType.KVM.equals(host.getHypervisorType())) {
            return null;
        }
        if(!MAINTENANCE_RESOURCE_STATES.contains(host.getResourceState())) {
            return null;
        }
        String address = vmDetails.get(VmDetailConstants.KVM_VNC_ADDRESS);
        String port = vmDetails.get(VmDetailConstants.KVM_VNC_PORT);
        if (ObjectUtils.allNotNull(address, port)) {
            return new Pair<>(address, Integer.valueOf(port));
        }
        logger.warn("KVM Host in ErrorInMaintenance/ErrorInPrepareForMaintenance but " +
                "no VNC Address/Port was available. Falling back to default one from MS.");
        return null;
    }

    protected ConsoleConnectionDetails getConsoleConnectionDetails(VirtualMachine vm, HostVO host) {
        String locale = null;
        String tag = vm.getUuid();
        String displayName = vm.getHostName();
        if (vm instanceof UserVm) {
            displayName = ((UserVm) vm).getDisplayName();
        }
        Map<String, String> vmDetails = vmInstanceDetailsDao.listDetailsKeyPairs(vm.getId(),
                List.of(VmDetailConstants.KEYBOARD, VmDetailConstants.KVM_VNC_ADDRESS, VmDetailConstants.KVM_VNC_PORT));
        if (vmDetails.get(VmDetailConstants.KEYBOARD) != null) {
            locale = vmDetails.get(VmDetailConstants.KEYBOARD);
        }
        ConsoleConnectionDetails details = new ConsoleConnectionDetails(vm.getVncPassword(), locale, tag, displayName);
        if (Hypervisor.HypervisorType.External.equals(host.getHypervisorType())) {
            return getConsoleConnectionDetailsForExternalVm(details, vm, host);
        }
        Pair<String, Integer> hostPortInfo = getHostAndPortForKVMMaintenanceHostIfNeeded(host, vmDetails);
        if (hostPortInfo == null) {
            hostPortInfo = managementServer.getVncPort(vm);
        }
        logger.debug("Retrieved VNC host and port info :[{}, {}] for {} on {}", hostPortInfo.first(),
                hostPortInfo.second(), vm, host);
        Ternary<String, String, String> parsedHostInfo = parseHostInfo(hostPortInfo.first());
        details.setHost(parsedHostInfo.first());
        details.setTunnelUrl(parsedHostInfo.second());
        details.setTunnelSession(parsedHostInfo.third());
        details.setPort(hostPortInfo.second());
        if (hostPortInfo.second() == -9) {
            details.setUsingRDP(true);
            details.setPort(Integer.parseInt(managementServer.findDetail(host.getId(), "rdp.server.port")
                    .getValue()));
            logger.debug("HyperV RDP port for {} on {} is: {}", vm, host, details.getPort());
        }
        return details;
    }

    protected ConsoleEndpoint composeConsoleAccessEndpoint(String rootUrl, VirtualMachine vm, HostVO hostVo, String addr,
                         String sessionUuid, String extraSecurityToken) {
        ConsoleConnectionDetails result = getConsoleConnectionDetails(vm, hostVo);
        if (result == null) {
            return new ConsoleEndpoint(false, null, "Console access to this instance cannot be provided");
        }

        if (ConsoleConnectionDetails.Mode.Direct.equals(result.getMode())) {
            persistConsoleSession(sessionUuid, vm.getId(), hostVo.getId(), addr);
            return new ConsoleEndpoint(true, result.getDirectUrl());
        }

        String ticket = genAccessTicket(result.getHost(), String.valueOf(result.getPort()), result.getSid(),
                result.getTag(), sessionUuid);
        ConsoleProxyPasswordBasedEncryptor encryptor = new ConsoleProxyPasswordBasedEncryptor(getEncryptorPassword());
        ConsoleProxyClientParam param = generateConsoleProxyClientParam(result, ticket, sessionUuid, addr,
                extraSecurityToken, vm, hostVo);
        String token = encryptor.encryptObject(ConsoleProxyClientParam.class, param);
        int vncPort = consoleProxyManager.getVncPort(vm.getDataCenterId());

        String url = generateConsoleAccessUrl(rootUrl, param, token, vncPort, vm, hostVo, result.getLocale());

        logger.debug("Adding allowed session: {}", sessionUuid);
        persistConsoleSession(sessionUuid, vm.getId(), hostVo.getId(), addr);
        managementServer.setConsoleAccessForVm(vm.getId(), sessionUuid);

        ConsoleEndpoint consoleEndpoint = new ConsoleEndpoint(true, url);
        consoleEndpoint.setWebsocketHost(managementServer.getConsoleAccessAddress(vm.getId()));
        consoleEndpoint.setWebsocketPort(String.valueOf(vncPort));
        consoleEndpoint.setWebsocketPath(WEB_SOCKET_PATH);
        consoleEndpoint.setWebsocketToken(token);
        if (StringUtils.isNotBlank(param.getExtraSecurityToken())) {
            consoleEndpoint.setWebsocketExtra(param.getExtraSecurityToken());
        }
        return consoleEndpoint;
    }

    protected void persistConsoleSession(String sessionUuid, long instanceId, long hostId, String consoleEndpointCreatorAddress) {
        CallContext caller = CallContext.current();

        ConsoleSessionVO consoleSessionVo = new ConsoleSessionVO();
        consoleSessionVo.setUuid(sessionUuid);
        consoleSessionVo.setDomainId(caller.getCallingAccount().getDomainId());
        consoleSessionVo.setAccountId(caller.getCallingAccountId());
        consoleSessionVo.setUserId(caller.getCallingUserId());
        consoleSessionVo.setInstanceId(instanceId);
        consoleSessionVo.setHostId(hostId);
        consoleSessionVo.setConsoleEndpointCreatorAddress(consoleEndpointCreatorAddress);
        consoleSessionDao.persist(consoleSessionVo);
    }

    protected String generateConsoleAccessUrl(String rootUrl, ConsoleProxyClientParam param, String token, int vncPort,
                                            VirtualMachine vm, HostVO hostVo, String locale) {
        StringBuilder sb = new StringBuilder(rootUrl);
        if (param.getHypervHost() != null || !ConsoleProxyManager.NoVncConsoleDefault.value()) {
            sb.append("/ajax?token=").append(token);
        } else {
            String showDot = NoVncConsoleShowDot.valueIn(vm.getDataCenterId()) ? "true" : "false";
            sb.append("/resource/noVNC/vnc.html")
                    .append("?autoconnect=true&show_dot=").append(showDot)
                    .append("&port=").append(vncPort)
                    .append("&token=").append(token);
            if (requiresVncOverWebSocketConnection(vm, hostVo) && StringUtils.isNotBlank(locale)) {
                sb.append("&language=").append(locale);
            }
        }

        if (StringUtils.isNotBlank(param.getExtraSecurityToken())) {
            sb.append("&extra=").append(param.getExtraSecurityToken());
        }

        // for console access, we need guest OS type to help implement keyboard
        long guestOs = vm.getGuestOSId();
        GuestOSVO guestOsVo = managementServer.getGuestOs(guestOs);
        if (guestOsVo.getCategoryId() == 6)
            sb.append("&guest=windows");

        if (logger.isDebugEnabled()) {
            logger.debug("Compose console url: {}", sb);
        }
        return sb.toString().startsWith("https") ? sb.toString() : "http:" + sb;
    }

    protected ConsoleProxyClientParam generateConsoleProxyClientParam(ConsoleConnectionDetails details, String ticket,
                        String sessionUuid, String addr, String extraSecurityToken, VirtualMachine vm, HostVO host) {
        ConsoleProxyClientParam param = new ConsoleProxyClientParam();
        param.setClientHostAddress(details.getHost());
        param.setClientHostPort(details.getPort());
        param.setClientHostPassword(details.getSid());
        param.setClientTag(details.getTag());
        param.setClientDisplayName(details.getDisplayName());
        param.setTicket(ticket);
        param.setSessionUuid(sessionUuid);
        param.setSourceIP(addr);
        param.setSessionRequiresNewViewer(details.isSessionRequiresNewViewer());

        if (StringUtils.isNotBlank(extraSecurityToken)) {
            param.setExtraSecurityToken(extraSecurityToken);
            logger.debug("Added security token for client validation");
        }

        if (requiresVncOverWebSocketConnection(vm, host)) {
            setWebsocketUrl(vm, param);
        }

        if (details.getLocale() != null) {
            param.setLocale(details.getLocale());
        }

        if (details.isUsingRDP()) {
            //For Hyperv Client Host Address will send Instance id
            param.setHypervHost(host.getPrivateIpAddress());
            param.setUsername(managementServer.findDetail(host.getId(), "username").getValue());
            param.setPassword(managementServer.findDetail(host.getId(), "password").getValue());
        }
        if (ObjectUtils.allNotNull(details.getTunnelUrl(), details.getTunnelSession())) {
            param.setClientTunnelUrl(details.getTunnelUrl());
            param.setClientTunnelSession(details.getTunnelSession());
        }
        return param;
    }

    public Ternary<String, String, String> parseHostInfo(String hostInfo) {
        String host = null;
        String tunnelUrl = null;
        String tunnelSession = null;

        logger.info("Parse host info returned from executing GetVNCPortCommand. host info: " + hostInfo);

        if (hostInfo != null) {
            if (hostInfo.startsWith("consoleurl")) {
                String[] tokens = hostInfo.split("&");

                if (hostInfo.length() > 19 && hostInfo.indexOf('/', 19) > 19) {
                    host = hostInfo.substring(19, hostInfo.indexOf('/', 19)).trim();
                    tunnelUrl = tokens[0].substring("consoleurl=".length());
                    tunnelSession = tokens[1].split("=")[1];
                } else {
                    host = "";
                }
            } else if (hostInfo.startsWith("instanceId")) {
                host = hostInfo.substring(hostInfo.indexOf('=') + 1);
            } else {
                host = hostInfo;
            }
        } else {
            host = hostInfo;
        }

        return new Ternary<>(host, tunnelUrl, tunnelSession);
    }

    /**
     * Since VMware 7.0 VNC servers are deprecated, it uses a ticket to create a VNC over websocket connection
     * Check: https://docs.vmware.com/en/VMware-vSphere/7.0/rn/vsphere-esxi-vcenter-server-70-release-notes.html
     */
    private boolean requiresVncOverWebSocketConnection(VirtualMachine vm, HostVO hostVo) {
        return vm.getHypervisorType() == Hypervisor.HypervisorType.VMware && hostVo.getHypervisorVersion().compareTo("7.0") >= 0;
    }

    @Override
    public String genAccessTicket(String host, String port, String sid, String tag, String sessionUuid) {
        return genAccessTicket(host, port, sid, tag, new Date(), sessionUuid);
    }

    @Override
    public String genAccessTicket(String host, String port, String sid, String tag, Date normalizedHashTime, String sessionUuid) {
        String params = "host=" + host + "&port=" + port + "&sid=" + sid + "&tag=" + tag + "&session=" + sessionUuid;

        try {
            Mac mac = Mac.getInstance("HmacSHA512");

            long ts = normalizedHashTime.getTime();
            ts = ts / 60000;        // round up to 1 minute
            String secretKey = secretKeysManager.getHashKey();

            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), mac.getAlgorithm());
            mac.init(keySpec);
            mac.update(params.getBytes());
            mac.update(String.valueOf(ts).getBytes());

            byte[] encryptedBytes = mac.doFinal();

            return Base64.encodeBase64String(encryptedBytes);
        } catch (Exception e) {
            logger.error("Unexpected exception ", e);
        }
        return "";
    }

    protected String getEncryptorPassword() {
        String key = keysManager.getEncryptionKey();
        String iv = keysManager.getEncryptionIV();

        ConsoleProxyPasswordBasedEncryptor.KeyIVPair keyIvPair = new ConsoleProxyPasswordBasedEncryptor.KeyIVPair(key, iv);
        return gson.toJson(keyIvPair);
    }

    /**
     * Sets the URL to establish a VNC over websocket connection
     */
    private void setWebsocketUrl(VirtualMachine vm, ConsoleProxyClientParam param) {
        String ticket = acquireVncTicketForVmwareVm(vm);
        if (StringUtils.isBlank(ticket)) {
            logger.error(String.format("Could not obtain VNC ticket for VM %s", vm));
            return;
        }
        String wsUrl = composeWebsocketUrlForVmwareVm(ticket, param);
        param.setWebsocketUrl(wsUrl);
    }

    /**
     * Format expected: wss://<ESXi_HOST_IP>:443/ticket/<TICKET_ID>
     */
    private String composeWebsocketUrlForVmwareVm(String ticket, ConsoleProxyClientParam param) {
        param.setClientHostPort(443);
        return String.format("wss://%s:%s/ticket/%s", param.getClientHostAddress(), param.getClientHostPort(), ticket);
    }

    /**
     * Acquires a ticket to be used for console proxy as described in 'Removal of VNC Server from ESXi' on:
     * https://docs.vmware.com/en/VMware-vSphere/7.0/rn/vsphere-esxi-vcenter-server-70-release-notes.html
     */
    private String acquireVncTicketForVmwareVm(VirtualMachine vm) {
        try {
            logger.info("Acquiring VNC ticket for VM = {}", vm);
            GetVmVncTicketCommand cmd = new GetVmVncTicketCommand(vm.getInstanceName());
            Answer answer = agentManager.send(vm.getHostId(), cmd);
            GetVmVncTicketAnswer ans = (GetVmVncTicketAnswer) answer;
            if (!ans.getResult()) {
                logger.info("VNC ticket could not be acquired correctly: " + ans.getDetails());
            }
            return ans.getTicket();
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            logger.error("Error acquiring ticket", e);
            return null;
        }
    }

    protected static class ConsoleConnectionDetails {
        public enum Mode {
            ConsoleProxy,
            Direct
        }

        private Mode mode = Mode.ConsoleProxy;
        private String host;
        private int port = -1;
        private String sid;
        private String locale;
        private String tag;
        private String displayName;
        private String tunnelUrl = null;
        private String tunnelSession = null;
        private boolean usingRDP;
        private String directUrl;
        private boolean sessionRequiresNewViewer = false;

        ConsoleConnectionDetails(String sid, String locale, String tag, String displayName) {
            this.sid = sid;
            this.locale = locale;
            this.tag = tag;
            this.displayName = displayName;
        }

        public Mode getMode() {
            return mode;
        }

        public void setModeFromExternalProtocol(String protocol) {
            this.mode = Mode.ConsoleProxy;
            if (StringUtils.isBlank(protocol)) {
                return;
            }
            if (Mode.Direct.name().toLowerCase().equalsIgnoreCase(protocol)) {
                this.mode = Mode.Direct;
            }
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getSid() {
            return sid;
        }

        public void setSid(String sid) {
            this.sid = sid;
        }

        public String getLocale() {
            return locale;
        }

        public String getTag() {
            return tag;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getTunnelUrl() {
            return tunnelUrl;
        }

        public void setTunnelUrl(String tunnelUrl) {
            this.tunnelUrl = tunnelUrl;
        }

        public String getTunnelSession() {
            return tunnelSession;
        }

        public void setTunnelSession(String tunnelSession) {
            this.tunnelSession = tunnelSession;
        }

        public boolean isUsingRDP() {
            return usingRDP;
        }

        public void setUsingRDP(boolean usingRDP) {
            this.usingRDP = usingRDP;
        }

        public String getDirectUrl() {
            return directUrl;
        }

        public void setDirectUrl(String directUrl) {
            this.directUrl = directUrl;
        }

        public boolean isSessionRequiresNewViewer() {
            return sessionRequiresNewViewer;
        }

        public void setSessionRequiresNewViewer(boolean sessionRequiresNewViewer) {
            this.sessionRequiresNewViewer = sessionRequiresNewViewer;
        }
    }
}
