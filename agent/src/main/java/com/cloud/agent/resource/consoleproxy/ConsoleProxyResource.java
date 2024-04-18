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
package com.cloud.agent.resource.consoleproxy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.naming.ConfigurationException;

import com.cloud.agent.api.proxy.AllowConsoleAccessCommand;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;

import com.cloud.agent.Agent.ExitStatus;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckHealthAnswer;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.ConsoleAccessAuthenticationAnswer;
import com.cloud.agent.api.ConsoleAccessAuthenticationCommand;
import com.cloud.agent.api.ConsoleProxyLoadReportCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupProxyCommand;
import com.cloud.agent.api.proxy.CheckConsoleProxyLoadCommand;
import com.cloud.agent.api.proxy.ConsoleProxyLoadAnswer;
import com.cloud.agent.api.proxy.StartConsoleProxyAgentHttpHandlerCommand;
import com.cloud.agent.api.proxy.WatchConsoleProxyLoadCommand;
import com.cloud.exception.AgentControlChannelException;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.resource.ServerResource;
import com.cloud.resource.ServerResourceBase;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.script.Script;
import com.google.gson.Gson;

/**
 *
 * I don't want to introduce extra cross-cutting concerns into console proxy
 * process, as it involves configurations like zone/pod, agent auto self-upgrade
 * etc. I also don't want to introduce more module dependency issues into our
 * build system, cross-communication between this resource and console proxy
 * will be done through reflection. As a result, come out with following
 * solution to solve the problem of building a communication channel between
 * consoole proxy and management server.
 *
 * We will deploy an agent shell inside console proxy VM, and this agent shell
 * will launch current console proxy from within this special server resource,
 * through it console proxy can build a communication channel with management
 * server.
 *
 */
public class ConsoleProxyResource extends ServerResourceBase implements ServerResource {

    private final Properties properties = new Properties();
    private Thread consoleProxyMain = null;

    long proxyVmId;
    int proxyPort;

    String localGateway;
    String eth1Ip;
    String eth1Mask;
    String publicIp;

    @Override
    public Answer executeRequest(final Command cmd) {
        if (cmd instanceof CheckConsoleProxyLoadCommand) {
            return execute((CheckConsoleProxyLoadCommand)cmd);
        } else if (cmd instanceof WatchConsoleProxyLoadCommand) {
            return execute((WatchConsoleProxyLoadCommand)cmd);
        } else if (cmd instanceof ReadyCommand) {
            logger.info("Receive ReadyCommand, response with ReadyAnswer");
            return new ReadyAnswer((ReadyCommand)cmd);
        } else if (cmd instanceof CheckHealthCommand) {
            return new CheckHealthAnswer((CheckHealthCommand)cmd, true);
        } else if (cmd instanceof StartConsoleProxyAgentHttpHandlerCommand) {
            return execute((StartConsoleProxyAgentHttpHandlerCommand) cmd);
        } else if (cmd instanceof AllowConsoleAccessCommand) {
            return execute((AllowConsoleAccessCommand) cmd);
        } else {
            return Answer.createUnsupportedCommandAnswer(cmd);
        }
    }

    private Answer execute(AllowConsoleAccessCommand cmd) {
        String sessionUuid = cmd.getSessionUuid();
        try {
            Class<?> consoleProxyClazz = Class.forName("com.cloud.consoleproxy.ConsoleProxy");
            Method methodSetup = consoleProxyClazz.getMethod("addAllowedSession", String.class);
            methodSetup.invoke(null, sessionUuid);
            return new Answer(cmd);
        } catch (SecurityException | NoSuchMethodException | ClassNotFoundException | InvocationTargetException | IllegalAccessException e) {
            String errorMsg = "Unable to add allowed session due to: " + e.getMessage();
            logger.error(errorMsg, e);
            return new Answer(cmd, false, errorMsg);
        }
    }

    private Answer execute(StartConsoleProxyAgentHttpHandlerCommand cmd) {
        logger.info("Invoke launchConsoleProxy() in responding to StartConsoleProxyAgentHttpHandlerCommand");
        launchConsoleProxy(cmd.getKeystoreBits(), cmd.getKeystorePassword(), cmd.getEncryptorPassword(), cmd.isSourceIpCheckEnabled());
        return new Answer(cmd);
    }

    private void disableRpFilter() {
        try (FileWriter fstream = new FileWriter("/proc/sys/net/ipv4/conf/eth2/rp_filter");
             BufferedWriter out = new BufferedWriter(fstream);)
        {
            out.write("0");
        } catch (IOException e) {
            logger.warn("Unable to disable rp_filter");
        }
    }

