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
import java.util.Collections;
import java.util.Date;
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

import org.apache.cloudstack.framework.security.keys.KeysManager;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.cloud.exception.PermissionDeniedException;
import com.cloud.host.HostVO;
import com.cloud.server.ManagementServer;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.utils.ConstantTimeComparator;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;

/**
 * Thumbnail access : /console?cmd=thumbnail&vm=xxx&w=xxx&h=xxx
 * Console access : /conosole?cmd=access&vm=xxx
 * Authentication : /console?cmd=auth&vm=xxx&sid=xxx
 */
@Component("consoleServlet")
public class ConsoleProxyServlet extends HttpServlet {
    private static final long serialVersionUID = -5515382620323808168L;
    public static final Logger s_logger = Logger.getLogger(ConsoleProxyServlet.class.getName());
    private static final int DEFAULT_THUMBNAIL_WIDTH = 144;
    private static final int DEFAULT_THUMBNAIL_HEIGHT = 110;

    private static final String SANITIZATION_REGEX = "[\n\r]";

    @Inject
    AccountManager _accountMgr;
    @Inject
    VirtualMachineManager _vmMgr;
    @Inject
    ManagementServer _ms;
    @Inject
    EntityManager _entityMgr;
    @Inject
    KeysManager _keysMgr;

    static KeysManager s_keysMgr;

    private final Gson _gson = new GsonBuilder().create();

    public ConsoleProxyServlet() {
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, config.getServletContext());
        s_keysMgr = _keysMgr;
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

            if (_keysMgr.getHashKey() == null) {
                s_logger.debug("Console/thumbnail access denied. Ticket service is not ready yet");
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
                    s_logger.debug("Invalid web session or API key in request, reject console/thumbnail access");
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
                s_logger.debug("Invalid user/account, reject console/thumbnail access");
                sendResponse(resp, "Access denied. Invalid or inconsistent account is found");
                return;
            }

            String cmd = req.getParameter("cmd");
            if (cmd == null || !isValidCmd(cmd)) {
                if (cmd != null) {
                    cmd = cmd.replaceAll(SANITIZATION_REGEX, "_");
                    s_logger.debug(String.format("invalid console servlet command [%s].", cmd));
                } else {
                    s_logger.debug("Null console servlet command.");
                }

                sendResponse(resp, "");
                return;
            }

            String vmIdString = req.getParameter("vm");
            VirtualMachine vm = _entityMgr.findByUuid(VirtualMachine.class, vmIdString);
            if (vm == null) {
                if (vmIdString != null) {
                    vmIdString = vmIdString.replaceAll(SANITIZATION_REGEX, "_");
                    s_logger.info(String.format("invalid console servlet command vm parameter[%s].", vmIdString));
                } else {
                    s_logger.info("Null console servlet command VM parameter.");
                }

                sendResponse(resp, "");
                return;
            }

            Long vmId = vm.getId();

            if (!checkSessionPermision(req, vmId, accountObj)) {
                sendResponse(resp, "Permission denied");
                return;
            }

