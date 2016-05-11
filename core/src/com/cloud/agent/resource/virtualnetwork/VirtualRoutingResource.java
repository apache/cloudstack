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

package com.cloud.agent.resource.virtualnetwork;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckRouterAnswer;
import com.cloud.agent.api.CheckRouterCommand;
import com.cloud.agent.api.CheckS2SVpnConnectionsAnswer;
import com.cloud.agent.api.CheckS2SVpnConnectionsCommand;
import com.cloud.agent.api.GetDomRVersionAnswer;
import com.cloud.agent.api.GetDomRVersionCmd;
import com.cloud.agent.api.GetRouterAlertsAnswer;
import com.cloud.agent.api.routing.AggregationControlCommand;
import com.cloud.agent.api.routing.AggregationControlCommand.Action;
import com.cloud.agent.api.routing.GetRouterAlertsCommand;
import com.cloud.agent.api.routing.GroupAnswer;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.resource.virtualnetwork.facade.AbstractConfigItemFacade;
import com.cloud.utils.ExecutionResult;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 * VirtualNetworkResource controls and configures virtual networking
 *
 * @config
 * {@table
 *    || Param Name | Description | Values | Default ||
 *  }
 **/
public class VirtualRoutingResource {

    private static final Logger s_logger = Logger.getLogger(VirtualRoutingResource.class);
    private VirtualRouterDeployer _vrDeployer;
    private Map<String, Queue<NetworkElementCommand>> _vrAggregateCommandsSet;
    protected Map<String, Lock> _vrLockMap = new HashMap<String, Lock>();

    private String _name;
    private int _sleep;
    private int _retry;
    private int _port;
    private int _eachTimeout;

    private String _cfgVersion = "1.0";

    public VirtualRoutingResource(VirtualRouterDeployer deployer) {
        _vrDeployer = deployer;
    }

    public Answer executeRequest(final NetworkElementCommand cmd) {
        boolean aggregated = false;
        String routerName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        Lock lock;
        if (_vrLockMap.containsKey(routerName)) {
            lock = _vrLockMap.get(routerName);
        } else {
            lock = new ReentrantLock();
            _vrLockMap.put(routerName, lock);
        }
        lock.lock();

        try {
            ExecutionResult rc = _vrDeployer.prepareCommand(cmd);
            if (!rc.isSuccess()) {
                s_logger.error("Failed to prepare VR command due to " + rc.getDetails());
                return new Answer(cmd, false, rc.getDetails());
            }

            assert cmd.getRouterAccessIp() != null : "Why there is no access IP for VR?";

            if (cmd.isQuery()) {
                return executeQueryCommand(cmd);
            }

            if (cmd instanceof AggregationControlCommand) {
                return execute((AggregationControlCommand)cmd);
            }

            if (_vrAggregateCommandsSet.containsKey(routerName)) {
                _vrAggregateCommandsSet.get(routerName).add(cmd);
                aggregated = true;
                // Clean up would be done after command has been executed
                //TODO: Deal with group answer as well
                return new Answer(cmd);
            }

            List<ConfigItem> cfg = generateCommandCfg(cmd);
            if (cfg == null) {
                return Answer.createUnsupportedCommandAnswer(cmd);
            }

            return applyConfig(cmd, cfg);
        } catch (final IllegalArgumentException e) {
            return new Answer(cmd, false, e.getMessage());
        } finally {
            lock.unlock();
            if (!aggregated) {
                ExecutionResult rc = _vrDeployer.cleanupCommand(cmd);
                if (!rc.isSuccess()) {
                    s_logger.error("Failed to cleanup VR command due to " + rc.getDetails());
                }
            }
        }
    }

    private Answer executeQueryCommand(NetworkElementCommand cmd) {
        if (cmd instanceof CheckRouterCommand) {
            return execute((CheckRouterCommand)cmd);
        } else if (cmd instanceof GetDomRVersionCmd) {
            return execute((GetDomRVersionCmd)cmd);
        } else if (cmd instanceof CheckS2SVpnConnectionsCommand) {
            return execute((CheckS2SVpnConnectionsCommand) cmd);
        } else if (cmd instanceof GetRouterAlertsCommand) {
            return execute((GetRouterAlertsCommand)cmd);
        } else {
            s_logger.error("Unknown query command in VirtualRoutingResource!");
            return Answer.createUnsupportedCommandAnswer(cmd);
        }
    }

    private ExecutionResult applyConfigToVR(String routerAccessIp, ConfigItem c) {
        return applyConfigToVR(routerAccessIp, c, VRScripts.DEFAULT_EXECUTEINVR_TIMEOUT);
    }