    protected Answer execute(final CheckConsoleProxyLoadCommand cmd) {
        return executeProxyLoadScan(cmd, cmd.getProxyVmId(), cmd.getProxyVmName(), cmd.getProxyManagementIp(), cmd.getProxyCmdPort());
    }

    protected Answer execute(final WatchConsoleProxyLoadCommand cmd) {
        return executeProxyLoadScan(cmd, cmd.getProxyVmId(), cmd.getProxyVmName(), cmd.getProxyManagementIp(), cmd.getProxyCmdPort());
    }

    private Answer executeProxyLoadScan(final Command cmd, final long proxyVmId, final String proxyVmName, final String proxyManagementIp, final int cmdPort) {
        String result = null;

        final StringBuffer sb = new StringBuffer();
        sb.append("http://").append(proxyManagementIp).append(":" + cmdPort).append("/cmd/getstatus");

        boolean success = true;
        try {
            final URL url = new URL(sb.toString());
            final URLConnection conn = url.openConnection();

            final InputStream is = conn.getInputStream();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(is,"UTF-8"));
            final StringBuilder sb2 = new StringBuilder();
            String line = null;
            try {
                while ((line = reader.readLine()) != null)
                    sb2.append(line + "\n");
                result = sb2.toString();
            } catch (final IOException e) {
                success = false;
            } finally {
                try {
                    is.close();
                } catch (final IOException e) {
                    logger.warn("Exception when closing , console proxy address : {}", proxyManagementIp);
                    success = false;
                }
            }
        } catch (final IOException e) {
            logger.warn("Unable to open console proxy command port url, console proxy address : {}", proxyManagementIp);
            success = false;
        }