            if (cmd.equalsIgnoreCase("thumbnail")) {
                handleThumbnailRequest(req, resp, vmId);
            } else {
                handleAuthRequest(req, resp, vmId);
            }
        } catch (Exception e) {
            s_logger.error("Unexepected exception in ConsoleProxyServlet", e);
            sendResponse(resp, "Server Internal Error");
        }
    }

    private void handleThumbnailRequest(HttpServletRequest req, HttpServletResponse resp, long vmId) {
        VirtualMachine vm = _vmMgr.findById(vmId);
        if (vm == null) {
            s_logger.warn("VM " + vmId + " does not exist, sending blank response for thumbnail request");
            sendResponse(resp, "");
            return;
        }

        if (vm.getHostId() == null) {
            s_logger.warn("VM " + vmId + " lost host info, sending blank response for thumbnail request");
            sendResponse(resp, "");
            return;
        }

        HostVO host = _ms.getHostBy(vm.getHostId());
        if (host == null) {
            s_logger.warn("VM " + vmId + "'s host does not exist, sending blank response for thumbnail request");
            sendResponse(resp, "");
            return;
        }

        String rootUrl = _ms.getConsoleAccessUrlRoot(vmId);
        if (rootUrl == null) {
            sendResponse(resp, "");
            return;
        }

        int w = DEFAULT_THUMBNAIL_WIDTH;
        int h = DEFAULT_THUMBNAIL_HEIGHT;

        String value = req.getParameter("w");
        try {
            w = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            s_logger.info("[ignored] not a number: " + value);
        }

        value = req.getParameter("h");
        try {
            h = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            s_logger.info("[ignored] not a number: " + value);
        }

        try {
            resp.sendRedirect(composeThumbnailUrl(rootUrl, vm, host, w, h));
        } catch (IOException e) {
            s_logger.info("Client may already close the connection", e);
        }
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
            if(sid != null) {
                sid = sid.replaceAll(SANITIZATION_REGEX, "_");
                s_logger.warn(String.format("sid [%s] in url does not match stored sid.", sid));
            } else {
                s_logger.warn("Null sid in URL.");
            }

            sendResponse(resp, "failed");
            return;
        }

        sendResponse(resp, "success");
    }

    // put the ugly stuff here
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

    private String getEncryptorPassword() {
        String key = _keysMgr.getEncryptionKey();
        String iv = _keysMgr.getEncryptionIV();

        ConsoleProxyPasswordBasedEncryptor.KeyIVPair keyIvPair = new ConsoleProxyPasswordBasedEncryptor.KeyIVPair(key, iv);
        return _gson.toJson(keyIvPair);
    }

    private String composeThumbnailUrl(String rootUrl, VirtualMachine vm, HostVO hostVo, int w, int h) {
        StringBuffer sb = new StringBuffer(rootUrl);

        String host = hostVo.getPrivateIpAddress();

        Pair<String, Integer> portInfo = _ms.getVncPort(vm);
        Ternary<String, String, String> parsedHostInfo = parseHostInfo(portInfo.first());

        String sid = vm.getVncPassword();
        String tag = vm.getUuid();

        int port = -1;
        if (portInfo.second() == -9) {
            //for hyperv
            port = Integer.parseInt(_ms.findDetail(hostVo.getId(), "rdp.server.port").getValue());
        } else {
            port = portInfo.second();
        }

        String ticket = genAccessTicket(parsedHostInfo.first(), String.valueOf(port), sid, tag);

        ConsoleProxyPasswordBasedEncryptor encryptor = new ConsoleProxyPasswordBasedEncryptor(getEncryptorPassword());
        ConsoleProxyClientParam param = new ConsoleProxyClientParam();
        param.setClientHostAddress(parsedHostInfo.first());
        param.setClientHostPort(portInfo.second());
        param.setClientHostPassword(sid);
        param.setClientTag(tag);
        param.setTicket(ticket);
        if (portInfo.second() == -9) {
            //For Hyperv Clinet Host Address will send Instance id
            param.setHypervHost(host);
            param.setUsername(_ms.findDetail(hostVo.getId(), "username").getValue());
            param.setPassword(_ms.findDetail(hostVo.getId(), "password").getValue());
        }
        if (parsedHostInfo.second() != null && parsedHostInfo.third() != null) {
            param.setClientTunnelUrl(parsedHostInfo.second());
            param.setClientTunnelSession(parsedHostInfo.third());
        }

        sb.append("/ajaximg?token=" + encryptor.encryptObject(ConsoleProxyClientParam.class, param));
        sb.append("&w=").append(w).append("&h=").append(h).append("&key=0");

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Compose thumbnail url: " + sb.toString());
        }
        return sb.toString();
    }

    public static String genAccessTicket(String host, String port, String sid, String tag) {
        return genAccessTicket(host, port, sid, tag, new Date());
    }

    public static String genAccessTicket(String host, String port, String sid, String tag, Date normalizedHashTime) {
        String params = "host=" + host + "&port=" + port + "&sid=" + sid + "&tag=" + tag;

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

    private void sendResponse(HttpServletResponse resp, String content) {
        try {
            resp.setContentType("text/html");
            resp.getWriter().print(content);
        } catch (IOException e) {
            s_logger.info("Client may already close the connection", e);
        }
    }

    private boolean checkSessionPermision(HttpServletRequest req, long vmId, Account accountObj) {

        VirtualMachine vm = _vmMgr.findById(vmId);
        if (vm == null) {
            s_logger.debug("Console/thumbnail access denied. VM " + vmId + " does not exist in system any more");
            return false;
        }

        // root admin can access anything
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
                        || accountObj.getType() == Account.Type.READ_ONLY_ADMIN) {
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

    private boolean isValidCmd(String cmd) {
        if (cmd.equalsIgnoreCase("thumbnail") || cmd.equalsIgnoreCase("access") || cmd.equalsIgnoreCase("auth")) {
            return true;
        }

        return false;
    }

    public boolean verifyUser(Long userId) {
        // copy from ApiServer.java, a bit ugly here
        User user = _accountMgr.getUserIncludingRemoved(userId);
        Account account = null;
        if (user != null) {
            account = _accountMgr.getAccount(user.getAccountId());
        }

        if ((user == null) || (user.getRemoved() != null) || !user.getState().equals(Account.State.ENABLED) || (account == null) ||
            !account.getState().equals(Account.State.ENABLED)) {
            s_logger.warn("Deleted/Disabled/Locked user with id=" + userId + " attempting to access public API");
            return false;
        }
        return true;
    }

    // copied and modified from ApiServer.java.
    // TODO need to replace the whole servlet with a API command
    private boolean verifyRequest(Map<String, Object[]> requestParameters) {
        try {
            String apiKey = null;
            String secretKey = null;
            String signature = null;
            String unsignedRequest = null;

            // - build a request string with sorted params, make sure it's all lowercase
            // - sign the request, verify the signature is the same
            List<String> parameterNames = new ArrayList<String>();

            for (Object paramNameObj : requestParameters.keySet()) {
                parameterNames.add((String)paramNameObj); // put the name in a list that we'll sort later
            }

            Collections.sort(parameterNames);

            for (String paramName : parameterNames) {
                // parameters come as name/value pairs in the form String/String[]
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

            // if api/secret key are passed to the parameters
            if ((signature == null) || (apiKey == null)) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("expired session, missing signature, or missing apiKey -- ignoring request...sig: " + signature + ", apiKey: " + apiKey);
                }
                return false; // no signature, bad request
            }

            TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
            User user = null;
            // verify there is a user with this api key
            Pair<User, Account> userAcctPair = _accountMgr.findUserByApiKey(apiKey);
            if (userAcctPair == null) {
                s_logger.debug("apiKey does not map to a valid user -- ignoring request, apiKey: " + apiKey);
                return false;
            }

            user = userAcctPair.first();
            Account account = userAcctPair.second();

            if (!user.getState().equals(Account.State.ENABLED) || !account.getState().equals(Account.State.ENABLED)) {
                s_logger.debug("disabled or locked user accessing the api, userid = " + user.getId() + "; name = " + user.getUsername() + "; state: " + user.getState() +
                    "; accountState: " + account.getState());
                return false;
            }

            // verify secret key exists
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
            s_logger.error("unable to verify request signature", ex);
        }
        return false;
    }

    public static final String escapeHTML(String content) {
        if (content == null || content.isEmpty())
            return content;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            switch (c) {
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '&':
                    sb.append("&amp;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case ' ':
                    sb.append("&nbsp;");
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }
}
