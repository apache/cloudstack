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

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetVmVncTicketAnswer;
import com.cloud.agent.api.GetVmVncTicketCommand;
import com.cloud.consoleproxy.ConsoleProxyManager;
import com.cloud.exception.AgentUnavailableException;
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
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.ConsoleSessionVO;
import com.cloud.vm.UserVmDetailVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.ConsoleSessionDao;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.cloudstack.api.command.user.consoleproxy.ConsoleEndpoint;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.security.keys.KeysManager;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ConsoleAccessManagerImpl extends ManagerBase implements ConsoleAccessManager {

    @Inject
    private AccountManager accountManager;
    @Inject
    private VirtualMachineManager virtualMachineManager;
    @Inject
    private ManagementServer managementServer;
    @Inject
    private EntityManager entityManager;
    @Inject
    private UserVmDetailsDao userVmDetailsDao;
    @Inject
    private KeysManager keysManager;
    @Inject
    private AgentManager agentManager;
    @Inject
    private ConsoleProxyManager consoleProxyManager;
    @Inject
    private ConsoleSessionDao consoleSessionDao;

    private static KeysManager secretKeysManager;
    private final Gson gson = new GsonBuilder().create();

    public static final Logger s_logger = Logger.getLogger(ConsoleAccessManagerImpl.class.getName());

    private static final List<VirtualMachine.State> unsupportedConsoleVMState = Arrays.asList(
            VirtualMachine.State.Stopped, VirtualMachine.State.Error, VirtualMachine.State.Destroyed
    );

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        ConsoleAccessManagerImpl.secretKeysManager = keysManager;
        return super.configure(name, params);
    }

    @Override
    public ConsoleEndpoint generateConsoleEndpoint(Long vmId, String extraSecurityToken, String clientAddress) {
        try {
            if (ObjectUtils.anyNull(accountManager, virtualMachineManager, managementServer)) {
                return new ConsoleEndpoint(false, null, "Console service is not ready");
            }

            if (keysManager.getHashKey() == null) {
                String msg = "Console access denied. Ticket service is not ready yet";
                s_logger.debug(msg);
                return new ConsoleEndpoint(false, null, msg);
            }

            Account account = CallContext.current().getCallingAccount();

            // Do a sanity check here to make sure the user hasn't already been deleted
            if (account == null) {
                s_logger.debug("Invalid user/account, reject console access");
                return new ConsoleEndpoint(false, null,"Access denied. Invalid or inconsistent account is found");
            }

            VirtualMachine vm = entityManager.findById(VirtualMachine.class, vmId);
            if (vm == null) {
                s_logger.info("Invalid console servlet command parameter: " + vmId);
                return new ConsoleEndpoint(false, null, "Cannot find VM with ID " + vmId);
            }

            if (!checkSessionPermission(vm, account)) {
                return new ConsoleEndpoint(false, null, "Permission denied");
            }

            String sessionUuid = UUID.randomUUID().toString();
            return generateAccessEndpoint(vmId, sessionUuid, extraSecurityToken, clientAddress);
        } catch (Exception e) {
            String errorMsg = String.format("Unexepected exception in ConsoleAccessManager - vmId: %s, clientAddress: %s",
                    vmId, clientAddress);
            s_logger.error(errorMsg, e);
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

    @Override
    public void removeSession(String sessionUuid) {
        consoleSessionDao.removeSession(sessionUuid);
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
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("VM access is denied for VM ID " + vm.getUuid() + ". VM owner account " +
                                    vm.getAccountId() + " does not match the account id in session " +
                                    account.getId() + " and caller is a normal user");
                        }
                    } else if ((accountManager.isDomainAdmin(account.getId())
                            || account.getType() == Account.Type.READ_ONLY_ADMIN) && s_logger.isDebugEnabled()) {
                        s_logger.debug("VM access is denied for VM ID " + vm.getUuid() + ". VM owner account " +
                                vm.getAccountId() + " does not match the account id in session " +
                                account.getId() + " and the domain-admin caller does not manage the target domain");
                    }
                    return false;
                }
                break;

            case DomainRouter:
            case ConsoleProxy:
            case SecondaryStorageVm:
                return false;

            default:
                s_logger.warn("Unrecoginized virtual machine type, deny access by default. type: " + vm.getType());
                return false;
        }

        return true;
    }

    private ConsoleEndpoint generateAccessEndpoint(Long vmId, String sessionUuid, String extraSecurityToken, String clientAddress) {
        VirtualMachine vm = virtualMachineManager.findById(vmId);
        String msg;
        if (vm == null) {
            msg = "VM " + vmId + " does not exist, sending blank response for console access request";
            s_logger.warn(msg);
            throw new CloudRuntimeException(msg);
        }

        String vmUuid = vm.getUuid();
        if (unsupportedConsoleVMState.contains(vm.getState())) {
            msg = "VM " + vmUuid + " must be running to connect console, sending blank response for console access request";
            s_logger.warn(msg);
            throw new CloudRuntimeException(msg);
        }

        Long hostId = vm.getState() != VirtualMachine.State.Migrating ? vm.getHostId() : vm.getLastHostId();
        if (hostId == null) {
            msg = "VM " + vmUuid + " lost host info, sending blank response for console access request";
            s_logger.warn(msg);
            throw new CloudRuntimeException(msg);
        }

        HostVO host = managementServer.getHostBy(hostId);
        if (host == null) {
            msg = "VM " + vmUuid + "'s host does not exist, sending blank response for console access request";
            s_logger.warn(msg);
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
        s_logger.debug("The console URL is: " + consoleEndpoint.getUrl());
        return consoleEndpoint;
    }

    private ConsoleEndpoint composeConsoleAccessEndpoint(String rootUrl, VirtualMachine vm, HostVO hostVo, String addr,
                                                         String sessionUuid, String extraSecurityToken) {
        String host = hostVo.getPrivateIpAddress();

        Pair<String, Integer> portInfo = null;
        if (hostVo.getHypervisorType() == Hypervisor.HypervisorType.KVM &&
                (hostVo.getResourceState().equals(ResourceState.ErrorInMaintenance) ||
                        hostVo.getResourceState().equals(ResourceState.ErrorInPrepareForMaintenance))) {
            UserVmDetailVO detailAddress = userVmDetailsDao.findDetail(vm.getId(), VmDetailConstants.KVM_VNC_ADDRESS);
            UserVmDetailVO detailPort = userVmDetailsDao.findDetail(vm.getId(), VmDetailConstants.KVM_VNC_PORT);
            if (detailAddress != null && detailPort != null) {
                portInfo = new Pair<>(detailAddress.getValue(), Integer.valueOf(detailPort.getValue()));
            } else {
                s_logger.warn("KVM Host in ErrorInMaintenance/ErrorInPrepareForMaintenance but " +
                        "no VNC Address/Port was available. Falling back to default one from MS.");
            }
        }

        if (portInfo == null) {
            portInfo = managementServer.getVncPort(vm);
        }

        if (s_logger.isDebugEnabled())
            s_logger.debug("Port info " + portInfo.first());

        Ternary<String, String, String> parsedHostInfo = parseHostInfo(portInfo.first());

        int port = -1;
        if (portInfo.second() == -9) {
            //for hyperv
            port = Integer.parseInt(managementServer.findDetail(hostVo.getId(), "rdp.server.port").getValue());
        } else {
            port = portInfo.second();
        }

        String sid = vm.getVncPassword();
        UserVmDetailVO details = userVmDetailsDao.findDetail(vm.getId(), VmDetailConstants.KEYBOARD);

        String tag = vm.getUuid();
        String displayName = vm.getHostName();
        if (vm instanceof UserVm) {
            displayName = ((UserVm) vm).getDisplayName();
        }

        String ticket = genAccessTicket(parsedHostInfo.first(), String.valueOf(port), sid, tag, sessionUuid);
        ConsoleProxyPasswordBasedEncryptor encryptor = new ConsoleProxyPasswordBasedEncryptor(getEncryptorPassword());
        ConsoleProxyClientParam param = generateConsoleProxyClientParam(parsedHostInfo, port, sid, tag, ticket,
                sessionUuid, addr, extraSecurityToken, vm, hostVo, details, portInfo, host, displayName);
        String token = encryptor.encryptObject(ConsoleProxyClientParam.class, param);
        int vncPort = consoleProxyManager.getVncPort();

        String url = generateConsoleAccessUrl(rootUrl, param, token, vncPort, vm);

        s_logger.debug("Adding allowed session: " + sessionUuid);
        persistConsoleSession(sessionUuid, vm.getId(), hostVo.getId());
        managementServer.setConsoleAccessForVm(vm.getId(), sessionUuid);

        ConsoleEndpoint consoleEndpoint = new ConsoleEndpoint(true, url);
        consoleEndpoint.setWebsocketHost(managementServer.getConsoleAccessAddress(vm.getId()));
        consoleEndpoint.setWebsocketPort(String.valueOf(vncPort));
        consoleEndpoint.setWebsocketPath("websockify");
        consoleEndpoint.setWebsocketToken(token);
        if (StringUtils.isNotBlank(param.getExtraSecurityToken())) {
            consoleEndpoint.setWebsocketExtra(param.getExtraSecurityToken());
        }
        return consoleEndpoint;
    }

    protected void persistConsoleSession(String sessionUuid, long instanceId, long hostId) {
        ConsoleSessionVO consoleSessionVo = new ConsoleSessionVO();
        consoleSessionVo.setUuid(sessionUuid);
        consoleSessionVo.setAccountId(CallContext.current().getCallingAccountId());
        consoleSessionVo.setUserId(CallContext.current().getCallingUserId());
        consoleSessionVo.setInstanceId(instanceId);
        consoleSessionVo.setHostId(hostId);
        consoleSessionDao.persist(consoleSessionVo);
    }

    private String generateConsoleAccessUrl(String rootUrl, ConsoleProxyClientParam param, String token, int vncPort,
                                            VirtualMachine vm) {
        StringBuilder sb = new StringBuilder(rootUrl);
        if (param.getHypervHost() != null || !ConsoleProxyManager.NoVncConsoleDefault.value()) {
            sb.append("/ajax?token=" + token);
        } else {
            sb.append("/resource/noVNC/vnc.html")
                    .append("?autoconnect=true")
                    .append("&port=" + vncPort)
                    .append("&token=" + token);
        }

        if (StringUtils.isNotBlank(param.getExtraSecurityToken())) {
            sb.append("&extra=" + param.getExtraSecurityToken());
        }

        // for console access, we need guest OS type to help implement keyboard
        long guestOs = vm.getGuestOSId();
        GuestOSVO guestOsVo = managementServer.getGuestOs(guestOs);
        if (guestOsVo.getCategoryId() == 6)
            sb.append("&guest=windows");

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Compose console url: " + sb);
        }
        return sb.toString().startsWith("https") ? sb.toString() : "http:" + sb;
    }

    private ConsoleProxyClientParam generateConsoleProxyClientParam(Ternary<String, String, String> parsedHostInfo,
                                                                    int port, String sid, String tag, String ticket,
                                                                    String sessionUuid, String addr,
                                                                    String extraSecurityToken, VirtualMachine vm,
                                                                    HostVO hostVo, UserVmDetailVO details,
                                                                    Pair<String, Integer> portInfo, String host,
                                                                    String displayName) {
        ConsoleProxyClientParam param = new ConsoleProxyClientParam();
        param.setClientHostAddress(parsedHostInfo.first());
        param.setClientHostPort(port);
        param.setClientHostPassword(sid);
        param.setClientTag(tag);
        param.setClientDisplayName(displayName);
        param.setTicket(ticket);
        param.setSessionUuid(sessionUuid);
        param.setSourceIP(addr);

        if (StringUtils.isNotBlank(extraSecurityToken)) {
            param.setExtraSecurityToken(extraSecurityToken);
            s_logger.debug("Added security token for client validation");
        }

        if (requiresVncOverWebSocketConnection(vm, hostVo)) {
            setWebsocketUrl(vm, param);
        }

        if (details != null) {
            param.setLocale(details.getValue());
        }

        if (portInfo.second() == -9) {
            //For Hyperv Clinet Host Address will send Instance id
            param.setHypervHost(host);
            param.setUsername(managementServer.findDetail(hostVo.getId(), "username").getValue());
            param.setPassword(managementServer.findDetail(hostVo.getId(), "password").getValue());
        }
        if (parsedHostInfo.second() != null  && parsedHostInfo.third() != null) {
            param.setClientTunnelUrl(parsedHostInfo.second());
            param.setClientTunnelSession(parsedHostInfo.third());
        }
        return param;
    }

    public static Ternary<String, String, String> parseHostInfo(String hostInfo) {
        String host = null;
        String tunnelUrl = null;
        String tunnelSession = null;

        s_logger.info("Parse host info returned from executing GetVNCPortCommand. host info: " + hostInfo);

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

    public static String genAccessTicket(String host, String port, String sid, String tag, String sessionUuid) {
        return genAccessTicket(host, port, sid, tag, new Date(), sessionUuid);
    }

    public static String genAccessTicket(String host, String port, String sid, String tag, Date normalizedHashTime, String sessionUuid) {
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
            s_logger.error("Unexpected exception ", e);
        }
        return "";
    }

    private String getEncryptorPassword() {
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
            s_logger.error("Could not obtain VNC ticket for VM " + vm.getInstanceName());
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
            s_logger.info("Acquiring VNC ticket for VM = " + vm.getHostName());
            GetVmVncTicketCommand cmd = new GetVmVncTicketCommand(vm.getInstanceName());
            Answer answer = agentManager.send(vm.getHostId(), cmd);
            GetVmVncTicketAnswer ans = (GetVmVncTicketAnswer) answer;
            if (!ans.getResult()) {
                s_logger.info("VNC ticket could not be acquired correctly: " + ans.getDetails());
            }
            return ans.getTicket();
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            s_logger.error("Error acquiring ticket", e);
            return null;
        }
    }

}