    private ExecutionResult applyConfigToVR(String routerAccessIp, ConfigItem c, int timeout) {
        if (c instanceof FileConfigItem) {
            FileConfigItem configItem = (FileConfigItem)c;
            return _vrDeployer.createFileInVR(routerAccessIp, configItem.getFilePath(), configItem.getFileName(), configItem.getFileContents());
        } else if (c instanceof ScriptConfigItem) {
            ScriptConfigItem configItem = (ScriptConfigItem)c;
            return _vrDeployer.executeInVR(routerAccessIp, configItem.getScript(), configItem.getArgs(), timeout);
        }
        throw new CloudRuntimeException("Unable to apply unknown configitem of type " + c.getClass().getSimpleName());
    }


    private Answer applyConfig(NetworkElementCommand cmd, List<ConfigItem> cfg) {


        if (cfg.isEmpty()) {
            return new Answer(cmd, true, "Nothing to do");
        }

        List<ExecutionResult> results = new ArrayList<ExecutionResult>();
        List<String> details = new ArrayList<String>();
        boolean finalResult = false;
        for (ConfigItem configItem : cfg) {
            long startTimestamp = System.currentTimeMillis();
            ExecutionResult result = applyConfigToVR(cmd.getRouterAccessIp(), configItem);
            if (s_logger.isDebugEnabled()) {
                long elapsed = System.currentTimeMillis() - startTimestamp;
                s_logger.debug("Processing " + configItem + " took " + elapsed + "ms");
            }
            if (result == null) {
                result = new ExecutionResult(false, "null execution result");
            }
            results.add(result);
            details.add(configItem.getInfo() + (result.isSuccess() ? " - success: " : " - failed: ") + result.getDetails());
            finalResult = result.isSuccess();
        }

        // Not sure why this matters, but log it anyway
        if (cmd.getAnswersCount() != results.size()) {
            s_logger.warn("Expected " + cmd.getAnswersCount() + " answers while executing " + cmd.getClass().getSimpleName() + " but received " + results.size());
        }


        if (results.size() == 1) {
            return new Answer(cmd, finalResult, results.get(0).getDetails());
        } else {
            return new GroupAnswer(cmd, finalResult, results.size(), details.toArray(new String[details.size()]));
        }
    }

    private CheckS2SVpnConnectionsAnswer execute(CheckS2SVpnConnectionsCommand cmd) {

        StringBuffer buff = new StringBuffer();
        for (String ip : cmd.getVpnIps()) {
            buff.append(ip);
            buff.append(" ");
        }
        ExecutionResult result = _vrDeployer.executeInVR(cmd.getRouterAccessIp(), VRScripts.S2SVPN_CHECK, buff.toString());
        return new CheckS2SVpnConnectionsAnswer(cmd, result.isSuccess(), result.getDetails());
    }

    private GetRouterAlertsAnswer execute(GetRouterAlertsCommand cmd) {

        String routerIp = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        String args = cmd.getPreviousAlertTimeStamp();

        ExecutionResult result = _vrDeployer.executeInVR(routerIp, VRScripts.ROUTER_ALERTS, args);
        String alerts[] = null;
        String lastAlertTimestamp = null;

        if (result.isSuccess()) {
            if (!result.getDetails().isEmpty() && !result.getDetails().trim().equals("No Alerts")) {
                alerts = result.getDetails().trim().split("\\\\n");
                String[] lastAlert = alerts[alerts.length - 1].split(",");
                lastAlertTimestamp = lastAlert[0];
            }
            return new GetRouterAlertsAnswer(cmd, alerts, lastAlertTimestamp);
        } else {
            return new GetRouterAlertsAnswer(cmd, result.getDetails());
        }
    }

    private Answer execute(CheckRouterCommand cmd) {
        final ExecutionResult result = _vrDeployer.executeInVR(cmd.getRouterAccessIp(), VRScripts.RVR_CHECK, null);
        if (!result.isSuccess()) {
            return new CheckRouterAnswer(cmd, result.getDetails());
        }
        return new CheckRouterAnswer(cmd, result.getDetails(), true);
    }

    private Answer execute(GetDomRVersionCmd cmd) {
        final ExecutionResult result = _vrDeployer.executeInVR(cmd.getRouterAccessIp(), VRScripts.VERSION, null);
        if (!result.isSuccess()) {
            return new GetDomRVersionAnswer(cmd, "GetDomRVersionCmd failed");
        }
        String[] lines = result.getDetails().split("&");
        if (lines.length != 2) {
            return new GetDomRVersionAnswer(cmd, result.getDetails());
        }
        return new GetDomRVersionAnswer(cmd, result.getDetails(), lines[0], lines[1]);
    }

    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        _name = name;

        String value = (String)params.get("ssh.sleep");
        _sleep = NumbersUtil.parseInt(value, 10) * 1000;

