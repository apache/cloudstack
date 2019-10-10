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
package com.cloud.servlet;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.cloud.consoleproxy.ConsoleProxyManager;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.server.ManagementServer;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.uservm.UserVm;
import com.cloud.utils.ConstantTimeComparator;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.UserVmDetailVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.UserVmDetailsDao;

/**
 * Console access : /novncconsole?cmd=access&vm=xxx
 */
@Component("noVncConsoleServlet")
public class NoVncConsoleProxyServlet extends HttpServlet {
    private static final long serialVersionUID = -5515382620323808169L;
    public static final Logger s_logger = Logger.getLogger(NoVncConsoleProxyServlet.class.getName());
    private final static List<String> s_clientAddressHeaders = Collections
            .unmodifiableList(Arrays.asList("X-Forwarded-For",
                    "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR", "Remote_Addr"));

    @Inject
    AccountManager _accountMgr;
    @Inject
    VirtualMachineManager _vmMgr;
    @Inject
    ManagementServer _ms;
    @Inject
    EntityManager _entityMgr;
    @Inject
    UserVmDetailsDao _userVmDetailsDao;

    private final Gson _gson = new GsonBuilder().create();

    public NoVncConsoleProxyServlet() {
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, config.getServletContext());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        doGet(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {

        try {
            if (_accountMgr == null || _vmMgr == null || _ms == null) {
                sendResponse(resp, "Service is not ready");
                return;
            }

            if (ConsoleProxyManager.NoVncEncryptionKey.value() == null || ConsoleProxyManager.NoVncEncryptionIV.value() == null) {
                s_logger.debug("novnc Console access denied. novnc.security.key or novnc.security.iv is not set. Ticket service is not ready yet");
                sendResponse(resp, "Service is not ready");
                return;
            }

            String userId = null;
            String account = null;
            Account accountObj = null;

            Map<String, Object[]> params = new HashMap<String, Object[]>();
            params.putAll(req.getParameterMap());

            HttpSession session = req.getSession(false);
            if (session == null) {
                if (verifyRequest(params)) {
                    userId = (String)params.get("userid")[0];
                    account = (String)params.get("account")[0];
                    accountObj = (Account)params.get("accountobj")[0];
                } else {
                    s_logger.debug("Invalid web session or API key in request, reject novnc console access");
                    sendResponse(resp, "Access denied. Invalid web session or API key in request");
                    return;
                }
            } else {
                // adjust to latest API refactoring changes
                if (session.getAttribute("userid") != null) {
                    userId = ((Long)session.getAttribute("userid")).toString();
                }

                accountObj = (Account)session.getAttribute("accountobj");
                if (accountObj != null) {
                    account = "" + accountObj.getId();
                }
            }

            // Do a sanity check here to make sure the user hasn't already been deleted
            if ((userId == null) || (account == null) || (accountObj == null) || !verifyUser(Long.valueOf(userId))) {
                s_logger.debug("Invalid user/account, reject novnc console access");
                sendResponse(resp, "Access denied. Invalid or inconsistent account is found");
                return;
            }

            String cmd = req.getParameter("cmd");
            if (cmd == null || (!cmd.equalsIgnoreCase("access") && !cmd.equalsIgnoreCase("auth"))) {
                s_logger.debug("invalid console servlet command: " + cmd);
                sendResponse(resp, "");
                return;
            }

            String vmIdString = req.getParameter("vm");
            VirtualMachine vm = _entityMgr.findByUuid(VirtualMachine.class, vmIdString);
            if (vm == null) {
                s_logger.info("invalid console servlet command parameter: " + vmIdString);
                sendResponse(resp, "");
                return;
            }

            Long vmId = vm.getId();

            if (!checkSessionPermision(req, vmId, accountObj)) {
                sendResponse(resp, "Permission denied");
                return;
            }

            if (cmd.equalsIgnoreCase("access")) {
                handleAccessRequest(req, resp, vmId);
            } else {
                handleAuthRequest(req, resp, vmId);
            }
        } catch (Throwable e) {
            s_logger.error("Unexepected exception in NoVncConsoleProxyServlet", e);
            sendResponse(resp, "Server Internal Error");
        }
    }

    private void handleAccessRequest(HttpServletRequest req, HttpServletResponse resp, long vmId) {
        VirtualMachine vm = _vmMgr.findById(vmId);
        if (vm == null) {
            s_logger.warn("VM " + vmId + " does not exist, sending blank response for console access request");
            sendResponse(resp, "");
            return;
        }

        if (vm.getHostId() == null) {
            s_logger.warn("VM " + vmId + " lost host info, sending blank response for console access request");
            sendResponse(resp, "");
            return;
        }

        HostVO host = _ms.getHostBy(vm.getHostId());
        if (host == null) {
            s_logger.warn("VM " + vmId + "'s host does not exist, sending blank response for console access request");
            sendResponse(resp, "");
            return;
        }

        if (Hypervisor.HypervisorType.LXC.equals(vm.getHypervisorType())){
            sendResponse(resp, "<html><body><p>Console access is not supported for LXC</p></body></html>");
            return;
        }

        String rootUrl = _ms.getConsoleAccessUrlRoot(vmId);
        if (rootUrl == null) {
            sendResponse(resp, "<html><body><p>Console access will be ready in a few minutes. Please try it again later.</p></body></html>");
            return;
        }

        String vmName = vm.getHostName();
        if (vm.getType() == VirtualMachine.Type.User) {
            UserVm userVm = _entityMgr.findById(UserVm.class, vmId);
            String displayName = userVm.getDisplayName();
            if (displayName != null && !displayName.isEmpty() && !displayName.equals(vmName)) {
                vmName += "(" + displayName + ")";
            }
        }

        StringBuffer sb = new StringBuffer();
        sb.append("<html><link rel=\"shortcut icon\" href=\"images/cloud.ico\" /><title>").append(ConsoleProxyServlet.escapeHTML(vmName)).append("</title><frameset><frame src=\"").append(composeNoVncConsoleAccessUrl(rootUrl, vm, host, req));
        sb.append("\"></frame></frameset></html>");
        s_logger.debug("the console url is :: " + sb.toString());
        sendResponse(resp, sb.toString());
    }

    private void handleAuthRequest(HttpServletRequest req, HttpServletResponse resp, long vmId) {

        // TODO authentication channel between console proxy VM and management server needs to be secured,
        // the data is now being sent through private network, but this is apparently not enough
        VirtualMachine vm = _vmMgr.findById(vmId);
        if (vm == null) {
            s_logger.warn("VM " + vmId + " does not exist, sending failed response for authentication request from console proxy");
            sendResponse(resp, "failed");
            return;
        }

        if (vm.getHostId() == null) {
            s_logger.warn("VM " + vmId + " lost host info, failed response for authentication request from console proxy");
            sendResponse(resp, "failed");
            return;
        }

        HostVO host = _ms.getHostBy(vm.getHostId());
        if (host == null) {
            s_logger.warn("VM " + vmId + "'s host does not exist, sending failed response for authentication request from console proxy");
            sendResponse(resp, "failed");
            return;
        }

        String sid = req.getParameter("sid");
        if (sid == null || !sid.equals(vm.getVncPassword())) {
            s_logger.warn("sid " + sid + " in url does not match stored sid.");
            sendResponse(resp, "failed");
            return;
        }

        sendResponse(resp, "success");
    }

    private String composeNoVncConsoleAccessUrl(String rootUrl, VirtualMachine vm, HostVO hostVo, HttpServletRequest req) {
        StringBuffer sb = new StringBuffer(rootUrl);
        String host = hostVo.getPrivateIpAddress();

        Pair<String, Integer> portInfo = _ms.getVncPort(vm);
        if (s_logger.isDebugEnabled())
            s_logger.debug("Port info " + portInfo.first());

        Ternary<String, String, String> parsedHostInfo = ConsoleProxyServlet.parseHostInfo(portInfo.first());

        int port = -1;
        if (portInfo.second() == -9) {
            //for hyperv
            port = Integer.parseInt(_ms.findDetail(hostVo.getId(), "rdp.server.port").getValue());
        } else {
            port = portInfo.second();
        }

        String sid = vm.getVncPassword();
        UserVmDetailVO details = _userVmDetailsDao.findDetail(vm.getId(), "keyboard");

        String tag = vm.getUuid();

        String clientIp = getClientAddress(req);

        String ticket = ConsoleProxyServlet.genAccessTicket(parsedHostInfo.first(), String.valueOf(port), sid, tag);
        NoVncConsoleProxyEncryptor encryptor = new NoVncConsoleProxyEncryptor(ConsoleProxyManager.NoVncEncryptionKey.value(), ConsoleProxyManager.NoVncEncryptionIV.value());
        NoVncConsoleProxyClientParam param = new NoVncConsoleProxyClientParam();
        param.setProxy(rootUrl.substring(rootUrl.lastIndexOf("/") + 1, rootUrl.length()));
        param.setClientHostAddress(parsedHostInfo.first());
        param.setClientHostPort(port);
        param.setClientHostPassword(sid);
        param.setClientIp(clientIp);
        param.setClientTag(tag);
        param.setTicket(ticket);

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

        String vmName = vm.getHostName();
        if (vm.getType() == VirtualMachine.Type.User) {
            UserVm userVm = _entityMgr.findById(UserVm.class, vm.getId());
            String displayName = userVm.getDisplayName();
            if (displayName != null && !displayName.isEmpty() && !displayName.equals(vmName)) {
                vmName += "(" + displayName + ")";
            }
        }
        param.setVmName(vmName);

        sb.append(":" + ConsoleProxyManager.DEFAULT_NOVNC_PORT + "/vnc_lite.html?" + encryptor.encrypt(param));

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Compose novnc console url: " + sb.toString());
        }
        return sb.toString();
    }

