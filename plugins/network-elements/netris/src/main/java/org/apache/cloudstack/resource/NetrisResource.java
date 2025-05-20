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
package org.apache.cloudstack.resource;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckHealthAnswer;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.host.Host;
import com.cloud.network.vpc.StaticRoute;
import com.cloud.resource.ServerResource;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.agent.api.CreateOrUpdateNetrisACLCommand;
import org.apache.cloudstack.agent.api.AddOrUpdateNetrisStaticRouteCommand;
import org.apache.cloudstack.agent.api.CreateNetrisVnetCommand;
import org.apache.cloudstack.agent.api.CreateNetrisVpcCommand;
import org.apache.cloudstack.agent.api.CreateOrUpdateNetrisLoadBalancerRuleCommand;
import org.apache.cloudstack.agent.api.CreateOrUpdateNetrisNatCommand;
import org.apache.cloudstack.agent.api.DeleteNetrisACLCommand;
import org.apache.cloudstack.agent.api.DeleteNetrisLoadBalancerRuleCommand;
import org.apache.cloudstack.agent.api.DeleteNetrisNatRuleCommand;
import org.apache.cloudstack.agent.api.DeleteNetrisStaticRouteCommand;
import org.apache.cloudstack.agent.api.DeleteNetrisVnetCommand;
import org.apache.cloudstack.agent.api.DeleteNetrisVpcCommand;
import org.apache.cloudstack.agent.api.ListNetrisStaticRoutesAnswer;
import org.apache.cloudstack.agent.api.ListNetrisStaticRoutesCommand;
import org.apache.cloudstack.agent.api.NetrisAnswer;
import org.apache.cloudstack.StartupNetrisCommand;
import org.apache.cloudstack.agent.api.ReleaseNatIpCommand;
import org.apache.cloudstack.agent.api.SetupNetrisPublicRangeCommand;
import org.apache.cloudstack.agent.api.UpdateNetrisVnetCommand;
import org.apache.cloudstack.agent.api.UpdateNetrisVpcCommand;
import org.apache.cloudstack.service.NetrisApiClient;
import org.apache.cloudstack.service.NetrisApiClientImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.naming.ConfigurationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetrisResource implements ServerResource {
    protected Logger logger = LogManager.getLogger(getClass());

    private String name;
    protected String endpointUrl;
    protected String username;
    protected String password;
    protected String guid;
    protected String zoneId;
    protected String siteName;
    protected String adminTenantName;

    protected NetrisApiClient netrisApiClient;

    @Override
    public Host.Type getType() {
        return Host.Type.Routing;
    }

    @Override
    public StartupCommand[] initialize() {
        StartupNetrisCommand sc = new StartupNetrisCommand();
        sc.setGuid(guid);
        sc.setName(name);
        sc.setDataCenter(zoneId);
        sc.setPod("");
        sc.setPrivateIpAddress("");
        sc.setStorageIpAddress("");
        sc.setVersion("");
        return new StartupCommand[] {sc};
    }

    @Override
    public PingCommand getCurrentStatus(long id) {
        return null;
    }

    @Override
    public Answer executeRequest(Command cmd) {
        if (cmd instanceof ReadyCommand) {
            return executeRequest((ReadyCommand) cmd);
        } else if (cmd instanceof CheckHealthCommand) {
            return executeRequest((CheckHealthCommand) cmd);
        } else if (cmd instanceof CreateNetrisVpcCommand) {
            return executeRequest((CreateNetrisVpcCommand) cmd);
        } else if (cmd instanceof UpdateNetrisVpcCommand) {
          return executeRequest((UpdateNetrisVpcCommand) cmd);
        } else if (cmd instanceof DeleteNetrisVpcCommand) {
            return executeRequest((DeleteNetrisVpcCommand) cmd);
        } else if (cmd instanceof CreateNetrisVnetCommand) {
            return executeRequest((CreateNetrisVnetCommand) cmd);
        } else if (cmd instanceof UpdateNetrisVnetCommand) {
          return executeRequest((UpdateNetrisVnetCommand) cmd);
        } else if (cmd instanceof DeleteNetrisVnetCommand) {
          return executeRequest((DeleteNetrisVnetCommand) cmd);
        } else if (cmd instanceof SetupNetrisPublicRangeCommand) {
            return executeRequest((SetupNetrisPublicRangeCommand) cmd);
        } else if (cmd instanceof DeleteNetrisNatRuleCommand) {
            return executeRequest((DeleteNetrisNatRuleCommand) cmd);
        } else if (cmd instanceof CreateOrUpdateNetrisNatCommand) {
          return executeRequest((CreateOrUpdateNetrisNatCommand) cmd);
        } else if (cmd instanceof CreateOrUpdateNetrisACLCommand) {
            return executeRequest((CreateOrUpdateNetrisACLCommand) cmd);
        } else if (cmd instanceof DeleteNetrisACLCommand) {
            return executeRequest((DeleteNetrisACLCommand) cmd);
        } else if (cmd instanceof ListNetrisStaticRoutesCommand) {
            return executeRequest((ListNetrisStaticRoutesCommand) cmd);
        } else if (cmd instanceof DeleteNetrisStaticRouteCommand) {
            return executeRequest((DeleteNetrisStaticRouteCommand) cmd);
        } else if (cmd instanceof AddOrUpdateNetrisStaticRouteCommand) {
            return executeRequest((AddOrUpdateNetrisStaticRouteCommand) cmd);
        } else if (cmd instanceof ReleaseNatIpCommand) {
          return executeRequest((ReleaseNatIpCommand) cmd);
        } else if (cmd instanceof CreateOrUpdateNetrisLoadBalancerRuleCommand) {
            return executeRequest((CreateOrUpdateNetrisLoadBalancerRuleCommand) cmd);
        } else if (cmd instanceof DeleteNetrisLoadBalancerRuleCommand) {
          return executeRequest((DeleteNetrisLoadBalancerRuleCommand) cmd);
        } else {
            return Answer.createUnsupportedCommandAnswer(cmd);
        }
    }

    @Override
    public void disconnected() {
        // Do nothing
    }

    @Override
    public IAgentControl getAgentControl() {
        return null;
    }

    @Override
    public void setAgentControl(IAgentControl agentControl) {
        // Do nothing
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setConfigParams(Map<String, Object> params) {
        // Do nothing
    }

    @Override
    public Map<String, Object> getConfigParams() {
        return new HashMap<>();
    }

    @Override
    public int getRunLevel() {
        return 0;
    }

    @Override
    public void setRunLevel(int level) {

    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        endpointUrl = (String) params.get("url");
        if (endpointUrl == null) {
            throw new ConfigurationException("Missing Netris provider URL from params: " + params);
        }

        username = (String) params.get("username");
        if (username == null) {
            throw new ConfigurationException("Missing Netris username from params: " + params);
        }

        password = (String) params.get("password");
        if (password == null) {
            throw new ConfigurationException("Missing Netris password from params: " + params);
        }

        this.name = (String) params.get("name");
        if (this.name == null) {
            throw new ConfigurationException("Unable to find name");
        }

        guid = (String) params.get("guid");
        if (guid == null) {
            throw new ConfigurationException("Unable to find the guid");
        }

        zoneId = (String) params.get("zoneId");
        if (zoneId == null) {
            throw new ConfigurationException("Unable to find zone");
        }

        siteName = (String) params.get("siteName");
        if (siteName == null) {
            throw new ConfigurationException("Unable to find the Netris site name");
        }

        adminTenantName = (String) params.get("tenantName");
        if (adminTenantName == null) {
            throw new ConfigurationException("Unable to find the Netris admin tenant name");
        }

        netrisApiClient = new NetrisApiClientImpl(endpointUrl, username, password, siteName, adminTenantName);
        return netrisApiClient.isSessionAlive();
    }

    private Answer executeRequest(ReadyCommand cmd) {
        return new ReadyAnswer(cmd);
    }

    private Answer executeRequest(CheckHealthCommand cmd) {
        return new CheckHealthAnswer(cmd, netrisApiClient.isSessionAlive());
    }

    private Answer executeRequest(CreateNetrisVpcCommand cmd) {
        try {
            boolean result = netrisApiClient.createVpc(cmd);
            if (!result) {
                return new NetrisAnswer(cmd, false, String.format("Netris VPC %s creation failed", cmd.getName()));
            }
            return new NetrisAnswer(cmd, true, "OK");
        } catch (CloudRuntimeException e) {
            String msg = String.format("Error creating Netris VPC: %s", e.getMessage());
            logger.error(msg, e);
            return new NetrisAnswer(cmd, new CloudRuntimeException(msg));
        }
    }

    private Answer executeRequest(UpdateNetrisVpcCommand cmd) {
        try {
            boolean result = netrisApiClient.updateVpc(cmd);
            if (!result) {
                return new NetrisAnswer(cmd, false, String.format("Netris VPC %s creation failed", cmd.getName()));
            }
            return new NetrisAnswer(cmd, true, "OK");
        } catch (CloudRuntimeException e) {
            String msg = String.format("Error creating Netris VPC: %s", e.getMessage());
            logger.error(msg, e);
            return new NetrisAnswer(cmd, new CloudRuntimeException(msg));
        }
    }

    private Answer executeRequest(CreateNetrisVnetCommand cmd) {
        try {
            String vpcName = cmd.getVpcName();
            boolean result = netrisApiClient.createVnet(cmd);
            if (!result) {
                return new NetrisAnswer(cmd, false, String.format("Netris VPC %s creation failed", vpcName));
            }
            return new NetrisAnswer(cmd, true, "OK");
        } catch (CloudRuntimeException e) {
            String msg = String.format("Error creating Netris VPC: %s", e.getMessage());
            logger.error(msg, e);
            return new NetrisAnswer(cmd, new CloudRuntimeException(msg));
        }
    }

    private Answer executeRequest(UpdateNetrisVnetCommand cmd) {
        try {
            String networkName = cmd.getName();
            String prevNetworkName = cmd.getPrevNetworkName();
            boolean result = netrisApiClient.updateVnet(cmd);
            if (!result) {
                return new NetrisAnswer(cmd, false, String.format("Failed to update network name from %s to %s", prevNetworkName, networkName));
            }
            return new NetrisAnswer(cmd, true, "OK");
        } catch (CloudRuntimeException e) {
            String msg = String.format("Error updating Netris vNet: %s", e.getMessage());
            logger.error(msg, e);
            return new NetrisAnswer(cmd, new CloudRuntimeException(msg));
        }
    }

    private Answer executeRequest(DeleteNetrisVnetCommand cmd) {
        try {
            String networkName = cmd.getName();
            boolean result = netrisApiClient.deleteVnet(cmd);
            if (!result) {
                return new NetrisAnswer(cmd, false, String.format("Netris vNet: %s deletion failed", networkName));
            }
            return new NetrisAnswer(cmd, true, "OK");
        } catch (CloudRuntimeException e) {
            String msg = String.format("Error deleting Netris vNet: %s", e.getMessage());
            logger.error(msg, e);
            return new NetrisAnswer(cmd, new CloudRuntimeException(msg));
        }
    }

    private Answer executeRequest(DeleteNetrisVpcCommand cmd) {
        boolean result = netrisApiClient.deleteVpc(cmd);
        if (!result) {
            return new NetrisAnswer(cmd, false, String.format("Netris VPC %s deletion failed", cmd.getName()));
        }
        return new NetrisAnswer(cmd, true, "OK");
    }

    private Answer executeRequest(SetupNetrisPublicRangeCommand cmd) {
        boolean result = netrisApiClient.setupZoneLevelPublicRange(cmd);
        if (!result) {
            return new NetrisAnswer(cmd, false, "Netris Setup for Public Range failed");
        }
        return new NetrisAnswer(cmd, true, "OK");
    }

    private Answer executeRequest(CreateOrUpdateNetrisNatCommand cmd) {
        String natRuleType = cmd.getNatRuleType();
        if ("SNAT".equals(natRuleType)) {
            boolean result = netrisApiClient.createOrUpdateSNATRule(cmd);
            if (!result) {
                return new NetrisAnswer(cmd, false, String.format("Failed to create SNAT rule on Netris for network %s", cmd.getName()));
            }
        } else if ("DNAT".equals(natRuleType)) {
            boolean result = netrisApiClient.createOrUpdateDNATRule(cmd);
            if (!result) {
                return new NetrisAnswer(cmd, false, String.format("Failed to create DNAT rule on Netris for network %s", cmd.getName()));
            }
        } else if ("STATICNAT".equals(natRuleType)) {
            boolean result = netrisApiClient.createStaticNatRule(cmd);
            if (!result) {
                return new NetrisAnswer(cmd, false, String.format("Failed to create SNAT rule on Netris for network %s", cmd.getName()));
            }
        }

        return new NetrisAnswer(cmd, true, "OK");
    }

    private Answer executeRequest(DeleteNetrisNatRuleCommand cmd) {
        boolean result = netrisApiClient.deleteNatRule(cmd);
        if (!result) {
            return new NetrisAnswer(cmd, false, String.format("Netris NAT rule: %s deletion failed", cmd.getNatRuleName()));
        }
        return new NetrisAnswer(cmd, true, "OK");
    }

    private Answer executeRequest(CreateOrUpdateNetrisACLCommand cmd) {
        boolean result = netrisApiClient.addOrUpdateAclRule(cmd, false);
        if (!result) {
            return new NetrisAnswer(cmd, false, String.format("Creation of Netris ACL rule: %s failed", cmd.getNetrisAclName()));
        }
        return new NetrisAnswer(cmd, true, "OK");
    }

    private Answer executeRequest(DeleteNetrisACLCommand cmd) {
        boolean result = netrisApiClient.deleteAclRule(cmd, false);
        if (!result) {
            return new NetrisAnswer(cmd, false, String.format("Failed to delete Netris ACLs: %s", String.join("'", cmd.getAclRuleNames())));
        }
        return new NetrisAnswer(cmd, true, "OK");
    }

    private Answer executeRequest(ListNetrisStaticRoutesCommand cmd) {
        List<StaticRoute> staticRoutes = netrisApiClient.listStaticRoutes(cmd);
        return new ListNetrisStaticRoutesAnswer(cmd, staticRoutes);
    }

    private Answer executeRequest(AddOrUpdateNetrisStaticRouteCommand cmd) {
        boolean result = netrisApiClient.addOrUpdateStaticRoute(cmd);
        if (!result) {
            return new NetrisAnswer(cmd, false, String.format("Failed to add static route for VPC: %s, prefix: %s, nextHop: %s ", cmd.getName(), cmd.getPrefix(), cmd.getNextHop()));
        }
        return new NetrisAnswer(cmd, true, "OK");
    }

    private Answer executeRequest(DeleteNetrisStaticRouteCommand cmd) {
        boolean result = netrisApiClient.deleteStaticRoute(cmd);
        if (!result) {
            return new NetrisAnswer(cmd, false, String.format("Failed to add static route for VPC: %s, prefix: %s, nextHop: %s ", cmd.getName(), cmd.getPrefix(), cmd.getNextHop()));
        }
        return new NetrisAnswer(cmd, true, "OK");
    }

    private Answer executeRequest(ReleaseNatIpCommand cmd) {
        boolean result = netrisApiClient.releaseNatIp(cmd);
        if (!result) {
            return new NetrisAnswer(cmd, false, String.format("Failed to release NAT IP: %s", cmd.getNatIp()));
        }
        return new NetrisAnswer(cmd, true, "OK");
    }

    private Answer executeRequest(CreateOrUpdateNetrisLoadBalancerRuleCommand cmd) {
        boolean result = netrisApiClient.createOrUpdateLbRule(cmd);
        if (!result) {
            return new NetrisAnswer(cmd, false, String.format("Failed to create Netris LB rule for %s: %s, " +
                    "for private port: %s and public port: %s", getNetworkType(cmd.isVpc()), cmd.getName(), cmd.getPrivatePort(), cmd.getPublicPort()));
        }
        return new NetrisAnswer(cmd, true, "OK");
    }

    private Answer executeRequest(DeleteNetrisLoadBalancerRuleCommand cmd) {
        boolean result = netrisApiClient.deleteLbRule(cmd);
        if (!result) {
            return new NetrisAnswer(cmd, false, String.format("Failed to delete Netris LB rule for %s: %s", getNetworkType(cmd.isVpc()), cmd.getName()));
        }
        return new NetrisAnswer(cmd, true, "OK");
    }

    private String getNetworkType(Boolean isVpc) {
        if (isVpc) {
            return "VPC";
        }
        return "Network";
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