        value = (String)params.get("ssh.retry");
        _retry = NumbersUtil.parseInt(value, 36);

        value = (String)params.get("ssh.port");
        _port = NumbersUtil.parseInt(value, 3922);

        value = (String)params.get("router.aggregation.command.each.timeout");
        _eachTimeout = NumbersUtil.parseInt(value, 3);

        if (_vrDeployer == null) {
            throw new ConfigurationException("Unable to find the resource for VirtualRouterDeployer!");
        }

        _vrAggregateCommandsSet = new HashMap<>();
        return true;
    }

    public boolean connect(final String ipAddress) {
        return connect(ipAddress, _port);
    }

    public boolean connect(final String ipAddress, final int port) {
        return connect(ipAddress, port, _sleep);
    }

    public boolean connect(final String ipAddress, int retry, int sleep) {
        for (int i = 0; i <= retry; i++) {
            SocketChannel sch = null;
            try {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Trying to connect to " + ipAddress);
                }
                sch = SocketChannel.open();
                sch.configureBlocking(true);

                final InetSocketAddress addr = new InetSocketAddress(ipAddress, _port);
                sch.connect(addr);
                return true;
            } catch (final IOException e) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Could not connect to " + ipAddress);
                }
            } finally {
                if (sch != null) {
                    try {
                        sch.close();
                    } catch (final IOException e) {
                    }
                }
            }
            try {
                Thread.sleep(sleep);
            } catch (final InterruptedException e) {
            }
        }

        s_logger.debug("Unable to logon to " + ipAddress);

        return false;
    }

    private List<ConfigItem> generateCommandCfg(NetworkElementCommand cmd) {
        /*
         * [TODO] Still have to migrate LoadBalancerConfigCommand and BumpUpPriorityCommand
         * [FIXME] Have a look at SetSourceNatConfigItem
         */
        s_logger.debug("Transforming " + cmd.getClass().getCanonicalName() + " to ConfigItems");

        final AbstractConfigItemFacade configItemFacade = AbstractConfigItemFacade.getInstance(cmd.getClass());

        return configItemFacade.generateConfig(cmd);
    }

    private Answer execute(AggregationControlCommand cmd) {
        Action action = cmd.getAction();
        String routerName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        assert routerName != null;
        assert cmd.getRouterAccessIp() != null;

        if (action == Action.Start) {
            assert (!_vrAggregateCommandsSet.containsKey(routerName));

            Queue<NetworkElementCommand> queue = new LinkedBlockingQueue<>();
            _vrAggregateCommandsSet.put(routerName, queue);
            return new Answer(cmd, true, "Command aggregation started");
        } else if (action == Action.Finish) {
            Queue<NetworkElementCommand> queue = _vrAggregateCommandsSet.get(routerName);
            int answerCounts = 0;
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("#Apache CloudStack Virtual Router Config File\n");
                sb.append("<version>\n" + _cfgVersion + "\n</version>\n");
                for (NetworkElementCommand command : queue) {
                    answerCounts += command.getAnswersCount();
                    List<ConfigItem> cfg = generateCommandCfg(command);
                    if (cfg == null) {
                        s_logger.warn("Unknown commands for VirtualRoutingResource, but continue: " + cmd.toString());
                        continue;
                    }

                    for (ConfigItem c : cfg) {
                        sb.append(c.getAggregateCommand());
                    }
                }

                // TODO replace with applyConfig with a stop on fail
                String cfgFileName = "VR-"+ UUID.randomUUID().toString() + ".cfg";
                FileConfigItem fileConfigItem = new FileConfigItem(VRScripts.CONFIG_CACHE_LOCATION, cfgFileName, sb.toString());
                ScriptConfigItem scriptConfigItem = new ScriptConfigItem(VRScripts.VR_CFG, "-c " + VRScripts.CONFIG_CACHE_LOCATION + cfgFileName);
                // 120s is the minimal timeout
                int timeout = answerCounts * _eachTimeout;
                if (timeout < 120) {
                    timeout = 120;
                }

                ExecutionResult result = applyConfigToVR(cmd.getRouterAccessIp(), fileConfigItem);
                if (!result.isSuccess()) {
                    return new Answer(cmd, false, result.getDetails());
                }

                result = applyConfigToVR(cmd.getRouterAccessIp(), scriptConfigItem, timeout);
                if (!result.isSuccess()) {
                    return new Answer(cmd, false, result.getDetails());
                }

                return new Answer(cmd, true, "Command aggregation finished");
            } finally {
                queue.clear();
                _vrAggregateCommandsSet.remove(routerName);
            }
        }
        return new Answer(cmd, false, "Fail to recongize aggregation action " + action.toString());
    }
}