        return new ConsoleProxyLoadAnswer(cmd, proxyVmId, proxyVmName, success, result);
    }

    @Override
    protected String getDefaultScriptsDir() {
        return null;
    }

    @Override
    public Type getType() {
        return Host.Type.ConsoleProxy;
    }

    @Override
    public synchronized StartupCommand[] initialize() {
        final StartupProxyCommand cmd = new StartupProxyCommand();
        fillNetworkInformation(cmd);
        cmd.setProxyPort(proxyPort);
        cmd.setProxyVmId(proxyVmId);
        if (publicIp != null)
            cmd.setPublicIpAddress(publicIp);
        return new StartupCommand[] {cmd};
    }

    @Override
    public void disconnected() {
    }

    @Override
    public PingCommand getCurrentStatus(long id) {
        return new PingCommand(Type.ConsoleProxy, id);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        localGateway = (String)params.get("localgw");
        eth1Mask = (String)params.get("eth1mask");
        eth1Ip = (String)params.get("eth1ip");
        if (eth1Ip != null) {
            params.put("private.network.device", "eth1");
        } else {
            logger.info("eth1ip parameter has not been configured, assuming that we are not inside a system vm");
        }

        String eth2ip = (String)params.get("eth2ip");
        if (eth2ip != null) {
            params.put("public.network.device", "eth2");
        } else {
            logger.info("eth2ip parameter is not found, assuming that we are not inside a system vm");
        }

        super.configure(name, params);

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            properties.put(entry.getKey(), entry.getValue());
        }

        String value = (String)params.get("premium");
        if (value != null && value.equals("premium"))
            proxyPort = 443;
        else {
            value = (String)params.get("consoleproxy.httpListenPort");
            proxyPort = NumbersUtil.parseInt(value, 80);
        }

        value = (String)params.get("proxy_vm");
        proxyVmId = NumbersUtil.parseLong(value, 0);

        if (localGateway != null) {
            String mgmtHosts = (String)params.get("host");
            if (eth1Ip != null) {
                for (final String mgmtHost : mgmtHosts.split(",")) {
                    addRouteToInternalIpOrCidr(localGateway, eth1Ip, eth1Mask, mgmtHost);
                }
                String internalDns1 = (String) params.get("internaldns1");
                if (internalDns1 == null) {
                    logger.warn("No DNS entry found during configuration of ConsoleProxy");
                } else {
                    addRouteToInternalIpOrCidr(localGateway, eth1Ip, eth1Mask, internalDns1);
                }
                String internalDns2 = (String) params.get("internaldns2");
                if (internalDns2 != null) {
                    addRouteToInternalIpOrCidr(localGateway, eth1Ip, eth1Mask, internalDns2);
                }
            }
        }

        publicIp = (String)params.get("public.ip");

        value = (String)params.get("disable_rp_filter");
        if (value != null && value.equalsIgnoreCase("true")) {
            disableRpFilter();
        }

        logger.info("Receive proxyVmId in ConsoleProxyResource configuration as {}", proxyVmId);

        return true;
    }

    private void addRouteToInternalIpOrCidr(String localgw, String eth1ip, String eth1mask, String destIpOrCidr) {
        logger.debug("addRouteToInternalIp: localgw={}, eth1ip={}, eth1mask={}, destIp={}", localgw, eth1ip, eth1mask, destIpOrCidr);
        if (destIpOrCidr == null) {
            logger.debug("addRouteToInternalIp: destIp is null");
            return;
        }
        if (!NetUtils.isValidIp4(destIpOrCidr) && !NetUtils.isValidIp4Cidr(destIpOrCidr)) {
            logger.warn(" destIp is not a valid ip address or cidr destIp={}", destIpOrCidr);
            return;
        }
        boolean inSameSubnet = false;
        if (NetUtils.isValidIp4(destIpOrCidr)) {
            if (eth1ip != null && eth1mask != null) {
                inSameSubnet = NetUtils.sameSubnet(eth1ip, destIpOrCidr, eth1mask);
            } else {
                logger.warn("addRouteToInternalIp: unable to determine same subnet: eth1ip={}, dest ip={}, eth1mask={}", eth1ip, destIpOrCidr, eth1mask);
            }
        } else {
            inSameSubnet = NetUtils.isNetworkAWithinNetworkB(destIpOrCidr, NetUtils.ipAndNetMaskToCidr(eth1ip, eth1mask));
        }
        if (inSameSubnet) {
            logger.debug("addRouteToInternalIp: dest ip {} is in the same subnet as eth1 ip {}", destIpOrCidr, eth1ip);
            return;
        }
        Script command = new Script("/bin/bash", logger);
        command.add("-c");
        command.add("ip route delete " + destIpOrCidr);
        command.execute();
        command = new Script("/bin/bash", logger);
        command.add("-c");
        command.add("ip route add " + destIpOrCidr + " via " + localgw);
        String result = command.execute();
        if (result != null) {
            logger.warn("Error in configuring route to internal ip err={}", result);
        } else {
            logger.debug("addRouteToInternalIp: added route to internal ip={} via {}", destIpOrCidr, localgw);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    private void launchConsoleProxy(final byte[] ksBits, final String ksPassword, final String encryptorPassword, final Boolean isSourceIpCheckEnabled) {
        final Object resource = this;
        logger.info("Building class loader for com.cloud.consoleproxy.ConsoleProxy");
        if (consoleProxyMain == null) {
            logger.info("Running com.cloud.consoleproxy.ConsoleProxy with encryptor password={}", encryptorPassword);
            consoleProxyMain = new Thread(new ManagedContextRunnable() {
                @Override
                protected void runInContext() {
                    try {
                        Class<?> consoleProxyClazz = Class.forName("com.cloud.consoleproxy.ConsoleProxy");
                        try {
                            logger.info("Invoke startWithContext()");
                            Method method = consoleProxyClazz.getMethod("startWithContext", Properties.class, Object.class, byte[].class, String.class, String.class, Boolean.class);
                            method.invoke(null, properties, resource, ksBits, ksPassword, encryptorPassword, isSourceIpCheckEnabled);
                        } catch (SecurityException e) {
                            logger.error("Unable to launch console proxy due to SecurityException", e);
                            System.exit(ExitStatus.Error.value());
                        } catch (NoSuchMethodException e) {
                            logger.error("Unable to launch console proxy due to NoSuchMethodException", e);
                            System.exit(ExitStatus.Error.value());
                        } catch (IllegalArgumentException e) {
                            logger.error("Unable to launch console proxy due to IllegalArgumentException", e);
                            System.exit(ExitStatus.Error.value());
                        } catch (IllegalAccessException e) {
                            logger.error("Unable to launch console proxy due to IllegalAccessException", e);
                            System.exit(ExitStatus.Error.value());
                        } catch (InvocationTargetException e) {
                            logger.error("Unable to launch console proxy due to InvocationTargetException {}", e.getTargetException().toString(), e);
                            System.exit(ExitStatus.Error.value());
                        }
                    } catch (final ClassNotFoundException e) {
                        logger.error("Unable to launch console proxy due to ClassNotFoundException");
                        System.exit(ExitStatus.Error.value());
                    }
                }
            }, "Console-Proxy-Main");
            consoleProxyMain.setDaemon(true);
            consoleProxyMain.start();
        } else {
            logger.info("com.cloud.consoleproxy.ConsoleProxy is already running");

            try {
                Class<?> consoleProxyClazz = Class.forName("com.cloud.consoleproxy.ConsoleProxy");
                Method methodSetup = consoleProxyClazz.getMethod("setEncryptorPassword", String.class);
                methodSetup.invoke(null, encryptorPassword);
                methodSetup = consoleProxyClazz.getMethod("setIsSourceIpCheckEnabled", Boolean.class);
                methodSetup.invoke(null, isSourceIpCheckEnabled);
            } catch (SecurityException e) {
                logger.error("Unable to launch console proxy due to SecurityException", e);
                System.exit(ExitStatus.Error.value());
            } catch (NoSuchMethodException e) {
                logger.error("Unable to launch console proxy due to NoSuchMethodException", e);
                System.exit(ExitStatus.Error.value());
            } catch (IllegalArgumentException e) {
                logger.error("Unable to launch console proxy due to IllegalArgumentException", e);
                System.exit(ExitStatus.Error.value());
            } catch (IllegalAccessException e) {
                logger.error("Unable to launch console proxy due to IllegalAccessException", e);
                System.exit(ExitStatus.Error.value());
            } catch (InvocationTargetException e) {
                logger.error("Unable to launch console proxy due to InvocationTargetException " + e.getTargetException().toString(), e);
                System.exit(ExitStatus.Error.value());
            } catch (final ClassNotFoundException e) {
                logger.error("Unable to launch console proxy due to ClassNotFoundException", e);
                System.exit(ExitStatus.Error.value());
            }
        }
    }

    public String authenticateConsoleAccess(String host, String port, String vmId, String sid, String ticket,
                                            Boolean isReauthentication, String sessionToken) {

        ConsoleAccessAuthenticationCommand cmd = new ConsoleAccessAuthenticationCommand(host, port, vmId, sid, ticket, sessionToken);
        cmd.setReauthenticating(isReauthentication);

        ConsoleProxyAuthenticationResult result = new ConsoleProxyAuthenticationResult();
        result.setSuccess(false);
        result.setReauthentication(isReauthentication);

        try {
            AgentControlAnswer answer = getAgentControl().sendRequest(cmd, 10000);

            if (answer != null) {
                ConsoleAccessAuthenticationAnswer authAnswer = (ConsoleAccessAuthenticationAnswer)answer;
                result.setSuccess(authAnswer.succeeded());
                result.setHost(authAnswer.getHost());
                result.setPort(authAnswer.getPort());
                result.setTunnelUrl(authAnswer.getTunnelUrl());
                result.setTunnelSession(authAnswer.getTunnelSession());
            } else {
                logger.error("Authentication failed for vm: {} with sid: {}", vmId, sid);
            }
        } catch (AgentControlChannelException e) {
            logger.error("Unable to send out console access authentication request due to {}", e.getMessage(), e);
        }

        return new Gson().toJson(result);
    }

    public void reportLoadInfo(String gsonLoadInfo) {
        ConsoleProxyLoadReportCommand cmd = new ConsoleProxyLoadReportCommand(proxyVmId, gsonLoadInfo);
        try {
            getAgentControl().postRequest(cmd);
            logger.debug("Report proxy load info, proxy : {}, load: {}", proxyVmId, gsonLoadInfo);
        } catch (AgentControlChannelException e) {
            logger.error("Unable to send out load info due to {}", e.getMessage(), e);
        }
    }

    public void ensureRoute(String address) {
        if (localGateway != null) {
            logger.debug("Ensure route for {} via {}", address, localGateway);

            // this method won't be called in high frequency, serialize access
            // to script execution
            synchronized (this) {
                try {
                    addRouteToInternalIpOrCidr(localGateway, eth1Ip, eth1Mask, address);
                } catch (Throwable e) {
                    logger.warn("Unexpected exception while adding internal route to {}", address, e);
                }
            }
        }
    }

    @Override
    public void setName(String name) {
    }

    @Override
    public void setConfigParams(Map<String, Object> params) {
    }

    @Override
    public Map<String, Object> getConfigParams() {
        return new HashMap<String, Object>();
    }

    @Override
    public int getRunLevel() {
        return 0;
    }

    @Override
    public void setRunLevel(int level) {
    }
}
