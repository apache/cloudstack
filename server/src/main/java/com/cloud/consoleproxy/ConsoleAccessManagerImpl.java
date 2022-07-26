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

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetVmVncTicketAnswer;
import com.cloud.agent.api.GetVmVncTicketCommand;
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
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmDetailVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.cloudstack.api.command.user.consoleproxy.ConsoleEndpoint;
import org.apache.cloudstack.consoleproxy.ConsoleAccessManager;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.security.keys.KeysManager;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ConsoleAccessManagerImpl extends ManagerBase implements ConsoleAccessManager {

    @Inject
    private AccountManager _accountMgr;
    @Inject
    private VirtualMachineManager _vmMgr;
    @Inject
    private ManagementServer _ms;
    @Inject
    private EntityManager _entityMgr;
    @Inject
    private UserVmDetailsDao _userVmDetailsDao;
    @Inject
    private KeysManager _keysMgr;
    @Inject
    private AgentManager agentManager;

    private static KeysManager s_keysMgr;
    private final Gson _gson = new GsonBuilder().create();

    public static final Logger s_logger = Logger.getLogger(ConsoleAccessManagerImpl.class.getName());

    private static Set<String> allowedSessions;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        s_keysMgr = _keysMgr;
        allowedSessions = new HashSet<>();
        return super.configure(name, params);
    }

    @Override
    public ConsoleEndpoint generateConsoleEndpoint(Long vmId, String clientSecurityToken, String clientAddress) {
        try {
            if (_accountMgr == null || _vmMgr == null || _ms == null) {
                return new ConsoleEndpoint(false, null,"Console service is not ready");
            }

            if (_keysMgr.getHashKey() == null) {
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

            VirtualMachine vm = _entityMgr.findById(VirtualMachine.class, vmId);
            if (vm == null) {
                s_logger.info("Invalid console servlet command parameter: " + vmId);
                return new ConsoleEndpoint(false, null, "Cannot find VM with ID " + vmId);
            }

            if (!checkSessionPermision(vm, account)) {
                return new ConsoleEndpoint(false, null, "Permission denied");
            }

            String sessionToken = UUID.randomUUID().toString();
            return generateAccessEndpoint(vmId, sessionToken, clientSecurityToken, clientAddress);
        } catch (Throwable e) {
            s_logger.error("Unexepected exception in ConsoleProxyServlet", e);
            return new ConsoleEndpoint(false, null, "Server Internal Error: " + e.getMessage());
        }
    }

    @Override
    public boolean isSessionAllowed(String sessionUuid) {
        return allowedSessions.contains(sessionUuid);
    }

    @Override
    public void removeSessions(String[] sessionUuids) {
        for (String r : sessionUuids) {
            allowedSessions.remove(r);
        }
    }

    private boolean checkSessionPermision(VirtualMachine vm, Account account) {
        if (_accountMgr.isRootAdmin(account.getId())) {
            return true;
        }

        switch (vm.getType()) {
            case User:
                try {
                    _accountMgr.checkAccess(account, null, true, vm);
                } catch (PermissionDeniedException ex) {
                    if (_accountMgr.isNormalUser(account.getId())) {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("VM access is denied. VM owner account " + vm.getAccountId() + " does not match the account id in session " +
                                    account.getId() + " and caller is a normal user");
                        }
                    } else if (_accountMgr.isDomainAdmin(account.getId())
                            || account.getType() == Account.Type.READ_ONLY_ADMIN) {
                        if(s_logger.isDebugEnabled()) {
                            s_logger.debug("VM access is denied. VM owner account " + vm.getAccountId()
                                    + " does not match the account id in session " + account.getId() + " and the domain-admin caller does not manage the target domain");
                        }
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

    private ConsoleEndpoint generateAccessEndpoint(Long vmId, String sessionToken, String clientSecurityToken, String clientAddress) {
        VirtualMachine vm = _vmMgr.findById(vmId);
        String msg;
        if (vm == null) {
            msg = "VM " + vmId + " does not exist, sending blank response for console access request";
            s_logger.warn(msg);
            throw new CloudRuntimeException(msg);
        }

        if (vm.getHostId() == null) {
            msg = "VM " + vmId + " lost host info, sending blank response for console access request";
            s_logger.warn(msg);
            throw new CloudRuntimeException(msg);
        }

        HostVO host = _ms.getHostBy(vm.getHostId());
        if (host == null) {
            msg = "VM " + vmId + "'s host does not exist, sending blank response for console access request";
            s_logger.warn(msg);
            throw new CloudRuntimeException(msg);
        }

        if (Hypervisor.HypervisorType.LXC.equals(vm.getHypervisorType())) {
            throw new CloudRuntimeException("Console access is not supported for LXC");
        }

        String rootUrl = _ms.getConsoleAccessUrlRoot(vmId);
        if (rootUrl == null) {
            throw new CloudRuntimeException("Console access will be ready in a few minutes. Please try it again later.");
        }

        ConsoleEndpoint consoleEndpoint = composeConsoleAccessEndpoint(rootUrl, vm, host, clientAddress, sessionToken, clientSecurityToken);
        s_logger.debug("The console URL is: " + consoleEndpoint.getUrl());
        return consoleEndpoint;
    }

    private ConsoleEndpoint composeConsoleAccessEndpoint(String rootUrl, VirtualMachine vm, HostVO hostVo, String addr,
                                                         String sessionUuid, String clientSecurityToken) {
        StringBuffer sb = new StringBuffer(rootUrl);
        String host = hostVo.getPrivateIpAddress();

        Pair<String, Integer> portInfo = null;
        if (hostVo.getHypervisorType() == Hypervisor.HypervisorType.KVM &&
                (hostVo.getResourceState().equals(ResourceState.ErrorInMaintenance) ||
                        hostVo.getResourceState().equals(ResourceState.ErrorInPrepareForMaintenance))) {
            UserVmDetailVO detailAddress = _userVmDetailsDao.findDetail(vm.getId(), VmDetailConstants.KVM_VNC_ADDRESS);
            UserVmDetailVO detailPort = _userVmDetailsDao.findDetail(vm.getId(), VmDetailConstants.KVM_VNC_PORT);
            if (detailAddress != null && detailPort != null) {
                portInfo = new Pair<>(detailAddress.getValue(), Integer.valueOf(detailPort.getValue()));
            } else {
                s_logger.warn("KVM Host in ErrorInMaintenance/ErrorInPrepareForMaintenance but " +
                        "no VNC Address/Port was available. Falling back to default one from MS.");
            }
        }

        if (portInfo == null) {
            portInfo = _ms.getVncPort(vm);
        }

        if (s_logger.isDebugEnabled())
            s_logger.debug("Port info " + portInfo.first());

        Ternary<String, String, String> parsedHostInfo = parseHostInfo(portInfo.first());

        int port = -1;
        if (portInfo.second() == -9) {
            //for hyperv
            port = Integer.parseInt(_ms.findDetail(hostVo.getId(), "rdp.server.port").getValue());
        } else {
            port = portInfo.second();
        }

        String sid = vm.getVncPassword();
        UserVmDetailVO details = _userVmDetailsDao.findDetail(vm.getId(), VmDetailConstants.KEYBOARD);

        String tag = vm.getUuid();

        String ticket = genAccessTicket(parsedHostInfo.first(), String.valueOf(port), sid, tag, sessionUuid);
        ConsoleProxyPasswordBasedEncryptor encryptor = new ConsoleProxyPasswordBasedEncryptor(getEncryptorPassword());
        ConsoleProxyClientParam param = new ConsoleProxyClientParam();
        param.setClientHostAddress(parsedHostInfo.first());
        param.setClientHostPort(port);
        param.setClientHostPassword(sid);
        param.setClientTag(tag);
        param.setTicket(ticket);
        param.setSessionUuid(sessionUuid);
        param.setSourceIP(addr);

        if (StringUtils.isNotBlank(clientSecurityToken)) {
            param.setClientSecurityHeader(ConsoleAccessManager.ConsoleProxyExtraSecurityHeaderName.value());
            param.setClientSecurityToken(clientSecurityToken);
            s_logger.debug("Added security token " + clientSecurityToken + " for header " + ConsoleAccessManager.ConsoleProxyExtraSecurityHeaderName.value());
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
            param.setUsername(_ms.findDetail(hostVo.getId(), "username").getValue());
            param.setPassword(_ms.findDetail(hostVo.getId(), "password").getValue());
        }
        if (parsedHostInfo.second() != null  && parsedHostInfo.third() != null) {
            param.setClientTunnelUrl(parsedHostInfo.second());
            param.setClientTunnelSession(parsedHostInfo.third());
        }

        String token = encryptor.encryptObject(ConsoleProxyClientParam.class, param);
        if (param.getHypervHost() != null || !ConsoleProxyManager.NoVncConsoleDefault.value()) {
            sb.append("/ajax?token=" + token);
        } else {
            sb.append("/resource/noVNC/vnc.html")
                    .append("?autoconnect=true")
                    .append("&port=" + ConsoleProxyManager.NoVncConsolePort.value())
                    .append("&token=" + token);
        }

        // for console access, we need guest OS type to help implement keyboard
        long guestOs = vm.getGuestOSId();
        GuestOSVO guestOsVo = _ms.getGuestOs(guestOs);
        if (guestOsVo.getCategoryId() == 6)
            sb.append("&guest=windows");

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Compose console url: " + sb);
        }
        s_logger.debug("Adding allowed session: " + sessionUuid);
        allowedSessions.add(sessionUuid);
        String url = !sb.toString().startsWith("http") ? ConsoleAccessManager.ConsoleProxySchema.value() + ":" + sb : sb.toString();
        return new ConsoleEndpoint(true, url);
    }

    static public Ternary<String, String, String> parseHostInfo(String hostInfo) {
        String host = null;
        String tunnelUrl = null;
        String tunnelSession = null;

        s_logger.info("Parse host info returned from executing GetVNCPortCommand. host info: " + hostInfo);

        if (hostInfo != null) {
            if (hostInfo.startsWith("consoleurl")) {
                String tokens[] = hostInfo.split("&");

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

        return new Ternary<String, String, String>(host, tunnelUrl, tunnelSession);
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
            Mac mac = Mac.getInstance("HmacSHA1");

            long ts = normalizedHashTime.getTime();
            ts = ts / 60000;        // round up to 1 minute
            String secretKey = s_keysMgr.getHashKey();

            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA1");
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
        String key = _keysMgr.getEncryptionKey();
        String iv = _keysMgr.getEncryptionIV();

        ConsoleProxyPasswordBasedEncryptor.KeyIVPair keyIvPair = new ConsoleProxyPasswordBasedEncryptor.KeyIVPair(key, iv);
        return _gson.toJson(keyIvPair);
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

    @Override
    public String getConfigComponentName() {
        return ConsoleAccessManagerImpl.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[] { ConsoleProxySchema, ConsoleProxyExtraSecurityHeaderName,
                ConsoleProxyExtraSecurityHeaderEnabled };
    }
}