    private void sendResponse(HttpServletResponse resp, String content) {
        try {
            resp.setContentType("text/html");
            resp.getWriter().print(content);
        } catch (IOException e) {
            s_logger.info("Client may already close the connection", e);
        }
    }

    // This is copied from ApiServlet
    static String getClientAddress(final HttpServletRequest request) {
        for(final String header : s_clientAddressHeaders) {
            final String ip = getCorrectIPAddress(request.getHeader(header));
            if (ip != null) {
                return ip;
            }
        }

        return request.getRemoteAddr();
    }
    // This is copied from ApiServlet
    private static String getCorrectIPAddress(String ip) {
        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            return null;
        }
        if(NetUtils.isValidIp4(ip) || NetUtils.isValidIp6(ip)) {
            return ip;
        }
        String[] ips = ip.split(",");
        for(String i : ips) {
            if(NetUtils.isValidIp4(i.trim()) || NetUtils.isValidIp6(i.trim())) {
                return i.trim();
            }
        }
        return null;
    }

    // This is copied from ConsoleProxyServlet
    private boolean checkSessionPermision(HttpServletRequest req, long vmId, Account accountObj) {

        VirtualMachine vm = _vmMgr.findById(vmId);
        if (vm == null) {
            s_logger.debug("Console/thumbnail access denied. VM " + vmId + " does not exist in system any more");
            return false;
        }

        if (_accountMgr.isRootAdmin(accountObj.getId()))
            return true;

        switch (vm.getType()) {
            case User:
            try {
                _accountMgr.checkAccess(accountObj, null, true, vm);
            } catch (PermissionDeniedException ex) {
                if (_accountMgr.isNormalUser(accountObj.getId())) {
                    if (s_logger.isDebugEnabled()) {
                            s_logger.debug("VM access is denied. VM owner account " + vm.getAccountId() + " does not match the account id in session " +
                                accountObj.getId() + " and caller is a normal user");
                    }
                } else if (_accountMgr.isDomainAdmin(accountObj.getId())
                        || accountObj.getType() == Account.ACCOUNT_TYPE_READ_ONLY_ADMIN) {
                    if(s_logger.isDebugEnabled()) {
                        s_logger.debug("VM access is denied. VM owner account " + vm.getAccountId()
                                + " does not match the account id in session " + accountObj.getId() + " and the domain-admin caller does not manage the target domain");
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

    // This is copied from ConsoleProxyServlet
    public boolean verifyUser(Long userId) {
        User user = _accountMgr.getUserIncludingRemoved(userId);
        Account account = null;
        if (user != null) {
            account = _accountMgr.getAccount(user.getAccountId());
        }

        if ((user == null) || (user.getRemoved() != null) || !user.getState().equals(Account.State.enabled) || (account == null) ||
            !account.getState().equals(Account.State.enabled)) {
            s_logger.warn("Deleted/Disabled/Locked user with id=" + userId + " attempting to access public API");
            return false;
        }
        return true;
    }

    // This is copied from ConsoleProxyServlet
    private boolean verifyRequest(Map<String, Object[]> requestParameters) {
        try {
            String apiKey = null;
            String secretKey = null;
            String signature = null;
            String unsignedRequest = null;

            List<String> parameterNames = new ArrayList<String>();

            for (Object paramNameObj : requestParameters.keySet()) {
                parameterNames.add((String)paramNameObj); // put the name in a list that we'll sort later
            }

            Collections.sort(parameterNames);

            for (String paramName : parameterNames) {
                String paramValue = ((String[])requestParameters.get(paramName))[0];

                if ("signature".equalsIgnoreCase(paramName)) {
                    signature = paramValue;
                } else {
                    if ("apikey".equalsIgnoreCase(paramName)) {
                        apiKey = paramValue;
                    }

                    if (unsignedRequest == null) {
                        unsignedRequest = paramName + "=" + URLEncoder.encode(paramValue, "UTF-8").replaceAll("\\+", "%20");
                    } else {
                        unsignedRequest = unsignedRequest + "&" + paramName + "=" + URLEncoder.encode(paramValue, "UTF-8").replaceAll("\\+", "%20");
                    }
                }
            }

            if ((signature == null) || (apiKey == null)) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("expired session, missing signature, or missing apiKey -- ignoring request...sig: " + signature + ", apiKey: " + apiKey);
                }
                return false; // no signature, bad request
            }

            TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
            User user = null;
            Pair<User, Account> userAcctPair = _accountMgr.findUserByApiKey(apiKey);
            if (userAcctPair == null) {
                s_logger.debug("apiKey does not map to a valid user -- ignoring request, apiKey: " + apiKey);
                return false;
            }

            user = userAcctPair.first();
            Account account = userAcctPair.second();

            if (!user.getState().equals(Account.State.enabled) || !account.getState().equals(Account.State.enabled)) {
                s_logger.debug("disabled or locked user accessing the api, userid = " + user.getId() + "; name = " + user.getUsername() + "; state: " + user.getState() +
                    "; accountState: " + account.getState());
                return false;
            }

            secretKey = user.getSecretKey();
            if (secretKey == null) {
                s_logger.debug("User does not have a secret key associated with the account -- ignoring request, username: " + user.getUsername());
                return false;
            }

            unsignedRequest = unsignedRequest.toLowerCase();

            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA1");
            mac.init(keySpec);
            mac.update(unsignedRequest.getBytes());
            byte[] encryptedBytes = mac.doFinal();
            String computedSignature = Base64.encodeBase64String(encryptedBytes);
            boolean equalSig = ConstantTimeComparator.compareStrings(signature, computedSignature);
            if (!equalSig) {
                s_logger.debug("User signature: " + signature + " is not equaled to computed signature: " + computedSignature);
            }

            if (equalSig) {
                requestParameters.put("userid", new Object[] {String.valueOf(user.getId())});
                requestParameters.put("account", new Object[] {account.getAccountName()});
                requestParameters.put("accountobj", new Object[] {account});
            }
            return equalSig;
        } catch (Exception ex) {
            s_logger.error("unable to verifty request signature", ex);
        }
        return false;
    }


}
